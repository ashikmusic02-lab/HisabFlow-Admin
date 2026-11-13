package com.example.ui.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyStat
import com.example.data.model.DashboardData
import com.example.data.model.UiState
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: UiState<DashboardData>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_pull_to_refresh")
    ) {
        when (uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
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
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.testTag("dashboard_retry_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(id = R.string.retry))
                            }
                        }
                    }
                }
            }

            is UiState.Success -> {
                DashboardContent(data = uiState.data)
            }
        }
    }
}

@Composable
private fun DashboardContent(data: DashboardData) {
    val numberFormat = NumberFormat.getIntegerInstance(Locale.ENGLISH)
    val todayUtc = data.last7Days.lastOrNull()?.dateUtc.orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatCard(
                        title = stringResource(id = R.string.dashboard_total_users),
                        value = numberFormat.format(data.totalInstalls),
                        subtitle = "stats/total",
                        icon = Icons.Rounded.People,
                        isPrimary = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_users")
                    )
                    KpiStatCard(
                        title = stringResource(id = R.string.dashboard_active_today),
                        value = numberFormat.format(data.todayActive),
                        subtitle = if (todayUtc.isNotEmpty()) "$todayUtc UTC" else "UTC",
                        icon = Icons.Rounded.Bolt,
                        isPrimary = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_active_today")
                    )
                }
            }

            item {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.dashboard_approx_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                WeeklyActiveChartCard(
                    stats = data.last7Days,
                    numberFormat = numberFormat
                )
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val gradient = if (isPrimary) {
        Brush.linearGradient(
            colors = listOf(colorScheme.primary, colorScheme.primary.copy(alpha = 0.85f))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(colorScheme.secondaryContainer, colorScheme.tertiaryContainer.copy(alpha = 0.7f))
        )
    }
    val contentColor = if (isPrimary) colorScheme.onPrimary else colorScheme.onSecondaryContainer

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPrimary) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor.copy(alpha = 0.9f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                color = contentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun WeeklyActiveChartCard(
    stats: List<DailyStat>,
    numberFormat: NumberFormat
) {
    val colorScheme = MaterialTheme.colorScheme
    val maxVal = (stats.maxOfOrNull { it.active } ?: 0L).coerceAtLeast(1L)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_chart_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_last_7_days),
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface
            )

            // Visual Bar Chart
            if (stats.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        stats.forEach { day ->
                            Text(
                                text = numberFormat.format(day.active),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    val primaryColor = colorScheme.primary
                    val todayColor = colorScheme.tertiary
                    val gridColor = colorScheme.outline.copy(alpha = 0.35f)
                    val trackColor = colorScheme.surfaceVariant.copy(alpha = 0.55f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                            val y = size.height * (1f - fraction)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dash
                            )
                        }

                        val slotWidth = size.width / stats.size
                        val barWidth = (slotWidth * 0.46f).coerceAtMost(28.dp.toPx())
                        val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                        stats.forEachIndexed { index, item ->
                            val left = slotWidth * index + (slotWidth - barWidth) / 2f
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(left, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = corner
                            )
                            val ratio = (item.active.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                            val barHeight = if (item.active > 0L) {
                                (size.height * ratio).coerceAtLeast(8.dp.toPx())
                            } else {
                                3.dp.toPx()
                            }
                            val barColor = if (index == stats.lastIndex) todayColor else primaryColor
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = corner
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        stats.forEachIndexed { index, day ->
                            Text(
                                text = day.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (index == stats.lastIndex) FontWeight.Bold else FontWeight.Medium,
                                color = if (index == stats.lastIndex) {
                                    colorScheme.primary
                                } else {
                                    colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.asReversed().forEachIndexed { idx, day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (idx == 0) {
                                    colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (idx == 0) "${day.dateUtc} (আজ)" else day.dateUtc,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (idx == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = numberFormat.format(day.active),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (idx == 0) colorScheme.primary else colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
