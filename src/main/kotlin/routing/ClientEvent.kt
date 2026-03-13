package routing

import data.Page
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WebSocketEvent {

    // ─── Scene / XmlDataStore ─────────────────────────────────────────────────

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

    // ─── Rundown ──────────────────────────────────────────────────────────────

    /** Request the lightweight rundown index. */
    @Serializable
    @SerialName("ListRundowns")
    object ListRundowns : WebSocketEvent()

    /** Request the full data for a single rundown. */
    @Serializable
    @SerialName("LoadRundown")
    data class LoadRundown(val id: String) : WebSocketEvent()

    /**
     * Create or update a rundown.
     * - New rundown  → leave [id] blank; the server assigns a UUID.
     * - Update       → pass the existing [id]; [name] may differ (rename).
     */
    @Serializable
    @SerialName("SaveRundown")
    data class SaveRundown(
        val id: String,       // blank = new rundown
        val name: String,
        val pages: List<Page>
    ) : WebSocketEvent()

    /**
     * Rename only — does not touch page data.
     * More efficient than a full SaveRundown when only the name changed.
     */
    @Serializable
    @SerialName("RenameRundown")
    data class RenameRundown(
        val id: String,
        val name: String
    ) : WebSocketEvent()

    @Serializable
    @SerialName("DeleteRundown")
    data class DeleteRundown(val id: String) : WebSocketEvent()

    /** Force an immediate disk flush of all dirty rundowns. */
    @Serializable
    @SerialName("FlushRundowns")
    object FlushRundowns : WebSocketEvent()
}