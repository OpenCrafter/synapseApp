package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.usecase.settings.ObserveAccessibilitySettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SyncAccessibilitySettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessibilityViewModel @Inject constructor(
    private val observeAccessibilitySettingsUseCase: ObserveAccessibilitySettingsUseCase,
    private val syncAccessibilitySettingsUseCase: SyncAccessibilitySettingsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val increaseContrastEnabled: StateFlow<Boolean> = observeAccessibilitySettingsUseCase.increaseContrastEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val highContrastTextEnabled: StateFlow<Boolean> = observeAccessibilitySettingsUseCase.highContrastTextEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val reduceAnimationsEnabled: StateFlow<Boolean> = observeAccessibilitySettingsUseCase.reduceAnimationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val autoplayAnimationsEnabled: StateFlow<Boolean> = observeAccessibilitySettingsUseCase.autoplayAnimationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateIncreaseContrast(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                syncAccessibilitySettingsUseCase.updateIncreaseContrast(enabled)
            } catch (e: Exception) {
                _error.value = "Failed to save increase contrast setting"
                android.util.Log.e("AccessibilityViewModel", "updateIncreaseContrast failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateHighContrastText(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                syncAccessibilitySettingsUseCase.updateHighContrastText(enabled)
            } catch (e: Exception) {
                _error.value = "Failed to save high contrast text setting"
                android.util.Log.e("AccessibilityViewModel", "updateHighContrastText failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateReduceAnimations(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                syncAccessibilitySettingsUseCase.updateReduceAnimations(enabled)
            } catch (e: Exception) {
                _error.value = "Failed to save reduce animations setting"
                android.util.Log.e("AccessibilityViewModel", "updateReduceAnimations failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAutoplayAnimations(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                syncAccessibilitySettingsUseCase.updateAutoplayAnimations(enabled)
            } catch (e: Exception) {
                _error.value = "Failed to save autoplay animations setting"
                android.util.Log.e("AccessibilityViewModel", "updateAutoplayAnimations failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
