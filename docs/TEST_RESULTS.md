# Resultados de pruebas (v0.3.1, 2026-10-31)

**Teléfono real:** Tecno LJ7, HiOS 16.3, Android 16, 1080×2460, con APK de depuración 0.3.1 instalado encima de la 0.3.0 y gestos inyectados por `adb`. Resultados en la sección siguiente.
**Emulador:** Android 36 x86_64 (1080×2400), usado para la v0.3.0. Sus resultados siguen más abajo.

## Prueba en el teléfono real (v0.3.1)

Se hizo una copia de los datos antes de cada actualización, fuera del repositorio. Las capturas del teléfono tampoco se guardan aquí porque contienen datos personales.

| Prueba | Resultado | Cómo se comprobó |
|---|---|---|
| Actualizar 0.2 → 0.3.0 → 0.3.1 sin perder datos | Correcto | Apps, carpetas, widget y ajustes siguen; launcher predeterminado sin cambios |
| Composición de Inicio con tus apps | Correcto | Captura: reloj, fotos, tiempo de pantalla, Drive, Gmail, búsqueda, Notion, IA, Classroom, BNB, Brave, UNImóvil y Obsidian |
| Dock con Teléfono, WhatsApp, Spotify y Cámara | Correcto | Cada icono abre su actividad, según `dumpsys activity` |
| Modo edición, guías y barra de acciones | Correcto | Captura y jerarquía de vistas |
| Mover Gmail a un hueco y devolverlo | Correcto | Posiciones antes y después |
| Redimensionar el reloj y devolverlo | Correcto | Altura de 2 a 1 fila y vuelta a 2 |
| Crear carpeta soltando Drive sobre Gmail, y deshacerla | Correcto | La carpeta contiene las dos apps; al deshacer no se pierde ninguna |
| Carpeta IA | Correcto | ChatGPT, Claude, Gemini, Copilot, Grok y DeepSeek |
| Bienestar comparado con `dumpsys usagestats` | Coherente | Brave: 55 min y 55:34. TikTok: 12 aperturas hoy y 16 en el periodo de Android, que empieza la tarde anterior |
| Immersive Mode | Correcto | Sin barra en la captura, `visible=false`; vuelve a `true` al desactivar |
| Galería con widgets reales | Correcto | Vistas previas de Binance, Pinterest, Recorder, Roaming y Shazam, entre otros |
| Ajustes y seis pantallas | Correcto | Captura y jerarquía de vistas |
| Registro de fallos | Vacío | `logcat -b crash` durante toda la prueba |

**Medidas en el teléfono, versión de depuración.** La release no se midió en el teléfono.

| Medida | Resultado |
|---|---|
| Arranque en frío (`Displayed`) | 933 ms, una sola medida |
| Memoria (TOTAL PSS) | Pico de 362 MB al arrancar, 171 MB a los 45 s y 231 MB tras recorrer la galería, en un teléfono con 7,6 GB |
| Fotogramas con tirones | 0,64 % en una sesión de 2028 fotogramas; percentil 90: 18 ms; percentil 99: 150 ms |

### Fallos que solo aparecieron en el teléfono, corregidos en 0.3.1

1. **El dock abría Contactos en vez de Teléfono.** En HiOS, los dos iconos pertenecen al mismo paquete, y se elegía el primero. Ahora se busca la actividad exacta que atiende la marcación.
2. **«550 h en los últimos 30 días».** Android solo conservaba 10 días. Una pausa suelta se contaba desde el inicio de la ventana, lo que inventaba 20 días. Ahora el dato es 68 h. El mismo cambio evita contar como uso una noche con la pantalla apagada si cruza el inicio de una ventana.
3. **Aperturas infladas:** 57 de TikTok, frente a 16 de Android. Desbloquear el teléfono contaba como abrir. Ahora se sigue el criterio de Android: volver a una app desde otra.
4. **Tu widget de reloj de HiOS, traído de la 0.2, hacía que Inicio se desplazara.** No cabía en la composición y quedaba debajo, sin verse. Ahora lo que no cabe pasa a Widgets o a Apps útiles.
5. **Ese widget se veía en blanco, sin explicación.** Es del launcher de HiOS y no envió contenido a este launcher. Ahora muestra un aviso, y la galería advierte de los widgets de otro launcher.
6. **WhatsApp aparecía a la vez en el dock y en la cuadrícula** tras migrar. Una app del dock ya no se repite.
7. **Restaurar la distribución movía la carpeta IA** a otro sitio. Ahora vuelve a su celda.
8. Frase rota en Bienestar («Y de 10 de los últimos 30 días») y tarjeta de Ajustes más baja que el resto en la biblioteca.

