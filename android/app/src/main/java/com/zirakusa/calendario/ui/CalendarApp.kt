package com.zirakusa.calendario.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zirakusa.calendario.data.View
import com.zirakusa.calendario.ui.screens.AgendaScreen
import com.zirakusa.calendario.ui.screens.BottomButtons
import com.zirakusa.calendario.ui.screens.EventEditor
import com.zirakusa.calendario.ui.screens.GoToDialog
import com.zirakusa.calendario.ui.screens.Header
import com.zirakusa.calendario.ui.screens.ImportantScreen
import com.zirakusa.calendario.ui.screens.MonthScreen
import com.zirakusa.calendario.ui.screens.SearchScreen
import com.zirakusa.calendario.ui.screens.SettingsScreen
import com.zirakusa.calendario.ui.screens.WeekScreen
import com.zirakusa.calendario.ui.screens.YearScreen
import com.zirakusa.calendario.ui.theme.CalColors

/** What the screens ask of the phone (the activity gives the real ones; tests leave the defaults). */
class AppActions(
    val saved: () -> Unit = {},
    val toast: (String) -> Unit = {},
    val copy: (String) -> Unit = {},
    val openArka: () -> Unit = {},
)

/** The whole app: the chosen view under the header with «Hoy» and «+», or a page over it. */
@Composable fun CalendarApp(state: CalState, actions: AppActions = AppActions()) {
    when (state.page) {
        Page.EDITOR -> return EventEditor(state, actions.saved)
        Page.SEARCH -> return SearchScreen(state)
        Page.IMPORTANT -> return ImportantScreen(state)
        Page.SETTINGS -> return SettingsScreen(state, actions.toast, actions.openArka)
        Page.MAIN -> Unit
    }
    Box(Modifier.fillMaxSize().background(CalColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            Header(state)
            Box(Modifier.weight(1f)) {
                when (state.view) {
                    View.YEAR -> YearScreen(state)
                    View.MONTH -> Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) { MonthScreen(state) }
                    View.WEEK -> WeekScreen(state)
                    View.AGENDA -> AgendaScreen(state)
                }
            }
        }
        BottomButtons(Modifier.align(Alignment.BottomCenter), if (state.view == View.YEAR) null else state::goToday) { state.newEvent() }
    }
    if (state.goingTo) GoToDialog(state)
}
