// RundownStore.kt
package data

import com.example.config.AppPaths
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Rundown(
    val name: String,
    val rows: List<Map<String, String>>,
)

object RundownStore {
    private val rundowns = mutableMapOf<String, Rundown>() // id → Rundown
    private val rundownsDir: File get() = File(AppPaths.workDir, "rundowns")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json  = Json { prettyPrint = true }

    fun init() {
        rundownsDir.mkdirs()
        loadAll()
        // Periodic flush every 60 seconds
        scope.launch {
            while (isActive) {
                delay(60_000)
                flushAll()
            }
        }
        println("RundownStore: loaded ${rundowns.size} rundown(s) from ${rundownsDir.path}")
    }

    /** Called on application shutdown */
    fun shutdown() {
        flushAll()
        scope.cancel()
        println("RundownStore: flushed on shutdown")
    }

    fun list(): List<Map<String, String>> =
        rundowns.values.map { mapOf("id" to it.name, "name" to it.name) }

    fun get(id: String): Rundown? = rundowns[id]

    fun save(id: String, name: String, rows: List<Map<String, String>>): Rundown {
        // Handle rename: remove old file if id changed
        if (id != name && rundowns.containsKey(id)) {
            rundowns.remove(id)
            fileFor(id).delete()
        }
        val rundown = Rundown(name = name, rows = rows)
        rundowns[name] = rundown
        writeFile(rundown)
        return rundown
    }

    fun delete(id: String): Boolean {
        rundowns.remove(id) ?: return false
        fileFor(id).delete()
        return true
    }

    private fun loadAll() {
        rundownsDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
            try {
                val rundown = json.decodeFromString<Rundown>(file.readText())
                rundowns[rundown.name] = rundown
            } catch (e: Exception) {
                println("RundownStore: failed to load ${file.name} — ${e.message}")
            }
        }
    }

    private fun flushAll() {
        rundowns.values.forEach { writeFile(it) }
    }

    private fun writeFile(rundown: Rundown) {
        try {
            fileFor(rundown.name).writeText(json.encodeToString(rundown))
        } catch (e: Exception) {
            println("RundownStore: failed to write '${rundown.name}' — ${e.message}")
        }
    }

    private fun fileFor(id: String): File {
        val safe = id.replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
        return File(rundownsDir, "$safe.json")
    }
}