package com.synapse.social.studioasinc.feature.shared.video

/**
 * Immutable state of an inline (in-card) video player.
 *
 * [InlineVideoViewModel] owns every playback decision; the composable only renders this state and
 * forwards user intents, so the feed stays a single source of truth for playback (ROST section 3).
 */
data class InlineVideoUiState(
    val videoUrl: String? = null,
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val isMuted: Boolean = true,
    val hasError: Boolean = false,
    /** Real media aspect ratio once the codec reports it; 0 until then so the card can fall back. */
    val aspectRatio: Float = 0f,
    val progress: Float = 0f,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)
