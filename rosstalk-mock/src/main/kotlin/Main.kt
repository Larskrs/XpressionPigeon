package com.example.mock

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 7788
    println("RossTalk Mock listening on port $port...")

    ServerSocket(port).use { server ->
        while (true) {
            println("Waiting for connection...")
            val client = server.accept()
            println("Client connected: ${client.inetAddress.hostAddress}")

            try {
                BufferedReader(InputStreamReader(client.getInputStream())).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        println(">> $line")
                    }
                }
            } catch (e: Exception) {
                println("Connection error: ${e.message}")
            } finally {
                println("Client disconnected.")
            }
        }
    }
}