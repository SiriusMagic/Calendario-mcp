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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zirakusa.calendario.R
import com.zirakusa.calendario.data.Calendar
import com.zirakusa.calendario.data.Kind
import com.zirakusa.calendario.ui.CalState
import com.zirakusa.calendario.ui.Page
import com.zirakusa.calendario.ui.theme.CalColors

/** A plain page: back button and title, then the content. */
@Composable fun PageTop(title: String, onBack: () -> Unit) = Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
    RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), size = 52.dp, onClick = onBack)
    Text(title, Modifier.padding(start = 16.dp), fontSize = 22.sp)
}

/** «Días importantes»: birthdays and anniversaries with how many days are left, or the empty picture; + adds one. */
@Composable fun ImportantScreen(state: CalState) = Box(Modifier.fillMaxSize().background(CalColors.Background).systemBarsPadding()) {
    val l = locale()
    Column(Modifier.fillMaxSize()) {
        PageTop(stringResource(R.string.important_days)) { state.page = Page.MAIN }
        val list = state.important.map { it to Calendar.daysUntil(it, state.now) }.sortedBy { it.second ?: Long.MAX_VALUE }
        if (list.isEmpty()) Empty(stringResource(R.string.no_important), Modifier.padding(top = 160.dp))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(list, key = { it.first.id }) { (e, days) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(CalColors.Card)
                    .clickable { state.returnToImportant = true; state.edit(e) }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(eventTitle(e), fontSize = 19.sp)
                        Text(dayLine(e.firstDay, l).substringAfter(", "), color = CalColors.Muted, fontSize = 15.sp)
                    }
                    Text(when (days) { null -> ""; 0L -> stringResource(R.string.today); else -> pluralStringResource(R.plurals.days_left, days.toInt(), days) },
                        color = Violet, fontSize = 17.sp)
                }
            }
        }
    }
    BottomButtons(Modifier.align(Alignment.BottomCenter), null) { state.returnToImportant = true; state.newEvent(Kind.IMPORTANT) }
}

/** «Buscar»: the field at the bottom with back; results by title, place or notes. */
@Composable fun SearchScreen(state: CalState) = Column(Modifier.fillMaxSize().background(CalColors.Background).systemBarsPadding().imePadding()) {
    var query by rememberSaveable { mutableStateOf("") }
    val l = locale()
    val found = remember(query, state.events) { Calendar.search(state.events, query) }
    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp)) {
        if (query.isNotBlank() && found.isEmpty()) item { Empty(stringResource(R.string.no_results)) }
        items(found, key = { it.id }) { e ->
            Column(Modifier.fillMaxWidth().clickable { state.select(e.firstDay); state.edit(e) }.padding(vertical = 12.dp)) {
                Text(eventTitle(e), fontSize = 19.sp)
                Text(listOf(dayFull(e.firstDay, l), e.location).filter { it.isNotBlank() }.joinToString(" · "), color = CalColors.Muted, fontSize = 15.sp)
            }
        }
    }
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), size = 52.dp) { state.page = Page.MAIN }
        TextField(query, { query = it.take(60) }, Modifier.weight(1f).padding(start = 12.dp).clip(CircleShape), singleLine = true,
            placeholder = { Text(stringResource(R.string.search)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
            colors = TextFieldDefaults.colors(focusedContainerColor = CalColors.Card, unfocusedContainerColor = CalColors.Card,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = CalColors.Accent))
    }
}

/** «Ir a»: the full date on top, month/day/year wheels, Cancelar and Aceptar. */
@Composable fun GoToDialog(state: CalState) = Dialog({ state.goingTo = false }) {
    var date by remember { mutableStateOf(state.selected) }
    Column(Modifier.clip(RoundedCornerShape(32.dp)).background(CalColors.Card).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(dayFull(date, locale()), fontSize = 20.sp)
        DateWheels(date) { date = it }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton({ state.goingTo = false }, Modifier.weight(1f).clip(CircleShape).background(CalColors.Chip)) { Text(stringResource(R.string.cancel), color = CalColors.Text) }
            TextButton({ state.select(date); state.goingTo = false }, Modifier.weight(1f).clip(CircleShape).background(CalColors.Accent)) {
                Text(stringResource(R.string.accept), color = CalColors.OnAccent)
            }
        }
    }
}
