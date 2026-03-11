package com.example

import com.example.routing.RossTalkClient
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import kotlinx.serialization.json.Json
import routing.handleSocket
import routing.json

fun Application.module() {
    install(ContentNegotiation) {
      json(Json {
          ignoreUnknownKeys = true
          prettyPrint = true
      })
    }
    install(WebSockets) {
        Json { ignoreUnknownKeys = true }
    }
    routing {
        staticResources("/", "frontend/dist") {
            default("index.html")
        }

        webSocket("/ws") {
            handleSocket(this)
        }
    }


    RossTalkClient.takeScene("LeftPersonSuper")
    RossTalkClient.takeScene("RightPersonSuper")

}

fun startServer(port: Int) {
    println("Starting server...")
    embeddedServer(Netty, port = port) {
        module()
    }.start(wait = true)
}