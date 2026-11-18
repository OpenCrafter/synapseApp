package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import kotlinx.coroutines.launch

/** The owner's own profile image that an action sheet targets. */
enum class ProfilePhotoType { AVATAR, COVER }

/**
 * Action sheet shown when the signed-in user taps their own profile picture or cover photo.
 *
 * Change actions hand off to the existing profile media flow; remove and view actions are
 * offered only when the targeted image actually exists, so no action is ever a dead end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoActionsBottomSheet(
    target: ProfilePhotoType,
    hasImage: Boolean,
    onDismiss: () -> Unit,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onViewFullScreen: () -> Unit
) {
    // The sheet is only composed while a target is selected, so it opens expanded.
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            onDismiss()
            action()
        }
    }

    val isAvatar = target == ProfilePhotoType.AVATAR

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = Sizes.CornerMassive,
            topEnd = Sizes.CornerMassive
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.space16,
                    end = Spacing.space16,
                    bottom = Spacing.space16
                )
        ) {
            Text(
                text = stringResource(
                    if (isAvatar) R.string.section_profile_photo else R.string.section_cover_photo
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = Spacing.space8,
                    top = Spacing.space4,
                    bottom = Spacing.space12
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                PhotoActionItem(
                    icon = Icons.Rounded.Edit,
                    title = stringResource(
                        if (isAvatar) R.string.action_change_photo else R.string.action_change_cover
                    ),
                    onClick = { dismissThen(onChangePhoto) }
                )

                if (hasImage) {
                    PhotoActionItem(
                        icon = Icons.Rounded.Delete,
                        title = stringResource(
                            if (isAvatar) R.string.action_remove_photo else R.string.action_remove_cover
                        ),
                        onClick = { dismissThen(onRemovePhoto) }
                    )
                    PhotoActionItem(
                        icon = Icons.Rounded.Fullscreen,
                        title = stringResource(R.string.photo_actions_view_full_screen),
                        onClick = { dismissThen(onViewFullScreen) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Sizes.CornerMedium),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space16, vertical = Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(Sizes.IconMassive),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconLarge)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.space16))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
