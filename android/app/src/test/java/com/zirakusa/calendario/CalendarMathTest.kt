package com.zirakusa.calendario

import com.zirakusa.calendario.data.Calendar
import com.zirakusa.calendario.data.Event
import com.zirakusa.calendario.data.EventStore
import com.zirakusa.calendario.data.FirstDay
import com.zirakusa.calendario.data.Holiday
import com.zirakusa.calendario.data.Holidays
import com.zirakusa.calendario.data.Kind
import com.zirakusa.calendario.data.MapValues
import com.zirakusa.calendario.data.Recur
import com.zirakusa.calendario.data.Remind
import com.zirakusa.calendario.data.Settings
import com.zirakusa.calendario.data.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** The pure rules: repetitions, ranges, month grid, holidays, reminders, search, store and settings. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarMathTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)
    private fun t(m: Int, day: Int, h: Int, min: Int = 0, y: Int = 2026) = LocalDateTime.of(y, m, day, h, min)

    @Test fun repetitions() {
        val weekly = Event(1, "Yoga", start = t(11, 2, 18), end = t(11, 2, 19), recur = Recur.WEEKLY)
        assertEquals(listOf(d(11, 2), d(11, 9), d(11, 16), d(11, 23), d(11, 30)), Calendar.between(listOf(weekly), d(11, 1), d(11, 30)).map { it.day })
        val monthly31 = Event(2, "Pago", start = t(1, 31, 9), recur = Recur.MONTHLY)
        assertEquals(listOf(1, 3, 5, 7, 8, 10, 12), Calendar.between(listOf(monthly31), d(1, 1), d(12, 31)).map { it.day.monthValue })
        val leap = Event.important(d(2, 29, 2024), "Bisiesto")
        assertEquals(listOf(2024, 2028), Calendar.between(listOf(leap), d(1, 1, 2024), d(12, 31, 2028)).map { it.day.year })
        val daily = Event(3, "Agua", start = t(11, 20, 8), recur = Recur.DAILY)
        assertEquals(11, Calendar.between(listOf(daily), d(11, 20), d(11, 30)).size)
        assertTrue(Calendar.between(listOf(daily), d(11, 1), d(11, 19)).isEmpty())
        assertFalse(Calendar.startsOn(weekly, d(10, 26)))
    }

    @Test fun spansAndDays() {
        val trip = Event(1, "Viaje", allDay = true, start = d(11, 20).atStartOfDay(), end = d(11, 23).atStartOfDay())
        val night = Event(2, "Turno", start = t(11, 21, 22), end = t(11, 22, 6))
        val midnight = Event(3, "Fiesta", start = t(11, 21, 20), end = t(11, 22, 0))
        val events = listOf(trip, night, midnight)
        assertEquals(listOf("Viaje", "Turno", "Fiesta"), Calendar.on(events, d(11, 21)).map { it.event.title }.sortedBy { listOf("Viaje", "Turno", "Fiesta").indexOf(it) })
        assertEquals(listOf("Viaje", "Turno"), Calendar.on(events, d(11, 22)).map { it.event.title })   // the party ends at 0:00
        assertEquals(listOf("Viaje"), Calendar.on(events, d(11, 23)).map { it.event.title })
        assertEquals(3, trip.extraDays); assertEquals(0, midnight.extraDays)
        assertEquals(1, Calendar.between(events, d(11, 22), d(11, 22)).count { it.event.id == 1L })            // started before the range
        val first = Calendar.on(events, d(11, 21)).first()
        assertTrue(first.event.allDay)                                                                           // all day first
    }

    @Test fun monthGridAndWeeks() {
        val nov = Calendar.weeks(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        assertEquals(6, nov.size); assertEquals(d(11, 1), nov[0][6]); assertNull(nov[0][0]); assertEquals(d(11, 30), nov[5][0])
        val sunday = Calendar.weeks(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(5, sunday.size); assertEquals(d(11, 1), sunday[0][0])
        assertEquals((16..22).map { d(11, it) }, Calendar.week(d(11, 21), DayOfWeek.MONDAY))
        assertEquals(d(11, 15), Calendar.week(d(11, 21), DayOfWeek.SUNDAY).first())
        assertEquals(47, Calendar.weekNumber(d(11, 21)))
    }

    @Test fun holidaysOfEcuador() {
        assertEquals(d(4, 5), Holidays.easter(2026))
        val h = Holidays.ecuador(2026).toMap()
        assertEquals(Holiday.CARNIVAL, h[d(2, 16)]); assertEquals(Holiday.CARNIVAL, h[d(2, 17)]); assertEquals(Holiday.GOOD_FRIDAY, h[d(4, 3)])
        assertEquals(Holiday.SOULS, h[d(11, 2)]); assertEquals(Holiday.CUENCA, h[d(11, 3)]); assertEquals(Holiday.CHRISTMAS, h[d(12, 25)])
        assertEquals(3, Holidays.between(true, d(11, 1), d(12, 31)).size)
        assertEquals(2, Holidays.between(true, d(12, 20), d(1, 5, 2027)).size)                                  // Christmas and New Year
        assertTrue(Holidays.between(false, d(1, 1), d(12, 31)).isEmpty())
    }

    @Test fun reminders() {
        val meeting = Event(1, "Reunión", start = t(11, 23, 9), reminder = 10)
        val birthday = Event.important(d(12, 2), "Cumpleaños").copy(id = 2)
        val dayBefore = Event(3, "Médico", start = t(11, 24, 15), reminder = 1440)
        val none = Event(4, "Nada", start = t(11, 23, 10), reminder = Event.NO_REMINDER)
        val all = listOf(meeting, birthday, dayBefore, none)
        assertEquals(t(11, 23, 8, 50), Remind.next(all, t(11, 21, 6)))
        val due = Remind.due(all, t(11, 23, 8, 0), t(11, 24, 16))
        assertEquals(listOf(t(11, 23, 8, 50), t(11, 23, 15)), due.map { it.second })
        assertEquals(t(12, 2, 9), Remind.next(all, t(11, 25, 0)))                                                 // birthday that morning
        assertEquals(t(12, 2, 9, y = 2027), Remind.next(listOf(birthday), t(12, 3, 0)))                           // next year
        assertNull(Remind.next(listOf(none), t(11, 1, 0)))
    }

    @Test fun storeSearchAndSettings() {
        val kv = MapValues()
        val store = EventStore(kv)
        val a = store.save(Event(title = "Reunión de trabajo", start = t(11, 23, 9), location = "Oficina", notes = "Llevar informe"))
        val b = store.save(Event(title = "Cena", start = t(1, 2, 20, y = 2025)))
        store.save(Event.important(d(5, 10, 2020), "Día de la madre"))
        assertEquals(a, EventStore(kv).get(a.id)); assertEquals(3, store.list().size)
        assertEquals(listOf(a), Calendar.search(store.list(), "reunion")); assertEquals(listOf(a), Calendar.search(store.list(), "OFICINA"))
        assertEquals(listOf(a), Calendar.search(store.list(), "informe")); assertTrue(Calendar.search(store.list(), " ").isEmpty())
        assertEquals(1, store.deleteExpired(d(11, 21)))                                     // the old dinner; the yearly day stays
        assertNull(store.get(b.id)); assertEquals(Kind.IMPORTANT, store.list().first().kind)
        assertTrue(store.delete(a.id)); assertFalse(store.delete(a.id))
        assertEquals(0L, Calendar.daysUntil(Event.important(d(11, 21, 2000)), d(11, 21)))
        assertEquals(11L, Calendar.daysUntil(Event.important(d(12, 2, 2000)), d(11, 21)))
        kv.put("events", "{"); assertTrue(store.list().isEmpty())

        val s = Settings(MapValues())
        assertEquals(FirstDay.REGION, s.firstDay); assertTrue(s.holidays); assertTrue(s.reminders); assertEquals(View.MONTH, s.view)
        s.firstDay = FirstDay.SUNDAY; s.view = View.AGENDA; assertEquals(FirstDay.SUNDAY, s.firstDay); assertEquals(View.AGENDA, s.view)
    }

    @Test fun summaryForInicio() {
        val day = d(11, 21)
        val events = listOf(Event(1, "Reunión", start = t(11, 21, 9)), Event(2, "", allDay = true, start = day.atStartOfDay(), end = day.atStartOfDay()),
            Event(3, "Turno", start = t(11, 20, 22), end = t(11, 21, 6)))
        val lines = SummaryLines.today(Calendar.on(events, day), day, "Navidad", "Todo el día", "(Sin título)") { h, m -> "%02d:%02d".format(h, m) }
        assertEquals(listOf("Navidad", "Todo el día · Turno", "Todo el día · (Sin título)", "09:00 Reunión"), lines)   // the night shift began yesterday
        assertTrue(SummaryLines.today(emptyList(), day, null, "", "") { _, _ -> "" }.isEmpty())
        assertEquals(SummaryLines.MAX, SummaryLines.today(List(9) { Calendar.on(events, day).first() }, day, null, "a", "b") { _, _ -> "" }.size)
    }
}
