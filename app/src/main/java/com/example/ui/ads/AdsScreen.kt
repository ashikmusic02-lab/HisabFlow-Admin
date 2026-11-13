package com.example.ui.ads

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdUnits
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdField
import com.example.data.model.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdsScreen(
    uiState: UiState<AdsScreenState>,
    onToggleSwitch: (AdField, Boolean) -> Unit,
    onIntervalInputChange: (String) -> Unit,
    onSaveInterval: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is UiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 460.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onRetry) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.retry))
                        }
                    }
                }
            }
        }

        is UiState.Success -> {
            AdsContent(
                state = uiState.data,
                onToggleSwitch = onToggleSwitch,
                onIntervalInputChange = onIntervalInputChange,
                onSaveInterval = onSaveInterval,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AdsContent(
    state: AdsScreenState,
    onToggleSwitch: (AdField, Boolean) -> Unit,
    onIntervalInputChange: (String) -> Unit,
    onSaveInterval: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    }
    val updatedText = state.config.updatedAtMillis?.let {
        "${stringResource(id = R.string.ads_last_updated)}: ${dateFormatter.format(Date(it))}"
    } ?: if (state.config.existsOnServer) {
        "${stringResource(id = R.string.ads_last_updated)}: —"
    } else {
        stringResource(id = R.string.ads_not_saved_yet)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "app_config/ads",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                            )
                            Text(
                                text = updatedText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            item {
                AdSwitchCard(
                    title = stringResource(id = R.string.ads_banner_label),
                    fieldKey = stringResource(id = R.string.ads_banner_key),
                    icon = Icons.Rounded.ViewAgenda,
                    checked = state.config.bannerEnabled,
                    isSaving = AdField.BANNER in state.savingFields,
                    onCheckedChange = { onToggleSwitch(AdField.BANNER, it) },
                    testTag = "switch_banner"
                )
            }

            item {
                AdSwitchCard(
                    title = stringResource(id = R.string.ads_native_label),
                    fieldKey = stringResource(id = R.string.ads_native_key),
                    icon = Icons.Rounded.AdsClick,
                    checked = state.config.nativeEnabled,
                    isSaving = AdField.NATIVE in state.savingFields,
                    onCheckedChange = { onToggleSwitch(AdField.NATIVE, it) },
                    testTag = "switch_native"
                )
            }

            item {
                AdSwitchCard(
                    title = stringResource(id = R.string.ads_interstitial_label),
                    fieldKey = stringResource(id = R.string.ads_interstitial_key),
                    icon = Icons.Rounded.Fullscreen,
                    checked = state.config.interstitialEnabled,
                    isSaving = AdField.INTERSTITIAL in state.savingFields,
                    onCheckedChange = { onToggleSwitch(AdField.INTERSTITIAL, it) },
                    testTag = "switch_interstitial"
                )
            }

            item {
                AdSwitchCard(
                    title = stringResource(id = R.string.ads_rewarded_label),
                    fieldKey = stringResource(id = R.string.ads_rewarded_key),
                    icon = Icons.Rounded.Stars,
                    checked = state.config.rewardedEnabled,
                    isSaving = AdField.REWARDED in state.savingFields,
                    onCheckedChange = { onToggleSwitch(AdField.REWARDED, it) },
                    testTag = "switch_rewarded"
                )
            }

            item {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdUnits,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(id = R.string.ads_interval_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "interstitial_min_interval_sec",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.intervalInput,
                            onValueChange = onIntervalInputChange,
                            singleLine = true,
                            isError = state.intervalError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                Text(
                                    text = if (state.intervalError) {
                                        stringResource(id = R.string.ads_interval_error)
                                    } else {
                                        stringResource(id = R.string.ads_interval_helper)
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("interval_input")
                        )

                        Button(
                            onClick = onSaveInterval,
                            enabled = !state.isSavingInterval && !state.intervalError && state.intervalInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_interval_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (state.isSavingInterval) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(id = R.string.ads_save_button))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdSwitchCard(
    title: String,
    fieldKey: String,
    icon: ImageVector,
    checked: Boolean,
    isSaving: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (checked) colorScheme.primaryContainer
                        else colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = fieldKey,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = !isSaving,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
