package com.zirakusa.calendario

import com.zirakusa.calendario.data.Event
import com.zirakusa.calendario.data.EventStore
import com.zirakusa.calendario.data.MapValues
import com.zirakusa.calendario.data.Recur
import com.zirakusa.calendario.mcp.Access
import com.zirakusa.calendario.mcp.CalendarData
import com.zirakusa.calendario.mcp.HubLink
import com.zirakusa.calendario.mcp.Kind
import com.zirakusa.calendario.mcp.McpHandler
import com.zirakusa.calendario.mcp.Token
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/** Calendario's MCP: tools list, good calls, bad arguments, not found, read-only tokens, wrong and paused tokens, hub link. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class McpTest {
    private val store = EventStore(MapValues())
    private val data = object : CalendarData {
        var shown = true
        override fun events() = store.list()
        override fun save(event: Event) = store.save(event)
        override fun delete(id: Long) = store.delete(id)
        override fun holidays() = shown
    }
    private val writer = Token("w", "Claude", "cal_" + "w".repeat(40), Access.WRITE, Kind.HUB, "Agenda")
    private val reader = Token("r", "Lector", "cal_" + "r".repeat(40), Access.READ)
    private val paused = Token("p", "Viejo", "cal_" + "p".repeat(40), Access.WRITE, paused = true)
    private val handler = McpHandler({ listOf(writer, reader, paused) }, data, "0.1.0")

    init {
        store.save(Event(title = "Yoga", start = LocalDateTime.of(2026, 11, 2, 18, 0), end = LocalDateTime.of(2026, 11, 2, 19, 0), recur = Recur.WEEKLY, location = "Parque"))
        store.save(Event.important(java.time.LocalDate.of(1990, 12, 2), "Cumpleaños de Ana"))
    }

    private fun rpc(token: Token, method: String, params: JSONObject = JSONObject()) = runBlocking {
        JSONObject(handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(),
            JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()).body).getJSONObject("result")
    }
    private fun tool(name: String, args: JSONObject, token: Token = writer) = rpc(token, "tools/call", JSONObject().put("name", name).put("arguments", args))
    private fun ok(name: String, args: JSONObject = JSONObject()) =
        tool(name, args).also { assertFalse(it.toString(), it.optBoolean("isError")) }.getJSONObject("structuredContent")
    private fun code(name: String, args: JSONObject = JSONObject(), token: Token = writer) = tool(name, args, token).also { assertTrue(it.optBoolean("isError")) }
        .getJSONObject("structuredContent").getJSONObject("error").getString("code")
    private fun names(token: Token) = rpc(token, "tools/list").getJSONArray("tools").let { t -> (0 until t.length()).map { t.getJSONObject(it).getString("name") } }

    @Test fun listsTheTools() {
        assertEquals(listOf("calendar_events", "calendar_day", "calendar_holidays", "calendar_event_create", "calendar_event_delete"), names(writer))
        assertEquals(listOf("calendar_events", "calendar_day", "calendar_holidays"), names(reader))
        val init = rpc(writer, "initialize")
        assertEquals("calendario", init.getJSONObject("serverInfo").getString("name"))
        assertEquals("hub", init.getJSONObject("_meta").getJSONObject(McpHandler.TOKEN_META).getString("kind"))
    }

    @Test fun readsEventsDaysAndHolidays() {
        val nov = ok("calendar_events", JSONObject().put("from", "2026-11-01").put("to", "2026-11-30"))
        assertEquals(5, nov.getInt("total"))
        assertEquals("2026-11-02T18:00", nov.getJSONArray("events").getJSONObject(0).getString("start"))
        assertEquals(1, ok("calendar_events", JSONObject().put("from", "2026-12-01").put("to", "2026-12-06").put("query", "ana")).getInt("total"))
        val day = ok("calendar_day", JSONObject().put("date", "2026-11-02"))
        assertEquals("souls", day.getString("holiday")); assertEquals("monday", day.getString("weekday"))
        assertEquals("Yoga", day.getJSONArray("events").getJSONObject(0).getString("title"))
        assertEquals(11, ok("calendar_holidays", JSONObject().put("year", 2026)).getJSONArray("holidays").length())
        data.shown = false
        assertEquals(0, ok("calendar_holidays", JSONObject().put("year", 2026)).getJSONArray("holidays").length())
    }

    @Test fun createsAndDeletes() {
        val e = ok("calendar_event_create", JSONObject().put("title", "Dentista").put("date", "2026-11-24").put("time", "15:30")).getJSONObject("event")
        assertEquals("2026-11-24T15:30", e.getString("start")); assertEquals("2026-11-24T16:30", e.getString("end")); assertEquals(10, e.getInt("reminder_minutes"))
        val all = ok("calendar_event_create", JSONObject().put("title", "Vacaciones").put("date", "2026-12-20").put("end_date", "2026-12-27")).getJSONObject("event")
        assertTrue(all.getBoolean("all_day")); assertEquals("2026-12-27", all.getString("end"))
        val imp = ok("calendar_event_create", JSONObject().put("title", "Aniversario").put("date", "2015-06-14").put("important", true)).getJSONObject("event")
        assertEquals("important", imp.getString("kind")); assertEquals("yearly", imp.getString("repeat"))
        assertEquals(e.getLong("id"), ok("calendar_event_delete", JSONObject().put("id", e.getLong("id"))).getLong("deleted"))
        assertEquals(4, store.list().size)
    }

    @Test fun badArgumentsAreRefused() {
        assertEquals("BAD_ARGUMENT", code("calendar_events", JSONObject().put("from", "2026-11-01")))
        assertEquals("BAD_ARGUMENT", code("calendar_events", JSONObject().put("from", "2026-11-30").put("to", "2026-11-01")))
        assertEquals("BAD_ARGUMENT", code("calendar_events", JSONObject().put("from", "2026-01-01").put("to", "2027-06-01")))
        assertEquals("BAD_ARGUMENT", code("calendar_day", JSONObject().put("date", "mañana")))
        assertEquals("BAD_ARGUMENT", code("calendar_holidays", JSONObject().put("year", "2026")))
        assertEquals("BAD_ARGUMENT", code("calendar_event_create", JSONObject().put("title", " ").put("date", "2026-11-24")))
        assertEquals("BAD_ARGUMENT", code("calendar_event_create", JSONObject().put("title", "X").put("date", "2026-11-24").put("time", "25:00")))
        assertEquals("BAD_ARGUMENT", code("calendar_event_create", JSONObject().put("title", "X").put("date", "2026-11-24").put("time", "10:00").put("end_time", "09:00")))
        assertEquals("BAD_ARGUMENT", code("calendar_event_create", JSONObject().put("title", "X").put("date", "2026-11-24").put("repeat", "hourly")))
        assertEquals("BAD_ARGUMENT", code("calendar_event_create", JSONObject().put("title", "X").put("date", "2026-11-24").put("sql", "DROP")))
        assertEquals("BAD_ARGUMENT", code("calendar_event_delete", JSONObject().put("id", "1")))
        assertEquals(2, store.list().size)
    }

    @Test fun notFoundUnknownAndReadOnly() {
        assertEquals("NOT_FOUND", code("calendar_event_delete", JSONObject().put("id", 99)))
        assertEquals("UNKNOWN_TOOL", code("calendar_shell"))
        assertEquals("READ_ONLY", code("calendar_event_delete", JSONObject().put("id", 1), reader))
        assertEquals("READ_ONLY", code("calendar_event_create", JSONObject().put("title", "X").put("date", "2026-11-24"), reader))
        assertEquals(0, ok("calendar_events", JSONObject().put("from", "2026-11-03").put("to", "2026-11-08")).getInt("total"))
    }

    @Test fun wrongAndPausedTokensAndHubLink() = runBlocking {
        val body = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${paused.secret}"), emptyMap(), body).status)
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer cal_wrong"), emptyMap(), body).status)
        assertEquals(401, handler.handle("POST", "/mcp", emptyMap(), emptyMap(), body).status)
        assertEquals(200, handler.handle("POST", "/mcp", emptyMap(), mapOf("token" to reader.secret), body).status)
        assertEquals(404, handler.handle("POST", "/other", emptyMap(), emptyMap(), body).status)
        assertTrue(Token.secret().startsWith("cal_"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.answer(false).getString("error"))
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
    }
}
