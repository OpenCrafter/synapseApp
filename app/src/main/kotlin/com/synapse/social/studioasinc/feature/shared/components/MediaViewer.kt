package com.synapse.social.studioasinc.feature.shared.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Full-screen viewer for the images attached to a single post.
 *
 * Callers hand over only the tapped post media in feed order plus the index the user tapped, so
 * the viewer always opens on the tapped image and can never page into another post media.
 * It is rendered as an overlay by the host screen, which keeps that screen (and its scroll
 * position) alive underneath until the viewer is dismissed.
 */
@Composable
fun MediaViewer(
    mediaUrls: List<String>,
    initialPage: Int = 0,
    onDismiss: () -> Unit
) {
    if (mediaUrls.isEmpty()) {
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, mediaUrls.lastIndex),
        pageCount = { mediaUrls.size }
    )
    // Each page tracks its own load result, so one broken image never blanks the rest.
    val pageStatus = remember(mediaUrls) { mutableStateMapOf<Int, MediaPageStatus>() }

    // Back must dismiss the overlay instead of leaving the screen that opened it.
    BackHandler(enabled = true, onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Intentional: full-screen media viewer requires black background
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val status = pageStatus[page] ?: MediaPageStatus.LOADING

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = mediaUrls[page],
                    contentDescription = stringResource(R.string.cd_post_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onLoading = { pageStatus[page] = MediaPageStatus.LOADING },
                    onSuccess = { pageStatus[page] = MediaPageStatus.READY },
                    onError = { pageStatus[page] = MediaPageStatus.FAILED }
                )

                when (status) {
                    MediaPageStatus.LOADING -> CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    MediaPageStatus.FAILED -> UnavailableMediaMessage()

                    MediaPageStatus.READY -> Unit
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.space16)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_close),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        if (mediaUrls.size > 1) {
            Text(
                text = (pagerState.currentPage + 1).toString() + " / " + mediaUrls.size,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.space32)
            )
        }
    }
}

/**
 * Shown in place of an image that failed to load or is no longer available, so the viewer stays
 * usable and the user can keep paging through the other images of the post.
 */
@Composable
private fun UnavailableMediaMessage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(Sizes.IconHuge)
        )
        Text(
            text = stringResource(R.string.failed_to_load_image),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Spacing.space8)
        )
    }
}

private enum class MediaPageStatus { LOADING, READY, FAILED }
