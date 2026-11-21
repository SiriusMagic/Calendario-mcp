package com.zirakusa.calendario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calendario.R
import com.zirakusa.calendario.data.Calendar
import com.zirakusa.calendario.data.Holiday
import com.zirakusa.calendario.data.Occurrence
import com.zirakusa.calendario.ui.CalState
import com.zirakusa.calendario.ui.theme.CalColors
import java.time.LocalDate
import java.time.YearMonth

/** «Año»: the twelve months small, three a row; today marked; tap a month to open it; swipe for another year. */
@Composable fun YearScreen(state: CalState) = Column(Modifier.fillMaxSize().swipe(state::shiftYear).verticalScroll(rememberScrollState())) {
    val l = locale()
    val year = state.selected.year
    Title(year.toString(), null)
    (1..12).chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { m ->
                val month = YearMonth.of(year, m)
                val current = month == YearMonth.from(state.now)
                Column(Modifier.weight(1f).clickable { state.openMonth(month) }) {
                    Text(monthShort(month.month, l), Modifier.padding(start = 4.dp, bottom = 4.dp), fontSize = 21.sp, fontWeight = FontWeight.Medium,
                        color = if (current) CalColors.AccentText else CalColors.Text)
                    Row { Calendar.week(month.atDay(1), state.firstDay).forEach { d ->
                        Text(dayLetter(d.dayOfWeek, l), Modifier.weight(1f), fontSize = 9.sp, color = CalColors.Muted, textAlign = TextAlign.Center) } }
                    Calendar.weeks(month, state.firstDay).forEach { week ->
                        Row { week.forEach { d ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (d != null) Text(d.dayOfMonth.toString(), Modifier.size(17.dp).clip(CircleShape)
                                    .background(if (d == state.now) CalColors.Accent else Color.Transparent), fontSize = 10.sp, textAlign = TextAlign.Center,
                                    color = if (d == state.now) CalColors.OnAccent else CalColors.Text)
                            }
                        } }
                    }
                }
            }
        }
    }
    Box(Modifier.padding(bottom = 96.dp))
}

/** «Agenda»: the week strip, then every event and holiday from the chosen day on (a year ahead), by date. */
@Composable fun AgendaScreen(state: CalState) = Column(Modifier.fillMaxSize()) {
    val l = locale()
    Title(monthName(state.month.month, l), state.month.year.toString())
    Column(Modifier.swipe(state::shiftWeek)) {
        WeekdayRow(state)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Calendar.week(state.selected, state.firstDay).forEach { d -> DayCell(d, state, state.on(d).isNotEmpty() || state.holidayOn(d) != null, Modifier.weight(1f)) }
        }
    }
    val from = state.selected; val to = from.plusDays(365)
    val items: List<Pair<LocalDate, Any>> = (state.between(from, to).map { (if (it.day.isBefore(from)) from else it.day) to it } +
        state.holidaysBetween(from, to).map { it.first to it.second }).sortedBy { it.first }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        if (items.none { it.first == from }) item { AgendaLine(from, true, l, state.now) { Text(stringResource(R.string.no_events), color = CalColors.Muted, fontSize = 18.sp) } }
        var year = from.year
        items.forEachIndexed { i, (day, what) ->
            if (day.year != year) { year = day.year
                item { HorizontalDivider(Modifier.padding(vertical = 12.dp), color = CalColors.Chip); Text(year.toString(), color = CalColors.Muted, fontSize = 18.sp) } }
            item {
                AgendaLine(day, i == 0 || items[i - 1].first != day, l, state.now) {
                    when (what) {
                        is Holiday -> HolidayRow(what)
                        is Occurrence -> EventRow(what, state.h24) { state.edit(what.event) }
                    }
                }
            }
        }
        item { Box(Modifier.padding(bottom = 96.dp)) }
    }
}

@Composable private fun AgendaLine(day: LocalDate, showDate: Boolean, l: java.util.Locale, today: LocalDate, content: @Composable () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.width(88.dp).padding(top = 10.dp)) {
            if (showDate) {
                Text(monthShort(day.month, l) + " " + day.dayOfMonth, fontSize = 17.sp, color = if (day == today) CalColors.AccentText else CalColors.Text)
                Text(dayShort(day.dayOfWeek, l), fontSize = 17.sp, color = if (day == today) CalColors.AccentText else CalColors.Text)
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
