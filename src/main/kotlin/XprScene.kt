import kotlinx.serialization.Serializable

@Serializable
data class XprScene (
    val id: String,
    val takeId: Int,
    val layer: Int,
    val values: List<String>,
)