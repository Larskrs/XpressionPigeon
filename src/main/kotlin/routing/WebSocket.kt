package routing

import com.example.routing.RossTalkClient
import com.example.routing.XmlDataStore
import data.RundownStore
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WebSocket {

    val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }

    private val sessions = mutableSetOf<DefaultWebSocketServerSession>()
    private val sessionsMutex = Mutex()

    suspend fun handleSocket(session: DefaultWebSocketServerSession) {
        sessionsMutex.withLock { sessions.add(session) }

        // On connect, send full current state to the new client
        session.sendEvent(ServerEvent.FullState(XmlDataStore.getAll()))
        session.sendEvent(ServerEvent.RundownList(RundownStore.list()))

        try {
            for (frame in session.incoming) {
                frame as? Frame.Text ?: continue
                val text = frame.readText()

                println("event: " + json.decodeFromString<WebSocketEvent>(text))

                when (val event = json.decodeFromString<WebSocketEvent>(text)) {

                    // ── Scene ──────────────────────────────────────────────────

                    is WebSocketEvent.UpdateContent -> {
                        println("UpdateContent ${event.sceneId} ${event.key}=${event.value}")
                        XmlDataStore.update(event.sceneId, event.key, event.value)
                        broadcast(ServerEvent.ContentUpdated(event.sceneId, event.key, event.value))
                    }

                    is WebSocketEvent.TakeScene -> {
                        RossTalkClient.takeScene(event.sceneId)
                        broadcast(ServerEvent.SceneTaken(event.sceneId))
                    }

                    is WebSocketEvent.OutScene -> {
                        RossTalkClient.outScene(event.sceneId)
                        broadcast(ServerEvent.SceneOut(event.sceneId))
                    }

                    is WebSocketEvent.UpdateAndTake -> {
                        XmlDataStore.update(event.sceneId, event.key, event.value)
                        RossTalkClient.takeScene(event.sceneId)
                        broadcast(ServerEvent.ContentUpdated(event.sceneId, event.key, event.value))
                        broadcast(ServerEvent.SceneTaken(event.sceneId))
                    }

                    // ── Rundown ────────────────────────────────────────────────

                    is WebSocketEvent.ListRundowns -> {
                        session.sendEvent(ServerEvent.RundownList(RundownStore.list()))
                    }

                    is WebSocketEvent.LoadRundown -> {
                        val rundown = RundownStore.get(event.id)
                        rundown?.pages?.forEach { println(it) }
                        if (rundown != null) {
                            XmlDataStore.activeRundownId = rundown.id
                            session.sendEvent(
                                ServerEvent.RundownData(rundown.id, rundown.name, rundown.pages, rundown.placeholders)
                            )
                        } else {
                            session.sendEvent(ServerEvent.RundownNotFound(event.id))
                        }
                    }

                    is WebSocketEvent.SaveRundown -> {
                        val previousId = event.id.ifBlank { null }
                        println("Saving $previousId")
                        val saved = RundownStore.save(event.id, event.name, event.pages, event.placeholders)
                        println(event)
                        broadcast(
                            ServerEvent.RundownSaved(
                                id = saved.id,
                                name = saved.name,
                                pages = event.pages,
                            )
                        )
                    }

                    is WebSocketEvent.RenameRundown -> {
                        val updated = RundownStore.rename(event.id, event.name)
                        if (updated != null) {
                            broadcast(ServerEvent.RundownRenamed(updated.id, updated.name))
                        } else {
                            session.sendEvent(ServerEvent.RundownNotFound(event.id))
                        }
                    }

                    is WebSocketEvent.DeleteRundown -> {
                        RundownStore.delete(event.id)
                        broadcast(ServerEvent.RundownDeleted(event.id))
                    }

                    is WebSocketEvent.FlushRundowns -> {
                        RundownStore.flushAll()
                        session.sendEvent(ServerEvent.RundownsFlushed)
                    }


                    is WebSocketEvent.GetPlaceholders -> {
                        val placeholders = RundownStore.getPlaceholders(event.rundownId)
                        if (placeholders != null) {
                            session.sendEvent(
                                ServerEvent.PlaceholdersUpdated(event.rundownId, placeholders)
                            )
                        } else {
                            session.sendEvent(ServerEvent.RundownNotFound(event.rundownId))
                        }
                    }

                    is WebSocketEvent.SetPlaceholder -> {
                        val updated = RundownStore.setPlaceholder(event.rundownId, event.key, event.value)
                        if (updated != null) {
                            broadcast(
                                ServerEvent.PlaceholdersUpdated(event.rundownId, updated.placeholders)
                            )

                        } else {
                            session.sendEvent(ServerEvent.RundownNotFound(event.rundownId))
                        }
                    }

                    is WebSocketEvent.DeletePlaceholder -> {
                        val updated = RundownStore.deletePlaceholder(event.rundownId, event.key)
                        if (updated != null) {
                            broadcast(
                                ServerEvent.PlaceholdersUpdated(event.rundownId, updated.placeholders)
                            )
                        } else {
                            session.sendEvent(ServerEvent.RundownNotFound(event.rundownId))
                        }
                    }
                }
            }
        } finally {
            sessionsMutex.withLock { sessions.remove(session) }
            println("WebSocket session closed (${sessions.size} remaining)")
        }
    }

    suspend fun broadcast(event: ServerEvent) {
        val encoded = json.encodeToString(ServerEvent.serializer(), event)
        sessionsMutex.withLock {
            sessions.forEach { it.send(encoded) }
        }
    }

    private suspend fun DefaultWebSocketServerSession.sendEvent(event: ServerEvent) {
        send(json.encodeToString(ServerEvent.serializer(), event))
    }
}