// RossTalkClient.kt
package com.example.routing

import config.AppConfig
import java.io.PrintWriter
import java.net.Socket

object RossTalkClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private lateinit var config: AppConfig

    fun connect(config: AppConfig) {
        this.config = config
        println("Connecting to RossTalk at ${config.host}:${config.port}...")
        socket = Socket(config.host, config.port)
        writer = PrintWriter(socket!!.getOutputStream(), true)
        println("RossTalk connected!")
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
        println("RossTalk → $cmd")
        writer?.print("$cmd\r\n")
        writer?.flush()
    }

    fun disconnect() {
        writer?.close()
        socket?.close()
    }
}