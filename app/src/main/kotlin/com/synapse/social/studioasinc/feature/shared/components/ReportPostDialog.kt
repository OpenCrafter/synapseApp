package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.layout.*
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ReportPostDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    var selectedReason by remember { mutableStateOf("") }

    val reasons = listOf(
        "Spam",
        "Harassment or bullying",
        "Hate speech",
        "Violence or dangerous content",
        "Nudity or sexual content",
        "False information",
        "Other"
    )

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(R.string.report_post_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.report_post_title),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Spacing.space8)
                )

                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = Spacing.space8)
                    )
                }

                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            .padding(vertical = Spacing.space8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(Spacing.space8))
                        Text(text = reason)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (!isLoading && selectedReason.isNotEmpty()) onConfirm(selectedReason) },
                enabled = !isLoading && selectedReason.isNotEmpty()
            ) {
                if (isLoading) {
                    ExpressiveLoadingIndicator(
                        modifier = Modifier.size(Sizes.IconSemiMedium)
                    )
                    Spacer(modifier = Modifier.width(Spacing.space8))
                }
                Text(stringResource(R.string.action_report))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}
