package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import javax.inject.Inject

/**
 * Clears the signed-in user's profile picture so the default avatar is shown again.
 */
class RemoveProfilePhotoUseCase @Inject constructor(
    private val repository: EditProfileRepositoryImpl
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        val updateData = mapOf<String, Any?>("avatar" to null)
        return repository.updateProfile(userId, updateData)
    }
}
