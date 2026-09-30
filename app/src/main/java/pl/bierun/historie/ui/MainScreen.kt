package pl.bierun.historie.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.bierun.historie.R
import pl.bierun.historie.data.*
import pl.bierun.historie.media.AudioPlayerManager
import pl.bierun.historie.model.PoiImage
import pl.bierun.historie.model.PoiItem
import pl.bierun.historie.util.LocationUtils
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    languageManager: AppLanguageManager,
    poiRepository: PoiRepository,
    audioPlayerManager: AudioPlayerManager,
    podcastRepository: PodcastRepository,
    userProgressRepository: UserProgressRepository,
    archiveRepository: ArchiveRepository,
    updateManager: UpdateManager,
    isLocationPermissionGranted: Boolean
) {
    var currentLanguage by remember { mutableStateOf(languageManager.getCurrentLanguage()) }
    val context = LocalContext.current
    
    val localizedContext = remember(currentLanguage) {
        val locale = Locale(currentLanguage)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    CompositionLocalProvider(LocalContext provides localizedContext) {
        val uriHandler = LocalUriHandler.current
        var currentTab by remember { mutableIntStateOf(0) }
        val pois = remember(currentLanguage) { poiRepository.getPoisForCurrentLanguage() }
        val visitedPois by userProgressRepository.visitedPoisFlow.collectAsState(initial = emptySet())
        
        val parkingPois = remember {
            listOf(
                PoiItem("parking_p1", "Parking darmowy P1", "Darmowy parking dla zwiedzających", 50.09282, 19.088958, 0f, ""),
                PoiItem("parking_p2", "Parking darmowy P2", "Darmowy parking dla zwiedzających", 50.09250, 19.08894, 0f, ""),
                PoiItem("parking_p3", "Parking P3 (Jez. Łysina)", "Płatny w miesiącach letnich", 50.089031, 19.073008, 0f, ""),
                PoiItem("parking_p4", "Parking darmowy P4 (Mini Arboretum)", "Darmowy parking dla odwiedząjących", 50.064361, 19.157637, 0f, ""),

            )
        }
        
        val bierunCenter = LatLng(50.09115, 19.08812)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(bierunCenter, 15f)
        }
        val scope = rememberCoroutineScope()

        var selectedPoi by remember { mutableStateOf<PoiItem?>(null) }
        var showLanguageMenu by remember { mutableStateOf(false) }
        var showPoiList by remember { mutableStateOf(false) }
        var fullScreenImage by remember { mutableStateOf<PoiImage?>(null) }
        val sheetState = rememberModalBottomSheetState()

        var developerClickCount by remember { mutableIntStateOf(0) }
        var showPinDialog by remember { mutableStateOf(false) }
        var showMockLocationDialog by remember { mutableStateOf(false) }
        var mockLocation by remember { mutableStateOf<LatLng?>(null) }
        var realLocation by remember { mutableStateOf<LatLng?>(null) }
        
        var updateInfo by remember { mutableStateOf<AppVersionInfo?>(null) }
        var isDownloadingUpdate by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            updateInfo = updateManager.checkForUpdates()
        }

        var isPlaying by remember { mutableStateOf(false) }
        var isBuffering by remember { mutableStateOf(false) }
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var currentTrackIndex by remember { mutableIntStateOf(0) }
        var totalTracks by remember { mutableIntStateOf(0) }
        var activeMediaId by remember { mutableStateOf<String?>(null) }

        DisposableEffect(audioPlayerManager.player) {
            val currentPlayer = audioPlayerManager.player
            if (currentPlayer == null) return@DisposableEffect onDispose {}

            val listener = object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == androidx.media3.common.Player.STATE_BUFFERING
                    if (state == androidx.media3.common.Player.STATE_READY || state == androidx.media3.common.Player.STATE_BUFFERING) {
                        val d = currentPlayer.duration
                        duration = if (d > 0) d else 0L
                    }
                    currentTrackIndex = currentPlayer.currentMediaItemIndex
                    totalTracks = currentPlayer.mediaItemCount
                    activeMediaId = currentPlayer.currentMediaItem?.mediaId
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    currentPosition = 0L
                    duration = 0L
                    currentTrackIndex = currentPlayer.currentMediaItemIndex
                    totalTracks = currentPlayer.mediaItemCount
                    activeMediaId = mediaItem?.mediaId
                }
            }
            currentPlayer.addListener(listener)
            onDispose { currentPlayer.removeListener(listener) }
        }

        LaunchedEffect(isPlaying, audioPlayerManager.player) {
            val currentPlayer = audioPlayerManager.player
            if (currentPlayer == null) return@LaunchedEffect
            
            while (isPlaying) {
                currentPosition = currentPlayer.currentPosition
                val d = currentPlayer.duration
                if (d > 0) duration = d
                delay(500)
            }
        }

        DisposableEffect(isLocationPermissionGranted) {
            if (isLocationPermissionGranted) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(localizedContext)
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { realLocation = LatLng(it.latitude, it.longitude) }
                    }
                }
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, localizedContext.mainLooper)
                onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
            } else onDispose {}
        }

        val currentUserLocation = mockLocation ?: realLocation
        LaunchedEffect(currentUserLocation) {
            currentUserLocation?.let { userLoc ->
                pois.forEach { poi ->
                    val poiLoc = LatLng(poi.latitude, poi.longitude)
                    if (LocationUtils.calculateDistance(userLoc, poiLoc) <= poi.radiusMeters && !visitedPois.contains(poi.id)) {
                        userProgressRepository.markPoiAsVisited(poi.id)
                        selectedPoi = poi
                    }
                }
            }
        }

        val currentlyPlayingPoi = remember(activeMediaId, pois) {
            val poiId = activeMediaId?.substringBeforeLast("_")
            pois.find { it.id == poiId }
        }

        Scaffold(
            topBar = {
                if (currentTab == 0) {
                    TopAppBar(
                        title = { 
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.clickable {
                                        developerClickCount++
                                        if (developerClickCount >= 5) { showPinDialog = false; showPinDialog = true; developerClickCount = 0 }
                                    }
                                )
                                Text(
                                    text = "v1.5 | " + stringResource(R.string.visited_count, visitedPois.intersect(pois.map { it.id }.toSet()).size, pois.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        },
                        actions = {
                            val infoUrl = stringResource(R.string.info_url)
                            val websiteUrl = stringResource(R.string.website_url)
                            
                            IconButton(onClick = { uriHandler.openUri(websiteUrl) }) {
                                Icon(Icons.Default.Public, contentDescription = stringResource(R.string.website_label))
                            }
                            IconButton(onClick = { uriHandler.openUri(infoUrl) }) {
                                Icon(Icons.Default.Info, contentDescription = stringResource(R.string.info_label))
                            }
                            IconButton(onClick = { showPoiList = true }) { Icon(Icons.AutoMirrored.Filled.List, "Lista") }
                        IconButton(onClick = { uriHandler.openUri("https://www.buycoffee.to/chris43150") }) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = "Wesprzyj projekt", tint = Color(0xFFE91E63))
                        }
                        IconButton(onClick = { showLanguageMenu = true }) {
                            val flagEmoji = when (currentLanguage) {
                                "pl" -> "🇵🇱"
                                "en" -> "🇬🇧"
                                "de" -> "🇩🇪"
                                else -> "🌍"
                            }
                            Text(text = flagEmoji, fontSize = 24.sp)
                        }
                            DropdownMenu(expanded = showLanguageMenu, onDismissRequest = { showLanguageMenu = false }) {
                                listOf("pl" to "Polski", "en" to "English", "de" to "Deutsch").forEach { (code, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        languageManager.setUserSelectedLanguage(code)
                                        currentLanguage = code
                                        showLanguageMenu = false
                                    })
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Map, "Mapa") },
                        label = { Text("Mapa") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.History, stringResource(R.string.archive_title)) },
                        label = { Text(stringResource(R.string.archive_title)) }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { uriHandler.openUri(podcastRepository.playlistUrl) },
                        icon = { Icon(Icons.Default.Podcasts, "Podcast") },
                        label = { Text("Podcast") }
                    )
                    val privacyUrl = stringResource(R.string.privacy_policy_url)
                    NavigationBarItem(
                        selected = false,
                        onClick = { uriHandler.openUri(privacyUrl) },
                        icon = { Icon(Icons.Default.PrivacyTip, stringResource(R.string.privacy_policy)) },
                        label = { Text(stringResource(R.string.privacy_policy)) }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            0 -> MapContent(cameraPositionState, isLocationPermissionGranted, pois, visitedPois, parkingPois, mockLocation, { selectedPoi = it }, { uriHandler.openUri(it) })
                            1 -> ArchiveScreen(archiveRepository, poiRepository, { fullScreenImage = it })
                        }
                    }
                    
                    // Mini Player Bar
                    if (selectedPoi == null && currentlyPlayingPoi != null && (isPlaying || audioPlayerManager.player?.playbackState != androidx.media3.common.Player.STATE_IDLE)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPoi = currentlyPlayingPoi },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentlyPlayingPoi.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1
                                    )
                                    if (isBuffering) {
                                        Text(stringResource(R.string.audio_loading), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                IconButton(onClick = { audioPlayerManager.togglePlayPause() }) {
                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                                }
                                IconButton(onClick = { 
                                    audioPlayerManager.stop() 
                                }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        }
                    }
                }

                if (showPinDialog) {
                    DeveloperPinDialog(onPinCorrect = { showPinDialog = false; showMockLocationDialog = true }, onDismiss = { showPinDialog = false })
                }

                if (showMockLocationDialog) {
                    MockLocationDialog(onLocationSet = { location ->
                        mockLocation = location
                        showMockLocationDialog = false
                        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(location, 17f)) }
                    }, onDismiss = { showMockLocationDialog = false })
                }
                
                updateInfo?.let { info ->
                    AlertDialog(
                        onDismissRequest = { updateInfo = null },
                        title = { Text("Dostępna aktualizacja") },
                        text = { 
                            Column {
                                Text("Nowa wersja: ${info.versionName}")
                                Spacer(Modifier.height(8.dp))
                                Text(info.releaseNotes)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { 
                                    updateManager.downloadAndInstall(info.downloadUrl) {
                                        isDownloadingUpdate = true
                                    }
                                    updateInfo = null
                                }
                            ) { Text(if (isDownloadingUpdate) "Pobieranie..." else "Pobierz i zainstaluj") }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateInfo = null }) { Text("Później") }
                        }
                    )
                }

                selectedPoi?.let { poi ->
                    PoiDetailCard(
                        poi = poi,
                        poiRepository = poiRepository,
                        audioPlayerManager = audioPlayerManager,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentPosition = currentPosition,
                        duration = duration,
                        currentTrackIndex = currentTrackIndex,
                        totalTracks = totalTracks,
                        activeMediaId = activeMediaId,
                        currentLanguage = currentLanguage,
                        onClose = { 
                            selectedPoi = null
                            audioPlayerManager.stop()
                        },
                        onImageClick = { fullScreenImage = it },
                        onNavigate = { uriHandler.openUri(it) }
                    )
                }

                fullScreenImage?.let { poiImage ->
                    val uri = poiRepository.getImageUri(poiImage.fileName)
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f))) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                ZoomableImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize())
                            }
                            
                            poiImage.description?.let { desc ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = desc,
                                        color = Color.White,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = { fullScreenImage = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        ) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
                    }
                }

                if (showPoiList) {
                    ModalBottomSheet(onDismissRequest = { showPoiList = false }, sheetState = sheetState) {
                        PoiListContent(parkingPois + pois) { poi ->
                            showPoiList = false
                            if (poi.id.startsWith("parking")) {
                                uriHandler.openUri("google.navigation:q=${poi.latitude},${poi.longitude}&mode=d")
                            } else {
                                selectedPoi = poi
                                scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(poi.latitude, poi.longitude), 17f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapContent(
    cameraPositionState: CameraPositionState,
    isLocationPermissionGranted: Boolean,
    pois: List<PoiItem>,
    visitedPois: Set<String>,
    parkingPois: List<PoiItem>,
    mockLocation: LatLng?,
    onPoiSelected: (PoiItem) -> Unit,
    onNavigate: (String) -> Unit
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = isLocationPermissionGranted),
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        pois.forEach { poi ->
            val isVisited = visitedPois.contains(poi.id)
            Marker(
                state = MarkerState(position = LatLng(poi.latitude, poi.longitude)),
                title = poi.title,
                snippet = if (isVisited) "Odwiedzono! \uD83C\uDF8A" else poi.description,
                icon = BitmapDescriptorFactory.defaultMarker(if (isVisited) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED),
                onClick = { onPoiSelected(poi); false }
            )
        }
        parkingPois.forEach { poi ->
            Marker(
                state = MarkerState(position = LatLng(poi.latitude, poi.longitude)),
                title = poi.title,
                snippet = "Kliknij, aby nawigować",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                onClick = { onNavigate("google.navigation:q=${poi.latitude},${poi.longitude}&mode=d"); true }
            )
        }
        mockLocation?.let {
            Marker(state = MarkerState(position = it), title = "Symulacja", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        }
    }
}

@Composable
fun PoiListContent(pois: List<PoiItem>, onPoiSelected: (PoiItem) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        item { Text(stringResource(R.string.poi_list_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp)) }
        items(pois) { poi ->
            ListItem(
                headlineContent = { Text(poi.title ?: "Bez nazwy") },
                supportingContent = { Text(text = if (poi.id.startsWith("parking")) stringResource(R.string.parking_navigation_hint) else (poi.description ?: ""), maxLines = 1) },
                leadingContent = { Icon(imageVector = if (poi.id.startsWith("parking")) Icons.Default.LocalParking else Icons.Default.Place, contentDescription = null, tint = if (poi.id.startsWith("parking")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary) },
                modifier = Modifier.clickable { onPoiSelected(poi) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun PoiDetailCard(
    poi: PoiItem,
    poiRepository: PoiRepository,
    audioPlayerManager: AudioPlayerManager,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    currentTrackIndex: Int,
    totalTracks: Int,
    activeMediaId: String?,
    currentLanguage: String,
    onClose: () -> Unit,
    onImageClick: (PoiImage) -> Unit,
    onNavigate: (String) -> Unit
) {
    val allImages = remember(poi) {
        val list = mutableListOf<PoiImage>()
        if (poi.gallery.isNotEmpty()) {
            list.addAll(poi.gallery)
        } else {
            poi.images.forEach { list.add(PoiImage(fileName = it)) }
        }
        list
    }

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), elevation = CardDefaults.cardElevation(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = poi.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
            if (allImages.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(contentPadding = PaddingValues(end = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allImages) { img ->
                        val uri = poiRepository.getImageUri(img.fileName)
                        var isLoadingImage by remember { mutableStateOf(true) }
                        
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            }
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onImageClick(img) },
                                contentScale = ContentScale.Crop,
                                onSuccess = { isLoadingImage = false },
                                onError = { isLoadingImage = false }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(poi.description)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onNavigate("google.navigation:q=${poi.latitude},${poi.longitude}&mode=w") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Navigation, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.navigate_walking))
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth()) {
                if (totalTracks > 1) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { audioPlayerManager.player?.seekToPreviousMediaItem() }) { Icon(Icons.Default.SkipPrevious, "Poprzedni") }
                        Text(text = stringResource(R.string.audio_recording_count, currentTrackIndex + 1, totalTracks), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { audioPlayerManager.player?.seekToNextMediaItem() }) { Icon(Icons.Default.SkipNext, "Następny") }
                    }
                }
                
                if (isBuffering) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp).padding(vertical = 0.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Spacer(Modifier.height(2.dp))
                }

                Slider(
                    value = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f, 
                    onValueChange = { audioPlayerManager.player?.seekTo((it * duration).toLong()) },
                    enabled = audioPlayerManager.player != null
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(currentPosition), style = MaterialTheme.typography.bodySmall)
                    Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            val isSamePoiAndLang = activeMediaId == "${poi.id}_$currentLanguage"
                            if (!isSamePoiAndLang) {
                                audioPlayerManager.playPoiAudio(poi)
                            } else {
                                audioPlayerManager.togglePlayPause()
                            }
                        }, 
                        shape = RoundedCornerShape(50), 
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBuffering || isPlaying // Allow pause even if buffering, but disable play while buffering initial load
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isBuffering) stringResource(R.string.audio_loading)
                            else if (isPlaying) stringResource(R.string.audio_pause) 
                            else stringResource(R.string.audio_listen)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
