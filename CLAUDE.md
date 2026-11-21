# Calendario (calendario MCP): reglas para agentes

- Lee `ARQUITECTURA.md` y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos.
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/Calendario-<versión>.apk` (borra el anterior)
  y anótalo en `CHANGELOG.md`.
- Reglas puras (repeticiones, días que cubre un evento, cuadrícula del mes, semanas, feriados de Ecuador, recordatorios,
  búsqueda) en `data/`, sin Android y con pruebas. Las pantallas solo pintan y avisan.
- Todo evento pasa por `EventStore` y, tras cada cambio, por `Reminders.sync` (también desde MCP).
- Privacidad: eventos y ajustes viven solo en el teléfono (fuera de copias). Nada sale salvo por el Modo MCP que el
  usuario active; internet es solo para eso.
- Cada herramienta MCP nueva lleva su prueba en `McpTest`: lista de herramientas, llamada correcta, argumentos malos y
  «no encontrado». Las que cambian algo exigen token de lectura y escritura (`READ_ONLY` si no).
- Prohibido: `eval`, motores de scripts, SQL concatenado.
- Firma: la de depuración del entorno, igual que Stone, Luma, Calculadora, Notas, Filem, Nivel, Reloj y Gateway Hub.
