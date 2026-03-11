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





    @Serializable
    @SerialName("ListRundowns")
    object ListRundowns : WebSocketEvent()

    @Serializable
    @SerialName("LoadRundown")
    data class LoadRundown(val id: String) : WebSocketEvent()

    @Serializable
    @SerialName("SaveRundown")
    data class SaveRundown(
        val id: String,                          // current id (empty string = new)
        val name: String,                        // desired name (may differ from id = rename)
        val rows: List<Map<String, String>>,
    ) : WebSocketEvent()

    @Serializable
    @SerialName("DeleteRundown")
    data class DeleteRundown(val id: String) : WebSocketEvent()

    @Serializable
    @SerialName("FlushRundowns")                 // explicit client-triggered save
    object FlushRundowns : WebSocketEvent()
}