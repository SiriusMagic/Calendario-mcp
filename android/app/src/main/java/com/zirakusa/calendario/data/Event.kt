package com.zirakusa.calendario.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/** «Evento» or «Días importantes» (a birthday, an anniversary: all day, every year by default). */
enum class Kind(val key: String) {
    EVENT("event"), IMPORTANT("important");
    companion object { fun from(key: String?) = entries.firstOrNull { it.key == key } ?: EVENT }
}

/** «Repetición». */
enum class Recur(val key: String) {
    NONE("none"), DAILY("daily"), WEEKLY("weekly"), MONTHLY("monthly"), YEARLY("yearly");
    companion object { fun from(key: String?) = entries.firstOrNull { it.key == key } }
}

/**
 * One event. A timed event runs from [start] to [end]; an all-day one covers the days from [start] to [end] (both
 * included, times ignored). [reminder] is minutes before the start (all-day events count from 9:00 of the day), or
 * [NO_REMINDER].
 */
data class Event(
    val id: Long = 0, val title: String = "", val kind: Kind = Kind.EVENT, val allDay: Boolean = false,
    val start: LocalDateTime, val end: LocalDateTime = start.plusHours(1), val recur: Recur = Recur.NONE,
    val reminder: Int = 10, val location: String = "", val notes: String = "",
) {
    val firstDay: LocalDate get() = start.toLocalDate()
    /** Days it covers each time it happens, minus one (0 = a single day). */
    val extraDays: Long get() {
        val last = if (allDay) end.toLocalDate() else end.minusNanos(1).toLocalDate()
        return (last.toEpochDay() - firstDay.toEpochDay()).coerceAtLeast(0)
    }

    fun json(): JSONObject = JSONObject().put("id", id).put("title", title).put("kind", kind.key).put("allDay", allDay)
        .put("start", start.toString()).put("end", end.toString()).put("recur", recur.key).put("reminder", reminder)
        .put("location", location).put("notes", notes)

    companion object {
        const val NO_REMINDER = -1
        const val TITLE_MAX = 100
        const val TEXT_MAX = 2000
        /** «Recordatorio» choices, in minutes before. */
        val REMINDERS = listOf(NO_REMINDER, 0, 5, 10, 15, 30, 60, 1440)
        /** All-day events are reminded from this hour of their day. */
        const val ALL_DAY_HOUR = 9

        fun parse(o: JSONObject): Event? = runCatching {
            val start = LocalDateTime.parse(o.getString("start"))
            val end = LocalDateTime.parse(o.getString("end")).let { if (it.isBefore(start)) start else it }
            Event(o.getLong("id"), o.optString("title").take(TITLE_MAX), Kind.from(o.optString("kind")), o.optBoolean("allDay"), start, end,
                Recur.from(o.optString("recur")) ?: Recur.NONE, o.optInt("reminder", 10), o.optString("location").take(TITLE_MAX),
                o.optString("notes").take(TEXT_MAX))
        }.getOrNull()

        /** A new «Días importantes»: all day, every year, reminded that morning. */
        fun important(day: LocalDate, title: String = "") = Event(title = title, kind = Kind.IMPORTANT, allDay = true,
            start = day.atStartOfDay(), end = day.atStartOfDay(), recur = Recur.YEARLY, reminder = 0)
    }
}

/** The events, as JSON in the preferences. */
class EventStore(private val kv: KeyValues) {
    fun list(): List<Event> = runCatching {
        val a = JSONArray(kv.get(KEY) ?: "[]")
        (0 until a.length()).mapNotNull { Event.parse(a.getJSONObject(it)) }
    }.getOrDefault(emptyList()).sortedWith(compareBy({ it.start }, { it.id }))

    fun get(id: Long) = list().firstOrNull { it.id == id }

    /** Adds ([Event.id] 0 gets a new id) or replaces it. */
    fun save(event: Event): Event {
        val all = list()
        val saved = if (event.id != 0L) event else event.copy(id = (all.maxOfOrNull { it.id } ?: 0) + 1)
        write(all.filterNot { it.id == saved.id } + saved)
        return saved
    }

    fun delete(id: Long): Boolean {
        val all = list()
        if (all.none { it.id == id }) return false
        write(all.filterNot { it.id == id }); return true
    }

    /** «Borrar todos los eventos caducados»: one-off events that ended before [today]. Returns how many. */
    fun deleteExpired(today: LocalDate): Int {
        val all = list()
        val keep = all.filterNot { it.recur == Recur.NONE && it.firstDay.plusDays(it.extraDays).isBefore(today) }
        write(keep); return all.size - keep.size
    }

    private fun write(list: List<Event>) = kv.put(KEY, JSONArray(list.map { it.json() }).toString())

    private companion object { const val KEY = "events" }
}
