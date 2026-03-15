// RossTalkClient.kt
package com.example.routing

import config.AppConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import routing.ServerEvent
import routing.WebSocket
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

// How often the ping health check runs (seconds)
private const val PING_INTERVAL_SEC = 10L

// Timeout for the probe connection attempt (milliseconds)
private const val PING_TIMEOUT_MS = 3000

object RossTalkClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private lateinit var config: AppConfig

    private val intentionalDisconnect = AtomicBoolean(false)
    private val isReconnecting = AtomicBoolean(false)
    private var lastStatus: RossTalkStatus? = null

    // Register a suspend callback here from Socket.kt to receive status changes.
    // e.g. RossTalkClient.onStatusChange = { status -> broadcast(ServerEvent.RossTalkStatusChange(status)) }
    var onStatusChange: (suspend (RossTalkStatus) -> Unit)? = null

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "rosstalk-reconnect").apply { isDaemon = true }
    }
    private var reconnectFuture: ScheduledFuture<*>? = null
    private var pingFuture: ScheduledFuture<*>? = null

    // Coroutine scope tied to the Ktor application for firing suspend callbacks
    private var appScope: kotlinx.coroutines.CoroutineScope? = null

    fun connect(config: AppConfig, scope: kotlinx.coroutines.CoroutineScope) {
        this.config = config
        this.appScope = scope
        intentionalDisconnect.set(false)
        attemptConnect()
    }

    private fun attemptConnect() {
        try {
            println("Connecting to RossTalk at ${config.host}:${config.port}...")
            socket = Socket(config.host, config.port)
            writer = PrintWriter(socket!!.getOutputStream(), true)
            isReconnecting.set(false)
            reconnectFuture?.cancel(false)
            reconnectFuture = null
            println("RossTalk connected!")
            notifyStatus(RossTalkStatus.CONNECTED)
            startPing()
        } catch (e: Exception) {
            println("RossTalk connection failed: ${e.message}")
            notifyStatus(RossTalkStatus.DISCONNECTED)
            if (!intentionalDisconnect.get()) scheduleReconnect()
        }
    }

    /**
     * Fires [onStatusChange] only when the status actually changes,
     * so clients don't get spammed on every ping cycle.
     */
    private fun notifyStatus(status: RossTalkStatus) {
        if (status == lastStatus) return
        lastStatus = status
        appScope?.launch { WebSocket.broadcast(ServerEvent.RossTalkStatusChange(status)) }
    }

    /**
     * Every [PING_INTERVAL_SEC] seconds, opens a fresh probe socket to the host
     * and port and immediately closes it. No data is sent — the TCP handshake
     * completing is sufficient to confirm the host is reachable.
     */
    private fun startPing() {
        pingFuture?.cancel(false)
        pingFuture = scheduler.scheduleWithFixedDelay(
            {
                if (intentionalDisconnect.get()) return@scheduleWithFixedDelay

                println("RossTalk ping ${config.host}:${config.port}...")
                val reachable = try {
                    Socket().use { probe ->
                        probe.connect(InetSocketAddress(config.host, config.port), PING_TIMEOUT_MS)
                        true
                    }
                } catch (e: Exception) {
                    println("RossTalk ping failed: ${e.message}")
                    false
                }

                if (reachable) {
                    println("RossTalk ping OK")
                    notifyStatus(RossTalkStatus.CONNECTED)
                } else {
                    println("RossTalk host unreachable — connection lost")
                    closeSocketQuietly()
                    notifyStatus(RossTalkStatus.DISCONNECTED)
                    scheduleReconnect()
                }
            },
            PING_INTERVAL_SEC, PING_INTERVAL_SEC, TimeUnit.SECONDS
        )
    }

    private fun scheduleReconnect() {
        if (isReconnecting.getAndSet(true)) return

        println("RossTalk lost connection — will retry in 5 seconds...")
        reconnectFuture = scheduler.scheduleWithFixedDelay(
            {
                if (intentionalDisconnect.get()) {
                    isReconnecting.set(false)
                    reconnectFuture?.cancel(false)
                    return@scheduleWithFixedDelay
                }
                println("RossTalk reconnect attempt...")
                attemptConnect()
            },
            5L, 5L, TimeUnit.SECONDS
        )
    }

    fun takeScene(sceneId: String) {
        val scene = config.scenes.find { it.id == sceneId } ?: run {
            println("Scene not found: $sceneId")
            return
        }
        send("SEQI ${scene.takeId}:${scene.layer}")
    }

    fun outScene(sceneId: String) {
        val scene = config.scenes.find { it.id == sceneId } ?: run {
            println("Scene not found: $sceneId")
            return
        }
        send("SEQO ${scene.takeId}")
    }

    private fun send(cmd: String) {
        try {
            val w = writer
            val s = socket
            if (w == null || s == null || s.isClosed || !s.isConnected) {
                println("RossTalk not connected — dropping command: $cmd")
                if (!intentionalDisconnect.get()) scheduleReconnect()
                return
            }

            println("RossTalk → $cmd")
            w.print("$cmd\r\n")
            w.flush()

            if (w.checkError()) {
                println("RossTalk write error detected — socket may be dead")
                closeSocketQuietly()
                if (!intentionalDisconnect.get()) scheduleReconnect()
            }
        } catch (e: Exception) {
            println("RossTalk send error: ${e.message}")
            closeSocketQuietly()
            if (!intentionalDisconnect.get()) scheduleReconnect()
        }
    }

    private fun closeSocketQuietly() {
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        writer = null
        socket = null
    }

    fun disconnect() {
        intentionalDisconnect.set(true)
        pingFuture?.cancel(false)
        pingFuture = null
        reconnectFuture?.cancel(false)
        reconnectFuture = null
        isReconnecting.set(false)
        closeSocketQuietly()
        notifyStatus(RossTalkStatus.DISCONNECTED)
        println("RossTalk disconnected.")
    }
}

@Serializable
enum class RossTalkStatus {
    CONNECTED,
    DISCONNECTED
}