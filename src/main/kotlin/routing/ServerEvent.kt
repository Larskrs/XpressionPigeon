package routing

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
}