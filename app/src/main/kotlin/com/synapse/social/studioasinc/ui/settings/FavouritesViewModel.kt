package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the Favourites settings screen.
 *
 * Wires the two display toggles ([showFavouritesInChatList] and [showFavouriteBadge]) to the
 * persisted [SettingsRepository] so user preferences survive navigation and process death instead
 * of being reset to their initial value on every composition.
 *
 * The toggles are single-sourced from the repository flows: toggling writes through the repository
 * and the live flow collection re-emits the saved value into the UI state. No manual StateFlow
 * assignment races against the collector, and cancellation is propagated correctly.
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
        _isLoading.value = true
        viewModelScope.launch {
            try {
                settingsRepository.showFavouritesInChatList
                    .combine(settingsRepository.showFavouriteBadge) { chatList, badge ->
                        Pair(chatList, badge)
                    }
                    .collect { (chatList, badge) ->
                        _showFavouritesInChatList.value = chatList
                        _showFavouriteBadge.value = badge
                        _isLoading.value = false
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Failed to load favourites settings"
                Log.e("FavouritesViewModel", "Failed to load favourites settings", e)
                _isLoading.value = false
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
                // Do not manually assign the StateFlow here: the repository flow re-emits the
                // saved value through loadFavouritesSettings' collector, which is the single
                // source of truth and avoids a write/read race.
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Failed to update show favourites in chat list"
                Log.e("FavouritesViewModel", "Failed to update show favourites in chat list", e)
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Failed to update show favourite badge"
                Log.e("FavouritesViewModel", "Failed to update show favourite badge", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
