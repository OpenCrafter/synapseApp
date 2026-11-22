package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.repository.PollRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PopulatePostPollsUseCaseTest {
    private class Polls : PollRepository {
        val voteRequests = mutableListOf<List<String>>()
        val countRequests = mutableListOf<List<String>>()
        var votes = mapOf("first" to 0)
        var failVotes = false
        override suspend fun submitVote(postId: String, optionIndex: Int) = Result.success(Unit)
        override suspend fun revokeVote(postId: String) = Result.success(Unit)
        override suspend fun getBatchUserVotes(postIds: List<String>): Result<Map<String, Int>> {
            voteRequests.add(postIds)
            return if (failVotes) Result.failure(IllegalStateException("offline"))
            else Result.success(votes.filterKeys { it in postIds })
        }
        override suspend fun getBatchPollVotes(postIds: List<String>): Result<Map<String, Map<Int, Int>>> {
            countRequests.add(postIds)
            return Result.success(postIds.associateWith { mapOf(0 to 3) })
        }
    }

    private fun poll(id: String) = Post(id = id, hasPoll = true,
        pollOptions = listOf(PollOption("A", 0), PollOption("B", 0)))

    @Test fun hydratesEachPageAndMatchesDetail() = runBlocking {
        val repository = Polls()
        val hydrate = PopulatePostPollsUseCase(repository)
        val (voted, unvoted) = hydrate(listOf(poll("first"), poll("second")))
        assertEquals(0, voted.userPollVote)
        assertEquals(listOf(3, 0), voted.pollOptions?.map { it.votes })
        assertTrue(unvoted.isPollStateAvailable)
        assertNull(unvoted.userPollVote)
        val next = hydrate(listOf(poll("third").copy(quotedPost = poll("first")))).single()
        assertEquals(voted, next.quotedPost)
        assertEquals(voted, hydrate(listOf(poll("first"))).single())
        assertEquals(listOf(listOf("first", "second"), listOf("third", "first"), listOf("first")), repository.voteRequests)
        assertEquals(repository.voteRequests, repository.countRequests)
    }

    @Test fun distinguishesFailedLookupFromRevokedOrAbsentVote() = runBlocking {
        val repository = Polls()
        val hydrate = PopulatePostPollsUseCase(repository)
        val voted = hydrate(listOf(poll("first"))).single()
        repository.failVotes = true
        val unavailable = hydrate(listOf(voted)).single()
        assertFalse(unavailable.isPollStateAvailable)
        assertNull(unavailable.userPollVote)
        repository.failVotes = false
        repository.votes = emptyMap()
        val revoked = hydrate(listOf(voted)).single()
        assertTrue(revoked.isPollStateAvailable)
        assertNull(revoked.userPollVote)
    }
}
