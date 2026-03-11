// Socket.kt
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

val json = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
}

// Track all connected sessions
private val sessions = mutableSetOf<DefaultWebSocketServerSession>()
private val sessionsMutex = Mutex()

suspend fun handleSocket(session: DefaultWebSocketServerSession) {
    sessionsMutex.withLock { sessions.add(session) }

    // On connect, send full current state to the new client
    val initialState = ServerEvent.FullState(XmlDataStore.getAll())
    session.send(json.encodeToString(ServerEvent.serializer(), initialState))

    try {
        for (frame in session.incoming) {
            frame as? Frame.Text ?: continue
            val text = frame.readText()

            when (val event = json.decodeFromString<WebSocketEvent>(text)) {
                is WebSocketEvent.UpdateContent -> {
                    println("Received update ${event.sceneId} ${event.key} ${event.value}")
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



                is WebSocketEvent.ListRundowns -> {
                    val list = ServerEvent.RundownList(RundownStore.list())
                    session.send(json.encodeToString(ServerEvent.serializer(), list))
                }

                is WebSocketEvent.LoadRundown -> {
                    val rundown = RundownStore.get(event.id)
                    if (rundown != null) {
                        val response = ServerEvent.RundownData(rundown.name, rundown.name, rundown.rows)
                        session.send(json.encodeToString(ServerEvent.serializer(), response))
                    }
                }

                is WebSocketEvent.SaveRundown -> {
                    val saved = RundownStore.save(event.id.ifBlank { event.name }, event.name, event.rows)
                    broadcast(ServerEvent.RundownSaved(saved.name, saved.name))
                }

                is WebSocketEvent.DeleteRundown -> {
                    RundownStore.delete(event.id)
                    broadcast(ServerEvent.RundownDeleted(event.id))
                }

                is WebSocketEvent.FlushRundowns -> {
                    //TODO:
                }
            }
        }
    } finally {
        sessionsMutex.withLock { sessions.remove(session) }
        println("WebSocket session closed (${sessions.size} remaining)")
    }
}

private suspend fun broadcast(event: ServerEvent) {
    val encoded = json.encodeToString(ServerEvent.serializer(), event)
    sessionsMutex.withLock {
        sessions.forEach { it.send(encoded) }
    }
}