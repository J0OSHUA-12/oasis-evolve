# Resultados de pruebas (v0.2.0, 2026-10-31)

**Entorno:** emulador Android 36 x86_64 (`sdk_gphone64_x86_64`), 1080×2400, en una carpeta temporal.
**No se ha probado en un teléfono real.**

## Pruebas unitarias

`.\gradlew.bat :app:testDebugUnitTest`: **39 de 39 correctas** (`UsageCalculatorTest` 14, `OrganizerTest` 15, `LogicTest` 10).

## Pruebas en el emulador

Ejecutadas con `adb` e interfaz real (toques, arrastres y lectura de la jerarquía de vistas), revisando capturas.

### Fase 1

| # | Prueba | Resultado |
|---|---|---|
| 1 | Instalar APK, asignar launcher por `cmd role` y pulsar Inicio | Correcto |
| 2 | Botón de la app para establecerlo como predeterminado | **No ejecutado** |
| 3 | Navegar las pantallas; Inicio y Atrás | Correcto |
| 4 | Estado inicial sin apps automáticas; elegir apps; abrir app | Correcto |
| 5 | Nivel 4, búsqueda deliberada e interrupción | Correcto |
| 6 | Uso con y sin Usage Access | Correcto, sin inventar datos |
| 7 | Notas; compartir abre el selector | Correcto; no se compartió con Obsidian |
| 8 | Riel de letras y búsqueda por iniciales | Correcto |
| 9 | Desinstalar y reinstalar una app | La lista se actualiza sola |

### Fase 2

| # | Prueba | Resultado |
|---|---|---|
| 10 | Arrastrar para reordenar en Inicio, y conservar tras reiniciar | Correcto |
| 11 | Pulsación larga sin mover abre opciones; tocar abre la app | Correcto |
| 12 | Arrastrar dentro de «Quiero usar más» | Correcto |
| 13 | Carpeta: crear, añadir, abrir app, conservar tras reiniciar | Correcto |
| 14 | Carpeta: renombrar, sacar app, deshacer | Correcto, tras corregir un fallo |
| 15 | Widget en Inicio: añadir, cambiar tamaño, conservar | Correcto (Chrome Dino) |
| 16 | Biblioteca: vista expandida y arrastrar una app a otra categoría | Correcto |
| 17 | Categorías: crear, rechazar repetida, renombrar, ocultar, mostrar, asignar | Correcto |
| 18 | Paquete de iconos de prueba: aparece, se aplica y se conserva | Correcto |
| 19 | Forma y tamaño de icono | Correcto, comprobado en captura |
| 20 | Acento, tipografía, tamaño de texto, tema oscuro | Correcto, comprobado en captura |
| 21 | Espaciado, márgenes, columnas y reducir animaciones | Aplicado sin fallos |
| 22 | Actualizar desde el APK v0.1 con datos | Conserva apps, nivel y nota |
| 23 | Versión release con R8, firmada con clave de depuración | Arrastre, carpeta, abrir app, persistencia, uso, biblioteca y nota correctos |
| 24 | Cristal esmerilado, con dos API oficiales | **Sin desenfoque visible en el emulador** |
| 25 | Registro de fallos (`logcat -b crash`) | Vacío en todas las tandas |

## Fallos encontrados y corregidos

**Fase 1:**

1. Texto ilegible sobre fondos oscuros con tema claro.
2. Hoja de acciones a media altura, que ocultaba niveles.
3. Búsqueda deliberada visible al volver a la pantalla.
4. Atrás no cerraba la interrupción.
5. «1 días» y Gmail en una categoría incorrecta.
6. Tarjetas de la biblioteca con alturas desiguales.
7. Texto invisible en tema oscuro.
8. Iconos de la barra de estado oscuros sobre fondos oscuros.

**Fase 2:**

9. Al renombrar, el cursor quedaba al principio y el texto se mezclaba.
10. Con teclado físico, cerrar una hoja hacía saltar el carrusel a otra página.
11. El icono de un paquete desinstalado podía seguir visible.
12. Carpeta poco visible en tema oscuro.
13. El tiempo en otros launchers contaba como uso de apps.

## Medidas

Solo orientativas: emulador con renderizado por software.

| Medida | Depuración v0.1 | Release v0.2 |
|---|---|---|
| Arranque en frío | 1,76–1,78 s (`am start -W`) | 1,05–1,37 s (`Displayed` de logcat, 3 medidas) |
| Memoria (TOTAL PSS) | unos 68 MB | unos 114 MB, de ellos 64 MB de código mapeado |
| APK | 12,7 MB | 1,85 MB |

Los métodos de arranque difieren, así que no son directamente comparables. La memoria de release es mayor de lo esperado y
debe medirse de nuevo en el teléfono real: en un emulador nuevo, el código puede no estar compilado de forma óptima. No se midió batería.

## Sin verificar

- Teléfono real, fabricante y ahorro de batería.
- Fluidez real de las animaciones y del arrastre con el dedo: las pruebas usaron gestos inyectados.
- Cristal esmerilado en un dispositivo físico.
- Teclado real en los diálogos.
- Conflictos entre gestos del sistema y el carrusel.
- Comparación con periodos anteriores, con historial real.
- Paquetes de iconos publicados, y la mayoría de widgets de terceros.
- Perfil de trabajo, fondo claro con `HINT_SUPPORTS_DARK_TEXT`, versiones de Android anteriores a la 36.
