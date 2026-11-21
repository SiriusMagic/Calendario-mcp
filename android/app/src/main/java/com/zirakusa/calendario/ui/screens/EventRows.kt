package com.zirakusa.calendario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calendario.R
import com.zirakusa.calendario.data.Holiday
import com.zirakusa.calendario.data.Kind
import com.zirakusa.calendario.data.Occurrence
import com.zirakusa.calendario.ui.theme.CalColors
import java.time.LocalDate

/** The colour bar of each kind: events in the accent, «Días importantes» violet, holidays amber. */
val Violet = Color(0xFFC77DFF)
val Amber = Color(0xFFF2B36B)
fun barColor(o: Occurrence) = if (o.event.kind == Kind.IMPORTANT) Violet else CalColors.Accent

/** One event: the coloured bar, its title and when (or that it is a «Día importante»). */
@Composable fun EventRow(o: Occurrence, h24: Boolean, day: LocalDate = o.day, onClick: () -> Unit) =
    Line(barColor(o), eventTitle(o.event), if (o.event.kind == Kind.IMPORTANT) stringResource(R.string.important_day) else spanText(o, h24, day),
        if (o.event.kind == Kind.IMPORTANT) Icons.Outlined.Cake else Icons.Outlined.Schedule, onClick)

@Composable fun HolidayRow(h: Holiday) = Line(Amber, holidayName(h), stringResource(R.string.holidays_ecuador), Icons.Outlined.Bookmark, null)

@Composable private fun Line(bar: Color, title: String, sub: String, icon: ImageVector, onClick: (() -> Unit)?) =
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp)) {
        Box(Modifier.width(3.dp).height(52.dp).clip(RoundedCornerShape(2.dp)).background(bar))
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, fontSize = 19.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp), tint = CalColors.Muted)
                Text(sub, Modifier.padding(start = 6.dp), color = CalColors.Muted, fontSize = 15.sp)
            }
        }
    }

/** A day's holiday and events, or «No hay eventos para mostrar» with the empty calendar picture. */
@Composable fun DayList(holiday: Holiday?, items: List<Occurrence>, day: LocalDate, h24: Boolean, onOpen: (Occurrence) -> Unit) =
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
        holiday?.let { HolidayRow(it) }
        items.forEach { o -> EventRow(o, h24, day) { onOpen(o) } }
        if (holiday == null && items.isEmpty()) Empty(stringResource(R.string.no_events))
    }

@Composable fun Empty(text: String, modifier: Modifier = Modifier) = Column(modifier.fillMaxWidth().padding(vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Box(Modifier.size(96.dp).clip(CircleShape).background(CalColors.Card), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.EventAvailable, null, Modifier.size(52.dp), tint = CalColors.Muted)
    }
    Text(text, color = CalColors.Muted, fontSize = 17.sp, textAlign = TextAlign.Center)
}

/** Swipe left or right: [onSwipe] gets +1 (next) or -1 (previous). */
fun Modifier.swipe(onSwipe: (Long) -> Unit): Modifier = pointerInput(Unit) {
    var total = 0f
    detectHorizontalDragGestures(onDragStart = { total = 0f }, onDragEnd = {
        if (total < -80.dp.toPx()) onSwipe(1) else if (total > 80.dp.toPx()) onSwipe(-1)
    }) { _, dx -> total += dx }
}
