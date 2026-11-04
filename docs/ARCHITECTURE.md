# Arquitectura

Un solo módulo Gradle (`app`), una sola actividad y paquetes por función.

```
MainActivity ── LauncherRoot (pager y hojas)
                    │
             LauncherViewModel ── estado combinado (ajustes + apps)
                    │
   ┌────────────────┼──────────────┬───────────────┬─────────────┐
AppRepository  SettingsRepository  EvolveDatabase  UsageRepository  WidgetHostController
LauncherApps    DataStore (JSON)   Room (notas)    UsageEvents      AppWidgetHost
```

## Decisiones de estructura

- **Sin biblioteca de navegación ni de inyección de dependencias.** El pager resuelve la navegación, y `EvolveApp`
  crea los repositorios. Para un proyecto personal, añadir Hilt o Navigation no compensa.
- **Ajustes como un único JSON en DataStore.** Escritura atómica y base directa para exportar e importar.
- **Identidad de una app:** `componente#serie_de_usuario`. Distingue la misma app en el perfil personal y el de trabajo.
- **Las listas guardan claves, no apps.** Una app desinstalada simplemente deja de mostrarse, y vuelve a su sitio si se reinstala.
- **Lógica sin Android.** `UsageCalculator`, `AppSearch`, `Categorizer`, `PeriodProgress` y `AccessLevel` se prueban en la JVM.

## Paquetes

| Paquete | Responsabilidad |
|---|---|
| `navigation` | Pager de seis pantallas, Atrás, Inicio, indicador |
| `home` | Cuadrícula de Inicio y Apps útiles, edición, dock, carpetas y hojas de acciones |
| `drawer` | Lista textual, riel, búsqueda normal y deliberada |
| `library` | Categorías automáticas y tarjetas |
| `wellbeing` | Uso, progreso del periodo y notas |
| `widgets` | Galería con vistas previas, widgets propios, Widget Center y ciclo oficial de `AppWidgetHost` |
| `usage` | Cálculo y lectura de eventos de uso |
| `focus` | Niveles de acceso, interrupción, temporizador y servicio de bloqueo (`GuardService`, lógica en `GuardBook`) |
| `quiet` | Filtro de notificaciones (`QuietService`, horario en `QuietSchedule`) |
| `settings` | Pantalla de ajustes |
| `storage` | DataStore y Room |
| `system` | `LauncherApps`, launcher predeterminado |
| `ui` | Tema, tarjetas, icono y contraste sobre el fondo |

## Arrastre y organización

- `storage/HomeLayout.kt`: modelo puro de la cuadrícula: rectángulos de celdas, solapes, huecos y tamaños.
- `home/GridDrag.kt`: decide qué pasaría al soltar (`resolveDrop`) y sigue el dedo.
- `home/GridScreen.kt`: dibuja Inicio y Apps útiles, el modo edición y el dock. Es una sola pantalla para las dos.
- `storage/Organizer.kt`: operaciones puras sobre carpetas, dock, categorías y orden, cubiertas por `OrganizerTest`.
- `storage/DefaultLayout.kt`: composición de referencia y mezcla con lo que ya hubiera.
- `ui/Reorder.kt`: arrastre en cuadrículas perezosas. Se usa dentro de carpetas y en la biblioteca expandida.

## Estadísticas

`usage/UsageCalculator.kt` contiene el único cálculo. `UsageRepository` lee los eventos de los últimos 60 días en una sola
pasada y calcula todas las ventanas a la vez. El resumen se reutiliza durante 60 segundos en Bienestar, en el widget y en las interrupciones.

## Variantes de compilación

- `debug`: depurable, con un gancho de pruebas para `adb` (`src/debug`) que nunca va en las demás.
- `personal`: la del uso diario. Optimizada con R8, como la release, pero con el identificador y la firma de depuración para instalarse encima sin perder datos.
- `release`: para firmar con tu propia clave.

## Módulo de pruebas

`fixtures/iconpack` genera un paquete de iconos mínimo para probar la carga. No se incluye en el launcher.

## Privacidad

El manifest elimina explícitamente `INTERNET`, de modo que ninguna dependencia pueda añadirlo. Se puede comprobar con:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" dump permissions app\build\outputs\apk\debug\app-debug.apk
```

## Puntos de extensión previstos

Las integraciones futuras (Telegram, Activepieces) no deben ser dependencias del núcleo. Si se añaden, necesitarán un módulo
aparte y una decisión explícita, porque exigirían permiso de red.
