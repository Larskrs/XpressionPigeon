package routing

import com.example.routing.RossTalkStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ServerEvent {

    @Serializable
    @SerialName("FullState")
    data class FullState(val data: Map<String, String>) : ServerEvent()

    @Serializable
    @SerialName("ContentUpdated")
    data class ContentUpdated(
        val sceneId: String,
        val key: String,
        val value: String
    ) : ServerEvent()

    @Serializable
    @SerialName("SceneTaken")
    data class SceneTaken(val sceneId: String) : ServerEvent()

    @Serializable
    @SerialName("SceneOut")
    data class SceneOut(val sceneId: String) : ServerEvent()






    @Serializable
    @SerialName("RundownList")
    data class RundownList(
        val rundowns: List<Map<String, String>>,  // [{id, name}, ...]
    ) : ServerEvent()

    @Serializable
    @SerialName("RundownData")
    data class RundownData(
        val id: String,
        val name: String,
        val rows: List<Map<String, String>>,
    ) : ServerEvent()

    @Serializable
    @SerialName("RundownSaved")
    data class RundownSaved(val id: String, val name: String) : ServerEvent()

    @Serializable
    @SerialName("RundownDeleted")
    data class RundownDeleted(val id: String) : ServerEvent()

    @Serializable
    @SerialName("RossTalkStatus")
    data class RossTalkStatusChange(val status: RossTalkStatus) : ServerEvent()
}