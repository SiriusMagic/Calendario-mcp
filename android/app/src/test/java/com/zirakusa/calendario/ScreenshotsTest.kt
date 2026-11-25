package com.zirakusa.calendario

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.calendario.data.Event
import com.zirakusa.calendario.data.EventStore
import com.zirakusa.calendario.data.Kind
import com.zirakusa.calendario.data.MapValues
import com.zirakusa.calendario.data.Recur
import com.zirakusa.calendario.data.View
import com.zirakusa.calendario.ui.CalState
import com.zirakusa.calendario.ui.CalendarApp
import com.zirakusa.calendario.ui.Page
import com.zirakusa.calendario.ui.theme.Accent
import com.zirakusa.calendario.ui.theme.CalTheme
import com.zirakusa.calendario.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private val values = MapValues()
    private val today = LocalDate.of(2026, 11, 21)
    private val state by lazy { CalState(values, today = { today }) }

    private fun shot(name: String) { compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/calendario-$name.png") }
    private fun show() = compose.setContent { CalTheme(state.themeMode, state.accent) { CalendarApp(state) } }
    private fun seed() {
        val store = EventStore(values)
        store.save(Event(title = "Reunión de equipo", start = LocalDateTime.of(2026, 11, 21, 9, 0), end = LocalDateTime.of(2026, 11, 21, 10, 0), location = "Oficina"))
        store.save(Event(title = "Yoga", start = LocalDateTime.of(2026, 11, 2, 18, 0), end = LocalDateTime.of(2026, 11, 2, 19, 0), recur = Recur.WEEKLY))
        store.save(Event.important(LocalDate.of(1995, 12, 2), "Cumpleaños de mi mejor amiga"))
        store.save(Event(title = "Viaje a Cuenca", allDay = true, start = LocalDate.of(2026, 11, 20).atStartOfDay(), end = LocalDate.of(2026, 11, 22).atStartOfDay()))
        state.reload()
    }

    @Test fun monthWeekYearAgenda() {
        seed(); show()
        compose.onNodeWithText("Noviembre").assertExists()
        compose.onNodeWithText("Sáb, 21 nov, hoy").assertExists()
        compose.onNodeWithText("Reunión de equipo").assertExists()
        shot("month")
        compose.onNodeWithContentDescription("2026-11-02").performClick()
        compose.onNodeWithText("Día de los Difuntos").assertExists()
        shot("month-holiday")
        compose.onNodeWithContentDescription("Vistas").performClick()
        shot("views")
        compose.onNodeWithText("Semana").performClick()
        assertEquals(View.WEEK, state.view)
        state.goToday()
        shot("week")
        state.changeView(View.YEAR)
        compose.onNodeWithText("2026").assertExists()
        shot("year")
        compose.onNodeWithText("Dic").performClick()
        assertEquals(View.MONTH, state.view); assertEquals(LocalDate.of(2026, 12, 1), state.selected)
        state.goToday(); state.changeView(View.AGENDA)
        compose.onNodeWithText("Cumpleaños de mi mejor amiga").assertExists()
        shot("agenda")
    }

    @Test fun newEventImportantSearchGoTo() {
        show()
        compose.onNodeWithText("No hay eventos para mostrar").assertExists()
        shot("month-empty")
        compose.onNodeWithContentDescription("Nuevo evento").performClick()
        compose.onNodeWithText("Mi evento").performTextInput("Cita médica")
        compose.onNodeWithText("Hora de inicio").performClick()
        shot("editor")
        compose.onNodeWithText("Repetición").performClick()
        compose.onNodeWithText("Cada mes").performClick()
        compose.onNodeWithContentDescription("Guardar").performClick()
        val saved = EventStore(values).list().single()
        assertEquals("Cita médica", saved.title); assertEquals(Recur.MONTHLY, saved.recur)

        state.page = Page.IMPORTANT
        compose.onNodeWithText("No hay días importantes").assertExists()
        shot("important-empty")
        compose.onNodeWithContentDescription("Nuevo evento").performClick()
        assertEquals(Kind.IMPORTANT, state.draft!!.kind)
        compose.onNodeWithText("Nombre (ej. Cumpleaños de Ana)").performTextInput("Aniversario")
        shot("editor-important")
        compose.onNodeWithContentDescription("Guardar").performClick()
        assertEquals(Page.IMPORTANT, state.page)
        compose.onNodeWithText("Aniversario").assertExists()
        compose.onNodeWithText("Hoy").assertExists()
        shot("important")

        state.page = Page.SEARCH
        compose.onNodeWithText("Buscar").performTextInput("medica")
        compose.onNodeWithText("Cita médica").assertExists()
        shot("search")
        state.page = Page.MAIN
        state.goingTo = true
        compose.onNodeWithText("sábado, 21 de noviembre de 2026").assertExists()
        shot("go-to")
    }

    @Test fun settingsMcpAndThemes() {
        show()
        state.page = Page.SETTINGS
        compose.onNodeWithText("Mostrar los números de la semana").performClick()
        compose.onNodeWithText("Domingo").performClick()
        compose.onNodeWithText("Modo MCP").assertExists()
        shot("settings")
        compose.onNodeWithText("Lo controla Arka", substring = true).assertExists()
        shot("mcp")
        state.page = Page.MAIN
        state.changeTheme(ThemeMode.LIGHT); state.changeAccent(Accent.AZUL)
        seed()
        shot("month-light")
    }
}
