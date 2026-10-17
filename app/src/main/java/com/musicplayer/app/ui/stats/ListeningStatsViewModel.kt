package com.musicplayer.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.toDomain
import com.musicplayer.app.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StatsRankingPeriod { DAILY, WEEKLY, MONTHLY, YEARLY }

data class RankedSong(
    val song: Song,
    val totalMs: Long
)

data class DayListening(
    val date: LocalDate,
    val durationMs: Long,
    val playCount: Int
)

data class ListeningStatsUiState(
    val month: YearMonth = YearMonth.now(),
    val monthDays: List<DayListening> = emptyList(),
    val monthTotalMs: Long = 0L,
    val activeDays: Int = 0,
    val curve: List<DayListening> = emptyList(),
    val selectedDate: LocalDate? = null,
    val rankingPeriod: StatsRankingPeriod = StatsRankingPeriod.WEEKLY,
    val ranking: List<RankedSong> = emptyList(),
    val totalMs: Long = 0L,
    val loading: Boolean = true
) {
    val selectedDay: DayListening?
        get() = monthDays.firstOrNull { it.date == selectedDate && it.durationMs > 0L }
}

@HiltViewModel
class ListeningStatsViewModel @Inject constructor(
    private val playRecordDao: PlayRecordDao,
    private val songDao: SongDao
) : ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val _uiState = MutableStateFlow(ListeningStatsUiState())
    val uiState: StateFlow<ListeningStatsUiState> = _uiState.asStateFlow()

    fun selectMonth(month: YearMonth) {
        load(month, _uiState.value.rankingPeriod, _uiState.value.selectedDate)
    }

    fun selectRankingPeriod(period: StatsRankingPeriod) {
        load(_uiState.value.month, period, _uiState.value.selectedDate)
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    private fun load(
        month: YearMonth,
        period: StatsRankingPeriod,
        selectedDate: LocalDate?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val today = LocalDate.now()
            val computed = withContext(Dispatchers.IO) {
                val songs = songDao.getSongs().map { it.toDomain() }
                val songById = songs.associateBy { it.id }

                val monthStart = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val monthRecords = playRecordDao.recordsBetween(monthStart, monthEnd)
                val monthByDate = monthRecords.groupBy { it.listenedAtEpochMs.toLocalDate() }
                val monthDays = buildList {
                    for (day in 1..month.lengthOfMonth()) {
                        val date = month.atDay(day)
                        val records = monthByDate[date].orEmpty()
                        add(
                            DayListening(
                                date = date,
                                durationMs = records.sumOf { it.durationMs },
                                playCount = records.size
                            )
                        )
                    }
                }

                val curveStart = today.minusDays(29)
                val curveStartMs = curveStart.atStartOfDay(zone).toInstant().toEpochMilli()
                val tomorrowMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val curveByDate = playRecordDao.recordsBetween(curveStartMs, tomorrowMs)
                    .groupBy { it.listenedAtEpochMs.toLocalDate() }
                val curve = (0 until 30).map { offset ->
                    val date = curveStart.plusDays(offset.toLong())
                    val records = curveByDate[date].orEmpty()
                    DayListening(
                        date = date,
                        durationMs = records.sumOf { it.durationMs },
                        playCount = records.size
                    )
                }

                val periodStart = when (period) {
                    StatsRankingPeriod.DAILY -> today
                    StatsRankingPeriod.WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    StatsRankingPeriod.MONTHLY -> today.withDayOfMonth(1)
                    StatsRankingPeriod.YEARLY -> today.withDayOfYear(1)
                }
                val periodStartMs = periodStart.atStartOfDay(zone).toInstant().toEpochMilli()
                val ranking = playRecordDao.topSongsSince(periodStartMs, limit = 50)
                    .mapNotNull { count -> songById[count.songId]?.let { RankedSong(it, count.totalMs) } }

                val totalMs = playRecordDao.topSongsSince(0L, limit = 100_000).sumOf { it.totalMs }

                Computed(
                    monthDays = monthDays,
                    curve = curve,
                    ranking = ranking,
                    totalMs = totalMs
                )
            }
            _uiState.update {
                it.copy(
                    month = month,
                    monthDays = computed.monthDays,
                    monthTotalMs = computed.monthDays.sumOf { day -> day.durationMs },
                    activeDays = computed.monthDays.count { day -> day.durationMs > 0L },
                    curve = computed.curve,
                    selectedDate = selectedDate?.takeIf { date -> YearMonth.from(date) == month },
                    rankingPeriod = period,
                    ranking = computed.ranking,
                    totalMs = computed.totalMs,
                    loading = false
                )
            }
        }
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    private data class Computed(
        val monthDays: List<DayListening>,
        val curve: List<DayListening>,
        val ranking: List<RankedSong>,
        val totalMs: Long
    )

    init {
        load(YearMonth.now(), StatsRankingPeriod.WEEKLY, LocalDate.now())
    }
}
