package com.example

import com.example.config.ConfigManager
import com.example.routing.RossTalkClient
import com.example.routing.XmlDataStore
import config.AppConfig
import data.RundownStore
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import routing.WebSocket.handleSocket

fun main() = runBlocking {
    val config = ConfigManager.loadOrCreate()
    XmlDataStore.init(config)
    RundownStore.init()

    launch { repeatTaskEverySecond() }

    startServer(config, 80)
}

suspend fun repeatTaskEverySecond() {
    while (true) {
        println("Task executed at ${System.currentTimeMillis()}")
        delay(1000L)
    }
}

fun Application.module(config: AppConfig) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
    install(WebSockets) {
        Json { ignoreUnknownKeys = true }
    }

    // 'this' is the Application, which is a CoroutineScope — safe to pass directly
    RossTalkClient.connect(config, scope = this)

    routing {
        staticResources("/", "frontend/dist") {
            default("index.html")
        }
        webSocket("/ws") {
            handleSocket(this)
        }
    }
}

fun startServer(config: AppConfig, port: Int) {
    println("Starting server...")
    embeddedServer(Netty, port = port) {
        module(config)
    }.start(wait = true)
}

fun sendRossMessage(writer: java.io.PrintWriter, cmd: String) {
    println("Sending Message... $cmd")
    writer.println("$cmd\r\n")
    writer.flush()
    println("Message sent!")
}