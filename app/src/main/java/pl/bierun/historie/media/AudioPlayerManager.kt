package pl.bierun.historie.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import pl.bierun.historie.data.PoiRepository
import pl.bierun.historie.model.PoiItem

import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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

    private val dataSourceFactory = CacheDataSource.Factory()
        .setCache(getCache(context))
        .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()

    fun playPoiAudio(poi: PoiItem) {
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
        
        player.stop() 
        player.clearMediaItems()
        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.playbackState == Player.STATE_IDLE) return
        
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0, 0L)
            }
            player.play()
        }
    }

    fun stop() {
        player.stop()
    }

    fun release() { player.release() }
}
