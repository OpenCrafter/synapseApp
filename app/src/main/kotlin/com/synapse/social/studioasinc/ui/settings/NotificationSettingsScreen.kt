package com.synapse.social.studioasinc.ui.settings

import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.window.Dialog
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPreferences
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing

private val SocialNotificationTypes = listOf(
    NotificationCategory.LIKES to R.string.settings_notification_likes_title,
    NotificationCategory.COMMENTS to R.string.settings_notification_comments_title,
    NotificationCategory.REPLIES to R.string.settings_notification_replies_title,
    NotificationCategory.FOLLOWS to R.string.settings_notification_follows_title,
    NotificationCategory.MENTIONS to R.string.settings_notification_mentions_title
)

private val ContentNotificationTypes = listOf(
    NotificationCategory.NEW_POSTS to R.string.settings_notification_new_posts_title,
    NotificationCategory.SHARES to R.string.settings_notification_shares_title
)

private enum class QuietHoursStep {
    START, END
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit
) {
    val notificationPreferences by viewModel.notificationPreferences.collectAsState()
    val soundPreferences by viewModel.soundPreferences.collectAsState()
    val error by viewModel.error.collectAsState()
    val quietHoursSaveStatus by viewModel.quietHoursSaveStatus.collectAsState()

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    // Tracks which quiet-hours step is awaiting its async save result so the dialog
    // is only dismissed/advanced once the result is known.
    var quietHoursAwaiting by remember { mutableStateOf<QuietHoursStep?>(null) }
    var quietHoursDialogError by remember { mutableStateOf<String?>(null) }

    NotificationSettingsContent(
        notificationPreferences = notificationPreferences,
        soundPreferences = soundPreferences,
        error = error,
        onBackClick = onBackClick,
        onClearError = viewModel::clearError,
        onToggleGlobal = viewModel::toggleGlobalNotifications,
        onToggleCategory = viewModel::toggleNotificationCategory,
        onToggleQuietHours = viewModel::toggleQuietHours,
        onToggleDoNotDisturb = viewModel::toggleDoNotDisturb,
        onToggleSoundEffects = viewModel::toggleSoundEffects,
        onToggleMessageTones = viewModel::toggleMessageTones,
        onToggleCallTones = viewModel::toggleCallTones,
        onToggleConversationTones = viewModel::toggleConversationTones,
        onToggleReminders = viewModel::toggleReminders,
        onToggleHighPriority = viewModel::toggleHighPriority,
        onToggleReactions = viewModel::toggleReactions,
        onShowStartTimePicker = { showStartTimePicker = true }
    )

    if (showStartTimePicker) {
        QuietHoursTimePickerDialog(
            title = stringResource(R.string.settings_quiet_hours_start_title),
            initialTime = notificationPreferences.quietHoursStart,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { time ->
                quietHoursDialogError = null
                quietHoursAwaiting = QuietHoursStep.START
                viewModel.setQuietHours(time, notificationPreferences.quietHoursEnd)
            },
            confirmText = stringResource(R.string.settings_next_button),
            isSaving = quietHoursSaveStatus is QuietHoursSaveStatus.Saving,
            error = quietHoursDialogError
        )
    }

    if (showEndTimePicker) {
        QuietHoursTimePickerDialog(
            title = stringResource(R.string.settings_quiet_hours_end_title),
            initialTime = notificationPreferences.quietHoursEnd,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { time ->
                quietHoursDialogError = null
                quietHoursAwaiting = QuietHoursStep.END
                viewModel.setQuietHours(notificationPreferences.quietHoursStart, time)
            },
            confirmText = stringResource(R.string.settings_done_button),
            isSaving = quietHoursSaveStatus is QuietHoursSaveStatus.Saving,
            error = quietHoursDialogError
        )
    }

    // Only dismiss or advance the quiet-hours dialog once the async save result is known.
    // On success the dialog closes (or advances to the end step). On failure the dialog stays
    // open with an in-dialog error so the user can retry, mirroring the fix pattern used for
    // ChatFolders, Inbox delete, Word Effects, Avatar, Passkeys, Group Info, API Key and Story.
    LaunchedEffect(quietHoursSaveStatus, quietHoursAwaiting) {
        when (val status = quietHoursSaveStatus) {
            is QuietHoursSaveStatus.Success -> {
                when (val step = quietHoursAwaiting) {
                    QuietHoursStep.START -> {
                        showStartTimePicker = false
                        showEndTimePicker = true
                    }
                    QuietHoursStep.END -> {
                        showStartTimePicker = false
                        showEndTimePicker = false
                    }
                    null -> {}
                }
                quietHoursDialogError = null
                quietHoursAwaiting = null
                viewModel.clearQuietHoursSaveStatus()
            }
            is QuietHoursSaveStatus.Failure -> {
                quietHoursDialogError = status.error
                quietHoursAwaiting = null
                viewModel.clearQuietHoursSaveStatus()
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsContent(
    notificationPreferences: NotificationPreferences,
    soundPreferences: SoundPreferences,
    error: String?,
    onBackClick: () -> Unit,
    onClearError: () -> Unit,
    onToggleGlobal: (Boolean) -> Unit,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onToggleDoNotDisturb: (Boolean) -> Unit,
    onToggleSoundEffects: (Boolean) -> Unit,
    onToggleMessageTones: (Boolean) -> Unit,
    onToggleCallTones: (Boolean) -> Unit,
    onToggleConversationTones: (Boolean) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onToggleHighPriority: (Boolean) -> Unit,
    onToggleReactions: (Boolean) -> Unit,
    onShowStartTimePicker: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.screenBackground,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.screenBackground,
                    scrolledContainerColor = AppColors.cardBackground
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier.padding(Spacing.space16),
                    action = {
                        TextButton(onClick = onClearError) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppSpacing.screenPadding),
            contentPadding = PaddingValues(bottom = Spacing.space32),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
        ) {
            item {
                GroupedNotificationSettingsSections(
                    notificationPreferences = notificationPreferences,
                    conversationTonesEnabled = soundPreferences.conversationTonesEnabled,
                    onConversationTonesChange = onToggleConversationTones,
                    onToggleGlobal = onToggleGlobal,
                    onToggleReminders = onToggleReminders,
                    onToggleHighPriority = onToggleHighPriority,
                    onToggleReactions = onToggleReactions
                )
            }

            item {
                SoundEffectsSection(
                    soundPreferences = soundPreferences,
                    onToggleSoundEffects = onToggleSoundEffects,
                    onToggleMessageTones = onToggleMessageTones,
                    onToggleCallTones = onToggleCallTones
                )
            }

            item {
                SocialInteractionsSection(
                    notificationPreferences = notificationPreferences,
                    onToggleCategory = onToggleCategory
                )
            }

            item {
                ContentUpdatesSection(
                    notificationPreferences = notificationPreferences,
                    onToggleCategory = onToggleCategory
                )
            }

            item {
                SystemSecuritySection(
                    notificationPreferences = notificationPreferences,
                    onToggleCategory = onToggleCategory
                )
            }

            item {
                AdvancedSettingsSection(
                    notificationPreferences = notificationPreferences,
                    onToggleQuietHours = onToggleQuietHours,
                    onToggleDoNotDisturb = onToggleDoNotDisturb,
                    onShowStartTimePicker = onShowStartTimePicker
                )
            }
        }
    }
}

@Composable
private fun SocialInteractionsSection(
    notificationPreferences: NotificationPreferences,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_social_interactions_title)) {
        SocialNotificationTypes.forEachIndexed { index, (category, label) ->
            val isEnabled = notificationPreferences.isEnabled(category)
            val position = when {
                SocialNotificationTypes.size == 1 -> SettingsItemPosition.Single
                index == 0 -> SettingsItemPosition.Top
                index == SocialNotificationTypes.lastIndex -> SettingsItemPosition.Bottom
                else -> SettingsItemPosition.Middle
            }
            SettingsToggleItem(
                imageVector = when (category) {
                    NotificationCategory.LIKES -> Icons.Default.EmojiEmotions
                    NotificationCategory.FOLLOWS -> Icons.Default.Group
                    else -> Icons.Default.Notifications
                },
                title = stringResource(label),
                subtitle = if (isEnabled) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
                checked = isEnabled,
                onCheckedChange = { onToggleCategory(category, it) },
                enabled = notificationPreferences.globalEnabled,
                position = position
            )
        }
    }
}

