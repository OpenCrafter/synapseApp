package com.synapse.social.studioasinc.feature.shared.reels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.synapse.social.studioasinc.core.media.VideoPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoPlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val progress: Float = 0f,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val isMuted: Boolean = false,
    val isEnded: Boolean = false,
    val hasError: Boolean = false
)

/**
 * Playback holder for one reel.
 *
 * Reels loop, start muted/unmuted according to the viewer-level mute preference (passed in by
 * [ReelItem]) and surface buffering and playback failures so the viewer can show them instead of a
 * frozen frame.
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val videoPlayerManager: VideoPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var player: ExoPlayer? = null
    private var videoUrl: String? = null
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    isEnded = playbackState == Player.STATE_ENDED
                )
            }
            if (playbackState == Player.STATE_READY) {
                _uiState.update { it.copy(duration = player?.duration ?: 0L) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Reel playback failed for " + videoUrl, error)
            _uiState.update { it.copy(hasError = true, isBuffering = false, isPlaying = false) }
        }
    }

    /**
     * Attaches [url] to a looping player. [muted] carries the viewer-level mute preference so moving
     * between reels never resets (or doubles up) the audio state.
     */
    fun initializePlayer(url: String, muted: Boolean = false) {
        if (videoUrl == url && player != null) {
            setMuted(muted)
            return
        }

        videoUrl = url
        player = videoPlayerManager.getPlayer(url, playbackKey = url, loop = true).apply {
            addListener(playerListener)
            volume = if (muted) 0f else 1f
        }
        _uiState.update { it.copy(isMuted = muted, hasError = false, isEnded = false) }

        startProgressUpdate()
    }

    fun releasePlayer() {
        videoUrl?.let { url ->
            progressJob?.cancel()
            player?.removeListener(playerListener)
            videoPlayerManager.releasePlayer(url)
            player = null
        }
    }

    fun getPlayerInstance(): ExoPlayer? = player

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun toggleMute() {
        setMuted(!_uiState.value.isMuted)
    }

    fun setMuted(muted: Boolean) {
        player?.volume = if (muted) 0f else 1f
        _uiState.update { it.copy(isMuted = muted) }
    }

    /** Retries the current reel after a playback failure. */
    fun retry() {
        val current = player ?: return
        _uiState.update { it.copy(hasError = false, isBuffering = true) }
        current.prepare()
        current.play()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                player?.let {
                    if (it.isPlaying) {
                        val current = it.currentPosition
                        val duration = it.duration.takeIf { d -> d > 0 } ?: 1L
                        _uiState.update { state ->
                            state.copy(
                                progress = current.toFloat() / duration.toFloat(),
                                currentPosition = current,
                                duration = duration
                            )
                        }
                    }
                }
                delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    private companion object {
        const val TAG = "VideoPlayerViewModel"
    }
}
