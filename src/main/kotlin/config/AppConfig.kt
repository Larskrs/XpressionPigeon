package com.example.config

import XprScene
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val port: Int = 7788,
    val host: String = "172.0.0.1",
    val scenes: List<XprScene> = listOf(
        XprScene("LeftPersonSuper", 8, 2, listOf("title", "subtitle")),
        XprScene("RightPersonSuper", 7, 3, listOf("title", "subtitle")),
        XprScene("ThemeSuper", 3, 3, listOf("title", "subtitle")),
    ),
)
