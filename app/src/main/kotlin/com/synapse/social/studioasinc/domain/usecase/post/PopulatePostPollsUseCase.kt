package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.repository.PollRepository
import javax.inject.Inject

class PopulatePostPollsUseCase @Inject constructor(
    private val pollRepository: PollRepository
) {
    suspend operator fun invoke(posts: List<Post>): List<Post> {
        val ids = posts.flatMap { listOfNotNull(it, it.quotedPost) }
            .filter { it.hasPoll == true }.map { it.id }.distinct()
        if (ids.isEmpty()) return posts
        val votes = pollRepository.getBatchUserVotes(ids)
        val counts = pollRepository.getBatchPollVotes(ids)
        listOf(votes.exceptionOrNull(), counts.exceptionOrNull()).forEach {
            if (it is kotlinx.coroutines.CancellationException) throw it
        }
        val available = votes.isSuccess && counts.isSuccess
        fun hydrate(post: Post): Post {
            if (post.hasPoll != true) return post
            val optionCounts = counts.getOrNull()?.get(post.id)
            return post.copy(
                pollOptions = post.pollOptions?.mapIndexed { index, option ->
                    option.copy(votes = optionCounts?.get(index) ?: 0)
                },
                userPollVote = if (available) votes.getOrNull()?.get(post.id) else null,
                isPollStateAvailable = available
            )
        }
        return posts.map { hydrate(it).copy(quotedPost = it.quotedPost?.let(::hydrate)) }
    }
}
