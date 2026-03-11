// XmlDataStore.kt
package com.example.routing

import config.AppConfig
import com.example.config.AppPaths
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlinx.coroutines.*

object XmlDataStore {
    private val data = mutableMapOf<String, String>()
    private lateinit var xmlFile: File

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var writeJob: Job? = null
    private const val DEBOUNCE_MS = 200L

    fun init(config: AppConfig) {
        xmlFile = File(AppPaths.workDir, "xpression_data.xml")
        xmlFile.parentFile.mkdirs()
        config.scenes.forEach { scene ->
            scene.values.forEach { key ->
                data["${scene.id}_$key"] = ""
            }
        }
        if (xmlFile.exists()) loadFromFile()
        writeXmlNow() // always do an immediate write on init
        debugPrint()
    }

    fun update(sceneId: String, key: String, value: String) {
        val fieldKey = "${sceneId}_$key"
        if (!data.containsKey(fieldKey)) {
            println("XmlDataStore: Unknown field '$fieldKey' — ignoring")
            return
        }
        data[fieldKey] = value
        scheduleWrite()
    }

    fun get(sceneId: String, key: String): String? {
        return data["${sceneId}_$key"]
    }

    fun getScene(sceneId: String): Map<String, String> {
        return data
            .filterKeys { it.startsWith("${sceneId}_") }
            .mapKeys { it.key.removePrefix("${sceneId}_") }
    }

    fun getAll(): Map<String, String> = data.toMap()

    fun debugPrint() {
        println("=== XmlDataStore ===")
        if (data.isEmpty()) {
            println("  (empty)")
        } else {
            data.forEach { (key, value) ->
                val display = if (value.isBlank()) "(empty)" else value
                println("  $key = $display")
            }
        }
        println("====================")
    }

    // Cancel any pending write and schedule a new one after DEBOUNCE_MS
    private fun scheduleWrite() {
        writeJob?.cancel()
        writeJob = scope.launch {
            delay(DEBOUNCE_MS)
            writeXmlNow()
        }
    }

    private fun loadFromFile() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val root = doc.documentElement
        val nodes = root.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                val key = node.nodeName
                val value = node.textContent
                if (data.containsKey(key)) data[key] = value
            }
        }
        println("XmlDataStore: Loaded existing data from ${xmlFile.path}")
    }

    private fun writeXmlNow() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val root = doc.createElement("XpressionData")
        doc.appendChild(root)

        // Snapshot the map to avoid holding any lock during IO
        val snapshot = synchronized(data) { data.toMap() }

        snapshot.forEach { (key, value) ->
            val element = doc.createElement(key)
            element.textContent = value
            root.appendChild(element)
        }

        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }
        transformer.transform(DOMSource(doc), StreamResult(xmlFile))
    }
}