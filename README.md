# Calendario (calendario MCP)

Calendario para Android en negro con acento rojo. Parte de la familia de Stone, Luma, Calculadora, Notas, Filem, Nivel y
Reloj: tu IA (Claude, ChatGPT…) puede **leer y crear tus eventos** por MCP.

## Estado (0.2.0)

- **Vistas:** Año, Mes, Semana y Agenda (botón del calendario arriba). Deslizar cambia de mes, semana o año; «Hoy»
  vuelve a hoy y «+» crea un evento.
- **Mes:** hoy en rojo, el día elegido en gris, puntos en los días con algo; debajo «Sáb, 21 nov, hoy» y sus eventos o
  «No hay eventos para mostrar». Números de semana opcionales.
- **Semana:** fila de todo el día y horas con cada evento como bloque. **Agenda:** un año por delante, por fecha.
- **Nuevo:** Evento o Días importantes, título, Todo el día, Hora de inicio y de finalización (ruedas), Repetición
  (único, diario, semanal, mensual, anual), Recordatorio, Ubicación y Comentar. Tocar un evento lo edita o elimina.
- **Días importantes:** cumpleaños y aniversarios que se repiten cada año, con los días que faltan.
- **⋮:** Días importantes, **Ir a** (ruedas de mes, día y año) y Ajustes. **Buscar** por título, lugar o comentario.
- **Feriados de Ecuador** en el calendario (se pueden ocultar).
- **Recordatorios** como notificación a la hora exacta, también tras reiniciar.
- **Ajustes:** primer día de la semana, números de semana, fiestas nacionales, recordatorios, borrar eventos caducados,
  **Modo MCP**, Temas (Sistema, Claro, Oscuro, Negro y color de acento), privacidad y versión.
- **Modo MCP:** `calendar_events`, `calendar_day`, `calendar_holidays` y, con token de lectura y escritura,
  `calendar_event_create` y `calendar_event_delete`. Mismo relay y Gateway Hub (módulo `calendar_`) que las demás apps.
- **Resumen para Inicio:** la tarjeta «Hoy en tu calendario» en la pantalla «Hoy» de Inicio.
- Sin anuncios ni estadísticas; internet solo para el Modo MCP (apagado de fábrica).

APK: [`android/Calendario-0.2.0.apk`](android/Calendario-0.2.0.apk) (`com.zirakusa.calendario`, firmado como Stone y
Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Siguiente

Ver [`ROADMAP.md`](ROADMAP.md).
