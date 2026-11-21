package com.zirakusa.calendario.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calendario.R
import com.zirakusa.calendario.ui.theme.CalColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@Composable fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = CalColors.Chip)

/** A borderless text field inside a card («Mi evento», «Ubicación», «Comentar»). */
@Composable fun Field(value: String, hint: String, max: Int, single: Boolean = true, onChange: (String) -> Unit) =
    TextField(value, { onChange(it.take(max)) }, Modifier.fillMaxWidth(), singleLine = single, minLines = if (single) 1 else 2,
        placeholder = { Text(hint, fontSize = 19.sp, color = CalColors.Muted) }, textStyle = TextStyle(fontSize = 19.sp, color = CalColors.Text),
        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = CalColors.Accent))

/** Month, day and year wheels (a five-year range around the date). */
@Composable fun DateWheels(date: LocalDate, onChange: (LocalDate) -> Unit) {
    val l = locale()
    val year0 = date.year - 2
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Wheel(12, date.monthValue - 1, { m -> onChange(date.withDayOfMonth(1).withMonth(m + 1).let { it.withDayOfMonth(date.dayOfMonth.coerceAtMost(it.lengthOfMonth())) }) },
            Modifier.weight(1f), fontSize = 26.sp, row = 46.dp, name = stringResource(R.string.month)) { monthShort(Month.of(it + 1), l) }
        Wheel(date.lengthOfMonth(), date.dayOfMonth - 1, { d -> onChange(date.withDayOfMonth(d + 1)) }, Modifier.weight(1f), fontSize = 26.sp, row = 46.dp,
            name = stringResource(R.string.day)) { (it + 1).toString() }
        Wheel(5, 2, { y -> onChange(date.withYear(year0 + y).let { if (date.monthValue == 2 && date.dayOfMonth == 29 && !it.isLeapYear) it.withDayOfMonth(28) else it }) },
            Modifier.weight(1f), cyclic = false, fontSize = 26.sp, row = 46.dp, name = stringResource(R.string.year)) { (year0 + it).toString() }
    }
}

/** The date wheels and, for a timed event, hour and minute (and a.m./p.m.). */
@Composable fun DateTimeWheels(value: LocalDateTime, withTime: Boolean, h24: Boolean, onChange: (LocalDateTime) -> Unit) = Column {
    DateWheels(value.toLocalDate()) { onChange(it.atTime(value.toLocalTime())) }
    if (withTime) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        if (h24) Wheel(24, value.hour, { h -> onChange(value.withHour(h)) }, Modifier.weight(1f), fontSize = 26.sp, row = 46.dp,
            name = stringResource(R.string.hour)) { "%02d".format(it) }
        else Wheel(12, (value.hour % 12).let { if (it == 0) 11 else it - 1 }, { i -> onChange(value.withHour((i + 1) % 12 + if (value.hour >= 12) 12 else 0)) },
            Modifier.weight(1f), fontSize = 26.sp, row = 46.dp, name = stringResource(R.string.hour)) { (it + 1).toString() }
        Wheel(12, value.minute / 5, { m -> onChange(value.withMinute(m * 5)) }, Modifier.weight(1f), fontSize = 26.sp, row = 46.dp,
            name = stringResource(R.string.minute)) { "%02d".format(it * 5) }
        if (!h24) {
            val am = stringResource(R.string.am); val pm = stringResource(R.string.pm)
            Wheel(2, if (value.hour < 12) 0 else 1, { i -> onChange(value.withHour(value.hour % 12 + 12 * i)) }, Modifier.weight(1f), cyclic = false,
                fontSize = 22.sp, row = 46.dp) { if (it == 0) am else pm }
        }
    }
}
