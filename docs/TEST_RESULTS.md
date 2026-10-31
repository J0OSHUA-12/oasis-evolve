# Resultados de pruebas (v0.1.0, 2026-10-31)

**Entorno:** emulador Android 36 x86_64 (`sdk_gphone64_x86_64`), 1080×2400, en una carpeta temporal, con la versión de depuración.
**No se ha probado en un teléfono real.** Tampoco se instaló ni ejecutó la versión release: solo se compiló.

## Pruebas unitarias

`.\gradlew.bat :app:testDebugUnitTest`: **23 de 23 correctas** (`UsageCalculatorTest` 14, `LogicTest` 9).
Cubren cálculo de uso, cobertura, comparación, proyección, búsqueda, categorías, niveles, progreso y ajustes.

## Pruebas en el emulador

Ejecutadas con `adb` e interfaz real (toques, deslizamientos y lectura de la jerarquía de vistas).

| # | Prueba | Resultado |
|---|---|---|
| 1 | Instalar APK | Correcto |
| 2 | Establecer como launcher predeterminado (`cmd role`) y pulsar Inicio | Correcto |
| 3 | Botón de la app para establecerlo como predeterminado | **No ejecutado:** el rol se asignó por ADB |
| 4 | Navegar las seis pantallas en ambos sentidos | Correcto |
| 5 | Botón Inicio vuelve a la pantalla central | Correcto |
| 6 | Estado inicial sin apps automáticas | Correcto |
| 7 | Elegir apps de inicio | Correcto |
| 8 | Abrir app real y volver | Correcto (Clock) |
| 9 | Persistencia de ajustes tras `force-stop` | Correcto |
| 10 | Nivel 4: desaparece de inicio y del drawer | Correcto (Chrome) |
| 11 | Búsqueda deliberada | Correcto, exige 3 letras |
| 12 | Interrupción: cuenta atrás, Continuar habilitado al final y apertura | Correcto |
| 13 | Interrupción: Volver y Atrás | Correcto, tras corregir un fallo |
| 14 | Biblioteca: categorías y abrir carpeta | Correcto |
| 15 | Desactivar biblioteca | Correcto, quedan cinco pantallas |
| 16 | Uso con Usage Access concedido por ADB | Muestra datos reales del emulador |
| 17 | Sin Usage Access | Explica el permiso, sin inventar datos |
| 18 | Notas: crear, copiar, persistir, borrar | Correcto |
| 19 | Compartir nota | Abre el selector de Android; no se compartió con Obsidian |
| 20 | Widget sin configuración: añadir, persistir y quitar | Correcto (Chrome Dino) |
| 21 | Widget con configuración: cancelar | Correcto, no se añade |
| 22 | Riel de letras y búsqueda por iniciales | Correcto |
| 23 | Reordenar, dock y «Quiero usar más» | Correcto |
| 24 | Desinstalar y reinstalar una app | La lista se actualiza sola |
| 25 | Tema oscuro y cambio de columnas | Aplicados sin fallos |
| 26 | Registro de fallos (`logcat -b crash`) | Vacío durante todas las pruebas |

## Fallos encontrados y corregidos en estas pruebas

1. Texto ilegible sobre fondos oscuros con tema claro.
2. Hoja de acciones abierta a media altura, que ocultaba los niveles 2 a 4.
3. Una búsqueda deliberada seguía visible al volver a la pantalla.
4. Atrás no cerraba la interrupción, y el teclado absorbía la pulsación.
5. «1 días» en la interrupción, y Gmail en la categoría incorrecta.
6. Tarjetas de la biblioteca con alturas desiguales.

## Medidas

Datos solo orientativos: versión de depuración sin optimizar, en un emulador con renderizado por software.

| Medida | Valor |
|---|---|
| Arranque en frío (`am start -W`, 3 veces) | 1,76–1,78 s |
| Memoria (TOTAL PSS) | unos 68 MB |
| APK de depuración | 12,7 MB |

No se midió batería, y estas cifras no predicen las de tu teléfono ni las de la versión release.

## Sin verificar

- Teléfono real, fabricante y ahorro de batería.
- Fluidez real de las animaciones.
- Comparación con periodos anteriores, con historial real.
- Completar la configuración de un widget, y la mayoría de widgets de terceros.
- Conflictos entre gestos del sistema y el pager en navegación por gestos.
- Perfil de trabajo, fondo claro con `HINT_SUPPORTS_DARK_TEXT`, versiones de Android anteriores a la 36.
- Versión release en ejecución (R8 puede revelar problemas que la depuración no muestra).
