package routing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WebSocketEvent {

    @Serializable
    @SerialName("UpdateContent")
    data class UpdateContent(
        val sceneId: String,
        val key: String,
        val value: String
    ) : WebSocketEvent()

    @Serializable
    @SerialName("TakeScene")
    data class TakeScene(val sceneId: String) : WebSocketEvent()

    @Serializable
    @SerialName("OutScene")
    data class OutScene(val sceneId: String) : WebSocketEvent()

    @Serializable
    @SerialName("UpdateAndTake")
    data class UpdateAndTake(
        val sceneId: String,
        val key: String,
        val value: String
    ) : WebSocketEvent()
}