package com.zirakusa.calendario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calendario.R
import com.zirakusa.calendario.data.Calendar
import com.zirakusa.calendario.ui.CalState
import com.zirakusa.calendario.ui.theme.CalColors
import java.time.LocalTime

private const val HOUR = 64

/** «Semana»: the week's days (swipe for the next), the all-day row and the hours with each event as a block. */
@Composable fun WeekScreen(state: CalState) = Column(Modifier.fillMaxSize()) {
    val l = locale()
    Title(monthName(state.month.month, l), state.month.year.toString())
    val days = Calendar.week(state.selected, state.firstDay)
    Column(Modifier.swipe(state::shiftWeek)) {
        Row(Modifier.fillMaxWidth().padding(start = 48.dp, end = 4.dp)) {
            days.forEach { d -> Text(dayShort(d.dayOfWeek, l), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 15.sp,
                color = if (weekend(d.dayOfWeek)) CalColors.Muted else CalColors.Text) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 48.dp, end = 4.dp)) { days.forEach { DayCell(it, state, false, Modifier.weight(1f), 60) } }
    }
    val occ = state.between(days.first(), days.last())
    HorizontalDivider(color = CalColors.Chip)
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp)) {
        Text(stringResource(R.string.all_day), Modifier.width(48.dp).padding(4.dp), fontSize = 11.sp, color = CalColors.Muted, textAlign = TextAlign.Center)
        days.forEach { d ->
            Column(Modifier.weight(1f).padding(1.dp)) {
                state.holidayOn(d)?.let { Chip(holidayName(it), Amber) {} }
                occ.filter { it.event.allDay && it.covers(d) }.forEach { o -> Chip(eventTitle(o.event), barColor(o)) { state.edit(o.event) } }
            }
        }
    }
    HorizontalDivider(color = CalColors.Chip)
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState(HOUR * 7 * 2))) {
        Column {
            (0..23).forEach { h ->
                Row(Modifier.height(HOUR.dp)) {
                    Text(if (h == 0) "" else timeText(LocalTime.of(h, 0), state.h24).replace(":00", ""), Modifier.width(48.dp), fontSize = 11.sp,
                        color = CalColors.Muted, textAlign = TextAlign.Center)
                    Box(Modifier.fillMaxWidth().padding(top = 0.dp)) { HorizontalDivider(color = CalColors.Chip) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 48.dp)) {
            days.forEach { d ->
                Box(Modifier.weight(1f).height((HOUR * 24).dp)) {
                    occ.filter { !it.event.allDay && it.covers(d) }.forEach { o ->
                        val from = if (o.day == d) o.start.toLocalTime() else LocalTime.MIDNIGHT
                        val to = if (o.end.toLocalDate() == d) o.end.toLocalTime() else LocalTime.MAX
                        val top = (from.toSecondOfDay() / 3600f * HOUR).dp
                        val tall = ((to.toSecondOfDay() - from.toSecondOfDay()).coerceAtLeast(1800) / 3600f * HOUR).dp
                        Box(Modifier.offset(y = top).height(tall).fillMaxWidth().padding(1.dp).clip(RoundedCornerShape(6.dp))
                            .background(barColor(o).copy(alpha = 0.35f)).clickable { state.edit(o.event) }.padding(3.dp)) {
                            Text(eventTitle(o.event), fontSize = 11.sp, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Chip(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) =
    Text(text, Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.35f))
        .clickable(onClick = onClick).padding(horizontal = 3.dp, vertical = 2.dp), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
