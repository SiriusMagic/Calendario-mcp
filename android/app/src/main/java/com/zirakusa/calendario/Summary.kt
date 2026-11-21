package com.zirakusa.calendario

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import com.zirakusa.calendario.data.Calendar
import com.zirakusa.calendario.data.EventStore
import com.zirakusa.calendario.data.Holiday
import com.zirakusa.calendario.data.Holidays
import com.zirakusa.calendario.data.Occurrence
import com.zirakusa.calendario.data.Settings
import com.zirakusa.calendario.data.SharedValues
import java.time.LocalDate

/** The lines of «Hoy» for the family summary: the holiday, then each event («09:00 Reunión», «Todo el día · Viaje»). Pure. */
object SummaryLines {
    const val MAX = 5
    fun today(items: List<Occurrence>, day: LocalDate, holiday: String?, allDay: String, untitled: String, time: (Int, Int) -> String): List<String> =
        (listOfNotNull(holiday) + items.map { o ->
            val title = o.event.title.ifBlank { untitled }
            if (o.event.allDay || o.day != day) "$allDay · $title" else "${time(o.start.hour, o.start.minute)} $title"
        }).take(MAX)
}

/**
 * The family summary (see Inicio's FAMILIA.md): what Inicio shows in its «Hoy» card for Calendario. Only apps signed
 * by the same developer may ask (Android's signature permission and the caller's signature checked here).
 */
class SummaryProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "summary") return null
        val context = context ?: return null
        val caller = callingPackage ?: return null
        if (context.packageManager.checkSignatures(context.packageName, caller) != PackageManager.SIGNATURE_MATCH) return null
        val values = SharedValues(context)
        val today = LocalDate.now()
        val holiday = Holidays.between(Settings(values).holidays, today, today).firstOrNull()?.second?.let { context.getString(holidayName(it)) }
        val h24 = DateFormat.is24HourFormat(context)
        val lines = SummaryLines.today(Calendar.on(EventStore(values).list(), today), today, holiday, context.getString(R.string.all_day),
            context.getString(R.string.no_title)) { h, m ->
            if (h24) "%02d:%02d".format(h, m) else "%d:%02d %s".format((h % 12).let { if (it == 0) 12 else it }, m, context.getString(if (h < 12) R.string.am else R.string.pm))
        }
        return Bundle().apply {
            putString("title", context.getString(R.string.summary_title))
            putStringArrayList("lines", ArrayList(lines))
            putString("empty", context.getString(R.string.no_events))
        }
    }
    private fun holidayName(h: Holiday) = when (h) {
        Holiday.NEW_YEAR -> R.string.h_new_year; Holiday.CARNIVAL -> R.string.h_carnival; Holiday.GOOD_FRIDAY -> R.string.h_good_friday
        Holiday.LABOUR -> R.string.h_labour; Holiday.PICHINCHA -> R.string.h_pichincha; Holiday.INDEPENDENCE -> R.string.h_independence
        Holiday.GUAYAQUIL -> R.string.h_guayaquil; Holiday.SOULS -> R.string.h_souls; Holiday.CUENCA -> R.string.h_cuenca; Holiday.CHRISTMAS -> R.string.h_christmas
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
