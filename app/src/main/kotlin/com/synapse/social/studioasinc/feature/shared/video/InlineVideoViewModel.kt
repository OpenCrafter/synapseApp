package com.synapse.social.studioasinc.feature.shared.video

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.synapse.social.studioasinc.core.media.VideoPlayerManager
import com.synapse.social.studioasinc.shared.domain.usecase.settings.ObserveAccessibilitySettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Playback holder for a single inline video post.
 *
 * Feed video posts are images with a poster badge today, which means the only way to watch them
 * is to leave the feed. This holder keeps one ExoPlayer (owned by [VideoPlayerManager]) per post,
 * plays it inline only while the post is the focused feed item, always starts autoplay muted and
 * releases the player as soon as the card leaves the composition.
 */
@HiltViewModel
class InlineVideoViewModel @Inject constructor(
    private val videoPlayerManager: VideoPlayerManager,
    observeAccessibilitySettingsUseCase: ObserveAccessibilitySettingsUseCase
) : ViewModel() {

    /**
     * Feed videos autoplay only while the user keeps autoplay enabled in
     * Settings -> Accessibility. Autoplay always starts muted, so that preference is the only gate
     * needed here; a user can still press play manually when it is off.
     */
    val autoplayEnabled: StateFlow<Boolean> = observeAccessibilitySettingsUseCase.autoplayAnimationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = true
        )

    private val _uiState = MutableStateFlow(InlineVideoUiState())
    val uiState: StateFlow<InlineVideoUiState> = _uiState.asStateFlow()

    private var player: ExoPlayer? = null
    private var playbackKey: String? = null
    private var progressJob: Job? = null

    /**
     * Null until the user touches the mute control, so a deliberate tap-to-play can start with
     * sound while autoplay stays muted.
     */
    private var userMuteOverride: Boolean? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update { state ->
                state.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    isEnded = playbackState == Player.STATE_ENDED
                )
            }
            if (playbackState == Player.STATE_READY) {
                val duration = player?.duration?.coerceAtLeast(0L) ?: 0L
                _uiState.update { it.copy(duration = duration) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            val width = videoSize.width
            val height = videoSize.height
            if (width <= 0 || height <= 0) return
            val ratio = (width * videoSize.pixelWidthHeightRatio) / height
            if (ratio.isFinite() && ratio > 0f) {
                _uiState.update { it.copy(aspectRatio = ratio) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Inline video playback failed for " + playbackKey, error)
            _uiState.update {
                it.copy(hasError = true, isBuffering = false, isPlaying = false, isEnded = false)
            }
        }
    }

    /**
     * Attaches the media for [url] and returns the player so the caller can hand it to a
     * PlayerView. Re-entering composition with the same source keeps the existing player.
     */
    fun prepare(url: String, key: String): ExoPlayer? {
        if (playbackKey == key && player != null && _uiState.value.videoUrl == url) {
            return player
        }
        release()
        val newPlayer = videoPlayerManager.getPlayer(url, playbackKey = key, loop = false)
        newPlayer.addListener(playerListener)
        val muted = userMuteOverride ?: true
        newPlayer.volume = if (muted) 0f else 1f
        player = newPlayer
        playbackKey = key
        _uiState.update {
            it.copy(
                videoUrl = url,
                isReady = true,
                isPlaying = false,
                isBuffering = false,
                isEnded = false,
                hasError = false,
                isMuted = muted,
                aspectRatio = 0f,
                progress = 0f,
                currentPosition = 0L,
                duration = 0L
            )
        }
        startProgressUpdates()
        return newPlayer
    }

    /** Starts muted playback for the video that scrolled into the focused feed slot. */
    fun playFromAutoplay() {
        val current = player ?: return
        if (current.isPlaying) return
        if (userMuteOverride == null) setMuted(true)
        rewindIfFinished(current)
        current.play()
    }

    /** Play/pause for an explicit tap on the video surface. */
    fun togglePlayPause() {
        val current = player ?: return
        if (current.isPlaying) {
            current.pause()
        } else {
            // A tap is an explicit play request: a video the user never muted starts with sound.
            if (userMuteOverride == null) setMuted(false)
            rewindIfFinished(current)
            current.play()
        }
    }

    fun pause() {
        val current = player ?: return
        if (current.isPlaying) current.pause()
    }

    fun toggleMute() {
        val muted = !_uiState.value.isMuted
        setMuted(muted)
        userMuteOverride = muted
    }

    /** Retries a failed media source without releasing the player slot. */
    fun retry() {
        val current = player ?: return
        _uiState.update { it.copy(hasError = false, isBuffering = true) }
        current.prepare()
        current.play()
    }

    /** Detaches and returns the player to the shared pool. Safe to call more than once. */
    fun release() {
        progressJob?.cancel()
        progressJob = null
        val current = player
        if (current != null) {
            current.removeListener(playerListener)
            playbackKey?.let { videoPlayerManager.releasePlayer(it) }
        }
        player = null
        playbackKey = null
        _uiState.update { it.copy(isPlaying = false, isBuffering = false, isReady = false) }
    }

    private fun setMuted(muted: Boolean) {
        player?.volume = if (muted) 0f else 1f
        _uiState.update { it.copy(isMuted = muted) }
    }

    private fun rewindIfFinished(current: ExoPlayer) {
        if (_uiState.value.isEnded || current.playbackState == Player.STATE_ENDED) {
            current.seekTo(0)
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val current = player
                if (current != null) {
                    val duration = current.duration.takeIf { value -> value > 0L } ?: 0L
                    if (duration > 0L) {
                        val position = current.currentPosition
                        _uiState.update {
                            it.copy(
                                currentPosition = position,
                                duration = duration,
                                progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                            )
                        }
                    }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        release()
    }

    private companion object {
        const val TAG = "InlineVideoViewModel"
        const val PROGRESS_INTERVAL_MS = 250L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
