package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.ScheduledPost
import com.synapse.social.studioasinc.domain.repository.ScheduledPostRepository
import com.synapse.social.studioasinc.feature.createpost.scheduling.ScheduledPostPublisherWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduledPostsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val posts: List<ScheduledPost> = emptyList(),
    val isOffline: Boolean = false,
    val inFlightPostIds: Set<String> = emptySet()
)

@HiltViewModel
class ScheduledPostsViewModel @Inject constructor(
    application: Application,
    private val scheduledPostRepository: ScheduledPostRepository
) : AndroidViewModel(application) {

    private val authService = com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService.getInstance(application)

    private val _uiState = MutableStateFlow(ScheduledPostsUiState())
    val uiState: StateFlow<ScheduledPostsUiState> = _uiState.asStateFlow()

    init {
        loadScheduledPosts()
    }

    fun loadScheduledPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            scheduledPostRepository.getScheduledPosts(userId)
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            posts = list.sortedBy { post -> post.scheduledAt },
                            error = null,
                            isOffline = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load scheduled posts",
                            isOffline = error is java.io.IOException
                        )
                    }
                }
        }
    }

    fun cancelScheduledPost(id: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(posts = state.posts.filter { it.id != id })
            }
            val result = scheduledPostRepository.cancelScheduledPost(id)
            if (result.isSuccess) {
                onResult(true)
            } else {
                loadScheduledPosts()
                onResult(false)
            }
        }
    }

    fun retryScheduledPost(id: String) {
        if (_uiState.value.inFlightPostIds.contains(id)) return
        viewModelScope.launch {
            _uiState.update { it.copy(inFlightPostIds = it.inFlightPostIds + id) }
            try {
                val post = _uiState.value.posts.find { it.id == id }
                scheduledPostRepository.retryScheduledPost(id)
                    .onSuccess {
                        post?.let { ScheduledPostPublisherWorker.scheduleAt(getApplication(), it.scheduledAt) }
                        loadScheduledPosts()
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(error = "Retry failed: ${error.message}") }
                    }
            } finally {
                _uiState.update { it.copy(inFlightPostIds = it.inFlightPostIds - id) }
            }
        }
    }

    fun reschedulePost(id: String, newScheduledAt: String) {
        if (_uiState.value.inFlightPostIds.contains(id)) return
        viewModelScope.launch {
            _uiState.update { it.copy(inFlightPostIds = it.inFlightPostIds + id) }
            try {
                val post = _uiState.value.posts.find { it.id == id } ?: return@launch
                scheduledPostRepository.updateScheduledPost(id, newScheduledAt, post.postRequest)
                    .onSuccess {
                        ScheduledPostPublisherWorker.scheduleAt(getApplication(), newScheduledAt)
                        loadScheduledPosts()
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(error = "Reschedule failed: ${error.message}") }
                    }
            } finally {
                _uiState.update { it.copy(inFlightPostIds = it.inFlightPostIds - id) }
            }
        }
    }
}
