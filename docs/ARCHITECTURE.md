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
| `home` | Inicio, reloj, dock y hojas de acciones y selección |
| `useful` | «Quiero usar más» y «Útiles» |
| `drawer` | Lista textual, riel, búsqueda normal y deliberada |
| `library` | Categorías automáticas y tarjetas |
| `wellbeing` | Uso, progreso del periodo y notas |
| `widgets` | Widget Center y ciclo oficial de `AppWidgetHost` |
| `usage` | Cálculo y lectura de eventos de uso |
| `focus` | Niveles de acceso e interrupción |
| `settings` | Pantalla de ajustes |
| `storage` | DataStore y Room |
| `system` | `LauncherApps`, launcher predeterminado |
| `ui` | Tema, tarjetas, icono y contraste sobre el fondo |

## Privacidad

El manifest elimina explícitamente `INTERNET`, de modo que ninguna dependencia pueda añadirlo. Se puede comprobar con:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" dump permissions app\build\outputs\apk\debug\app-debug.apk
```

## Puntos de extensión previstos

Las integraciones futuras (Telegram, Activepieces) no deben ser dependencias del núcleo. Si se añaden, necesitarán un módulo
aparte y una decisión explícita, porque exigirían permiso de red.