## Pruebas unitarias

`.\gradlew.bat :app:testDebugUnitTest`: **73 de 73 correctas** (`OrganizerTest` 33, `LogicTest` 19, `UsageCalculatorTest` 21).
Cubren cuadrícula, solapes, carpetas, dock, composición inicial, migración desde 0.2, estadísticas, comparaciones y progreso del tiempo.

## Criterios de aceptación (v0.3.0, emulador)

| Criterio | Resultado | Cómo se comprobó |
|---|---|---|
| Dock como la captura | Cumplido en emulador | Captura; falta verlo con tus 4 apps |
| Iconos del dock funcionales | Cumplido | Teléfono y Cámara abren; reemplazar, quitar y mover |
| Previsualización de widgets | Cumplido | Vistas previas reales de Calendar y Chrome; widgets propios dibujados |
| Mover apps y widgets | Cumplido | Arrastre real, destino libre y destino rechazado |
| Carpetas funcionales | Cumplido | Crear arrastrando, abrir, añadir, reordenar, renombrar, deshacer |
| Immersive oculta la barra | Cumplido en depuración | `visible=false` en `dumpsys`; captura sin barra; se recupera al desactivar |
| Porcentajes correctos | Cumplido | Comprobados a mano a las 20:15 del 31 de octubre: 84, 83, 99, 66 y 83 %, y pruebas de bisiestos |
| Estadísticas reales | Cumplido | Apps usadas durante la prueba, con sus tiempos y aperturas |
| Sin ajustes en la esquina de Inicio | Cumplido | Jerarquía de vistas y captura |
| Ajustes persistentes | Cumplido | Reinicio del proceso |
| Sin pérdida de funciones | Cumplido en emulador | Actualización desde 0.2 y repetición de funciones anteriores |

## Otras pruebas en el emulador (v0.3.0)

| Prueba | Resultado |
|---|---|
| Actualizar desde el APK 0.2.0 con apps, carpeta, «Quiero usar más» y nota | Se conserva todo |
| Elegir foto real en el selector del sistema y conservarla tras reiniciar | Correcto |
| Pulsación larga sobre un widget | Entra en edición; un toque corto sigue abriendo el widget |
| Soltar una app sobre un widget | Rechazado; la app no se mueve |
| Mover widget a hueco libre y redimensionar | Correcto y persistente |
| Dock a celda libre y cuadrícula al dock | Correcto y persistente |
| Restaurar distribución | Correcto, con confirmación; conserva lo colocado |
| Apps útiles: sin títulos, añadir apps, agrupar | Correcto |
| Ajustes desde la última pantalla y desde el modo edición | Correcto |
| Regresiones: drawer, nivel 4, búsqueda deliberada, interrupción y Atrás, biblioteca | Correcto |
| Versión release con R8: carpetas, edición, galería, persistencia, uso | Correcto |
| Registro de fallos (`logcat -b crash`) | Vacío en todas las tandas |

## Fallos encontrados y corregidos en la v0.3.0

1. Mantener pulsado un widget acababa abriendo su app al soltar.
2. «Faltan 0 días» cuando quedaban horas.
3. Servicios del sistema mostrados con su nombre interno en las estadísticas.

## Medidas en el emulador

Solo orientativas: renderizado por software.

| Medida | Release v0.2 | Release v0.3 |
|---|---|---|
| Arranque en frío (`Displayed` de logcat) | 1,05–1,37 s | 1,42–1,55 s, con widgets propios y datos de uso |
| Memoria (TOTAL PSS) | unos 114 MB | unos 89 MB |
| APK | 1,85 MB | 2,09 MB |

## Sin verificar

- Uso con tu dedo: los gestos fueron inyectados. Falta tu impresión sobre la fluidez del arrastre y posibles conflictos con los gestos de HiOS.
- Elegir fotos en el teléfono: no se abrió tu galería. Sí se probó en el emulador.
- Añadir en el teléfono un widget de otra app: se vieron sus vistas previas, pero no se añadió ninguno.
- Versión release en el teléfono: es una app distinta que no comparte datos con la de depuración.
- Consumo de batería.
- Comparación con periodos anteriores: Android solo tiene 10 días de datos en tu teléfono; la semanal aparecerá cuando haya una semana anterior completa.
- Animación de apertura en HiOS y cristal esmerilado.
- Ajustar el semestre académico desde el selector de fechas en la interfaz: su cálculo sí tiene pruebas.
- Paquetes de iconos publicados y la mayoría de widgets de terceros.
