package pl.bierun.historie.media

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import pl.bierun.historie.data.PoiRepository
import pl.bierun.historie.model.PoiItem
import java.io.File

@OptIn(UnstableApi::class)
class AudioPlayerManager(context: Context, private val poiRepository: PoiRepository) {
    
    companion object {
        private var cache: SimpleCache? = null
        
        @OptIn(UnstableApi::class)
        fun getCache(context: Context): SimpleCache {
            if (cache == null) {
                val cacheDir = File(context.cacheDir, "audio_cache")
                val cacheEvictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024L) // 100MB
                val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context)
                cache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)
            }
            return cache!!
        }
    }

    var player by mutableStateOf<Player?>(null)
        private set

    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            player = controllerFuture?.get()
        }, MoreExecutors.directExecutor())
    }

    fun playPoiAudio(poi: PoiItem) {
        val currentPlayer = player ?: return
        
        val files = try {
            if (poi.audioFiles.isNotEmpty()) poi.audioFiles else listOfNotNull(poi.audioFileName)
        } catch (e: Exception) {
            listOfNotNull(poi.audioFileName)
        }
        
        if (files.isEmpty()) return

        val lang = poiRepository.languageManager.getCurrentLanguage()
        
        val mediaItems = files.map { fileName ->
            MediaItem.Builder()
                .setUri(poiRepository.getAudioUri(fileName))
                .setMediaId("${poi.id}_$lang")
                .build()
        }
        
        currentPlayer.stop() 
        currentPlayer.clearMediaItems()
        currentPlayer.setMediaItems(mediaItems)
        currentPlayer.prepare()
        currentPlayer.play()
    }

    fun togglePlayPause() {
        val currentPlayer = player ?: return
        if (currentPlayer.playbackState == Player.STATE_IDLE) return
        
        if (currentPlayer.isPlaying) {
            currentPlayer.pause()
        } else {
            if (currentPlayer.playbackState == Player.STATE_ENDED) {
                currentPlayer.seekTo(0, 0L)
            }
            currentPlayer.play()
        }
    }

    fun stop() {
        player?.stop()
    }

    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
