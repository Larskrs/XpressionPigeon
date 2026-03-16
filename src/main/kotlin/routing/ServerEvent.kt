package routing

import com.example.routing.RossTalkStatus
import data.Page
import data.Placeholder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ServerEvent {

    // ─── Scene / XmlDataStore ─────────────────────────────────────────────────

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

    // ─── Rundown ──────────────────────────────────────────────────────────────

    /** Response to ListRundowns — lightweight index, no page data. */
    @Serializable
    @SerialName("RundownList")
    data class RundownList(
        val rundowns: List<Map<String, String>>   // [{ id, name }, ...]
    ) : ServerEvent()

    /** Full rundown payload — sent in response to LoadRundown. */
    @Serializable
    @SerialName("RundownData")
    data class RundownData(
        val id: String,
        val name: String,
        val pages: List<Page>,
        val placeholders: List<Placeholder>
    ) : ServerEvent()

    @Serializable
    @SerialName("RundownSaved")
    data class RundownSaved(
        val id: String,
        val name: String,
        val pages: List<Page>
    ) : ServerEvent()

    @Serializable
    @SerialName("RundownRenamed")
    data class RundownRenamed(
        val id: String,
        val name: String
    ) : ServerEvent()

    @Serializable
    @SerialName("RundownDeleted")
    data class RundownDeleted(val id: String) : ServerEvent()

    /** Sent when a requested rundown id does not exist. */
    @Serializable
    @SerialName("RundownNotFound")
    data class RundownNotFound(val id: String) : ServerEvent()

    /** Confirmation that a client-triggered flush completed. */
    @Serializable
    @SerialName("RundownsFlushed")
    object RundownsFlushed : ServerEvent()

    // ─── RossTalk ─────────────────────────────────────────────────────────────

    @Serializable
    @SerialName("RossTalkStatus")
    data class RossTalkStatusChange(val status: RossTalkStatus) : ServerEvent()




    @Serializable
    @SerialName("PlaceholdersUpdated")
    data class PlaceholdersUpdated(
        val rundownId: String,
        val placeholders: List<Placeholder>,
    ) : ServerEvent()
}