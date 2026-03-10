package com.example.config

import java.io.File
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object ConfigManager {

    private val yaml = Yaml.default

    val workDir = AppPaths.workDir
    private val configFile = File(workDir, "config.yaml")

    fun loadOrCreate(): AppConfig {
        if (!configFile.exists()) {
            val default = AppConfig()
            save(default)
            println("Created config.yaml")
            return default
        }

        println("Loading config.yml")
        return yaml.decodeFromString(configFile.readText())
    }

    fun save(config: AppConfig) {
        configFile.writeText(yaml.encodeToString(config))
    }
}

object AppPaths {

    val isDev = System.getenv("DEV_MODE") == "true"

    val workDir: File = if (isDev) {
        File(".dev").apply { mkdirs() }
    } else {
        File(System.getProperty("user.dir"))
    }
}
