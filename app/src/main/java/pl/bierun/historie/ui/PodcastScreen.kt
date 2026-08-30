package pl.bierun.historie.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pl.bierun.historie.data.PodcastRepository
import pl.bierun.historie.model.PodcastEpisode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(podcastRepository: PodcastRepository) {
    val episodes = podcastRepository.getEpisodes()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Podcast: Bieruńskie Historie") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Button(
                    onClick = { uriHandler.openUri(podcastRepository.playlistUrl) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Otwórz całą playlistę YouTube")
                }
            }
            items(episodes) { episode ->
                EpisodeItem(episode = episode, onClick = { uriHandler.openUri(episode.videoUrl) })
            }
        }
    }
}

@Composable
fun EpisodeItem(episode: PodcastEpisode, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
