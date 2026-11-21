package com.zirakusa.calendario.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.calendario.data.Calendar
import com.zirakusa.calendario.data.Event
import com.zirakusa.calendario.data.EventStore
import com.zirakusa.calendario.data.FirstDay
import com.zirakusa.calendario.data.Holiday
import com.zirakusa.calendario.data.Holidays
import com.zirakusa.calendario.data.KeyValues
import com.zirakusa.calendario.data.Kind
import com.zirakusa.calendario.data.Occurrence
import com.zirakusa.calendario.data.Settings
import com.zirakusa.calendario.data.View
import com.zirakusa.calendario.ui.theme.Accent
import com.zirakusa.calendario.ui.theme.ThemeMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Pages over the calendar. */
enum class Page { MAIN, EDITOR, SEARCH, IMPORTANT, SETTINGS, MCP }

/**
 * The only owner of what the screens show: the view, the day chosen, the events and the one being edited, and the
 * settings. [today] gives the date (fixed in tests); [regionFirstDay] is the phone's first day of the week; [onChanged]
 * plans the reminders again. The screens only read and call.
 */
class CalState(values: KeyValues, private val today: () -> LocalDate = LocalDate::now, private val regionFirstDay: DayOfWeek = DayOfWeek.MONDAY,
    private val onChanged: () -> Unit = {}) {
    private val settings = Settings(values)
    private val store = EventStore(values)

    var page by mutableStateOf(Page.MAIN)
    var view by mutableStateOf(settings.view); private set
    /** The row of views (Año, Mes, Semana, Agenda) under the top buttons. */
    var showViews by mutableStateOf(false)
    var selected by mutableStateOf(today()); private set
    var now by mutableStateOf(today()); private set
    var h24 by mutableStateOf(false)
    var goingTo by mutableStateOf(false)

    var events by mutableStateOf(store.list()); private set
    var draft by mutableStateOf<Event?>(null); private set

    var firstDaySetting by mutableStateOf(settings.firstDay); private set
    var weekNumbers by mutableStateOf(settings.weekNumbers); private set
    var holidays by mutableStateOf(settings.holidays); private set
    var reminders by mutableStateOf(settings.reminders); private set
    var themeMode by mutableStateOf(settings.theme?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.BLACK); private set
    var accent by mutableStateOf(settings.accent?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.ROJO); private set

    val firstDay: DayOfWeek get() = firstDaySetting.day ?: regionFirstDay
    val month: YearMonth get() = YearMonth.from(selected)
    val important get() = events.filter { it.kind == Kind.IMPORTANT }

    fun refreshToday() { now = today() }
    fun reload() { events = store.list() }

    // Moving around
    fun changeView(next: View) { view = next; settings.view = next; showViews = false }
    fun select(day: LocalDate) { selected = day }
    fun goToday() { now = today(); selected = now }
    fun shiftMonth(by: Long) { selected = selected.plusMonths(by).let { if (it.month == now.month && it.year == now.year) now else it.withDayOfMonth(1) } }
    fun shiftWeek(by: Long) { selected = selected.plusWeeks(by) }
    fun shiftYear(by: Long) { selected = selected.plusYears(by) }
    fun openMonth(m: YearMonth) { selected = if (m == YearMonth.from(now)) now else m.atDay(1); changeView(View.MONTH) }

    // What is on
    fun on(day: LocalDate): List<Occurrence> = Calendar.on(events, day)
    fun between(from: LocalDate, to: LocalDate) = Calendar.between(events, from, to)
    fun holidaysBetween(from: LocalDate, to: LocalDate): List<Pair<LocalDate, Holiday>> = Holidays.between(holidays, from, to)
    fun holidayOn(day: LocalDate) = holidaysBetween(day, day).firstOrNull()?.second

    // «Nuevo» / editing
    fun newEvent(kind: Kind = Kind.EVENT) {
        val day = selected
        draft = if (kind == Kind.IMPORTANT) Event.important(day) else {
            val hour = if (day == now) (LocalDateTime.now().hour + 1).coerceAtMost(23) else 9
            Event(start = day.atTime(hour, 0), end = day.atTime(hour, 0).plusHours(1))
        }
        page = Page.EDITOR
    }
    fun edit(event: Event) { draft = event; page = Page.EDITOR }
    fun change(f: (Event) -> Event) { draft = draft?.let(f) }
    /** «Evento» ↔ «Días importantes», keeping the title and the day. */
    fun changeKind(kind: Kind) = change { e ->
        if (e.kind == kind) e else if (kind == Kind.IMPORTANT) Event.important(e.firstDay, e.title).copy(id = e.id, location = e.location, notes = e.notes)
        else e.copy(kind = Kind.EVENT, recur = com.zirakusa.calendario.data.Recur.NONE)
    }
    /** The editor was opened from «Días importantes», so closing it goes back there. */
    var returnToImportant = false
    fun closeEditor() { draft = null; page = if (returnToImportant) Page.IMPORTANT else Page.MAIN; returnToImportant = false }
    fun saveDraft(): Event? {
        val d = draft ?: return null
        val fixed = if (d.end.isBefore(d.start)) d.copy(end = if (d.allDay) d.start else d.start.plusHours(1)) else d
        val saved = store.save(fixed.copy(title = fixed.title.trim()))
        if (d.kind != Kind.IMPORTANT) selected = saved.firstDay
        closeEditor(); changed()
        return saved
    }
    fun delete(id: Long) { store.delete(id); if (draft?.id == id) closeEditor(); changed() }
    fun deleteExpired(): Int = store.deleteExpired(now).also { changed() }
    private fun changed() { events = store.list(); onChanged() }

    // «Ajustes»
    fun changeFirstDay(v: FirstDay) { firstDaySetting = v; settings.firstDay = v }
    fun changeWeekNumbers(on: Boolean) { weekNumbers = on; settings.weekNumbers = on }
    fun changeHolidays(on: Boolean) { holidays = on; settings.holidays = on }
    fun changeReminders(on: Boolean) { reminders = on; settings.reminders = on; onChanged() }
    fun changeTheme(mode: ThemeMode) { themeMode = mode; settings.theme = mode.name }
    fun changeAccent(next: Accent) { accent = next; settings.accent = next.name }
}
