package com.synapse.social.studioasinc.feature.shared.reels

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.media.VideoPlayerManager
import com.synapse.social.studioasinc.shared.data.repository.ReelRepository
import com.synapse.social.studioasinc.shared.domain.model.Reel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

data class ReelsUiState(
    val reels: List<Reel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val isEndReached: Boolean = false,
    val isReportingReel: Boolean = false,
    val isBlockingCreator: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReelsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoPlayerManager: VideoPlayerManager,
    private val reelRepository: ReelRepository
) : ViewModel() {
    private var currentPage = 0
    private val pageSize = 10

    // Reel IDs with a like toggle currently in flight. Scoped per reel so that a rapid tap on one
    // reel never blocks like taps on a different reel.
    private val inFlightLikeReelIds = mutableSetOf<String>()
    // Reel IDs with an oppose toggle currently in flight. Scoped per reel.
    private val inFlightOpposeReelIds = mutableSetOf<String>()
    // Reel IDs with a report request currently in flight. Scoped per reel so a rapid tap on one
    // reel never blocks report taps on a different reel.
    private val inFlightReportReelIds = mutableSetOf<String>()
    // Creator IDs with a block request currently in flight. Prevents duplicate concurrent block
    // requests when the user taps Block repeatedly while the first is still in flight.
    private val inFlightBlockCreatorIds = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    init {
        loadReels()
    }

    fun preloadReels(urls: List<String>) {
        videoPlayerManager.preload(urls)
    }

    fun pauseAllPlayers() {
        videoPlayerManager.pauseAll()
    }

    fun releaseAllPlayers() {
        videoPlayerManager.releaseAll()
    }

    fun loadReels() {
        viewModelScope.launch {
            currentPage = 0
            _uiState.update { it.copy(isLoading = true, isEndReached = false, error = null) }
            val result = reelRepository.getReels(currentPage, pageSize)
            result.onSuccess { reels ->
                _uiState.update { it.copy(
                    reels = reels,
                    isLoading = false,
                    isEndReached = reels.size < pageSize
                ) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMoreReels() {
        val currentState = _uiState.value
        if (currentState.isLoadMoreLoading || currentState.isEndReached || currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadMoreLoading = true) }
            val nextPage = currentPage + 1
            val result = reelRepository.getReels(nextPage, pageSize)
            result.onSuccess { newReels ->
                if (newReels.isEmpty()) {
                    _uiState.update { it.copy(isEndReached = true, isLoadMoreLoading = false) }
                } else {
                    currentPage = nextPage
                    _uiState.update { state ->
                        val existingIds = state.reels.map { it.id }.toSet()
                        val uniqueNewReels = newReels.filter { it.id !in existingIds }
                        state.copy(
                            reels = state.reels + uniqueNewReels,
                            isLoadMoreLoading = false,
                            isEndReached = newReels.size < pageSize
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoadMoreLoading = false) }
            }
        }
    }

    fun likeReel(reelId: String) {
        val original = _uiState.value.reels.firstOrNull { it.id == reelId } ?: return
        // Guard against rapid-tap duplicate requests: ignore a second tap on the same reel while
        // its like toggle (or an opposing toggle) is in flight. Other reels remain tappable.
        synchronized(inFlightLikeReelIds) {
            if (inFlightLikeReelIds.contains(reelId) || inFlightOpposeReelIds.contains(reelId)) return
            inFlightLikeReelIds.add(reelId)
        }
        // Capture the pre-tap values before the optimistic update so a failed toggle can roll
        // back to the exact original state instead of flipping a possibly-refreshed server value.
        val wasLiked = original.isLikedByCurrentUser
        val wasCount = original.likesCount

        _uiState.update { state ->
            val updatedReels = state.reels.map { reel ->
                if (reel.id == reelId) {
                    val newLiked = !reel.isLikedByCurrentUser
                    val newCount = if (newLiked) reel.likesCount + 1 else max(0, reel.likesCount - 1)
                    reel.copy(isLikedByCurrentUser = newLiked, likesCount = newCount)
                } else reel
            }
            state.copy(reels = updatedReels)
        }

        viewModelScope.launch {
            try {
                 reelRepository.likeReel(reelId).onFailure {

                     _uiState.update { state ->
                        val updatedReels = state.reels.map { reel ->
                            if (reel.id == reelId) {
                                reel.copy(isLikedByCurrentUser = wasLiked, likesCount = wasCount)
                            } else reel
                        }
                        state.copy(reels = updatedReels, error = "Failed to update like")
                     }
                 }
            } finally {
                // Always clear the in-flight marker so the user can retry after success or failure.
                synchronized(inFlightLikeReelIds) {
                    inFlightLikeReelIds.remove(reelId)
                }
            }
        }
    }

    fun opposeReel(reelId: String) {
        val original = _uiState.value.reels.firstOrNull { it.id == reelId } ?: return
        // Guard against rapid-tap duplicate requests: ignore a second tap on the same reel while
        // its oppose toggle (or an opposing toggle) is in flight. Other reels remain tappable.
        synchronized(inFlightOpposeReelIds) {
            if (inFlightOpposeReelIds.contains(reelId) || inFlightLikeReelIds.contains(reelId)) return
            inFlightOpposeReelIds.add(reelId)
        }
        val wasOpposed = original.isOpposedByCurrentUser
        val wasCount = original.opposeCount

        _uiState.update { state ->
            val updatedReels = state.reels.map { reel ->
                if (reel.id == reelId) {
                    val newOpposed = !reel.isOpposedByCurrentUser
                    val newCount = if (newOpposed) reel.opposeCount + 1 else max(0, reel.opposeCount - 1)
                    reel.copy(isOpposedByCurrentUser = newOpposed, opposeCount = newCount)
                } else reel
            }
            state.copy(reels = updatedReels)
        }

        viewModelScope.launch {
            try {
                reelRepository.opposeReel(reelId).onFailure {

                     _uiState.update { state ->
                        val updatedReels = state.reels.map { reel ->
                            if (reel.id == reelId) {
                                reel.copy(isOpposedByCurrentUser = wasOpposed, opposeCount = wasCount)
                            } else reel
                        }
                        state.copy(reels = updatedReels, error = "Failed to update oppose")
                     }
             }
            } finally {
                // Always clear the in-flight marker so the user can retry after success or failure.
                synchronized(inFlightOpposeReelIds) {
                    inFlightOpposeReelIds.remove(reelId)
                }
            }
        }
    }

    suspend fun reportReel(reelId: String, reason: String): Result<Unit> {
        // Guard against rapid-tap duplicate requests: ignore a second report for the same reel
        // while the first is still in flight. Other reels remain reportable.
        synchronized(inFlightReportReelIds) {
            if (inFlightReportReelIds.contains(reelId)) {
                return Result.success(Unit)
            }
            inFlightReportReelIds.add(reelId)
        }
        return try {
            reelRepository.reportReel(reelId, reason)
                .onFailure { e ->
                    _uiState.update { it.copy(error = "Failed to report: ${e.message}") }
                }
        } finally {
            synchronized(inFlightReportReelIds) {
                inFlightReportReelIds.remove(reelId)
            }
        }
    }

    suspend fun blockCreator(creatorId: String): Result<Unit> {
        // Guard against rapid-tap duplicate requests: ignore a second block for the same creator
        // while the first is still in flight.
        synchronized(inFlightBlockCreatorIds) {
            if (inFlightBlockCreatorIds.contains(creatorId)) {
                return Result.success(Unit)
            }
            inFlightBlockCreatorIds.add(creatorId)
        }
        return try {
            reelRepository.blockCreator(creatorId)
                .onSuccess {
                    loadReels()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "Failed to block: ${e.message}") }
                }
        } finally {
            synchronized(inFlightBlockCreatorIds) {
                inFlightBlockCreatorIds.remove(creatorId)
            }
        }
    }

    fun downloadReel(videoUrl: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(videoUrl))
                .setTitle("Downloading Reel")
                .setDescription("Saving video to your device")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "Synapse/reel_${System.currentTimeMillis()}.mp4")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Download failed: ${e.message}") }
        }
    }

    fun shareReel(videoUrl: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out this reel on Synapse: $videoUrl")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
