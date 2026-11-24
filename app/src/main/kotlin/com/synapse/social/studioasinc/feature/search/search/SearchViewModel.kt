package com.synapse.social.studioasinc.ui.search
import com.synapse.social.studioasinc.data.local.database.SettingsDataStore
import com.synapse.social.studioasinc.feature.shared.viewmodel.BaseViewModel
import android.app.Application
import com.synapse.social.studioasinc.R
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.usecase.profile.FollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.UnfollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileFollowStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.shared.domain.model.SearchAccount
import com.synapse.social.studioasinc.shared.domain.model.SearchHashtag
import com.synapse.social.studioasinc.shared.domain.model.SearchNews
import com.synapse.social.studioasinc.shared.domain.usecase.search.GetSuggestedAccountsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.GetTrendingHashtagsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchHashtagsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchNewsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchPostsUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizePostUseCase
import com.synapse.social.studioasinc.domain.model.ProfileFollowStatus
import com.synapse.social.studioasinc.domain.usecase.post.RepostPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.UnrepostPostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import com.synapse.social.studioasinc.feature.shared.components.post.PostEvent
import com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

enum class SearchTab(val title: String) {
    FOR_YOU("For you"),
    TRENDING("Trending"),
    NEWS("News"),
    SPORTS("Sports"),
    ENTERTAINMENT("Entertainment"),
    TOP("Top"),
    LATEST("Latest"),
    PEOPLE("People"),
    MEDIA("Media"),
    LISTS("Lists")
}

