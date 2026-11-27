package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserPreferencesRepository
import kotlin.Result

class SyncChatSettingsUseCase constructor(
    private val settingsRepository: SettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) {
    suspend fun updateFontScale(scale: Float): Result<Unit> {
        settingsRepository.setChatFontScale(scale)
        return syncToRemote { it.copy(chatFontScale = scale) }
    }

    suspend fun updateMessageCornerRadius(radius: Int): Result<Unit> {
        settingsRepository.setChatMessageCornerRadius(radius)
        return syncToRemote { it.copy(chatMessageCornerRadius = radius) }
    }

    suspend fun updateThemePreset(preset: ChatThemePreset): Result<Unit> {
        settingsRepository.setChatThemePreset(preset)
        return syncToRemote { it.copy(chatThemePreset = preset.name) }
    }

    suspend fun updateWallpaperType(type: WallpaperType): Result<Unit> {
        settingsRepository.setChatWallpaperType(type)
        return syncToRemote { it.copy(chatWallpaperType = type.name) }
    }

    suspend fun updateWallpaperValue(value: String?): Result<Unit> {
        settingsRepository.setChatWallpaperValue(value)
        return syncToRemote { it.copy(chatWallpaperValue = value) }
    }

    suspend fun updateWallpaperBlur(blur: Float): Result<Unit> {
        settingsRepository.setChatWallpaperBlur(blur)
        return syncToRemote { it.copy(chatWallpaperBlur = blur) }
    }

    suspend fun updateListLayout(layout: ChatListLayout): Result<Unit> {
        settingsRepository.setChatListLayout(layout)
        return syncToRemote { it.copy(chatListLayout = layout.name) }
    }

    suspend fun updateSwipeGesture(gesture: ChatSwipeGesture): Result<Unit> {
        settingsRepository.setChatSwipeGesture(gesture)
        return syncToRemote { it.copy(chatSwipeGesture = gesture.name) }
    }

    private suspend fun syncToRemote(update: (com.synapse.social.studioasinc.shared.domain.model.UserPreferences) -> com.synapse.social.studioasinc.shared.domain.model.UserPreferences): Result<Unit> {
        val userId = authRepository.getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        return userPreferencesRepository.updatePreferences(userId, update)
    }
}
