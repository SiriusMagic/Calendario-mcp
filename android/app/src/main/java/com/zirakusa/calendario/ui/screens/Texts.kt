package com.zirakusa.calendario.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.zirakusa.calendario.R
import com.zirakusa.calendario.data.Event
import com.zirakusa.calendario.data.Holiday
import com.zirakusa.calendario.data.Occurrence
import com.zirakusa.calendario.data.Recur
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The phone's language. */
@Composable fun locale(): Locale = LocalConfiguration.current.locales[0]

private fun cap(s: String) = s.replaceFirstChar { it.titlecase() }
fun dayShort(day: DayOfWeek, l: Locale) = day.getDisplayName(TextStyle.SHORT, l).trimEnd('.')
fun dayLetter(day: DayOfWeek, l: Locale) = day.getDisplayName(TextStyle.NARROW, l).uppercase(l)
fun monthName(m: Month, l: Locale) = cap(m.getDisplayName(TextStyle.FULL_STANDALONE, l))
fun monthShort(m: Month, l: Locale) = cap(m.getDisplayName(TextStyle.SHORT_STANDALONE, l).trimEnd('.'))
/** «Sáb, 21 nov». */
fun dayLine(d: LocalDate, l: Locale) = cap(DateTimeFormatter.ofPattern("EEE, d MMM", l).format(d).replace(".", ""))
/** «sábado, 21 de noviembre». */
fun dayLong(d: LocalDate, l: Locale): String = DateTimeFormatter.ofPattern(if (l.language == "es") "EEEE, d 'de' MMMM" else "EEEE, MMMM d", l).format(d)
/** «lunes, 1 de noviembre de 2027». */
fun dayFull(d: LocalDate, l: Locale): String = DateTimeFormatter.ofPattern(if (l.language == "es") "EEEE, d 'de' MMMM 'de' yyyy" else "EEEE, MMMM d, yyyy", l).format(d)

@Composable fun timeText(t: LocalTime, h24: Boolean): String =
    if (h24) "%02d:%02d".format(t.hour, t.minute)
    else "%d:%02d %s".format((t.hour % 12).let { if (it == 0) 12 else it }, t.minute, stringResource(if (t.hour < 12) R.string.am else R.string.pm))

/** «9:00 a. m. - 10:00 a. m.», «Todo el día». */
@Composable fun spanText(o: Occurrence, h24: Boolean, day: LocalDate = o.day): String = when {
    o.event.allDay -> stringResource(R.string.all_day)
    o.start.toLocalDate() == o.end.toLocalDate() || day == o.day -> "${timeText(o.start.toLocalTime(), h24)} - ${timeText(o.end.toLocalTime(), h24)}"
    else -> stringResource(R.string.until, timeText(o.end.toLocalTime(), h24))
}

@Composable fun holidayName(h: Holiday) = stringResource(when (h) {
    Holiday.NEW_YEAR -> R.string.h_new_year; Holiday.CARNIVAL -> R.string.h_carnival; Holiday.GOOD_FRIDAY -> R.string.h_good_friday
    Holiday.LABOUR -> R.string.h_labour; Holiday.PICHINCHA -> R.string.h_pichincha; Holiday.INDEPENDENCE -> R.string.h_independence
    Holiday.GUAYAQUIL -> R.string.h_guayaquil; Holiday.SOULS -> R.string.h_souls; Holiday.CUENCA -> R.string.h_cuenca; Holiday.CHRISTMAS -> R.string.h_christmas
})

val recurNames = mapOf(Recur.NONE to R.string.recur_none, Recur.DAILY to R.string.recur_daily, Recur.WEEKLY to R.string.recur_weekly,
    Recur.MONTHLY to R.string.recur_monthly, Recur.YEARLY to R.string.recur_yearly)

/** «10 min antes», «1 h antes», «1 día antes», «A la hora del evento», «Ninguno»; all day: «El mismo día (9:00)». */
@Composable fun reminderText(minutes: Int, allDay: Boolean): String = when {
    minutes < 0 -> stringResource(R.string.reminder_none)
    minutes == 0 -> stringResource(if (allDay) R.string.reminder_morning else R.string.reminder_at)
    minutes % 1440 == 0 -> pluralStringResource(R.plurals.reminder_days, minutes / 1440, minutes / 1440)
    minutes % 60 == 0 -> stringResource(R.string.reminder_hours, minutes / 60)
    else -> stringResource(R.string.reminder_minutes, minutes)
}

@Composable fun eventTitle(e: Event) = e.title.ifBlank { stringResource(R.string.no_title) }
