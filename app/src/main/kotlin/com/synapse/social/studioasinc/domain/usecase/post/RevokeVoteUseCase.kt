package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.PollRepository
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.math.max

class RevokeVoteUseCase @Inject constructor(
    private val pollRepository: PollRepository
) {
    operator fun invoke(post: Post): Flow<Result<Post>> = flow {
        val currentVoteIndex = post.userPollVote
        if (currentVoteIndex == null) {
            emit(Result.success(post))
            return@flow
        }
        val currentOptions = post.pollOptions ?: throw IllegalArgumentException("No poll options")

        val updatedOptions = currentOptions.mapIndexed { index, option ->
            if (index == currentVoteIndex) option.copy(votes = max(0, option.votes - 1)) else option
        }

        val updatedPost = post.copy(
            pollOptions = updatedOptions,
            userPollVote = null
        )

        val result = pollRepository.revokeVote(post.id)
        val error = result.exceptionOrNull()
        if (error is kotlinx.coroutines.CancellationException) throw error
        when (result) {
            is Result.Success -> emit(Result.success(updatedPost))
            is Result.Failure -> emit(Result.failure(result.exception))
        }
    }
}
