# Referencias de diseño

Las imágenes se usan como referencia y **no se guardan en el repositorio**: incluyen contenido personal y material de terceros.

## Referencia 1: tu pantalla de inicio actual

Observado: widgets grandes con esquinas muy redondeadas (reloj y fecha, una foto personal, tiempo de pantalla), cuadrícula
de 4 columnas, iconos reales con forma de squircle, barra de búsqueda en píldora, indicador de páginas y dock flotante de
4 apps con fondo translúcido, sobre un fondo oscuro.

Decisiones:
- Reloj y fecha como primer elemento, con una jerarquía tipográfica clara.
- Dock flotante translúcido opcional; cuadrícula de 4 columnas por defecto.
- Se reduce la densidad: la pantalla prioriza el espacio libre y el fondo, y solo muestra lo que elijas.
- La píldora de búsqueda no va en inicio: la búsqueda vive en las pantallas de apps.

## Referencia 2: App Library de iPhone y su versión Android

Observado: dos columnas de carpetas grandes con esquinas redondeadas, cuatro iconos por carpeta, etiqueta debajo y una
barra de búsqueda arriba.

Decisiones:
- Tarjetas translúcidas en dos columnas con cuatro iconos.
- Identidad propia: sin copiar la disposición exacta de Apple (tres iconos más un grupo), usamos 2×2 regular con
  contador de apps restantes y etiqueta dentro de la tarjeta.
- Tocar una carpeta la expande en la misma pantalla.

## Referencia 3: página de widgets de Oasis

Observado: tarjetas de contorno fino blanco sobre negro, tipografía muy ligera y grande, módulos To-do, Notes, Calendar y
App Usage, y controles de icono muy discretos.

Decisiones:
- Se conserva el espíritu sobrio: títulos grandes y ligeros, poco color y controles discretos.
- Bienestar usa tarjetas apiladas con borde sutil, pero con fondo translúcido para integrarse con el fondo real.

## Referencia 4: ajustes de Oasis

Observado: título centrado, secciones plegables (Customization, Home Screen, App Drawer, Interrupts, Focus Mode, Font,
Gestures, More) e interruptores de contorno.

Decisiones:
- Misma estructura mental por secciones, con títulos e interruptores propios.
- Se elimina «Use device wallpaper»: el fondo real siempre está activo.

## Identidad de Oasis Evolve

| Elemento | Decisión |
|---|---|
| Tipografía | Fuente del sistema en v0.1, pesos ligeros para títulos y normales para contenido |
| Color | Monocromo sobre el fondo, con un único acento en verde jade (`#6EE7B7` oscuro, `#047857` claro) |
| Superficies | Translúcidas con borde fino de baja opacidad; radio de 24 dp en tarjetas y 16 dp en iconos |
| Movimiento | Transición con ligera escala y atenuación entre pantallas, desactivable en ajustes |
| Icono | Propio, una «E» sobre círculo jade; sin recursos de Oasis |

Nada de Apple ni de Oasis se copia: ni iconos, ni fuentes, ni animaciones Lottie.

## Dirección física de los gestos

Pantallas de izquierda a derecha:

`Widget Center ← Bienestar ← INICIO → Apps útiles → App Drawer → Biblioteca`

Para ir «hacia la derecha» (Apps útiles) deslizas el dedo **de derecha a izquierda**, como en cualquier launcher.
El orden se fuerza de izquierda a derecha aunque el sistema use un idioma RTL.
