package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Favourites settings screen.
 *
 * Wires the two display toggles ([showFavouritesInChatList] and [showFavouriteBadge]) to the
 * persisted [SettingsRepository] so user preferences survive navigation and process death instead
 * of being reset to their initial value on every composition.
 */
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepositoryImpl.getInstance(application)

    private val _showFavouritesInChatList = MutableStateFlow(false)
    val showFavouritesInChatList: StateFlow<Boolean> = _showFavouritesInChatList.asStateFlow()

    private val _showFavouriteBadge = MutableStateFlow(false)
    val showFavouriteBadge: StateFlow<Boolean> = _showFavouriteBadge.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadFavouritesSettings()
    }

    private fun loadFavouritesSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.showFavouritesInChatList.collect { enabled ->
                    _showFavouritesInChatList.value = enabled
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Failed to load favourites settings"
            }
        }
        viewModelScope.launch {
            try {
                settingsRepository.showFavouriteBadge.collect { enabled ->
                    _showFavouriteBadge.value = enabled
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Failed to load favourites badge setting"
            }
        }
    }

    fun toggleShowFavouritesInChatList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val current = _showFavouritesInChatList.value
                settingsRepository.setShowFavouritesInChatList(!current)
                _showFavouritesInChatList.value = !current
            } catch (e: Exception) {
                _error.value = "Failed to update show favourites in chat list"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleShowFavouriteBadge() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val current = _showFavouriteBadge.value
                settingsRepository.setShowFavouriteBadge(!current)
                _showFavouriteBadge.value = !current
            } catch (e: Exception) {
                _error.value = "Failed to update show favourite badge"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
