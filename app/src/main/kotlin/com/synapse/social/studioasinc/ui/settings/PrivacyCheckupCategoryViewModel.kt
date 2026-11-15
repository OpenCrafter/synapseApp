package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Privacy Checkup category screen.
 *
 * Wires the "Silence unknown callers" toggle to the persisted [SettingsRepository] so the
 * preference survives navigation and process death instead of being reset to its initial value
 * on every composition.
 *
 * The [SettingsRepository] is injected via Hilt (consistent with the other ViewModels in the
 * codebase) rather than obtained through the concrete repository singleton.
 */
@HiltViewModel
class PrivacyCheckupCategoryViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val silenceUnknownCallers: StateFlow<Boolean> = settingsRepository.silenceUnknownCallers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setSilenceUnknownCallers(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSilenceUnknownCallers(enabled)
        }
    }
}
