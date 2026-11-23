package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

data class PostOption(
    val label: String,
    val icon: ImageVector,
    val isDangerous: Boolean = false,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    post: Post,
    isOwner: Boolean,
    commentsDisabled: Boolean = false,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: suspend () -> Result<Unit>,
    onAddToThread: (() -> Unit)? = null,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onBookmark: () -> Unit,
    onReshare: () -> Unit = {},
    onToggleComments: suspend () -> Result<Unit>,
    onReport: suspend () -> Result<Unit>,
    onBlock: suspend () -> Result<Unit>,
    onRevokeVote: () -> Unit,
    onSummarize: () -> Unit = {},
    onShareToChat: (() -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    val onToggleCommentsAction: () -> Unit = {
        if (!isProcessing) {
            isProcessing = true
            scope.launch {
                try {
                    val result = onToggleComments()
                    if (result.isSuccess) {
                        onDismiss()
                    }
                } finally {
                    isProcessing = false
                }
            }
        }
        Unit
    }
    val onBlockAction: () -> Unit = {
        if (!isProcessing) {
            isProcessing = true
            scope.launch {
                try {
                    val result = onBlock()
                    if (result.isSuccess) {
                        onDismiss()
                    }
                } finally {
                    isProcessing = false
                }
            }
        }
        Unit
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.space16)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.space16, vertical = Spacing.space8),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAction(
                    icon = Icons.AutoMirrored.Filled.Reply,
                    label = "Reshare",
                    onClick = {
                        onReshare()
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Share",
                    onClick = {
                        onShare()
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = Icons.Filled.Link,
                    label = "Copy Link",
                    onClick = {
                        onCopyLink()
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = Icons.Filled.Bookmark,
                    label = "Save",
                    onClick = {
                        onBookmark()
                        onDismiss()
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.space8))

            val options = buildOptions(
                isOwner = isOwner,
                commentsDisabled = commentsDisabled,
                post = post,
                onEdit = { onEdit(); onDismiss() },
                onDelete = { showDeleteDialog = true },
                onAddToThread = { onAddToThread?.invoke(); onDismiss() },
                onToggleComments = onToggleCommentsAction,
                onReport = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                val result = onReport()
                                if (result.isSuccess) {
                                    onDismiss()
                                }
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                    Unit
                },
                onBlock = onBlockAction,
                onShare = { onShare(); onDismiss() },
                onRevokeVote = { onRevokeVote(); onDismiss() },
                onSummarize = { onSummarize(); onDismiss() },
                onShareToChat = onShareToChat?.let { shareToChat ->
                    { shareToChat(); onDismiss() }
                }
            )

            LazyColumn {
                items(options) { option ->
                    OptionItem(option = option)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_post_title)) },
            text = { Text(stringResource(R.string.delete_post_body)) },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = click@ {
                        // Do not dismiss the confirmation dialog until the delete result is known.
                        // Dismissing upfront closes the dialog before the async delete completes, so a
                        // failed delete leaves the user with no dialog to retry from and no clear outcome.
                        if (isDeleting) return@click
                        isDeleting = true
                        scope.launch {
                            try {
                                val result = onDelete()
                                if (result.isSuccess) {
                                    showDeleteDialog = false
                                    onDismiss()
                                }
                                // On failure the dialog stays open so the user can retry; the screen's
                                // existing error handling (e.g. the deletePostError snackbar) surfaces the
                                // failure while the dialog remains available for another attempt.
                            } finally {
                                isDeleting = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { if (!isDeleting) showDeleteDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
internal fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(Sizes.HeightDefault)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(Sizes.IconLarge)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.space4))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
internal fun OptionItem(option: PostOption) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = option.action)
            .padding(horizontal = Spacing.space16, vertical = Spacing.space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.label,
            tint = if (option.isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(Sizes.IconLarge)
        )
        Spacer(modifier = Modifier.width(Spacing.space16))
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (option.isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun buildOptions(
    isOwner: Boolean,
    commentsDisabled: Boolean,
    post: Post,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToThread: () -> Unit,
    onToggleComments: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onShare: () -> Unit,
    onRevokeVote: () -> Unit,
    onSummarize: () -> Unit,
    onShareToChat: (() -> Unit)?
): List<PostOption> {
    val options = mutableListOf<PostOption>()

    if (isOwner) {
        options.add(PostOption(stringResource(R.string.edit), Icons.Filled.EditNote, action = onEdit))
        options.add(PostOption(
            if (commentsDisabled) stringResource(R.string.turn_on_commenting) else stringResource(R.string.turn_off_commenting),
            Icons.Filled.CommentsDisabled,
            action = onToggleComments
        ))
        options.add(PostOption(stringResource(R.string.delete), Icons.Filled.Delete, isDangerous = true, action = onDelete))
    } else {
        options.add(PostOption(stringResource(R.string.report), Icons.Filled.Report, isDangerous = true, action = onReport))
        options.add(PostOption(stringResource(R.string.block_user), Icons.Filled.Block, isDangerous = true, action = onBlock))
    }

    if (post.userPollVote != null) {
        options.add(PostOption(stringResource(R.string.revoke_vote), Icons.Filled.Delete, action = onRevokeVote))
    }

    if ((post.postText?.length ?: 0) >= 120) {
        options.add(PostOption(stringResource(R.string.action_ai_summary), Icons.Filled.AutoAwesome, action = onSummarize))
    }
    onShareToChat?.let { shareToChat ->
        options.add(
            PostOption(
                stringResource(R.string.share_to_chat_action),
                Icons.AutoMirrored.Filled.Send,
                action = shareToChat
            )
        )
    }
    options.add(PostOption(stringResource(R.string.share_via), Icons.AutoMirrored.Filled.Send, action = onShare))

    return options
}
