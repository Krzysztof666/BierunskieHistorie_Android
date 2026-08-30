package pl.bierun.historie.model

data class PoiItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 0f,
    val audioFileName: String? = null,
    val audioFiles: List<String> = emptyList(),
    val images: List<String> = emptyList()
)