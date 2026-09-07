package pl.bierun.historie.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pl.bierun.historie.R
import pl.bierun.historie.data.ArchiveRepository
import pl.bierun.historie.data.PoiRepository
import pl.bierun.historie.model.ArchiveVideo
import pl.bierun.historie.model.PoiImage

@Composable
fun ArchiveScreen(
    archiveRepository: ArchiveRepository,
    poiRepository: PoiRepository,
    onImageClick: (PoiImage) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.archive_tab_photos),
        stringResource(R.string.archive_tab_artists),
        stringResource(R.string.archive_tab_events)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.archive_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTabIndex) {
                0 -> ArchivePhotosTab(archiveRepository, poiRepository, onImageClick)
                1 -> ArchiveVideoTab(archiveRepository.getArtistsVideos())
                2 -> ArchiveVideoTab(archiveRepository.getEventsVideos())
            }
        }
    }
}

@Composable
fun ArchivePhotosTab(
    archiveRepository: ArchiveRepository,
    poiRepository: PoiRepository,
    onImageClick: (PoiImage) -> Unit
) {
    val images = remember { archiveRepository.getArchiveImages() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gridItems(images) { img ->
            AsyncImage(
                model = poiRepository.getImageUri(img.fileName),
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImageClick(img) },
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ArchiveVideoTab(videos: List<ArchiveVideo>) {
    val uriHandler = LocalUriHandler.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        listItems(videos) { video ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { 
                                val url = if (video.youtubeId.startsWith("http")) video.youtubeId 
                                          else "https://www.youtube.com/watch?v=${video.youtubeId}"
                                uriHandler.openUri(url) 
                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play YouTube", modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
