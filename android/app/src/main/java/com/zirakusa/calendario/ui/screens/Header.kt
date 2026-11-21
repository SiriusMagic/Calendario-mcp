package com.zirakusa.calendario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calendario.R
import com.zirakusa.calendario.data.View
import com.zirakusa.calendario.ui.CalState
import com.zirakusa.calendario.ui.Page
import com.zirakusa.calendario.ui.theme.CalColors

/** The top: buscar, the view button (shows Año · Mes · Semana · Agenda) and ⋮ (Días importantes, Ir a, Ajustes). */
@Composable fun Header(state: CalState) = Column(Modifier.fillMaxWidth().statusBarsPadding()) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
        var menu by remember { mutableStateOf(false) }
        Row(Modifier.clip(CircleShape).background(CalColors.Card).border(1.dp, CalColors.Chip, CircleShape)) {
            Slot(Icons.Outlined.Search, stringResource(R.string.search)) { state.page = Page.SEARCH }
            Slot(Icons.Outlined.CalendarMonth, stringResource(R.string.views)) { state.showViews = !state.showViews }
            Box { Slot(Icons.Outlined.MoreVert, stringResource(R.string.more)) { menu = true }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text(stringResource(R.string.important_days)) }, { menu = false; state.page = Page.IMPORTANT })
                    DropdownMenuItem({ Text(stringResource(R.string.go_to)) }, { menu = false; state.goingTo = true })
                    DropdownMenuItem({ Text(stringResource(R.string.settings)) }, { menu = false; state.page = Page.SETTINGS })
                }
            }
        }
    }
    if (state.showViews) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(Triple(View.YEAR, Icons.Outlined.CalendarViewMonth, R.string.view_year), Triple(View.MONTH, Icons.Outlined.CalendarMonth, R.string.view_month),
            Triple(View.WEEK, Icons.Outlined.CalendarViewWeek, R.string.view_week), Triple(View.AGENDA, Icons.Outlined.EventNote, R.string.view_agenda))
            .forEach { (v, icon, label) ->
                val on = state.view == v
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(CalColors.Card).clickable { state.changeView(v) }.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, Modifier.size(28.dp), tint = if (on) CalColors.AccentText else CalColors.Muted)
                    Text(stringResource(label), fontSize = 14.sp, color = if (on) CalColors.AccentText else CalColors.Muted)
                }
            }
    }
}

@Composable private fun Slot(icon: ImageVector, label: String, onClick: () -> Unit) =
    Box(Modifier.size(54.dp).clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, label, tint = CalColors.Text) }

/** «Noviembre» big, «2026» small under it (the year view shows only the year). */
@Composable fun Title(big: String, small: String?) = Column(Modifier.padding(start = 28.dp, top = 8.dp, bottom = 8.dp)) {
    Text(big, fontSize = 40.sp, fontWeight = FontWeight.Medium)
    if (small != null) Text(small, fontSize = 15.sp, color = CalColors.Text)
}

/** «Hoy» on the left and «+» on the right, floating over the bottom. */
@Composable fun BottomButtons(modifier: Modifier, onToday: (() -> Unit)?, onAdd: () -> Unit) =
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        if (onToday != null) Text(stringResource(R.string.today), Modifier.align(Alignment.CenterStart).clip(CircleShape).background(CalColors.Card)
            .border(1.dp, CalColors.Chip, CircleShape).clickable(onClick = onToday).padding(horizontal = 24.dp, vertical = 16.dp), fontSize = 17.sp)
        Box(Modifier.align(Alignment.CenterEnd).size(58.dp).clip(CircleShape).background(CalColors.Card).border(1.dp, CalColors.Chip, CircleShape)
            .clickable(onClickLabel = stringResource(R.string.new_event), onClick = onAdd), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Add, stringResource(R.string.new_event), Modifier.size(30.dp), tint = CalColors.Text)
        }
    }
