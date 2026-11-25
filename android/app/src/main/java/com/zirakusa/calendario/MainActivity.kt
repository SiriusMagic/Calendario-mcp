package com.zirakusa.calendario

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.zirakusa.calendario.data.SharedValues
import com.zirakusa.calendario.data.View
import com.zirakusa.calendario.mcp.McpMode
import com.zirakusa.calendario.ui.AppActions
import com.zirakusa.calendario.ui.CalState
import com.zirakusa.calendario.ui.CalendarApp
import com.zirakusa.calendario.ui.Page
import com.zirakusa.calendario.ui.theme.CalColors
import com.zirakusa.calendario.ui.theme.CalTheme
import java.time.LocalDate
import java.time.temporal.WeekFields

/** Creates the state, plans the reminders, opens the day of a reminder and does what the screens ask. */
class MainActivity : ComponentActivity() {
    private lateinit var state: CalState
    private var asked = 0
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { asked++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppIcon.load(this)
        enableEdgeToEdge()
        val region = WeekFields.of(resources.configuration.locales[0]).firstDayOfWeek
        state = CalState(SharedValues(this), regionFirstDay = region, onChanged = { Reminders.sync(this) })
        McpMode.onChanged = { state.reload() }
        McpMode.load(this)
        Reminders.sync(this)
        open(intent)
        val actions = AppActions(saved = ::askNotifications, toast = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }, openArka = ::openArka)
        setContent {
            CalTheme(state.themeMode, state.accent) {
                val light = CalColors.isLight
                SideEffect { WindowCompat.getInsetsController(window, window.decorView).run { isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light } }
                BackHandler(state.page != Page.MAIN || state.showViews || state.view == View.YEAR, ::back)
                CalendarApp(state, actions)
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); open(intent) }

    override fun onResume() {
        super.onResume()
        state.h24 = DateFormat.is24HourFormat(this)
        state.refreshToday()
        state.reload()
    }

    /** A reminder's notification opens its day. */
    private fun open(intent: Intent?) {
        intent?.getStringExtra(EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let { state.select(it); state.page = Page.MAIN }
    }

    private fun back() = when {
        state.page == Page.EDITOR -> state.closeEditor()
        state.page != Page.MAIN -> state.page = Page.MAIN
        state.showViews -> state.showViews = false
        else -> state.changeView(View.MONTH)
    }

    /** After saving an event: ask for notifications (Android 13+), which carry the reminders. */
    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && asked < 2 && state.reminders &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** «Modo MCP» in Ajustes: it is turned on, allowed on the phone and given tokens in Arka, so this opens Arka. */
    private fun openArka() {
        val open = packageManager.getLaunchIntentForPackage("com.zirakusa.gatewayhub")
        if (open != null) startActivity(open) else Toast.makeText(this, R.string.mcp_no_arka, Toast.LENGTH_LONG).show()
    }

    companion object { const val EXTRA_DATE = "date" }
}