data class SearchUiState(
    val query: String = "",
    val active: Boolean = false,
    val isLoading: Boolean = false,
    val selectedTab: SearchTab = SearchTab.FOR_YOU,
    val error: String? = null,
    val posts: List<com.synapse.social.studioasinc.domain.model.Post> = emptyList(),
    val hashtags: List<SearchHashtag> = emptyList(),
    val news: List<SearchNews> = emptyList(),
    val accounts: List<SearchAccount> = emptyList(),
    val trendingHashtags: List<SearchHashtag> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val lastQuery: String = "",
    val cachedData: Map<SearchTab, Any> = emptyMap(),
    val blockSuccess: Boolean = false,
    val blockError: String? = null,
    val isReportDialogShowing: Boolean = false,
    val reportDialogPost: com.synapse.social.studioasinc.domain.model.Post? = null,
    val reportSuccess: Boolean = false,
    val reportError: String? = null,
    val isReportSubmitting: Boolean = false,
    val isSummarizing: Boolean = false,
    val postSummary: String? = null,
    val summaryError: String? = null,
    val bookmarkBusyPostIds: Set<String> = emptySet(),
    val followingBusyAccountIds: Set<String> = emptySet(),
    // User IDs with a block operation currently in flight. Scoped per user so that a rapid tap on
    // one user never blocks block taps on a different user.
    val blockingBusyUserIds: Set<String> = emptySet(),
    // Post IDs with a delete operation currently in flight. Scoped per post so that a rapid tap
    // never fires duplicate delete requests for the same post.
    val deletingBusyPostIds: Set<String> = emptySet(),
    // Post IDs with a toggle-comments operation currently in flight. Scoped per post so that a
    // rapid tap never fires duplicate toggle-comments requests for the same post.
    val togglingCommentsBusyPostIds: Set<String> = emptySet()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchPostsUseCase: SearchPostsUseCase,
    private val searchHashtagsUseCase: SearchHashtagsUseCase,
    private val searchNewsUseCase: SearchNewsUseCase,
    private val getSuggestedAccountsUseCase: GetSuggestedAccountsUseCase,
    private val getTrendingHashtagsUseCase: GetTrendingHashtagsUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getProfileFollowStatusUseCase: GetProfileFollowStatusUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val authRepository: AuthRepository,
    private val settingsDataStore: com.synapse.social.studioasinc.data.local.database.SettingsDataStore,
    private val reactToPostUseCase: com.synapse.social.studioasinc.domain.usecase.post.ReactToPostUseCase,
    private val bookmarkPostUseCase: com.synapse.social.studioasinc.domain.usecase.post.BookmarkPostUseCase,
    private val votePollUseCase: com.synapse.social.studioasinc.domain.usecase.post.VotePollUseCase,
    private val revokeVoteUseCase: com.synapse.social.studioasinc.domain.usecase.post.RevokeVoteUseCase,
    private val deletePostUseCase: com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase,
    private val togglePostCommentsUseCase: com.synapse.social.studioasinc.shared.domain.usecase.post.TogglePostCommentsUseCase,
    private val reportPostUseCase: com.synapse.social.studioasinc.domain.usecase.post.ReportPostUseCase,
    private val populatePostPollsUseCase: com.synapse.social.studioasinc.domain.usecase.post.PopulatePostPollsUseCase,
    private val populatePostReactionsUseCase: com.synapse.social.studioasinc.domain.usecase.reaction.PopulatePostReactionsUseCase,
    private val summarizePostUseCase: SummarizePostUseCase,
    private val application: Application,
    private val repostPostUseCase: RepostPostUseCase,
    private val unrepostPostUseCase: UnrepostPostUseCase
) : BaseViewModel<SearchUiState>(SearchUiState()) {
    private var searchJob: Job? = null
    private val inFlightRevokeVotePostIds = ConcurrentHashMap<String, Unit>()
    init {
        loadHistory()
        loadTrendingHashtags()
        refreshCurrentTab()
    }

    private fun loadTrendingHashtags() {
        viewModelScope.launch {
            getTrendingHashtagsUseCase().onSuccess { hashtags ->
                updateState { it.copy(trendingHashtags = hashtags) }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            settingsDataStore.searchHistory.collect { history ->
                updateState { it.copy(searchHistory = history) }
            }
        }
    }

    private fun saveHistory(history: List<String>) {
        viewModelScope.launch {
            settingsDataStore.setSearchHistory(history)
        }
    }

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        val currentHistory = currentState.searchHistory.toMutableList()
        currentHistory.remove(query)
        currentHistory.add(0, query)
        if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.lastIndex)
        }
        updateState { it.copy(searchHistory = currentHistory) }
        saveHistory(currentHistory)
    }

    fun onQueryChange(query: String) {
        updateState { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            if (query != currentState.lastQuery) {
                updateState { it.copy(cachedData = emptyMap(), lastQuery = query) }
            }
            performSearch(query)
        }
    }

    fun onTabSelected(tab: SearchTab) {
        updateState { it.copy(selectedTab = tab) }
        performSearch(currentState.query)
    }

    fun onSearch(query: String) {
         updateState { it.copy(query = query) }
         addToHistory(query)
         performSearch(query)
    }

    fun clearSearch() {
        updateState { it.copy(query = "") }
        performSearch("")
    }

    private fun performSearch(query: String) {
        refreshCurrentTab(query)
    }

    fun refreshCurrentTab(query: String = currentState.query) {
        val currentTab = currentState.selectedTab
        val cached = currentState.cachedData[currentTab]
        
        if (cached != null && query == currentState.lastQuery) {
            when (currentTab) {
                SearchTab.PEOPLE -> updateState { it.copy(accounts = cached as List<SearchAccount>, isLoading = false) }
                SearchTab.TRENDING, SearchTab.LISTS -> updateState { it.copy(hashtags = cached as List<SearchHashtag>, isLoading = false) }
                SearchTab.NEWS, SearchTab.SPORTS, SearchTab.ENTERTAINMENT -> updateState { it.copy(news = cached as List<SearchNews>, isLoading = false) }
                SearchTab.FOR_YOU, SearchTab.TOP, SearchTab.LATEST, SearchTab.MEDIA -> updateState { it.copy(posts = cached as List<com.synapse.social.studioasinc.domain.model.Post>, isLoading = false) }
            }
            return
        }
        
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            try {
                when (currentTab) {
                    SearchTab.PEOPLE -> {
                        val result = getSuggestedAccountsUseCase(query)
                        val currentUserId = authRepository.getCurrentUserId()
                        result.onSuccess { data ->
                            val filtered = data.filter { it.id != currentUserId }
                            updateState {
                                it.copy(
                                    accounts = filtered, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to filtered)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.TRENDING, SearchTab.LISTS -> {
                        val result = searchHashtagsUseCase(if (currentTab == SearchTab.TRENDING) "" else query)
                        result.onSuccess { data -> 
                            updateState {
                                it.copy(
                                    hashtags = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.NEWS -> {
                        val result = searchNewsUseCase(query)
                        result.onSuccess { data -> 
                            updateState {
                                it.copy(
                                    news = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.SPORTS -> {
                        val result = searchNewsUseCase("$query sports")
                        result.onSuccess { data -> 
                            updateState {
                                it.copy(
                                    news = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.ENTERTAINMENT -> {
                        val result = searchNewsUseCase("$query entertainment")
                        result.onSuccess { data -> 
                            updateState {
                                it.copy(
                                    news = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.FOR_YOU, SearchTab.TOP, SearchTab.LATEST, SearchTab.MEDIA -> {
                        val finalQuery = query
                        val result = searchPostsUseCase(finalQuery)
                        result.onSuccess { data ->
                            val posts = data.map { searchPost ->
                                com.synapse.social.studioasinc.domain.model.Post(
                                    id = searchPost.id,
                                    authorUid = searchPost.authorId,
                                    postText = searchPost.content,
                                    publishDate = searchPost.createdAt,
                                    timestamp = 0L, // Will be parsed if needed, but 0 is safe for now
                                    likesCount = searchPost.likesCount,
                                    replyCount = searchPost.commentsCount,
                                    resharesCount = searchPost.boostCount,
                                    username = searchPost.authorHandle,
                                    avatarUrl = searchPost.authorAvatar,
                                    mediaItems = searchPost.mediaUrls?.map { url ->
                                        com.synapse.social.studioasinc.domain.model.MediaItem(id = url, url = url, type = com.synapse.social.studioasinc.domain.model.MediaType.IMAGE)
                                    }?.toMutableList() ?: mutableListOf()
                                )
                            }
                            
                            val enrichedPosts = populatePostReactionsUseCase(posts)
                            val fullyEnrichedPosts = populatePostPollsUseCase(enrichedPosts)

                            val filtered = when (currentTab) {
                                SearchTab.FOR_YOU, SearchTab.TOP -> fullyEnrichedPosts.sortedByDescending { it.likesCount + it.replyCount + it.resharesCount }
                                SearchTab.LATEST -> fullyEnrichedPosts.sortedByDescending { it.publishDate }
                                SearchTab.MEDIA -> fullyEnrichedPosts.filter { it.mediaItems?.isNotEmpty() == true }
                                else -> fullyEnrichedPosts
                            }
                            updateState {
                                it.copy(
                                    posts = filtered, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to filtered)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleFollow(accountId: String) {
        if (currentState.followingBusyAccountIds.contains(accountId)) return
        val currentUserId = authRepository.getCurrentUserId() ?: return
        val account = currentState.accounts.find { it.id == accountId } ?: return
        val wasFollowing = account.isFollowing
        val delta = if (wasFollowing) -1 else 1

        // Mark busy synchronously before launching so a rapid second tap on the same account
        // sees followingBusyAccountIds containing accountId and no duplicate concurrent call slips
        // through the dispatch-delay window. Matches the bookmarkBusyPostIds convention.
        updateState { it.copy(followingBusyAccountIds = it.followingBusyAccountIds + accountId) }

        // Optimistic update: change follow state and followers count immediately,
        // and sync cache so tab switches don't revert the button.
        updateState { state ->
            val updatedAccounts = state.accounts.map { acc ->
                if (acc.id == accountId) {
                    acc.copy(
                        isFollowing = !wasFollowing,
                        followersCount = maxOf(0, acc.followersCount + delta)
                    )
                } else acc
            }
            state.copy(
                accounts = updatedAccounts,
                cachedData = state.cachedData + (SearchTab.PEOPLE to updatedAccounts)
            )
        }

        viewModelScope.launch {
            try {
                val result = if (wasFollowing) {
                    unfollowUserUseCase(currentUserId, accountId)
                } else {
                    followUserUseCase(currentUserId, accountId)
                }

                result.fold(
                    onSuccess = {
                        // Verify follow status with server to ensure UI reflects actual backend state
                        getProfileFollowStatusUseCase(accountId).fold(
                            onSuccess = { followStatus ->
                                updateState { state ->
                                    val updatedAccounts = state.accounts.map { acc ->
                                        if (acc.id == accountId) {
                                            acc.copy(
                                                isFollowing = followStatus == ProfileFollowStatus.FOLLOWING
                                            )
                                        } else acc
                                    }
                                    state.copy(
                                        accounts = updatedAccounts,
                                        cachedData = state.cachedData + (SearchTab.PEOPLE to updatedAccounts)
                                    )
                                }
                            },
                            onFailure = { error ->
                                android.util.Log.w("SearchViewModel", "Failed to verify follow status: ${error.message}")
                                // Keep optimistic state on verification failure
                            }
                        )
                    },
                    onFailure = { err ->
                        // Revert optimistic update on failure, including cache.
                        updateState { state ->
                            val revertedAccounts = state.accounts.map { acc ->
                                if (acc.id == accountId) {
                                    acc.copy(
                                        isFollowing = wasFollowing,
                                        followersCount = maxOf(0, acc.followersCount - delta)
                                    )
                                } else acc
                            }
                            state.copy(
                                error = err.message,
                                accounts = revertedAccounts,
                                cachedData = state.cachedData + (SearchTab.PEOPLE to revertedAccounts)
                            )
                        }
                    }
                )
            } finally {
                // Reset busy flag so subsequent taps on the same account are allowed again,
                // even if the coroutine is cancelled or an exception is thrown.
                updateState { state ->
                    state.copy(followingBusyAccountIds = state.followingBusyAccountIds - accountId)
                }
            }
        }
    }

    fun reactToPost(post: com.synapse.social.studioasinc.domain.model.Post, reactionType: com.synapse.social.studioasinc.domain.model.ReactionType) {
        viewModelScope.launch {
            reactToPostUseCase(post, reactionType).collect { result ->
                result.onSuccess { updatedPost ->
                    updateState { state ->
                        state.copy(posts = state.posts.map { if (it.id == updatedPost.id) updatedPost else it })
                    }
                }.onFailure { err -> updateState { it.copy(error = err.message) } }
            }
        }
    }

    fun likePost(post: com.synapse.social.studioasinc.domain.model.Post) {
        reactToPost(post, com.synapse.social.studioasinc.domain.model.ReactionType.LIKE)
    }

    fun bookmarkPost(post: com.synapse.social.studioasinc.domain.model.Post) {
        if (currentState.bookmarkBusyPostIds.contains(post.id)) return
        // Mark busy synchronously before launching so a rapid second tap on the same post
        // sees bookmarkBusyPostIds containing post.id and no duplicate concurrent call slips
        // through the dispatch-delay window.
        updateState { it.copy(bookmarkBusyPostIds = it.bookmarkBusyPostIds + post.id) }
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch
                bookmarkPostUseCase(post.id, userId, post.isBookmarked == true).collect { result ->
                    result.onFailure { err -> updateState { it.copy(error = err.message) } }
                }
            } finally {
                updateState { it.copy(bookmarkBusyPostIds = it.bookmarkBusyPostIds - post.id) }
            }
        }
    }

    fun sharePost(post: com.synapse.social.studioasinc.domain.model.Post) {
    }

    fun votePoll(post: com.synapse.social.studioasinc.domain.model.Post, optionIndex: Int) {
        viewModelScope.launch {
            votePollUseCase(post, optionIndex).collect { result ->
                result.onSuccess { updatedPost ->
                    updateState { state ->
                        state.copy(posts = state.posts.map { if (it.id == updatedPost.id) updatedPost else it })
                    }
                }.onFailure { err -> updateState { it.copy(error = err.message) } }
            }
        }
    }

    suspend fun deletePost(post: com.synapse.social.studioasinc.domain.model.Post): Result<Unit> {
        // Guard against rapid-tap duplicate delete requests: ignore a second tap on the same post
        // while its deletion is in flight. Other posts remain deletable.
        if (currentState.deletingBusyPostIds.contains(post.id)) {
            return Result.failure(IllegalStateException("Delete already in progress for post ${post.id}"))
        }
        // Mark in flight before any suspension point so a concurrent call sees the marker.
        updateState { it.copy(deletingBusyPostIds = it.deletingBusyPostIds + post.id) }
        return deletePostUseCase(post.id).first().also { result ->
            // Always clear the in-flight marker so the user can retry after success or failure.
            updateState { it.copy(deletingBusyPostIds = it.deletingBusyPostIds - post.id) }
            result.onSuccess {
                updateState { state -> state.copy(posts = state.posts.filter { it.id != post.id }) }
            }.onFailure { err -> updateState { it.copy(error = err.message) } }
        }
    }

    suspend fun toggleComments(post: com.synapse.social.studioasinc.domain.model.Post): Result<Unit> {
        // Guard against rapid-tap duplicate toggle-comments requests: ignore a second tap on the
        // same post while its toggle is in flight.
        if (currentState.togglingCommentsBusyPostIds.contains(post.id)) {
            return Result.failure(IllegalStateException("Toggle comments already in progress for post ${post.id}"))
        }
        // Mark in flight before any suspension point.
        updateState { it.copy(togglingCommentsBusyPostIds = it.togglingCommentsBusyPostIds + post.id) }
        return togglePostCommentsUseCase(post.id)
            .first()
            .also { result ->
                // Always clear the in-flight marker so the user can retry after success or failure.
                updateState { it.copy(togglingCommentsBusyPostIds = it.togglingCommentsBusyPostIds - post.id) }
            }
            .onFailure { err -> updateState { it.copy(error = err.message) } }
    }

    suspend fun reportPost(post: com.synapse.social.studioasinc.domain.model.Post): Result<Unit> {
        updateState { it.copy(isReportDialogShowing = true, reportDialogPost = post) }
        return Result.success(Unit)
    }

    fun submitReport(reason: String) {
        // Guard against rapid-tap duplicate submissions: ignore a second tap while a report
        // is already in flight. The flag is set synchronously below before the coroutine
        // launches, preventing a TOCTOU window where two near-simultaneous taps both pass.
        if (currentState.isReportSubmitting) return
        val post = currentState.reportDialogPost ?: return
        // Clear any stale error from a previous failed attempt so a retry
        // does not misleadingly show an old failure while the new request is in flight.
        updateState { it.copy(reportError = null, isReportSubmitting = true) }
        viewModelScope.launch {
            reportPostUseCase(post.id, reason, null).collect { result ->
                result.onSuccess {
                    updateState {
                        it.copy(
                            isReportDialogShowing = false,
                            reportDialogPost = null,
                            reportSuccess = true,
                            isReportSubmitting = false
                        )
                    }
                }.onFailure { err ->
                    // Keep the dialog open on failure so the user can see the error and retry
                    // without re-selecting the reason. Close only on success.
                    updateState {
                        it.copy(
                            isReportDialogShowing = true,
                            reportError = err.message ?: "Failed to report post",
                            isReportSubmitting = false
                        )
                    }
                }
            }
        }
    }

    fun dismissReportDialog() {
        updateState { it.copy(isReportDialogShowing = false, reportDialogPost = null, reportError = null) }
    }

    fun clearReportStatus() {
        updateState { it.copy(reportSuccess = false, reportError = null) }
    }

    suspend fun blockUser(userId: String): Result<Unit> {
        // Guard against rapid-tap duplicate requests: ignore a second block on the same user while
        // the first is in flight. Other users remain tappable.
        if (currentState.blockingBusyUserIds.contains(userId)) {
            return Result.failure(IllegalStateException("Block already in progress for user"))
        }
        // Mark in flight synchronously before the use-case call so a near-simultaneous second call
        // cannot pass the guard before the marker is set (TOCTOU). Mirrors bookmarkPost.
        updateState { it.copy(blockingBusyUserIds = it.blockingBusyUserIds + userId) }
        try {
            updateState { it.copy(blockSuccess = false, blockError = null) }
            
            return blockUserUseCase(userId)
                .onSuccess {
                    updateState { it.copy(blockSuccess = true) }
                    performSearch(currentState.query)
                }
                .onFailure { error ->
                    updateState { it.copy(blockError = error.message ?: "Failed to block user") }
                }
        } finally {
            // Always clear the in-flight marker so the user can retry after success or failure.
            updateState { it.copy(blockingBusyUserIds = it.blockingBusyUserIds - userId) }
        }
    }
    
    fun clearBlockStatus() {
        updateState { it.copy(blockSuccess = false, blockError = null) }
    }

    fun revokeVote(post: com.synapse.social.studioasinc.domain.model.Post) {
        // Guard against rapid-tap duplicate requests: ignore a second tap on the same post while
        // its revoke-vote is in flight. Other posts remain tappable.
        if (inFlightRevokeVotePostIds.putIfAbsent(post.id, Unit) != null) return
        viewModelScope.launch {
            try {
                revokeVoteUseCase(post).collect { result ->
                    result.onSuccess { updatedPost ->
                        updateState { state ->
                            state.copy(posts = state.posts.map { if (it.id == updatedPost.id) updatedPost else it })
                        }
                    }.onFailure { err -> updateState { it.copy(error = err.message) } }
                }
            } finally {
                inFlightRevokeVotePostIds.remove(post.id)
            }
        }
    }

    fun resharePost(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            val isCurrentlyReshared = post.isReshared
            val optimisticPost = post.copy(
                isReshared = !isCurrentlyReshared,
                resharesCount = if (isCurrentlyReshared) maxOf(0, post.resharesCount - 1) else post.resharesCount + 1
            )
            updateState { state ->
                state.copy(posts = state.posts.map { if (it.id == post.id) optimisticPost else it })
            }
            PostEventBus.emit(PostEvent.Updated(optimisticPost))

            val result = if (isCurrentlyReshared) {
                unrepostPostUseCase(post.id)
            } else {
                repostPostUseCase(post.id)
            }

            result.onSuccess {
                performSearch(currentState.query)
            }.onFailure {
                updateState { state ->
                    state.copy(posts = state.posts.map { if (it.id == post.id) post else it })
                }
                PostEventBus.emit(PostEvent.Updated(post))
                updateState {
                    it.copy(error = it.error ?: application.getString(R.string.error_reshare_failed))
                }
            }
        }
    }

    fun isPostOwner(post: com.synapse.social.studioasinc.domain.model.Post): Boolean {
        return authRepository.getCurrentUserId() == post.authorUid
    }

    fun areCommentsDisabled(post: com.synapse.social.studioasinc.domain.model.Post): Boolean {
        return post.postDisableReplies == "true"
    }

    fun summarizePost(post: com.synapse.social.studioasinc.domain.model.Post) {
        val content = post.postText.orEmpty().trim()
        if (content.isBlank()) {
            updateState { it.copy(summaryError = "No text content to summarize.") }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isSummarizing = true, postSummary = null, summaryError = null) }
            summarizePostUseCase(content, emptyList())
                .onSuccess { summary -> updateState { it.copy(isSummarizing = false, postSummary = summary) } }
                .onFailure { e -> updateState { it.copy(isSummarizing = false, summaryError = e.message ?: "Failed to summarize post.") } }
        }
    }

    fun clearPostSummary() {
        updateState { it.copy(postSummary = null, summaryError = null) }
    }
}
