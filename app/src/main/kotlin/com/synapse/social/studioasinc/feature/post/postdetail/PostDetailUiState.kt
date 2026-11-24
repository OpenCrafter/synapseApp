package com.synapse.social.studioasinc.feature.post.postdetail

import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.PostDetail
import com.synapse.social.studioasinc.domain.model.UserProfile

data class PostDetailUiState(
    val isLoading: Boolean = false,
    val post: PostDetail? = null,
    val rootComment: CommentWithUser? = null,
    val comments: List<CommentWithUser> = emptyList(),
    val isCommentsLoading: Boolean = false,
    val error: String? = null,
    val replyToComment: CommentWithUser? = null,
    val replyToParticipants: List<UserProfile> = emptyList(),
    val editingComment: CommentWithUser? = null,
    val hasMoreComments: Boolean = false,
    val currentUserId: String? = null,
    val commentActionsLoading: Set<String> = emptySet(),
    val currentUserAvatarUrl: String? = null,
    val currentUserDisplayName: String? = null,
    val refreshTrigger: Int = 0,
    val blockSuccess: Boolean = false,
    val blockError: String? = null,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
    val reportPostSuccess: Boolean = false,
    val reportPostError: String? = null,
    val commentSubmitted: Boolean = false,
    val ancestorComments: List<CommentWithUser> = emptyList(),
    val postSummary: String? = null,
    val commentError: String? = null,
    val commentActionCompleted: Boolean = false,
    val commentActionError: String? = null,
    val isSummarizing: Boolean = false,
    val summaryError: String? = null,
    val voteError: String? = null,
    val lastVoteOptionIndex: Int? = null,
    val revokeError: String? = null,
    val isRevokingVote: Boolean = false,
    val isBookmarking: Boolean = false,
    val isTogglingReaction: Boolean = false
)
