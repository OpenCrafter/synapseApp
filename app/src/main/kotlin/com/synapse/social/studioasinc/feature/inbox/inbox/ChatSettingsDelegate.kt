package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.core.util.ChatLockManager
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.synapse.social.studioasinc.shared.domain.usecase.chat.SetDisappearingModeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ChatSettingsDelegate(
    private val chatLockManager: ChatLockManager,
    private val setDisappearingModeUseCase: SetDisappearingModeUseCase,
    private val viewModelScope: CoroutineScope,
    private val _disappearingMode: MutableStateFlow<DisappearingMode>,
    private val currentChatIdProvider: () -> String?
) {

    // ===== Disappearing mode save status =====
    // Tracks the async save result so the UI can keep the confirmation dialog
    // open until the result is known.
    sealed class DisappearingModeSaveStatus {
        object Idle : DisappearingModeSaveStatus()
        object Saving : DisappearingModeSaveStatus()
        object Success : DisappearingModeSaveStatus()
        data class Failure(val message: String) : DisappearingModeSaveStatus()
    }

    private val _disappearingModeSaveStatus = MutableStateFlow<DisappearingModeSaveStatus>(DisappearingModeSaveStatus.Idle)
    val disappearingModeSaveStatus: StateFlow<DisappearingModeSaveStatus> = _disappearingModeSaveStatus.asStateFlow()

    fun resetDisappearingModeSaveStatus() {
        _disappearingModeSaveStatus.value = DisappearingModeSaveStatus.Idle
    }

    fun isChatLocked(): Boolean {
        return currentChatIdProvider()?.let { chatLockManager.isChatLocked(it) } ?: false
    }

    fun lockCurrentChat() {
        currentChatIdProvider()?.let { chatLockManager.lockChat(it) }
    }

    fun unlockCurrentChat() {
        currentChatIdProvider()?.let { chatLockManager.unlockChat(it) }
    }

    fun setDisappearingMode(mode: DisappearingMode) {
        _disappearingMode.value = mode
        val chatId = currentChatIdProvider() ?: return
        _disappearingModeSaveStatus.value = DisappearingModeSaveStatus.Saving
        viewModelScope.launch {
            val result = setDisappearingModeUseCase(chatId, mode)
            if (result.isSuccess) {
                _disappearingModeSaveStatus.value = DisappearingModeSaveStatus.Success
            } else {
                _disappearingModeSaveStatus.value = DisappearingModeSaveStatus.Failure(result.exceptionOrNull()?.message ?: "Failed to save disappearing mode")
            }
        }
    }
}
