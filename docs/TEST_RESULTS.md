# Resultados de pruebas (v0.3.0, 2026-10-31)

**Entorno:** emulador Android 36 x86_64 (1080×2400), en una carpeta temporal, con gestos inyectados por `adb`.
**Teléfono real:** el usuario probó la v0.2.0 en su Tecno LJ7 (HiOS 16.3, Android 16). La v0.3.0 aún no estaba instalada en él al escribir esto.

## Pruebas unitarias

`.\gradlew.bat :app:testDebugUnitTest`: **64 de 64 correctas** (`OrganizerTest` 28, `LogicTest` 19, `UsageCalculatorTest` 17).
Cubren cuadrícula, solapes, carpetas, dock, composición inicial, migración desde 0.2, estadísticas, comparaciones y progreso del tiempo.

## Criterios de aceptación

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

## Otras pruebas en el emulador

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

## Fallos encontrados y corregidos en esta versión

1. Mantener pulsado un widget acababa abriendo su app al soltar.
2. «Faltan 0 días» cuando quedaban horas.
3. Servicios del sistema mostrados con su nombre interno en las estadísticas.

## Medidas

Solo orientativas: emulador con renderizado por software.

| Medida | Release v0.2 | Release v0.3 |
|---|---|---|
| Arranque en frío (`Displayed` de logcat) | 1,05–1,37 s | 1,42–1,55 s, con widgets propios y datos de uso |
| Memoria (TOTAL PSS) | unos 114 MB | unos 89 MB |
| APK | 1,85 MB | 2,09 MB |

## Sin verificar

- La v0.3.0 en el teléfono real, con tus apps, tu densidad de pantalla y HiOS.
- Fluidez del arrastre con el dedo y conflictos con los gestos de HiOS.
- Consumo de batería.
- Comparación con periodos anteriores con historial real.
- Animación de apertura en HiOS y cristal esmerilado.
- Ajustar el semestre académico desde el selector de fechas en la interfaz: su cálculo sí tiene pruebas.
- Paquetes de iconos publicados y la mayoría de widgets de terceros.
