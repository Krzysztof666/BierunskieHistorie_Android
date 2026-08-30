package pl.bierun.historie.model

data class PodcastEpisode(
    val id: String,
    val title: String,
    val date: String,
    val thumbnailUrl: String,
    val videoUrl: String
)