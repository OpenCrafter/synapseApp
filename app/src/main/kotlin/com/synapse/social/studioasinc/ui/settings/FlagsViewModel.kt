package com.synapse.social.studioasinc.ui.settings

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

    fun setNavigationPlacement(placement: NavigationPlacement) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                settingsRepository.setNavigationPlacement(placement)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setMessageSuggestionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                settingsRepository.setMessageSuggestionEnabled(enabled)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setChatAvatarDisabled(disabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                settingsRepository.setChatAvatarDisabled(disabled)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setChatMessagePaginationLimit(limit: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                settingsRepository.setChatMessagePaginationLimit(limit)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
