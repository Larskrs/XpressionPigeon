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

object XmlDataStore {
    private val data = mutableMapOf<String, String>()
    private lateinit var xmlFile: File

    fun init(config: AppConfig) {
        xmlFile = File(AppPaths.workDir, "xpression_data.xml")
        xmlFile.parentFile.mkdirs()
        config.scenes.forEach { scene ->
            scene.values.forEach { key ->
                data["${scene.id}_$key"] = ""
            }
        }
        // If file already exists, load it first so we don't lose existing values
        if (xmlFile.exists()) loadFromFile()
        writeXml()
        debugPrint()
    }

    fun update(sceneId: String, key: String, value: String) {
        val fieldKey = "${sceneId}_$key"
        if (!data.containsKey(fieldKey)) {
            println("XmlDataStore: Unknown field '$fieldKey' — ignoring")
            return
        }
        data[fieldKey] = value
        writeXml()
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

    private fun loadFromFile() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val root = doc.documentElement
        val nodes = root.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                val key = node.nodeName
                val value = node.textContent
                if (data.containsKey(key)) data[key] = value  // only load known keys
            }
        }
        println("XmlDataStore: Loaded existing data from ${xmlFile.path}")
    }

    private fun writeXml() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val root = doc.createElement("XpressionData")
        doc.appendChild(root)

        data.forEach { (key, value) ->
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