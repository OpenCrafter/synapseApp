package com.synapse.social.studioasinc.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.database.settings.SettingsConstants
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlagsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val navigationPlacement: StateFlow<NavigationPlacement> = settingsRepository.navigationPlacement
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsConstants.DEFAULT_NAVIGATION_PLACEMENT
        )

    val messageSuggestionEnabled: StateFlow<Boolean> = settingsRepository.messageSuggestionEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val chatAvatarDisabled: StateFlow<Boolean> = settingsRepository.chatAvatarDisabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val chatMessagePaginationLimit: StateFlow<Int> = settingsRepository.chatMessagePaginationLimit
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsConstants.DEFAULT_CHAT_MESSAGE_PAGINATION_LIMIT
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setNavigationPlacement(placement: NavigationPlacement) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setNavigationPlacement(placement)
            } catch (e: Exception) {
                Log.e("FlagsViewModel", "Failed to update navigation placement", e)
                _error.value = "Failed to update navigation placement"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setMessageSuggestionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setMessageSuggestionEnabled(enabled)
            } catch (e: Exception) {
                Log.e("FlagsViewModel", "Failed to update message suggestions", e)
                _error.value = "Failed to update message suggestions"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setChatAvatarDisabled(disabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setChatAvatarDisabled(disabled)
            } catch (e: Exception) {
                Log.e("FlagsViewModel", "Failed to update chat avatar setting", e)
                _error.value = "Failed to update chat avatar setting"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setChatMessagePaginationLimit(limit: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setChatMessagePaginationLimit(limit)
            } catch (e: Exception) {
                Log.e("FlagsViewModel", "Failed to update message pagination limit", e)
                _error.value = "Failed to update message pagination limit"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
