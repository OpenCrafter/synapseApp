package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserPreferencesRepository
import com.synapse.social.studioasinc.shared.domain.model.UserPreferences

class SyncAccessibilitySettingsUseCase constructor(
    private val settingsRepository: SettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) {
    suspend fun updateIncreaseContrast(enabled: Boolean): Result<Unit> {
        settingsRepository.setIncreaseContrastEnabled(enabled)
        return syncToRemote { it.copy(increaseContrastEnabled = enabled) }
    }

    suspend fun updateHighContrastText(enabled: Boolean): Result<Unit> {
        settingsRepository.setHighContrastTextEnabled(enabled)
        return syncToRemote { it.copy(highContrastTextEnabled = enabled) }
    }

    suspend fun updateReduceAnimations(enabled: Boolean): Result<Unit> {
        settingsRepository.setReduceAnimationsEnabled(enabled)
        return syncToRemote { it.copy(reduceAnimationsEnabled = enabled) }
    }

    suspend fun updateAutoplayAnimations(enabled: Boolean): Result<Unit> {
        settingsRepository.setAutoplayAnimationsEnabled(enabled)
        return syncToRemote { it.copy(autoplayAnimationsEnabled = enabled) }
    }

    private suspend fun syncToRemote(update: (UserPreferences) -> UserPreferences): Result<Unit> {
        val userId = authRepository.getCurrentUserId() ?: return Result.failure(IllegalStateException("Not authenticated"))
        return userPreferencesRepository.updatePreferences(userId, update)
    }
}
