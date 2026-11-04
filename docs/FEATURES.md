# Funciones implementadas y pendientes (v0.4.3)

## Nuevo en 0.4

- **Bloqueo fuera del launcher** (opcional, con un servicio de accesibilidad limitado). Las apps de nivel 4 vuelven a pasar por la interrupción si se abren desde Recientes, desde una notificación o tras más de un minuto fuera. Probado en el teléfono con TikTok.
- **Temporizador por app**, en el menú de cada app o al pasar la interrupción: 5, 10, 15, 30 o 60 minutos. Al acabarse, vuelves al launcher; para volver a entrar pasas por la interrupción. Probado en el teléfono: el aviso con la app delante y la interrupción al reabrirla.
- **Filtro de notificaciones**: las de las apps que elijas se posponen hasta tu franja (por defecto, de 21:00 a 22:00). Usa la función de posponer de Android, sin guardar el texto. Probado en el teléfono con notificaciones de prueba.
- **Copia de seguridad**: exportar e importar ajustes, pantallas, carpetas y notas en un archivo JSON. Sin las fotos del widget ni los widgets de otras apps. Probado con pruebas unitarias.
- **Rendimiento**: al volver a Inicio solo se recalcula el día de hoy, y se descartan pronto los eventos que no cuentan. Hay una versión optimizada para el uso diario que se instala encima de la de depuración.


## Implementado y probado

Probado en emulador. Lo principal se comprobó también en el Tecno LJ7: ver [resultados](TEST_RESULTS.md).

**Pantalla principal**

- Composición inicial de referencia: reloj y fecha, fotografías, tiempo de pantalla, apps, búsqueda de Google e indicador de páginas. Se aplica una sola vez y se puede restaurar.
- Dock de hasta 5 apps con contenedor oscuro, translúcido y muy redondeado. Valor inicial: Teléfono, WhatsApp, Spotify y Cámara, si existen.
- Dock editable: abrir, reemplazar, quitar, mover, arrastrar a la cuadrícula y desde ella.
- Sin icono de ajustes en Inicio. Los ajustes están en la última pantalla y en el modo edición.

**Edición**

- Mantener pulsado un espacio libre o un widget activa el modo edición, con Apps, Widgets, Ajustes y Listo.
- Cuadrícula con guías, resaltado verde o rojo del destino y rechazo de solapes y posiciones imposibles.
- Mover apps y widgets, cambiar tamaño de widgets y quitar elementos sin desinstalar.
- Cambios guardados al instante; conservados tras reiniciar el proceso y al actualizar desde 0.2.
- Al aplicar la composición, lo que no cabe en Inicio pasa a Apps útiles o a Widgets, y una app del dock no se repite.

**Carpetas**

- Soltar una app sobre otra crea una carpeta con animación; soltarla sobre una carpeta la añade.
- Miniaturas, abrir, renombrar, añadir, sacar, reordenar y deshacer sin perder apps.
- Disponibles en Inicio y Apps útiles.

**Widgets**

- Galería visual con nombre, app, vista previa real publicada por la app, tamaño y opciones. Si una app no publica vista previa, se indica y se muestra su icono.
- Widgets propios con datos reales: reloj, fotografías, tiempo de pantalla y búsqueda de Google.
- Liberación del identificador al quitar un widget. Un widget que falle, o que no envíe contenido, muestra un aviso y no tumba la pantalla. La galería avisa de los widgets que pertenecen a otro launcher.

**Bienestar**

- Tarjetas grandes por app: tiempo de hoy, aperturas, promedio diario y proyecciones etiquetadas como estimación.
- Detalle por app: hoy, 24 h, 7 y 30 días, barras por día, comparación y proyección.
- Un único motor de estadísticas para el panel, el widget y las interrupciones.
- Progreso de día, semana, mes, semestre y año con su duración real; semana configurable; semestre calendario o académico.

**Ajustes y navegación**

- Immersive Mode: oculta solo la barra superior y solo dentro del launcher.
- Ajustes minimalistas con secciones desplegables animadas, siempre oscuros.
- Apps útiles sin títulos ni textos, con iconos, carpetas y widgets. Es la misma cuadrícula que Inicio.

**De versiones anteriores, comprobado de nuevo**

- App Drawer textual, búsqueda, niveles de acceso, búsqueda deliberada, interrupción y notas.
- Biblioteca compacta o expandida, categorías editables, arrastre entre categorías, temas, tipografías y paquetes de iconos.
- Versión release con R8.

## Implementado, sin prueba completa

- **Teléfono real:** gestos inyectados por `adb`, sin tu uso diario. Elegir fotos y añadir un widget de otra app no se probaron allí.
- **Animación al abrir apps:** se usa la API oficial, pero HiOS puede sustituirla por la suya.
- **Comparación con periodos anteriores:** lógica con pruebas unitarias. En tu teléfono aún no se muestra porque Android solo tiene 10 días de datos.
- **Immersive Mode en release:** comprobado por el sistema en depuración; en release la consulta no devolvió datos, aunque el código es el mismo.
- **Cristal esmerilado (experimental):** sin desenfoque visible en el emulador.

## Lo que aún no cumple tu petición

- **Clima en el reloj: no implementado.** Requiere Internet y romper la garantía de cero red. Alternativa sin cambiar nada: añadir desde la galería el widget de clima de una app instalada, si tu teléfono lo ofrece. El widget de reloj y clima de HiOS no sirve: quedó en blanco en este launcher.
- **Dos apps de tu referencia:** el icono de la estrella y el del libro no se pudieron identificar. Sus huecos se ocupan con otras apps o quedan libres; colócalas tú.
- **Focus Mode, Notification Filter y Gestures:** aparecen en Ajustes como «Disponible en una próxima versión». No se fingen.
- **Las secciones del menú** usan los nombres que pediste, más «Bienestar» para semana y semestre, y un acceso al sistema solo cuando falta un permiso.

## Problemas conocidos

- Sacar una app de una carpeta se hace desde su menú; aún no arrastrándola fuera.
- No se puede arrastrar una app de una pantalla a otra.
- El tamaño de un widget externo crece o encoge desde su esquina inferior derecha; no todos los widgets se adaptan igual de bien.
- Las apps ocultas o desinstaladas dejan su celda libre: si la ocupas y luego vuelven, debes colocarlas de nuevo.
- Quitar la pantalla de título de Apps útiles convierte «Quiero usar más» en una carpeta con ese nombre.

## Pendiente por fase

**Fase 3:** Focus Mode, tipos de interrupción (matemáticas, mensaje), interrupciones en apps visibles y carpetas, lista de
esenciales, filtro de notificaciones, gestos asignables y opciones restantes del drawer.

**Fase 4:** resumen diario propio para superar la retención del sistema, mensajes de mejora ampliados, límites de tiempo.

**Fase 5:** modo estudio, perfiles, widgets académicos y de proyecto, Pomodoro, calendario, enlaces a notas de Obsidian,
exportar e importar ajustes.
