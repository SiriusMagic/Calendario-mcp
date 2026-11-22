# Calendario 0.3.0

- Los tokens nuevos empiezan por **`cld_`** (antes `cal_`, que ya es de la Calculadora: Gateway Hub los confundía al
  vincular con token). Los tokens ya creados siguen funcionando; para que el hub los reconozca bien, crea uno nuevo.
- 16 pruebas.

# Calendario 0.2.0

- **Resumen de familia**: Inicio muestra «Hoy en tu calendario» (el feriado y los eventos de hoy, hasta 5) en su pantalla
  «Hoy». Lo da `SummaryProvider` (`${applicationId}.summary`), solo a apps firmadas por el mismo desarrollador.
- 16 pruebas.

# Calendario 0.1.0

Primera versión: calendario en negro con acento rojo.
- Vistas Año, Mes, Semana y Agenda, con deslizar, «Hoy» e «Ir a».
- Eventos con todo el día, inicio y fin, repetición (diaria, semanal, mensual, anual), recordatorio, ubicación y
  comentario; «Días importantes» anuales con los días que faltan; buscar sin acentos.
- Feriados nacionales de Ecuador (fechas de ley, Carnaval y Viernes Santo calculados).
- Recordatorios como notificación exacta; se reprograman al reiniciar o cambiar la hora.
- Ajustes: primer día de la semana, números de semana, fiestas nacionales, recordatorios, borrar caducados, Modo MCP y
  Temas.
- Modo MCP igual que las apps hermanas (tokens `cal_…`, Redireccionar a Hub, módulo `calendar_`), con 5 herramientas.
- Datos fuera de las copias en la nube. 15 pruebas (lógica, MCP y capturas).