@Composable
private fun ContentUpdatesSection(
    notificationPreferences: NotificationPreferences,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_content_updates_title)) {
        ContentNotificationTypes.forEachIndexed { index, (category, label) ->
            val isEnabled = notificationPreferences.isEnabled(category)
            val position = when {
                ContentNotificationTypes.size == 1 -> SettingsItemPosition.Single
                index == 0 -> SettingsItemPosition.Top
                index == ContentNotificationTypes.lastIndex -> SettingsItemPosition.Bottom
                else -> SettingsItemPosition.Middle
            }
            SettingsToggleItem(
                imageVector = Icons.Default.ContentCopy,
                title = stringResource(label),
                subtitle = if (isEnabled) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
                checked = isEnabled,
                onCheckedChange = { onToggleCategory(category, it) },
                enabled = notificationPreferences.globalEnabled,
                position = position
            )
        }
    }
}

@Composable
private fun SystemSecuritySection(
    notificationPreferences: NotificationPreferences,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_system_security_section)) {
        SettingsToggleItem(
            imageVector = Icons.Default.Info,
            title = stringResource(R.string.settings_security_alerts_title),
            subtitle = stringResource(R.string.settings_security_alerts_subtitle),
            checked = notificationPreferences.securityEnabled,
            onCheckedChange = { onToggleCategory(NotificationCategory.SECURITY_ALERTS, it) },
            enabled = notificationPreferences.globalEnabled,
            position = SettingsItemPosition.Top
        )
        SettingsToggleItem(
            imageVector = Icons.Default.Settings,
            title = stringResource(R.string.settings_app_updates_title),
            subtitle = if (notificationPreferences.updatesEnabled) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
            checked = notificationPreferences.updatesEnabled,
            onCheckedChange = { onToggleCategory(NotificationCategory.SYSTEM_UPDATES, it) },
            enabled = notificationPreferences.globalEnabled,
            position = SettingsItemPosition.Bottom
        )
    }
}

