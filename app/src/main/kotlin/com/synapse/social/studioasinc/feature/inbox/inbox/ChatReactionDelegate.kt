package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.usecase.chat.ToggleMessageReactionUseCase
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ChatReactionDelegate(
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase,
    private val viewModelScope: CoroutineScope,
    private val onOptimisticReactionChanged: (String, Message, ReactionType?, ReactionType?) -> Unit,
    private val onError: (String?, Message) -> Unit
) {

    // Message IDs with a reaction toggle currently in flight. Scoped per message so that a rapid tap
    // on one message never blocks reaction taps on a different message.
    private val inFlightReactionIds = MutableStateFlow<Set<String>>(emptySet())

    fun toggleMessageReaction(
        messageId: String,
        reactionType: ReactionType,
        messages: List<Message>
    ) {
        // Guard against rapid-tap duplicate requests: ignore a second tap on the same message while its
        // reaction toggle is in flight. Other messages remain tappable.
        if (inFlightReactionIds.value.contains(messageId)) return
        // Mark in flight synchronously before launching so a near-simultaneous second tap cannot pass
        // the guard before the coroutine sets the marker (TOCTOU).
        inFlightReactionIds.value = inFlightReactionIds.value + messageId
        viewModelScope.launch {
            try {
                val oldMessage = messages.find { it.id == messageId } ?: return@launch
                val oldUserReaction = oldMessage.userReaction

                val isSame = oldUserReaction == reactionType
                val newUserReaction = if (isSame) null else reactionType

                // Optimistic update
                onOptimisticReactionChanged(messageId, oldMessage, newUserReaction, oldUserReaction)

                val chatId = oldMessage.chatId
                toggleMessageReactionUseCase(messageId, reactionType.emoji, chatId).onFailure { e ->
                    // Revert
                    onError("Failed to toggle reaction: ${e.message}", oldMessage)
                }
            } finally {
                // Always clear the in-flight marker so the user can retry after success or failure.
                inFlightReactionIds.value = inFlightReactionIds.value - messageId
            }
        }
    }
}
