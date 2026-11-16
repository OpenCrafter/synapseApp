package com.synapse.social.studioasinc.feature.shared.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Inline video surface used by post cards (feed, post detail, profile).
 *
 * Behaviour mirrors the Facebook-style expectations of the feed: the video plays inside its own
 * card, taps on the surface toggle playback without triggering the card's navigation, autoplay
 * happens only for the focused card and always starts muted, and loading/buffering/error states are
 * shown in place. Playback is paused whenever the surface is not focused or the app is backgrounded,
 * and the player is returned to the shared pool when the card leaves the composition.
 */
@Composable
fun InlineVideoPlayer(
    videoUrl: String,
    playbackKey: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    viewModel: InlineVideoViewModel = hiltViewModel(key = "inline-video-" + playbackKey)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val autoplayEnabled by viewModel.autoplayEnabled.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isResumed = false
                Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> isResumed = true
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(videoUrl, playbackKey) {
        player = viewModel.prepare(videoUrl, playbackKey)
    }

    // Only the video the user is actually looking at plays; every other post is paused, so scrolling
    // can never leave two videos running (or two audios overlapping) at once.
    LaunchedEffect(isActive, isResumed, autoplayEnabled) {
        if (isActive && isResumed && autoplayEnabled) {
            viewModel.playFromAutoplay()
        } else if (!isActive || !isResumed) {
            viewModel.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.release() }
    }

    val aspectRatio = if (uiState.aspectRatio > 0f) {
        uiState.aspectRatio.coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
    } else {
        DEFAULT_ASPECT_RATIO
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space8)
            .clip(RoundedCornerShape(Sizes.CornerMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view -> view.player = player },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        )

        // Tap target over the player: toggling playback must not bubble up to the card's own click.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { viewModel.togglePlayPause() }
        )

        if (uiState.hasError) {
            VideoErrorOverlay(
                onRetry = { viewModel.retry() },
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (!uiState.isPlaying) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = stringResource(R.string.cd_play_video),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(Sizes.IconLarge),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        } else if (uiState.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(Sizes.IconMedium),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = Sizes.BorderDefault
            )
        }

        // Explicit, labelled controls keep playback reachable for keyboard and screen-reader users
        // even though the whole surface is also tappable.
        if (uiState.isReady && !uiState.hasError) {
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.space4)
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (uiState.isPlaying) R.string.cd_pause_video else R.string.cd_play_video
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = { viewModel.toggleMute() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.space4)
            ) {
                Icon(
                    imageVector = if (uiState.isMuted) {
                        Icons.AutoMirrored.Filled.VolumeOff
                    } else {
                        Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = stringResource(
                        if (uiState.isMuted) R.string.cd_unmute_video else R.string.cd_mute_video
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (uiState.isReady && !uiState.hasError && uiState.duration > 0L) {
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(Sizes.BorderDefault),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = PROGRESS_TRACK_ALPHA)
            )
        }
    }
}

@Composable
private fun VideoErrorOverlay(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Sizes.CornerMedium))
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = ERROR_SCRIM_ALPHA))
            .clickable(onClick = onRetry)
            .padding(Spacing.space16),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(Spacing.space4))
        Text(
            text = stringResource(R.string.video_error),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = stringResource(R.string.video_error_retry),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/** Fallback box shape until the codec reports the real media size. */
private const val DEFAULT_ASPECT_RATIO = 16f / 9f

/** Taller/shorter media is letterboxed inside a bounded box so surrounding feed UI stays stable. */
private const val MIN_ASPECT_RATIO = 0.8f
private const val MAX_ASPECT_RATIO = 1.78f
private const val PROGRESS_TRACK_ALPHA = 0.3f
private const val ERROR_SCRIM_ALPHA = 0.6f
