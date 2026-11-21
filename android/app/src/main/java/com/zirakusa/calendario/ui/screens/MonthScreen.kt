package com.zirakusa.calendario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calendario.R
import com.zirakusa.calendario.data.Calendar
import com.zirakusa.calendario.ui.CalState
import com.zirakusa.calendario.ui.theme.CalColors
import java.time.DayOfWeek
import java.time.LocalDate

/** «Mes»: the month's grid (swipe for the next or previous), then the chosen day's holiday and events. */
@Composable fun MonthScreen(state: CalState) = Column {
    val l = locale()
    Title(monthName(state.month.month, l), state.month.year.toString())
    Column(Modifier.swipe(state::shiftMonth)) {
        WeekdayRow(state)
        val month = state.month
        val busy = (state.between(month.atDay(1), month.atEndOfMonth()).flatMap { o -> (0..o.event.extraDays).map { o.day.plusDays(it) } } +
            state.holidaysBetween(month.atDay(1), month.atEndOfMonth()).map { it.first }).toSet()
        Calendar.weeks(month, state.firstDay).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.weekNumbers) Text(week.firstNotNullOf { it }.let { Calendar.weekNumber(it) }.toString(), Modifier.width(24.dp),
                    fontSize = 11.sp, color = CalColors.Muted, textAlign = TextAlign.Center)
                week.forEach { day -> DayCell(day, state, day in busy, Modifier.weight(1f)) }
            }
        }
    }
    Text(dayLine(state.selected, l) + if (state.selected == state.now) ", ${stringResource(R.string.today_lower)}" else "",
        Modifier.padding(start = 28.dp, top = 16.dp, bottom = 8.dp), color = CalColors.Muted, fontSize = 17.sp)
    DayList(state.holidayOn(state.selected), state.on(state.selected), state.selected, state.h24) { state.edit(it.event) }
}

/** «lun mar mié…» in the phone's order, the weekend dimmed, over a thin line. */
@Composable fun WeekdayRow(state: CalState) {
    val l = locale()
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (state.weekNumbers) Box(Modifier.width(24.dp))
        com.zirakusa.calendario.data.Calendar.week(state.selected, state.firstDay).forEach { d ->
            Text(dayShort(d.dayOfWeek, l), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 15.sp,
                color = if (weekend(d.dayOfWeek)) CalColors.Muted else CalColors.Text)
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = CalColors.Chip)
}

fun weekend(d: DayOfWeek) = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY

/** One day number: today in a filled accent circle, the chosen day in grey, a dot when something is on. */
@Composable fun DayCell(day: LocalDate?, state: CalState, dot: Boolean, modifier: Modifier, height: Int = 64) =
    Column(modifier.height(height.dp).then(if (day != null) Modifier.clickable { state.select(day) } else Modifier)
        .semantics { if (day != null) contentDescription = day.toString() }, horizontalAlignment = Alignment.CenterHorizontally) {
        if (day == null) return@Column
        val today = day == state.now; val chosen = day == state.selected
        Box(Modifier.padding(top = 6.dp).size(42.dp).clip(CircleShape).background(when {
            today -> CalColors.Accent; chosen -> CalColors.Chip; else -> Color.Transparent }), contentAlignment = Alignment.Center) {
            Text(day.dayOfMonth.toString(), fontSize = 21.sp, color = when {
                today -> CalColors.OnAccent; weekend(day.dayOfWeek) -> CalColors.Muted; else -> CalColors.Text })
        }
        if (dot) Box(Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(CalColors.Muted))
    }
