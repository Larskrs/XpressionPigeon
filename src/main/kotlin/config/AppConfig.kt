package config

import XprScene
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val port: Int = 7788,
    val host: String = "127.0.0.1",
    val scenes: List<XprScene> = listOf(
        XprScene("LeftPersonSuper", 8, 2, listOf("title", "subtitle")),
        XprScene("RightPersonSuper", 7, 3, listOf("title", "subtitle")),
        XprScene("ThemeSuper", 3, 1, listOf("title", "subtitle")),
        XprScene("Location", 6, -2, listOf("title", "subtitle")),
        XprScene("Pillar", 10, 10, listOf("url", "title", "subtitle", "image")),
    ),
)
