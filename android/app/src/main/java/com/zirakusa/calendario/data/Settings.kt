package com.zirakusa.calendario.data

import androidx.core.content.edit
import java.time.DayOfWeek

/** Where the settings live: the phone's preferences in the app, a map in the tests. */
interface KeyValues {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

class MapValues(private val map: MutableMap<String, String> = mutableMapOf()) : KeyValues {
    override fun get(key: String) = map[key]
    override fun put(key: String, value: String?) { if (value == null) map.remove(key) else map[key] = value }
}

/** «Primer día de la semana»: the region's, or a fixed one. */
enum class FirstDay(val day: DayOfWeek?) { REGION(null), MONDAY(DayOfWeek.MONDAY), SUNDAY(DayOfWeek.SUNDAY), SATURDAY(DayOfWeek.SATURDAY) }

/** The four ways to look at the calendar. */
enum class View { YEAR, MONTH, WEEK, AGENDA }

/** Every setting of Calendario, typed. Unknown or broken values fall back to the defaults. */
class Settings(private val kv: KeyValues) {
    private fun flag(key: String, default: Boolean) = kv.get(key)?.let { it == "1" } ?: default
    private fun setFlag(key: String, on: Boolean) = kv.put(key, if (on) "1" else "0")

    var firstDay: FirstDay get() = kv.get("first_day")?.let { runCatching { FirstDay.valueOf(it) }.getOrNull() } ?: FirstDay.REGION
        set(v) = kv.put("first_day", v.name)
    var weekNumbers: Boolean get() = flag("week_numbers", false); set(v) = setFlag("week_numbers", v)
    /** «Fiestas nacionales»: Ecuador's holidays on the calendar. */
    var holidays: Boolean get() = flag("holidays", true); set(v) = setFlag("holidays", v)
    var reminders: Boolean get() = flag("reminders", true); set(v) = setFlag("reminders", v)
    var view: View get() = kv.get("view")?.let { runCatching { View.valueOf(it) }.getOrNull() } ?: View.MONTH; set(v) = kv.put("view", v.name)
    /** The last reminder already shown (local time, ISO), so none is shown twice. */
    var reminded: String? get() = kv.get("reminded"); set(v) = kv.put("reminded", v)
    var theme: String? get() = kv.get("theme"); set(v) = kv.put("theme", v)
    var accent: String? get() = kv.get("accent"); set(v) = kv.put("accent", v)
}

/** The phone's preferences file «calendario» (kept out of backups). The reminder receiver and the MCP tools read the same. */
class SharedValues(context: android.content.Context) : KeyValues {
    private val prefs = context.applicationContext.getSharedPreferences("calendario", android.content.Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String?) {
        prefs.edit(commit = true) { if (value == null) remove(key) else putString(key, value) }
    }
}
