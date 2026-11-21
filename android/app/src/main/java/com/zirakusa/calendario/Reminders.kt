package com.zirakusa.calendario

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zirakusa.calendario.data.EventStore
import com.zirakusa.calendario.data.Occurrence
import com.zirakusa.calendario.data.Remind
import com.zirakusa.calendario.data.Settings
import com.zirakusa.calendario.data.SharedValues
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * «Recordatorio»: one alarm is set for the next reminder; when it fires, every reminder due since the last one is shown
 * as a notification and the next is set. Planned again after every change, at boot and when the clock or zone change.
 */
object Reminders {
    private const val CHANNEL = "reminders"
    private const val ACTION = "com.zirakusa.calendario.action.REMIND"

    @Synchronized fun sync(context: Context, fire: Boolean = false) {
        val app = context.applicationContext
        val values = SharedValues(app)
        val settings = Settings(values)
        val events = EventStore(values).list()
        val now = LocalDateTime.now()
        val last = settings.reminded?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }?.takeIf { it.isBefore(now) } ?: now
        if (fire && settings.reminders) Remind.due(events, last.minusSeconds(1), now).forEach { (o, _) -> show(app, o) }
        settings.reminded = now.toString()
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(app, 0, Intent(app, ReminderReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = if (settings.reminders) Remind.next(events, now) else null
        if (next == null) { manager.cancel(pending); return }
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        else manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    private fun show(context: Context, o: Occurrence) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH))
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(context, o.event.id.toInt(), Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DATE, o.day.toString()), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val whenText = if (o.event.allDay) DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(o.day)
            else DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).format(o.start)
        val text = listOf(whenText, o.event.location).filter { it.isNotBlank() }.joinToString(" · ")
        val n = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_stat_event)
            .setContentTitle(o.event.title.ifBlank { context.getString(R.string.no_title) }).setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_EVENT).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)
            .setContentIntent(open).build()
        runCatching { manager.notify((o.event.id * 31 + o.day.toEpochDay()).toInt(), n) }
    }
}

/** A reminder is due: show it and set the next. The clock changed or the phone started: plan again. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.sync(context, fire = intent.action == "com.zirakusa.calendario.action.REMIND")
    }
}
