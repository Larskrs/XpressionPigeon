package com.example

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun Application.module() {
    install(ContentNegotiation) {
      json(Json {
          ignoreUnknownKeys = true
          prettyPrint = true
      })
    }
    routing {
        post {

        }
    }

}

fun startServer(port: Int) {
    println("Starting server...")
    embeddedServer(Netty, port = port) {
        module()
    }.start(wait = true)
}