package com.synapse.social.studioasinc.core.media

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.LinkedList
import javax.inject.Inject
import com.synapse.social.studioasinc.core.config.AppConstants
import javax.inject.Singleton

@Singleton
@OptIn(UnstableApi::class)
class VideoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "VideoPlayerManager"
        private const val CACHE_SIZE_BYTES: Long = 200 * 1024 * 1024
        private const val MAX_PLAYER_POOL_SIZE = 3
        private const val PRELOAD_SIZE_BYTES: Long = 5 * 1024 * 1024
    }

    private val cacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
    private val databaseProvider = StandaloneDatabaseProvider(context)


    private val simpleCache: SimpleCache by lazy {
        SimpleCache(File(context.cacheDir, AppConstants.MEDIA_CACHE_DIR), cacheEvictor, databaseProvider)
    }

    private val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        val upstreamFactory = DefaultDataSource.Factory(context)
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }


    private val playerPool = LinkedList<ExoPlayer>()
    private val activePlayers = mutableMapOf<String, ExoPlayer>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Returns a prepared player for [url].
     *
     * [playbackKey] identifies the surface that owns the player, so the same media can be shown by
     * two different cards (for example a post and its reshare) without both fighting over a single
     * player instance. [loop] is used by the reels viewer; feed videos play once and hold their last
     * frame instead of restarting under the reader.
     */
    fun getPlayer(url: String, playbackKey: String = url, loop: Boolean = false): ExoPlayer {

        activePlayers[playbackKey]?.let { return it }

        val player = if (playerPool.isNotEmpty()) {
            playerPool.removeFirst()
        } else {
            createPlayer()
        }

        activePlayers[playbackKey] = player

        // A pooled player keeps the state of its previous owner, so reset what would otherwise leak
        // into the new surface: a pending play request, a muted volume and the previous repeat mode.
        player.playWhenReady = false
        player.volume = 1f
        player.repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF

        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        val mediaSource = DefaultMediaSourceFactory(cacheDataSourceFactory)
            .createMediaSource(mediaItem)

        player.setMediaSource(mediaSource)
        player.prepare()

        return player
    }

    fun releasePlayer(playbackKey: String) {
        val player = activePlayers.remove(playbackKey)
        if (player != null) {
            player.stop()
            player.clearMediaItems()
            if (playerPool.size < MAX_PLAYER_POOL_SIZE) {
                playerPool.add(player)
            } else {
                player.release()
            }
        }
    }

    fun preload(urls: List<String>) {
        scope.launch {
            urls.forEach { url ->
                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(url))
                    .setPosition(0)
                    .setLength(PRELOAD_SIZE_BYTES)
                    .build()

                val cacheWriter = CacheWriter(
                    cacheDataSourceFactory.createDataSource(),
                    dataSpec,
                    null,
                    null
                )
                try {
                    cacheWriter.cache()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cache video for url: $url", e)
                }
            }
        }
    }

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()
    }

    fun pauseAll() {
        activePlayers.values.forEach { it.pause() }
    }

    /**
     * Releases every player currently in use.
     *
     * The shared [simpleCache] is deliberately kept alive: it is a process-wide singleton created
     * through `lazy`, so releasing it here would leave every later playback with a dead cache. That
     * is reachable now that both the feed and the reels viewer share this manager.
     */
    fun releaseAll() {
        activePlayers.values.forEach { it.release() }
        activePlayers.clear()
        playerPool.forEach { it.release() }
        playerPool.clear()
    }
}
