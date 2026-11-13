package com.example.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PushHistoryItem
import com.example.data.model.TargetScreen
import com.example.data.model.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    formState: NotificationFormState,
    historyState: UiState<List<PushHistoryItem>>,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onScreenSelected: (TargetScreen) -> Unit,
    onSendTestClick: () -> Unit,
    onSendAllClick: () -> Unit,
    onConfirmSendAll: () -> Unit,
    onDismissConfirmAll: () -> Unit,
    onRetryHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH) }

    if (formState.showConfirmAllDialog) {
        AlertDialog(
            onDismissRequest = onDismissConfirmAll,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = stringResource(id = R.string.notif_confirm_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formState.title.trim(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formState.body.trim(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Text(
                        text = "Topic: all",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSendAll,
                    modifier = Modifier.testTag("confirm_send_all_button")
                ) {
                    Text(stringResource(id = R.string.notif_confirm_send))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmAll) {
                    Text(stringResource(id = R.string.notif_confirm_cancel))
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NotificationComposeCard(
                    formState = formState,
                    onTitleChange = onTitleChange,
                    onBodyChange = onBodyChange,
                    onImageUrlChange = onImageUrlChange,
                    onScreenSelected = onScreenSelected,
                    onSendTestClick = onSendTestClick,
                    onSendAllClick = onSendAllClick
                )
            }

            item {
                NotificationLivePreviewCard(formState = formState)
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.notif_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            when (historyState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = historyState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onRetryHistory) {
                                    Text(stringResource(id = R.string.retry))
                                }
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    if (historyState.data.isEmpty()) {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(id = R.string.notif_history_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(historyState.data, key = { it.id }) { item ->
                            PushHistoryCard(item = item, dateFormatter = dateFormatter)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationComposeCard(
    formState: NotificationFormState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onScreenSelected: (TargetScreen) -> Unit,
    onSendTestClick: () -> Unit,
    onSendAllClick: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.notif_compose_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = formState.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(id = R.string.notif_title_label)) },
                singleLine = true,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("${formState.title.length}/${NotificationFormState.MAX_TITLE_LENGTH}")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notif_title_input")
            )

            OutlinedTextField(
                value = formState.body,
                onValueChange = onBodyChange,
                label = { Text(stringResource(id = R.string.notif_message_label)) },
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("${formState.body.length}/${NotificationFormState.MAX_BODY_LENGTH}")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notif_body_input")
            )

            OutlinedTextField(
                value = formState.imageUrl,
                onValueChange = onImageUrlChange,
                label = { Text(stringResource(id = R.string.notif_image_url_label)) },
                placeholder = { Text("https://...") },
                singleLine = true,
                isError = !formState.isImageUrlValid,
                supportingText = if (!formState.isImageUrlValid) {
                    { Text(stringResource(id = R.string.notif_image_url_error)) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notif_image_url_input")
            )

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = formState.selectedScreen.displayLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.notif_screen_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("notif_screen_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    TargetScreen.entries.forEach { screen ->
                        DropdownMenuItem(
                            text = { Text(screen.displayLabel) },
                            onClick = {
                                onScreenSelected(screen)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !formState.statusMessage.isNullOrBlank()) {
                val isError = formState.statusIsError
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isError) {
                                Icons.Rounded.ErrorOutline
                            } else {
                                Icons.Rounded.CheckCircleOutline
                            },
                            contentDescription = null,
                            tint = if (isError) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        Text(
                            text = formState.statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSendTestClick,
                    enabled = formState.canSend,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("send_test_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (formState.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Science,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.notif_send_test))
                    }
                }

                Button(
                    onClick = onSendAllClick,
                    enabled = formState.canSend,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("send_all_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (formState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.notif_send_all))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationLivePreviewCard(formState: NotificationFormState) {
    val colorScheme = MaterialTheme.colorScheme
    val previewTitle = formState.title.trim().ifBlank { "নোটিফিকেশন শিরোনাম এখানে দেখাবে" }
    val previewBody = formState.body.trim().ifBlank { "আপনার বার্তাটি ব্যবহারকারীর ফোনে এমনভাবে প্রদর্শিত হবে।" }
    val validImageUrl = formState.imageUrl.trim().takeIf { it.startsWith("https://") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notif_live_preview_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(id = R.string.notif_preview_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary
                )
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.hisabflow_admin_icon_1794603476595),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "HisabFlow • এখন",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        if (formState.selectedScreen != TargetScreen.NONE) {
                            Spacer(Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = formState.selectedScreen.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = previewTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    Text(
                        text = previewBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )

                    if (validImageUrl != null) {
                        AsyncImage(
                            model = validImageUrl,
                            contentDescription = previewTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PushHistoryCard(
    item: PushHistoryItem,
    dateFormatter: SimpleDateFormat
) {
    val isTest = item.target == "tester"
    val timeText = item.timestampMillis?.let { dateFormatter.format(Date(it)) } ?: "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title.ifBlank { "শিরোনামহীন" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isTest) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = if (isTest) "tester" else "all",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isTest) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (item.body.isNotBlank()) {
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