@Composable
private fun AdvancedSettingsSection(
    notificationPreferences: NotificationPreferences,
    onToggleQuietHours: (Boolean) -> Unit,
    onToggleDoNotDisturb: (Boolean) -> Unit,
    onShowStartTimePicker: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_advanced_settings_title)) {
        QuietHoursItem(
            notificationPreferences = notificationPreferences,
            onToggleQuietHours = onToggleQuietHours,
            onShowStartTimePicker = onShowStartTimePicker,
            position = SettingsItemPosition.Top
        )
        SettingsToggleItem(
            imageVector = Icons.Default.DoNotDisturb,
            title = stringResource(R.string.settings_do_not_disturb_title),
            subtitle = if (notificationPreferences.doNotDisturb) stringResource(R.string.settings_active) else stringResource(R.string.settings_inactive),
            checked = notificationPreferences.doNotDisturb,
            onCheckedChange = onToggleDoNotDisturb,
            enabled = notificationPreferences.globalEnabled,
            position = SettingsItemPosition.Bottom
        )
    }
}

@Composable
private fun QuietHoursItem(
    notificationPreferences: NotificationPreferences,
    onToggleQuietHours: (Boolean) -> Unit,
    onShowStartTimePicker: () -> Unit,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    val enabled = notificationPreferences.globalEnabled
    val quietHoursSummary = if (notificationPreferences.quietHoursEnabled) {
        "${notificationPreferences.quietHoursStart} - ${notificationPreferences.quietHoursEnd}"
    } else {
        stringResource(R.string.settings_disabled)
    }

    SettingsListItem(
        title = stringResource(R.string.settings_quiet_hours_title),
        subtitle = quietHoursSummary,
        position = position,
        enabled = enabled,
        onClick = onShowStartTimePicker,
        leadingContent = {
            SettingsIconBadge(imageVector = Icons.Default.Schedule)
        },
        trailingContent = {
            Switch(
                checked = notificationPreferences.quietHoursEnabled,
                onCheckedChange = onToggleQuietHours,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietHoursTimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    confirmText: String,
    isSaving: Boolean = false,
    error: String? = null
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.split(":").getOrNull(0)?.toIntOrNull() ?: 0,
        initialMinute = initialTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(Spacing.space16),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(Spacing.space8))
                }
                Spacer(modifier = Modifier.height(Spacing.space16))
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(
                        onClick = {
                            val time = "${state.hour.toString().padStart(2, '0')}:${
                                state.minute.toString().padStart(2, '0')
                            }"
                            onConfirm(time)
                        },
                        enabled = !isSaving
                    ) { Text(confirmText) }
                }
            }
        }
    }
}
