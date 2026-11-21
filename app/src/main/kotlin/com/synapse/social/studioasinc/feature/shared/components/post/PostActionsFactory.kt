package com.synapse.social.studioasinc.feature.shared.components.post

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.feature.home.home.FeedViewModel
import com.synapse.social.studioasinc.feature.profile.profile.ProfileViewModel
import com.synapse.social.studioasinc.feature.post.postdetail.PostDetailViewModel

object PostActionsFactory {

    fun create(
        viewModel: FeedViewModel,
        onComment: (Post) -> Unit,
        onShare: (Post) -> Unit,
        onQuote: (Post) -> Unit,
        onUserClick: (String) -> Unit,
        onOptionClick: (Post) -> Unit,
        onMediaClick: (Post, Int) -> Unit,
        onLike: (Post) -> Unit = viewModel::likePost,
        onRepost: (Post) -> Unit = viewModel::resharePost
    ): PostActions {
        return PostActions(
            onLike = onLike,
            onComment = onComment,
            onShare = onShare,
            onRepost = onRepost,
            onQuote = onQuote,
            onBookmark = viewModel::bookmarkPost,
            onOptionClick = onOptionClick,
            onPollVote = viewModel::votePoll,
            onUserClick = onUserClick,
            onMediaClick = onMediaClick,
            onReactionSelected = viewModel::reactToPost
        )
    }

    fun create(
        viewModel: ProfileViewModel,
        onComment: (Post) -> Unit,
        onShare: (Post) -> Unit,
        onQuote: (Post) -> Unit,
        onUserClick: (String) -> Unit,
        onOptionClick: (Post) -> Unit,
        onMediaClick: (Post, Int) -> Unit
    ): PostActions {
        return PostActions(
            onLike = { post -> viewModel.reactToPost(post, ReactionType.LIKE) },
            onComment = onComment,
            onShare = onShare,
            onRepost = { post -> viewModel.resharePost(post) },
            onQuote = onQuote,
            onBookmark = { post -> viewModel.toggleSave(post) },
            onOptionClick = onOptionClick,
            onPollVote = viewModel::votePoll,
            onUserClick = onUserClick,
            onMediaClick = onMediaClick,
            onReactionSelected = viewModel::reactToPost
        )
    }

    fun create(
        viewModel: PostDetailViewModel,
        onComment: (Post) -> Unit,
        onShare: (Post) -> Unit,
        onQuote: (Post) -> Unit,
        onUserClick: (String) -> Unit,
        onOptionClick: (Post) -> Unit,
        onMediaClick: (Post, Int) -> Unit
    ): PostActions {
        return PostActions(
            onLike = { _ -> viewModel.toggleReaction(ReactionType.LIKE) },
            onComment = onComment,
            onShare = onShare,
            onRepost = { _ -> viewModel.toggleReshare() },
            onQuote = onQuote,
            onBookmark = { _ -> viewModel.toggleBookmark() },
            onOptionClick = onOptionClick,
            onPollVote = { _, index -> viewModel.votePoll(index) },
            onUserClick = onUserClick,
            onMediaClick = onMediaClick,
            onReactionSelected = { _, reaction -> viewModel.toggleReaction(reaction) }
        )
    }
}
