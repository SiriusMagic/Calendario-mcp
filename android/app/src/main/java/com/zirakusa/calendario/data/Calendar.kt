package com.zirakusa.calendario.data

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields

/** One time an event happens: the day it starts and its start and end then. */
data class Occurrence(val event: Event, val start: LocalDateTime, val end: LocalDateTime) {
    val day: LocalDate get() = start.toLocalDate()
    val lastDay: LocalDate get() = day.plusDays(event.extraDays)
    fun covers(date: LocalDate) = !date.isBefore(day) && !date.isAfter(lastDay)
}

/** When events happen and how the months and weeks are laid out. Pure. */
object Calendar {
    /** Whether a repetition of [e] starts on [day]. Monthly on the 31st skips shorter months; yearly on 29 Feb, other years. */
    fun startsOn(e: Event, day: LocalDate): Boolean {
        val first = e.firstDay
        if (day.isBefore(first)) return false
        return when (e.recur) {
            Recur.NONE -> day == first
            Recur.DAILY -> true
            Recur.WEEKLY -> day.dayOfWeek == first.dayOfWeek
            Recur.MONTHLY -> day.dayOfMonth == first.dayOfMonth
            Recur.YEARLY -> day.dayOfMonth == first.dayOfMonth && day.month == first.month
        }
    }

    /** Every occurrence that touches the days [from]..[to] (both included), by start. */
    fun between(events: List<Event>, from: LocalDate, to: LocalDate): List<Occurrence> {
        if (to.isBefore(from)) return emptyList()
        val length = { e: Event -> ChronoUnit.MINUTES.between(e.start, e.end) }
        return events.flatMap { e ->
            var day = maxOf(e.firstDay, from.minusDays(e.extraDays))
            val out = ArrayList<Occurrence>()
            while (!day.isAfter(to)) {
                if (startsOn(e, day)) {
                    val s = day.atTime(e.start.toLocalTime())
                    out += Occurrence(e, s, s.plusMinutes(length(e)))
                }
                day = if (e.recur == Recur.NONE) to.plusDays(1) else day.plusDays(1)
            }
            out
        }.sortedWith(compareBy<Occurrence>({ it.day }, { !it.event.allDay }, { it.start }, { it.event.id }))
    }

    /** What is on [date]: events that cover it (all-day first, then by time). */
    fun on(events: List<Event>, date: LocalDate) = between(events, date, date).filter { it.covers(date) }

    /** The days of [month] in weeks starting on [firstDay]; null where the week has days of other months. */
    fun weeks(month: YearMonth, firstDay: DayOfWeek): List<List<LocalDate?>> {
        val lead = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
        val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        return cells.chunked(7).map { it + List(7 - it.size) { null } }
    }

    /** The week (7 days) that holds [day]. */
    fun week(day: LocalDate, firstDay: DayOfWeek): List<LocalDate> {
        val start = day.minusDays(((day.dayOfWeek.value - firstDay.value + 7) % 7).toLong())
        return (0L until 7L).map { start.plusDays(it) }
    }

    /** ISO week number («Mostrar los números de la semana»). */
    fun weekNumber(day: LocalDate) = day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    /** Days from [today] to the next time [e] happens (0 = today), for «Días importantes»; null if never again. */
    fun daysUntil(e: Event, today: LocalDate): Long? =
        between(listOf(e), today, today.plusDays(400)).firstOrNull { !it.day.isBefore(today) }?.let { ChronoUnit.DAYS.between(today, it.day) }

    /** Without accents or case: «reunion» finds «Reunión». */
    fun fold(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()

    /** «Buscar»: events whose title, place or notes contain [query]. */
    fun search(events: List<Event>, query: String): List<Event> {
        val q = fold(query)
        if (q.isEmpty()) return emptyList()
        return events.filter { fold(it.title).contains(q) || fold(it.location).contains(q) || fold(it.notes).contains(q) }
    }
}

/** Ecuador's national holidays (Ajustes › Fiestas nacionales): dates of the law, Carnaval and Viernes Santo from Easter. */
enum class Holiday { NEW_YEAR, CARNIVAL, GOOD_FRIDAY, LABOUR, PICHINCHA, INDEPENDENCE, GUAYAQUIL, SOULS, CUENCA, CHRISTMAS }

object Holidays {
    fun ecuador(year: Int): List<Pair<LocalDate, Holiday>> {
        val easter = easter(year)
        return listOf(
            LocalDate.of(year, 1, 1) to Holiday.NEW_YEAR, easter.minusDays(48) to Holiday.CARNIVAL, easter.minusDays(47) to Holiday.CARNIVAL,
            easter.minusDays(2) to Holiday.GOOD_FRIDAY, LocalDate.of(year, 5, 1) to Holiday.LABOUR, LocalDate.of(year, 5, 24) to Holiday.PICHINCHA,
            LocalDate.of(year, 8, 10) to Holiday.INDEPENDENCE, LocalDate.of(year, 10, 9) to Holiday.GUAYAQUIL, LocalDate.of(year, 11, 2) to Holiday.SOULS,
            LocalDate.of(year, 11, 3) to Holiday.CUENCA, LocalDate.of(year, 12, 25) to Holiday.CHRISTMAS,
        )
    }

    /** The holidays between two days, when they are shown. */
    fun between(on: Boolean, from: LocalDate, to: LocalDate): List<Pair<LocalDate, Holiday>> = if (!on) emptyList()
        else (from.year..to.year).flatMap(::ecuador).filter { !it.first.isBefore(from) && !it.first.isAfter(to) }

    /** Easter Sunday (Gregorian, anonymous algorithm). */
    fun easter(year: Int): LocalDate {
        val a = year % 19; val b = year / 100; val c = year % 100
        val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30; val i = c / 4; val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31; val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }
}

/** When reminders are due. Pure. */
object Remind {
    /** The moment [o] is reminded, or null when it has no reminder. */
    fun at(o: Occurrence): LocalDateTime? {
        val e = o.event
        if (e.reminder < 0) return null
        val base = if (e.allDay) o.day.atTime(Event.ALL_DAY_HOUR, 0) else o.start
        return base.minusMinutes(e.reminder.toLong())
    }

    /** Reminders due after [after] and up to [upTo] (both as the phone's local time), in order. */
    fun due(events: List<Event>, after: LocalDateTime, upTo: LocalDateTime): List<Pair<Occurrence, LocalDateTime>> =
        Calendar.between(events, after.toLocalDate(), upTo.toLocalDate().plusDays(2))
            .mapNotNull { o -> at(o)?.let { o to it } }.filter { it.second.isAfter(after) && !it.second.isAfter(upTo) }.sortedBy { it.second }

    /** The next reminder after [after], looking a year ahead. */
    fun next(events: List<Event>, after: LocalDateTime): LocalDateTime? = due(events, after, after.plusDays(370)).firstOrNull()?.second
}
