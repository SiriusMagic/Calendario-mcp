package com.zirakusa.calendario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calendario.R
import com.zirakusa.calendario.data.Event
import com.zirakusa.calendario.data.Kind
import com.zirakusa.calendario.data.Recur
import com.zirakusa.calendario.ui.CalState
import com.zirakusa.calendario.ui.theme.CalColors
import java.time.LocalDateTime

/** «Nuevo» / «Editar»: Evento or Día importante, title, all day, start and end, Repetición, Recordatorio, place and notes. */
@Composable fun EventEditor(state: CalState, onSaved: () -> Unit) {
    val e = state.draft ?: return
    val l = locale()
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val toggle = { key: String -> open = if (open == key) null else key }
    val important = e.kind == Kind.IMPORTANT
    val am = stringResource(R.string.am); val pm = stringResource(R.string.pm)
    Column(Modifier.fillMaxSize().background(CalColors.Background).statusBarsPadding().padding(top = 16.dp)
        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(CalColors.Sheet)) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            RoundButton(Icons.Outlined.Close, stringResource(R.string.cancel), Modifier.align(Alignment.CenterStart), 52.dp, onClick = state::closeEditor)
            Text(stringResource(if (e.id == 0L) R.string.new_title else R.string.edit_title), Modifier.align(Alignment.Center), fontSize = 21.sp)
            RoundButton(Icons.Outlined.Check, stringResource(R.string.save), Modifier.align(Alignment.CenterEnd), 52.dp, on = true) { state.saveDraft(); onSaved() }
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KindChip(stringResource(R.string.kind_event), !important, CalColors.Accent) { state.changeKind(Kind.EVENT) }
            KindChip(stringResource(R.string.important_days), important, Violet) { state.changeKind(Kind.IMPORTANT) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Group { Field(e.title, stringResource(if (important) R.string.important_name else R.string.my_event), Event.TITLE_MAX) { t -> state.change { it.copy(title = t) } } }
            Group {
                if (!important) { SwitchItem(stringResource(R.string.all_day), null, e.allDay) { on -> state.change { it.copy(allDay = on, end = if (on) it.start else it.start.plusHours(1)) } }; Divider() }
                val shown = { t: LocalDateTime -> dayLong(t.toLocalDate(), l) + if (e.allDay) "" else " " + timeTextPlain(t, state.h24, am, pm) }
                Item(stringResource(if (important) R.string.date else R.string.start), shown(e.start), expand = true) { toggle("start") }
                if (open == "start") DateTimeWheels(e.start, !e.allDay, state.h24) { t ->
                    state.change { it.copy(start = t, end = t.plusMinutes(java.time.Duration.between(it.start, it.end).toMinutes())) }
                }
                if (!important) {
                    Divider()
                    Item(stringResource(R.string.end), shown(e.end), expand = true) { toggle("end") }
                    if (open == "end") DateTimeWheels(e.end, !e.allDay, state.h24) { t -> state.change { it.copy(end = t) } }
                }
            }
            Group {
                Item(stringResource(R.string.repeat), stringResource(recurNames.getValue(e.recur)), expand = true) { toggle("repeat") }
                if (open == "repeat") Recur.entries.forEach { r -> ChoiceRow(stringResource(recurNames.getValue(r)), e.recur == r) { state.change { it.copy(recur = r) }; open = null } }
                Divider()
                Item(stringResource(R.string.reminder), reminderText(e.reminder, e.allDay), expand = true) { toggle("reminder") }
                if (open == "reminder") Event.REMINDERS.forEach { m ->
                    ChoiceRow(reminderText(m, e.allDay), e.reminder == m) { state.change { it.copy(reminder = m) }; open = null }
                }
            }
            Group {
                Field(e.location, stringResource(R.string.location), Event.TITLE_MAX) { t -> state.change { it.copy(location = t) } }
                Divider()
                Field(e.notes, stringResource(R.string.notes), Event.TEXT_MAX, single = false) { t -> state.change { it.copy(notes = t) } }
            }
            if (e.id != 0L) TextButton({ state.delete(e.id) }, Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.delete_event), color = CalColors.Danger, fontSize = 17.sp)
            }
            Box(Modifier.navigationBarsPadding().padding(bottom = 24.dp))
        }
    }
}

@Composable private fun KindChip(text: String, on: Boolean, color: Color, onClick: () -> Unit) =
    Row(Modifier.clip(CircleShape).background(CalColors.Card).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            if (on) Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = Color.White)
        }
        Text(text, Modifier.padding(start = 10.dp), fontSize = 17.sp)
    }

fun timeTextPlain(t: LocalDateTime, h24: Boolean, am: String, pm: String): String =
    if (h24) "%02d:%02d".format(t.hour, t.minute) else "%d:%02d %s".format((t.hour % 12).let { if (it == 0) 12 else it }, t.minute, if (t.hour < 12) am else pm)
