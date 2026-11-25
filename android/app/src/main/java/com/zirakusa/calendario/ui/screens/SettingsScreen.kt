package com.zirakusa.calendario.ui.screens

import com.zirakusa.calendario.IconCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calendario.BuildConfig
import com.zirakusa.calendario.R
import com.zirakusa.calendario.data.FirstDay
import com.zirakusa.calendario.mcp.McpMode
import com.zirakusa.calendario.mcp.RelayLink
import com.zirakusa.calendario.ui.CalState
import com.zirakusa.calendario.ui.Page
import com.zirakusa.calendario.ui.theme.CalColors

/** «Ajustes»: week, holidays, reminders, clean-up, Modo MCP, Temas, privacy and version. */
@Composable fun SettingsScreen(state: CalState, toast: (String) -> Unit) {
    var privacy by remember { mutableStateOf(false) }
    var cleaning by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(CalColors.Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTop(stringResource(R.string.settings)) { state.page = Page.MAIN }
        Caption(stringResource(R.string.calendar))
        Group {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.first_day), fontSize = 18.sp)
                Segmented(listOf(R.string.first_region, R.string.first_monday, R.string.first_sunday, R.string.first_saturday).map { stringResource(it) },
                    state.firstDaySetting.ordinal, Modifier.fillMaxWidth()) { state.changeFirstDay(FirstDay.entries[it]) }
            }
            Divider()
            SwitchItem(stringResource(R.string.week_numbers), null, state.weekNumbers, onChange = state::changeWeekNumbers)
        }
        Caption(stringResource(R.string.region))
        Group { SwitchItem(stringResource(R.string.holidays), stringResource(if (state.holidays) R.string.ecuador else R.string.none), state.holidays,
            onChange = state::changeHolidays) }
        Caption(stringResource(R.string.notifications))
        Group { SwitchItem(stringResource(R.string.reminders), stringResource(R.string.reminders_hint), state.reminders, onChange = state::changeReminders) }
        Group { Item(stringResource(R.string.delete_expired), null, onClick = { cleaning = true }) }
        Group {
            val status = when {
                !McpMode.active -> stringResource(R.string.mcp_status_off)
                McpMode.hubRedirect -> stringResource(R.string.mcp_redirect)
                McpMode.state == RelayLink.State.ONLINE -> stringResource(R.string.mcp_online)
                McpMode.state == RelayLink.State.CONNECTING -> stringResource(R.string.mcp_connecting)
                else -> stringResource(R.string.mcp_offline)
            }
            Item(stringResource(R.string.mcp_title), "$status · ${stringResource(R.string.mcp_settings_hint)}") { state.page = Page.MCP }
        }
        IconCard(CalColors.Card, CalColors.Text, CalColors.Muted, CalColors.Accent)
        ThemeCard(state.themeMode, state.accent, state::changeTheme, state::changeAccent)
        Caption(stringResource(R.string.about))
        Group {
            Item(stringResource(R.string.privacy), null) { privacy = true }
            Divider()
            Item(stringResource(R.string.version_title), BuildConfig.VERSION_NAME, onClick = null)
        }
        Text("", Modifier.padding(bottom = 16.dp))
    }
    val deleted = stringResource(R.string.deleted_expired)
    if (cleaning) AlertDialog({ cleaning = false }, title = { Text(stringResource(R.string.delete_expired)) }, text = { Text(stringResource(R.string.delete_expired_text)) },
        confirmButton = { TextButton({ cleaning = false; toast(deleted.format(state.deleteExpired())) }) { Text(stringResource(R.string.delete), color = CalColors.Danger) } },
        dismissButton = { TextButton({ cleaning = false }) { Text(stringResource(R.string.cancel), color = CalColors.Text) } })
    if (privacy) AlertDialog({ privacy = false }, confirmButton = { TextButton({ privacy = false }) { Text(stringResource(R.string.done), color = CalColors.AccentText) } },
        title = { Text(stringResource(R.string.privacy)) }, text = { Text(stringResource(R.string.privacy_text)) })
}
