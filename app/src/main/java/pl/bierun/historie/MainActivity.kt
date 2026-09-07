package pl.bierun.historie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import pl.bierun.historie.data.AppLanguageManager
import pl.bierun.historie.data.PoiRepository
import pl.bierun.historie.media.AudioPlayerManager

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import pl.bierun.historie.data.ArchiveRepository
import pl.bierun.historie.data.PodcastRepository
import pl.bierun.historie.data.UpdateManager
import pl.bierun.historie.data.UserProgressRepository
import pl.bierun.historie.ui.MainScreen
import pl.bierun.historie.ui.SplashScreen
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    private lateinit var languageManager: AppLanguageManager
    private lateinit var poiRepository: PoiRepository
    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var podcastRepository: PodcastRepository
    private lateinit var archiveRepository: ArchiveRepository
    private lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        languageManager = AppLanguageManager(this)
        poiRepository = PoiRepository(this, languageManager)
        audioPlayerManager = AudioPlayerManager(this, poiRepository)
        userProgressRepository = UserProgressRepository(this)
        podcastRepository = PodcastRepository()
        archiveRepository = ArchiveRepository(this, languageManager)
        updateManager = UpdateManager(this)

        setContent {
            var isLoading by remember { mutableStateOf(true) }
            
            LaunchedEffect(Unit) {
                // Initialize language
                languageManager.initLanguageOnAppStart()
                // Simulate/ensure resources are loaded (you can adjust delay)
                delay(1500) 
                isLoading = false
            }

            var locationPermissionGranted by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions: Map<String, Boolean> ->
                locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            }

            LaunchedEffect(isLoading) {
                if (!isLoading && !locationPermissionGranted) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        SplashScreen()
                    } else {
                        MainScreen(
                            languageManager = languageManager,
                            poiRepository = poiRepository,
                            audioPlayerManager = audioPlayerManager,
                            podcastRepository = podcastRepository,
                            userProgressRepository = userProgressRepository,
                            archiveRepository = archiveRepository,
                            updateManager = updateManager,
                            isLocationPermissionGranted = locationPermissionGranted
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}