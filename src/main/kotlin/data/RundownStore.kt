// RundownStore.kt
package data

import com.example.config.AppPaths
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class Rundown(
    val id: String,
    val name: String,
    val pages: List<Page>,
    val placeholders: List<Placeholder> = emptyList(),
)

@Serializable
data class Placeholder(
    val key: String,
    val value: String,
)

@Serializable
data class Page(
    val id: String,
    val color: String? = null,
    val order: Int,
    val name: String,
    val rows: List<Row>,
)

@Serializable
data class Row(
    val id: String,
    val order: Int = 0,
    val name: String,
    val startTime: Long,
    val duration: Long,
    val notes: String,
    val values: List<Parameter>
)

@Serializable
data class Parameter(
    val name: String,
    val value: String,
)

object RundownStore {
    private val rundowns = ConcurrentHashMap<String, Rundown>()
    private val dirtyIds = ConcurrentHashMap.newKeySet<String>()

    private val rundownsDir: File get() = File(AppPaths.workDir, "rundowns")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json  = Json { prettyPrint = true }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    fun init() {
        rundownsDir.mkdirs()
        loadAll()
        scope.launch {
            while (isActive) {
                delay(60_000)
                flushDirty()
            }
        }
        println("RundownStore: loaded ${rundowns.size} rundown(s) from ${rundownsDir.path}")
    }

    fun shutdown() {
        flushDirty()
        scope.cancel()
        println("RundownStore: flushed on shutdown")
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun list(): List<Map<String, String>> =
        rundowns.values.map { mapOf("id" to it.id, "name" to it.name) }

    fun get(id: String): Rundown? = rundowns[id]

    /**
     * Create or update a rundown.
     * If [id] is blank a new UUID is assigned here — the caller receives the
     * real id back via the returned [Rundown] and must use it from that point on.
     */
    fun save(id: String, name: String, pages: List<Page>, placeholders: List<Placeholder>): Rundown {
        val resolvedId = id.ifBlank { UUID.randomUUID().toString() }
        val rundown    = Rundown(id = resolvedId, name = name, pages = pages, placeholders)
        rundowns[resolvedId] = rundown
        markDirty(resolvedId)
        return rundown
    }

    /**
     * Rename only — does not touch pages.
     * Returns null if [id] does not exist.
     */
    fun rename(id: String, newName: String): Rundown? {
        val existing = rundowns[id] ?: return null
        val updated  = existing.copy(name = newName)
        rundowns[id] = updated
        markDirty(id)
        return updated
    }

    fun delete(id: String): Boolean {
        rundowns.remove(id) ?: return false
        dirtyIds.remove(id)
        fileFor(id).delete()
        return true
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    private fun markDirty(id: String) {
        dirtyIds.add(id)
        writeFile(rundowns[id] ?: return)
        dirtyIds.remove(id)
    }

    private fun loadAll() {
        rundownsDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
            try {
                println("Loading ${file.name}")
                val rundown = json.decodeFromString<Rundown>(file.readText())
                rundowns[rundown.id] = rundown
            } catch (e: Exception) {
                println("RundownStore: failed to load ${file.name} — ${e.message}")
            }
        }
    }

    private fun flushDirty() {
        dirtyIds.toSet().forEach { id ->
            rundowns[id]?.let { writeFile(it) }
            dirtyIds.remove(id)
        }
    }

    fun flushAll() {
        rundowns.values.forEach { writeFile(it) }
        dirtyIds.clear()
    }

    private fun writeFile(rundown: Rundown) {
        try {
            fileFor(rundown.id).writeText(json.encodeToString(rundown))
        } catch (e: Exception) {
            println("RundownStore: failed to write '${rundown.id}' — ${e.message}")
        }
    }

    private fun fileFor(id: String): File {
        val safe = id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return File(rundownsDir, "$safe.json")
    }

    fun getPlaceholders(id: String): List<Placeholder>? =
        rundowns[id]?.placeholders

    fun setPlaceholders(id: String, placeholders: List<Placeholder>): Rundown? {
        val existing = rundowns[id] ?: return null
        val updated  = existing.copy(placeholders = placeholders)
        rundowns[id] = updated
        markDirty(id)
        return updated
    }

    fun setPlaceholder(id: String, key: String, value: String): Rundown? {
        val existing = rundowns[id] ?: return null
        val merged = existing.placeholders
            .filter { it.key != key } + Placeholder(key, value)
        val updated = existing.copy(placeholders = merged)
        rundowns[id] = updated
        markDirty(id)
        return updated
    }

    fun deletePlaceholder(id: String, key: String): Rundown? {
        val existing = rundowns[id] ?: return null
        val updated  = existing.copy(placeholders = existing.placeholders.filter { it.key != key })
        rundowns[id] = updated
        markDirty(id)
        return updated
    }

    /**
     * Replaces all %key% tokens in [text] using the placeholders
     * stored for the given rundown. Unknown tokens are left as-is.
     */
    fun resolvePlaceholders(rundownId: String, text: String): String {
        val placeholders = rundowns[rundownId]?.placeholders ?: return text
        return placeholders.fold(text) { acc, ph ->
            acc.replace("%${ph.key}%", ph.value)
        }
    }
}