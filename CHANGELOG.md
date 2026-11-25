# Calendario 0.5.0

- **El Modo MCP lo controla Arka**: se quita la pantalla «Modo MCP» (Activar MCP, Redireccionar a Hub, Crear token y la
  lista de tokens). Activarlo, permitirlo en local y crear tokens se hace desde **Arka › MCP**; Ajustes › Modo MCP solo
  muestra el estado y abre Arka. Si ya estaba conectada al hub, sigue conectada con el mismo enlace; los tokens LLM y Hub
  creados aquí dejan de funcionar (créalos en Arka).
- Lucía usa las herramientas en el teléfono aunque el enlace público esté apagado; lo decide «Permitir MCP local» en Arka.
- App más ligera: 1,85 MB → 1,81 MB.

# Calendario 0.4.0

- **Iconos nuevos**: Dibujo (el de fábrica), 3D, Óleo, Cristal y Píxel, en Ajustes › Icono de la app. Se quitan los
  anteriores; si tenías uno elegido, pasa solo al nuevo que ocupa su lugar.

# Calendario 0.3.2

- **Posponer**: nueva herramienta `calendar_event_move` (mueve un evento a otro día y, si quieres, a otra hora; conserva su
  duración, repetición, aviso, lugar y notas). Lucía la usa: «pospón la reunión con Ana para el viernes a las 4».

# Calendario 0.3.1

- **Conectar todas** (Gateway Hub 0.0.21): con un toque en el Hub se activa aquí «Redireccionar a Hub» y la app queda
  vinculada, sin entrar en ella. Solo lo puede pedir una app firmada por el mismo desarrollador.
- **Lucía** (la voz del Hub) usa las herramientas de esta app en el teléfono, **sin internet**, con las mismas reglas que
  por MCP. Solo mientras «Redireccionar a Hub» esté activado.
- **Icono de la app**: 5 iconos para elegir (Original, Claro, Dorado, Azul y Verde) en Ajustes › Icono de la app. Tras esta
  actualización quizá tengas que volver a poner el icono en tu pantalla de inicio (una sola vez).

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
