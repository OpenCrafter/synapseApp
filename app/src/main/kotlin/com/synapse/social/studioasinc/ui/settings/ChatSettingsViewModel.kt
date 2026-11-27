package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import com.synapse.social.studioasinc.shared.domain.usecase.settings.ObserveChatSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SyncChatSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val observeChatSettingsUseCase: ObserveChatSettingsUseCase,
    private val syncChatSettingsUseCase: SyncChatSettingsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val chatFontScale: StateFlow<Float> = observeChatSettingsUseCase.chatFontScale
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0f
        )

    val chatMessageCornerRadius: StateFlow<Int> = observeChatSettingsUseCase.chatMessageCornerRadius
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 16
        )

    val chatThemePreset: StateFlow<ChatThemePreset> = observeChatSettingsUseCase.chatThemePreset
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatThemePreset.DEFAULT
        )

    val chatWallpaperType: StateFlow<WallpaperType> = observeChatSettingsUseCase.chatWallpaperType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WallpaperType.DEFAULT
        )


    val chatWallpaperValue: StateFlow<String?> = observeChatSettingsUseCase.chatWallpaperValue
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val chatWallpaperBlur: StateFlow<Float> = observeChatSettingsUseCase.chatWallpaperBlur
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )

    val chatListLayout: StateFlow<ChatListLayout> = observeChatSettingsUseCase.chatListLayout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatListLayout.DOUBLE_LINE
        )

    val chatSwipeGesture: StateFlow<ChatSwipeGesture> = observeChatSettingsUseCase.chatSwipeGesture
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatSwipeGesture.ARCHIVE
        )

    val themeMode: StateFlow<ThemeMode> = observeChatSettingsUseCase.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    fun updateChatFontScale(scale: Float) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = syncChatSettingsUseCase.updateFontScale(scale)
                result.onFailure { error ->
                    _error.value = "Failed to save chat font scale"
                    Log.e("ChatSettingsViewModel", "updateFontScale failed", error)
                }
            } catch (e: Exception) {
                _error.value = "Failed to save chat font scale"
                Log.e("ChatSettingsViewModel", "updateFontScale failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateChatMessageCornerRadius(radius: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = syncChatSettingsUseCase.updateMessageCornerRadius(radius)
                result.onFailure { error ->
                    _error.value = "Failed to save chat message corner radius"
                    Log.e("ChatSettingsViewModel", "updateMessageCornerRadius failed", error)
                }
            } catch (e: Exception) {
                _error.value = "Failed to save chat message corner radius"
                Log.e("ChatSettingsViewModel", "updateMessageCornerRadius failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateChatThemePreset(preset: ChatThemePreset) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = syncChatSettingsUseCase.updateThemePreset(preset)
                result.onFailure { error ->
                    _error.value = "Failed to save chat theme preset"
                    Log.e("ChatSettingsViewModel", "updateThemePreset failed", error)
                }
            } catch (e: Exception) {
                _error.value = "Failed to save chat theme preset"
                Log.e("ChatSettingsViewModel", "updateThemePreset failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateChatWallpaperType(type: WallpaperType) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = syncChatSettingsUseCase.updateWallpaperType(type)
                result.onFailure { error ->
                    _error.value = "Failed to save chat wallpaper type"
                    Log.e("ChatSettingsViewModel", "updateWallpaperType failed", error)
                }
            } catch (e: Exception) {
                _error.value = "Failed to save chat wallpaper type"
                Log.e("ChatSettingsViewModel", "updateWallpaperType failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun updateChatWallpaperValue(value: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = syncChatSettingsUseCase.updateWallpaperValue(value)
                result.onFailure { error ->
                    _error.value = "Failed to save chat wallpaper value"
                    Log.e("ChatSettingsViewModel", "updateWallpaperValue failed", error)
                }
            } catch (e: Exception) {
                _error.value = "Failed to save chat wallpaper value"
                Log.e("ChatSettingsViewModel", "updateWallpaperValue failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateChatWallpaperBlur(blur: Float) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = syncChatSettingsUseCase.updateWallpaperBlur(blur)
                result.onFailure { error ->
                    _error.value = "Failed to save chat wallpaper blur"
                    Log.e("ChatSettingsViewModel", "updateWallpaperBlur failed", error)
                }
            } catch (e: Exception) {
                _error.value = "Failed to save chat wallpaper blur"
                Log.e("ChatSettingsViewModel", "updateWallpaperBlur failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateChatListLayout(layout: ChatListLayout) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = syncChatSettingsUseCase.updateListLayout(layout)
                result.onFailure { error ->
                    _error.value = "Failed to save chat list layout"
                    Log.e("ChatSettingsViewModel", "updateListLayout failed", error)
                }
            } catch (e: Exception) {
                _error.value = "Failed to save chat list layout"
                Log.e("ChatSettingsViewModel", "updateListLayout failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateChatSwipeGesture(gesture: ChatSwipeGesture) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = syncChatSettingsUseCase.updateSwipeGesture(gesture)
                result.onFailure { error ->
                    _error.value = "Failed to save chat swipe gesture"
                    Log.e("ChatSettingsViewModel", "updateSwipeGesture failed", error)
                }
            } catch (e: Exception) {
                _error.value = "Failed to save chat swipe gesture"
                Log.e("ChatSettingsViewModel", "updateSwipeGesture failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                observeChatSettingsUseCase.updateThemeMode(mode)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}