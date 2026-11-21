package com.zirakusa.calendario.mcp

import com.zirakusa.calendario.data.Calendar
import com.zirakusa.calendario.data.Event
import com.zirakusa.calendario.data.Holidays
import com.zirakusa.calendario.data.Kind
import com.zirakusa.calendario.data.Occurrence
import com.zirakusa.calendario.data.Recur
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** What the tools read and change; the app gives the real events (and reschedules reminders), tests a fake. */
interface CalendarData {
    fun events(): List<Event>
    /** Adds ([Event.id] 0) or replaces an event; returns it with its id. */
    fun save(event: Event): Event
    fun delete(id: Long): Boolean
    /** Whether Ecuador's holidays are shown. */
    fun holidays(): Boolean
    fun today(): LocalDate = LocalDate.now()
}

/** An expected failure the AI can read (structuredContent.error.code), never a crash. */
class ToolFailure(val code: String, message: String) : Exception(message)

/**
 * Calendario's MCP tools: read events, days and holidays; with a read-and-write token also create and delete events.
 * Arguments are checked one by one; nothing is ever run as code.
 */
object CalendarTools {
    class Tool(val name: String, val description: String, val write: Boolean, val schema: JSONObject)

    const val MAX_DAYS = 366L

    private fun schema(required: List<String>, vararg props: Pair<String, JSONObject>) = JSONObject()
        .put("type", "object").put("properties", JSONObject().apply { props.forEach { (k, v) -> put(k, v) } })
        .put("required", JSONArray(required)).put("additionalProperties", false)
    private fun prop(type: String, description: String) = JSONObject().put("type", type).put("description", description)

    val tools = listOf(
        Tool("calendar_events", "Events between two dates (both included, up to $MAX_DAYS days), repeating events expanded, by start; " +
            "optionally only those whose title, place or notes contain «query».", false,
            schema(listOf("from", "to"), "from" to prop("string", "YYYY-MM-DD."), "to" to prop("string", "YYYY-MM-DD."),
                "query" to prop("string", "Words to look for (accents ignored)."))),
        Tool("calendar_day", "One day: its events and the national holiday, if any.", false,
            schema(listOf("date"), "date" to prop("string", "YYYY-MM-DD."))),
        Tool("calendar_holidays", "Ecuador's national holidays of a year (empty when the user hid them).", false,
            schema(listOf("year"), "year" to prop("integer", "1900–2200."))),
        Tool("calendar_event_create", "Creates an event. Without «end» it lasts an hour (all day: that day). «important» makes it a " +
            "«Día importante» (birthday, anniversary): all day and every year.", true,
            schema(listOf("title", "date"), "title" to prop("string", "Up to ${Event.TITLE_MAX} characters."),
                "date" to prop("string", "YYYY-MM-DD, the first day."), "time" to prop("string", "HH:MM start, 24 h; omit for all day."),
                "end_date" to prop("string", "YYYY-MM-DD, the last day (default the first)."), "end_time" to prop("string", "HH:MM end."),
                "repeat" to JSONObject().put("type", "string").put("enum", JSONArray(Recur.entries.map { it.key })).put("description", "Default none."),
                "reminder_minutes" to prop("integer", "Minutes before (0–10080); -1 = none. Default 10 (all day: that morning)."),
                "location" to prop("string", "Place."), "notes" to prop("string", "Notes."),
                "important" to prop("boolean", "A «Día importante»."))),
        Tool("calendar_event_delete", "Deletes an event (every repetition).", true,
            schema(listOf("id"), "id" to prop("integer", "The event's id (from calendar_events)."))),
    )

    fun available(access: Access) = tools.filter { !it.write || access == Access.WRITE }

