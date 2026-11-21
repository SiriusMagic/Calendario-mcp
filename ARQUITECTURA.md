# Arquitectura de Calendario

Carpeta = package (`android/app/src/main/java/com/zirakusa/calendario/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | Lógica pura, con pruebas. `Event` (evento o «Día importante», todo el día, inicio y fin, `Recur`, recordatorio, ubicación, comentario) y `EventStore` (JSON en las preferencias «calendario», borrar caducados); `Calendar` (`Occurrence`, repeticiones, días que cubre, cuadrícula del mes y semana según el primer día, número de semana, días que faltan, búsqueda sin acentos); `Holidays` (feriados de Ecuador con Carnaval y Viernes Santo desde la Pascua); `Remind` (cuándo avisa cada recordatorio); `Settings` (`FirstDay`, `View` y ajustes sobre `KeyValues`). |
| `ui/` | `CalState` (único dueño del estado: vista, día elegido, eventos, el que se edita y ajustes) y `CalendarApp` (vista elegida con «Hoy» y «+», páginas encima; `AppActions`). |
| `ui/screens/` | `Header` (buscar, vistas Año/Mes/Semana/Agenda, ⋮ con Días importantes, Ir a y Ajustes; título; botones de abajo), `MonthScreen`, `WeekScreen`, `YearAgenda` (Año y Agenda), `EventRows` (filas de eventos y feriados, vacío, deslizar), `EventEditor` y `EditorParts` (ruedas de fecha y hora, campos), `Pages` (Días importantes, Buscar, Ir a), `SettingsScreen` y `SettingsParts`, `McpScreen` (igual que en las apps hermanas), `Wheel`, `Texts` y `Chrome`. Solo pintan y avisan. |
| `ui/theme/` | `Theme`: `ThemeMode` (Sistema, Claro, Oscuro, Negro de fábrica), `Accent` (rojo de fábrica…) y `CalColors`. |
| `mcp/` | Modo MCP, como las apps hermanas: `Tokens` (`cal_…`, cifrados con `SecretBox`), `CalendarTools` (`calendar_events`, `calendar_day`, `calendar_holidays`; con escritura `calendar_event_create`, `calendar_event_delete`; a través de `CalendarData`), `McpHandler`, `RelayLink` (relay de Stone), `McpMode` (`LiveData` guarda, reprograma y avisa a la pantalla) y `HubLink` (módulo `calendar_`). |
| `Reminders.kt` | Una alarma exacta para el próximo recordatorio; al sonar muestra las notificaciones pendientes y pone la siguiente. `ReminderReceiver` reprograma al arrancar y al cambiar la hora. |
| `Summary.kt` | `SummaryLines` (líneas de hoy, pura, con prueba) y `SummaryProvider`: el resumen de familia para Inicio (`FAMILIA.md` de Inicio), solo para apps con la misma firma. |
| `MainActivity.kt` | Crea el estado, programa recordatorios, abre el día de una notificación, pide notificaciones e «Ir al Hub». |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Colores siempre de `CalColors` (más `Violet` para días importantes y `Amber` para feriados, en `EventRows`).
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
- `ScreenshotsTest` usa el sábado 21 de noviembre de 2026 como hoy.
