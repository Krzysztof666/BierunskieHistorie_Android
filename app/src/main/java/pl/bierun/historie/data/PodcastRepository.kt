package pl.bierun.historie.data

import pl.bierun.historie.model.PodcastEpisode

class PodcastRepository {
    fun getEpisodes(): List<PodcastEpisode> {
        return listOf(
            PodcastEpisode(
                id = "1",
                title = "Odcinek 1: Początki Bierunia",
                date = "2024-01-15",
                thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg", // Placeholder
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            ),
            PodcastEpisode(
                id = "2",
                title = "Odcinek 2: Tajemnice Bieruńskiego Kopca",
                date = "2024-02-10",
                thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg", // Placeholder
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            ),
            PodcastEpisode(
                id = "3",
                title = "Odcinek 3: Legenda o Utopcu",
                date = "2024-03-05",
                thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg", // Placeholder
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            )
        )
    }

    val playlistUrl = "https://youtube.com/playlist?list=PLnGhDOKUawy9TdBvw3VbFzNmOCPjOVPHf&si=8VL2Nm8v-Js6qyj2"
}
