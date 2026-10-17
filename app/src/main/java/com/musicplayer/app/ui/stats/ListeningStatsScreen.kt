package com.musicplayer.app.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.LocalExtraStrings
import com.musicplayer.app.ui.i18n.LocalStrings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ListeningStatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListeningStatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 20.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = null)
            }
            Text(
                text = extra.listeningStats,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = extra.totalListening,
                        value = formatDuration(state.totalMs, strings.rankingHours),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = extra.activeDays,
                        value = extra.dayCountFormat.format(state.activeDays),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.listeningRankings,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatsRankingPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = state.rankingPeriod == period,
                                onClick = { viewModel.selectRankingPeriod(period) },
                                label = {
                                    Text(
                                        when (period) {
                                            StatsRankingPeriod.DAILY -> strings.daily
                                            StatsRankingPeriod.WEEKLY -> strings.weekly
                                            StatsRankingPeriod.MONTHLY -> strings.monthly
                                            StatsRankingPeriod.YEARLY -> strings.yearly
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
            if (state.ranking.isEmpty()) {
                item {
                    Text(
                        text = strings.noRankingData,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(
                    items = state.ranking,
                    key = { _, ranked -> ranked.song.id }
                ) { index, ranked ->
                    RankedSongRow(
                        rank = index + 1,
                        song = ranked.song,
                        totalMs = ranked.totalMs,
                        hoursLabel = strings.rankingHours
                    )
                }
            }

            item {
                CalendarSection(
                    state = state,
                    onPreviousMonth = { viewModel.selectMonth(state.month.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(state.month.plusMonths(1)) },
                    onSelectDate = viewModel::selectDate
                )
            }

            item {
                CurveSection(curve = state.curve)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun RankedSongRow(
    rank: Int,
    song: Song,
    totalMs: Long,
    hoursLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            color = if (rank <= 3) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatDuration(totalMs, hoursLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarSection(
    state: ListeningStatsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val extra = LocalExtraStrings.current
    val strings = LocalStrings.current
    val locale = Locale.getDefault()
    val monthFormatter = DateTimeFormatter.ofPattern("yyyy MMM", locale)
    val dayFormatter = DateTimeFormatter.ofPattern("MMM d", locale)
    val today = LocalDate.now()
    val maxMs = state.monthDays.maxOfOrNull { it.durationMs }?.coerceAtLeast(1L) ?: 1L
    val selectedDay = state.monthDays.firstOrNull { it.date == state.selectedDate }
    val cells: List<DayListening?> = buildList {
        repeat(state.month.atDay(1).dayOfWeek.value - 1) { add(null) }
        addAll(state.monthDays)
    }
    val weeks = cells.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = extra.listeningCalendar,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = null)
            }
            Text(
                text = state.month.format(monthFormatter),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            weeks.forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            CalendarDayCell(
                                day = day,
                                maxMs = maxMs,
                                isToday = day.date == today,
                                isSelected = day.date == state.selectedDate,
                                onClick = { onSelectDate(day.date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Text(
            text = state.selectedDate?.let { date ->
                extra.selectedDayFormat.format(
                    date.format(dayFormatter),
                    formatDuration(selectedDay?.durationMs ?: 0L, strings.rankingHours)
                )
            } ?: extra.selectedDayFormat.format(
                state.month.format(monthFormatter),
                formatDuration(state.monthTotalMs, strings.rankingHours)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarDayCell(
    day: DayListening,
    maxMs: Long,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intensity = if (day.durationMs <= 0L) 0f
    else 0.18f + 0.6f * (day.durationMs.toFloat() / maxMs.toFloat())
    val container = if (day.durationMs > 0L) {
        MaterialTheme.colorScheme.primary.copy(alpha = intensity)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val border = when {
        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        else -> null
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = container,
        border = border
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium
            )
            if (day.durationMs > 0L) {
                Text(
                    text = "${(day.durationMs / 60_000L).coerceAtLeast(1)}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CurveSection(curve: List<DayListening>) {
    val extra = LocalExtraStrings.current
    val strings = LocalStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxMs = curve.maxOfOrNull { it.durationMs } ?: 0L
    val dayFormatter = DateTimeFormatter.ofPattern("MM-dd", Locale.getDefault())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = extra.listeningCurve,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = extra.last30Days,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (maxMs <= 0L) {
            Text(
                text = extra.noListeningData,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val safeMax = maxMs.coerceAtLeast(1L)
                val topPadding = 8.dp.toPx()
                val usableHeight = size.height - topPadding
                val stepX = if (curve.size > 1) size.width / (curve.size - 1) else size.width
                fun x(index: Int): Float = index * stepX
                fun y(ms: Long): Float = size.height - (ms.toFloat() / safeMax) * usableHeight

                drawLine(
                    color = gridColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                val fillPath = Path().apply {
                    moveTo(0f, size.height)
                    curve.forEachIndexed { index, day -> lineTo(x(index), y(day.durationMs)) }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0.35f), Color.Transparent)
                    )
                )

                val linePath = Path().apply {
                    curve.forEachIndexed { index, day ->
                        if (index == 0) moveTo(x(index), y(day.durationMs))
                        else lineTo(x(index), y(day.durationMs))
                    }
                }
                drawPath(
                    path = linePath,
                    color = primary,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            if (curve.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = curve.first().date.format(dayFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = strings.rankingHours.let { formatDuration(maxMs, it) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = curve.last().date.format(dayFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long, hoursLabel: String): String {
    val hours = ms / 3_600_000L
    val minutes = (ms % 3_600_000L) / 60_000L
    return if (hours > 0) "$hours$hoursLabel ${minutes}min" else "${minutes}min"
}
