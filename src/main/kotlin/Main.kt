package com.example

import com.example.config.ConfigManager
import com.example.routing.RossTalkClient
import com.example.routing.XmlDataStore
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

fun main() {
    val config = ConfigManager.loadOrCreate()

    try {
        RossTalkClient.connect(config)
    } catch (e: Exception) {
        println("RossTalk connection failed: ${e.message}")
        // continue anyway so Ktor still starts
    }

    XmlDataStore.init(config)

    startServer(80)
}


//fun main() {
//    val config = ConfigManager.loadOrCreate()
////    RossTalkClient.connect(config)
//    XmlDataStore.init(config)
//
//    val host = config.host
//    val port = config.port
//
//    println("Connecting to XPression Smart GPI on $host:$port...")
//
//    Socket(host, port).use { socket ->
//        println("Connected!")
//        val writer = PrintWriter(socket.getOutputStream(), true)
//        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
//
//        // Now send RossTalk commands
//        sendRossMessage(writer, "SEQI 007:3")
//        sendRossMessage(writer, "SEQI 0007:4")
//        sendRossMessage(writer, "SEQI 0008:2")
//        sendRossMessage(writer, "SEQI 008:1")
//
//
//        // Optionally listen for responses
//        while (true) {
//            val line = reader.readLine() ?: break
//            println("Received: $line")
//        }
//    }
//}

fun sendRossMessage (writer: PrintWriter, cmd: String) {
    println("Sending Message... $cmd")
    writer.println("$cmd\r\n")
    writer.flush()
    println("Message sent!")
}