    fun call(name: String, args: JSONObject, access: Access, data: CalendarData): JSONObject {
        val tool = tools.firstOrNull { it.name == name } ?: throw ToolFailure("UNKNOWN_TOOL", "No tool called «$name».")
        if (tool.write && access != Access.WRITE) throw ToolFailure("READ_ONLY", "This token can only read. Create a read-and-write token in Calendario.")
        val props = tool.schema.getJSONObject("properties")
        args.keys().forEach { if (!props.has(it)) throw ToolFailure("BAD_ARGUMENT", "Unknown argument «$it».") }
        val required = tool.schema.getJSONArray("required")
        (0 until required.length()).map { required.getString(it) }.forEach { if (!args.has(it)) throw ToolFailure("BAD_ARGUMENT", "Missing «$it».") }
        return when (name) {
            "calendar_events" -> events(args, data)
            "calendar_day" -> day(date(args, "date")!!, data)
            "calendar_holidays" -> {
                val year = (args.opt("year") as? Int)?.takeIf { it in 1900..2200 } ?: throw ToolFailure("BAD_ARGUMENT", "year must be an integer from 1900 to 2200.")
                JSONObject().put("year", year).put("holidays", JSONArray(Holidays.between(data.holidays(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                    .map { JSONObject().put("date", it.first.toString()).put("holiday", it.second.name.lowercase()) }))
            }
            "calendar_event_create" -> JSONObject().put("event", describe(data.save(create(args)), null))
            else -> {
                val id = (args.opt("id") as? Number)?.takeIf { it is Int || it is Long }?.toLong() ?: throw ToolFailure("BAD_ARGUMENT", "id must be an integer.")
                if (!data.delete(id)) throw ToolFailure("NOT_FOUND", "No event with id $id.")
                JSONObject().put("deleted", id)
            }
        }
    }

    private fun date(args: JSONObject, key: String): LocalDate? = if (!args.has(key)) null
        else (args.opt(key) as? String)?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() } ?: throw ToolFailure("BAD_ARGUMENT", "$key must be YYYY-MM-DD.")
    private fun time(args: JSONObject, key: String): LocalTime? = if (!args.has(key)) null
        else (args.opt(key) as? String)?.let { Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$").matchEntire(it.trim()) }
            ?.let { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) } ?: throw ToolFailure("BAD_ARGUMENT", "$key must be HH:MM (24 h).")
    private fun text(args: JSONObject, key: String, max: Int): String = if (!args.has(key)) ""
        else (args.opt(key) as? String)?.trim()?.takeIf { it.length <= max } ?: throw ToolFailure("BAD_ARGUMENT", "$key must be text of up to $max characters.")

    private fun events(args: JSONObject, data: CalendarData): JSONObject {
        val from = date(args, "from")!!; val to = date(args, "to")!!
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) throw ToolFailure("BAD_ARGUMENT", "to must be on or after from, at most $MAX_DAYS days.")
        val query = text(args, "query", Event.TITLE_MAX)
        val events = if (query.isBlank()) data.events() else Calendar.search(data.events(), query)
        val found = Calendar.between(events, from, to)
        return JSONObject().put("total", found.size).put("events", JSONArray(found.take(500).map { describe(it.event, it) }))
    }

    private fun day(date: LocalDate, data: CalendarData): JSONObject {
        val holiday = Holidays.between(data.holidays(), date, date).firstOrNull()
        return JSONObject().put("date", date.toString()).put("weekday", date.dayOfWeek.name.lowercase())
            .put("holiday", holiday?.second?.name?.lowercase() ?: JSONObject.NULL)
            .put("events", JSONArray(Calendar.on(data.events(), date).map { describe(it.event, it) }))
    }

    private fun create(args: JSONObject): Event {
        val title = text(args, "title", Event.TITLE_MAX).ifBlank { throw ToolFailure("BAD_ARGUMENT", "title must not be empty.") }
        val day = date(args, "date")!!
        val important = if (!args.has("important")) false else args.opt("important") as? Boolean ?: throw ToolFailure("BAD_ARGUMENT", "important must be true or false.")
        val start = time(args, "time"); val endTime = time(args, "end_time")
        val endDay = date(args, "end_date") ?: day
        val recur = if (!args.has("repeat")) null else Recur.from(args.opt("repeat") as? String)
            ?: throw ToolFailure("BAD_ARGUMENT", "repeat must be ${Recur.entries.joinToString { it.key }}.")
        val reminder = if (!args.has("reminder_minutes")) null else (args.opt("reminder_minutes") as? Int)?.takeIf { it == -1 || it in 0..10080 }
            ?: throw ToolFailure("BAD_ARGUMENT", "reminder_minutes must be -1 or 0–10080.")
        val location = text(args, "location", Event.TITLE_MAX); val notes = text(args, "notes", Event.TEXT_MAX)
        if (important) return Event.important(day, title).copy(reminder = reminder ?: 0, location = location, notes = notes)
        val allDay = start == null
        val begin = day.atTime(start ?: LocalTime.MIDNIGHT)
        val end: LocalDateTime = when {
            allDay -> endDay.atStartOfDay()
            endTime != null -> endDay.atTime(endTime)
            else -> begin.plusHours(1).let { if (endDay.isAfter(day)) endDay.atTime(start) else it }
        }
        if (end.isBefore(begin) || (!allDay && end == begin && endTime != null)) throw ToolFailure("BAD_ARGUMENT", "The end must be after the start.")
        return Event(title = title, allDay = allDay, start = begin, end = end, recur = recur ?: Recur.NONE,
            reminder = reminder ?: if (allDay) 0 else 10, location = location, notes = notes)
    }

    private fun describe(e: Event, o: Occurrence?): JSONObject {
        val start = o?.start ?: e.start; val end = o?.end ?: e.end
        return JSONObject().put("id", e.id).put("title", e.title).put("kind", if (e.kind == Kind.IMPORTANT) "important" else "event")
            .put("all_day", e.allDay).put("start", if (e.allDay) start.toLocalDate().toString() else start.toString())
            .put("end", if (e.allDay) start.toLocalDate().plusDays(e.extraDays).toString() else end.toString())
            .put("repeat", e.recur.key).put("reminder_minutes", e.reminder).put("location", e.location).put("notes", e.notes)
    }
}
