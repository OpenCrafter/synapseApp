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
import kotlinx.coroutines.launch

/**
 * ViewModel for the Privacy Checkup category screens.
 *
 * Wires the "Silence unknown callers" toggle to the persisted [SettingsRepository] so the
 * preference survives navigation and process death instead of being reset to its initial value
 * on every composition.
 *
 * The toggle is single-sourced from the repository flow: toggling writes through the repository
 * and the live flow collection re-emits the saved value into the UI state.
 */
@HiltViewModel
class PrivacyCheckupViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepositoryImpl.getInstance(application)

    private val _silenceUnknownCallers = MutableStateFlow(false)
    val silenceUnknownCallers: StateFlow<Boolean> = _silenceUnknownCallers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSilenceUnknownCallers()
    }

    private fun loadSilenceUnknownCallers() {
        viewModelScope.launch {
            try {
                settingsRepository.silenceUnknownCallers.collect { enabled ->
                    _silenceUnknownCallers.value = enabled
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Failed to load silence unknown callers setting"
                Log.e("PrivacyCheckupViewModel", "Failed to load silence unknown callers setting", e)
            }
        }
    }

    fun setSilenceUnknownCallers(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setSilenceUnknownCallers(enabled)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Failed to update silence unknown callers setting"
                Log.e("PrivacyCheckupViewModel", "Failed to update silence unknown callers setting", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
