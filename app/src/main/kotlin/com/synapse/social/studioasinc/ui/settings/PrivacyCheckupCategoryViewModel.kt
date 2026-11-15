package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Privacy Checkup category screens.
 *
 * Wires the [silenceUnknownCallers] toggle to the persisted [SettingsRepository] so the user's
 * preference survives navigation and process death instead of being reset to its initial value
 * on every composition.
 */
@HiltViewModel
class PrivacyCheckupCategoryViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepositoryImpl.getInstance(application)

    val silenceUnknownCallers: StateFlow<Boolean> = settingsRepository.silenceUnknownCallers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setSilenceUnknownCallers(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSilenceUnknownCallers(enabled)
        }
    }
}
