# Plan de implementación

## Arquitectura elegida

**Aplicación propia, nativa, en Kotlin y Jetpack Compose.** MVVM ligero: repositorios, un ViewModel y pantallas.

### Base propia frente a base open source

| Criterio | Base propia (elegida) | Bifurcar Lawnchair |
|---|---|---|
| Encaje con seis pantallas y bienestar | Directo | Exige desmontar el modelo de Launcher3 |
| Tamaño y mantenimiento | Pequeño y comprensible | Grande, basado en vistas y código de AOSP |
| Actualizaciones | Solo las nuestras | Dependencia de un proyecto enorme |
| Funciones ya resueltas (arrastre, iconos) | Hay que escribirlas | Incluidas |
| Licencia | Tuya | GitHub no identifica la licencia automáticamente; requiere revisión |

Kvaesitso, Olauncher, Neo Launcher y KISS figuran como GPL-3.0 según la API de GitHub. Se pueden estudiar sus ideas, pero
incorporar su código obliga a publicar el proyecto bajo GPL-3.0 si se distribuye. **No se copia código de ninguno.**
Todas las licencias deben volver a revisarse antes de reutilizar cualquier fragmento.

## Versiones

| Componente | Versión | Motivo |
|---|---|---|
| Gradle | 8.14.3 | Ya en caché |
| Android Gradle Plugin | 8.13.0 | Ya en caché, compatible con API 36 |
| compileSdk / targetSdk | 36 | SDK instalado |
| minSdk | 26 | `LauncherApps` y canales modernos; cubre tu teléfono |
| JDK | Temurin 21 | Instalado |

## Estructura

```
com.oasisevolve.launcher
├── home/          Pantalla principal
├── navigation/    Pager de seis pantallas
├── useful/        Apps que aportan valor
├── drawer/        App Drawer textual
├── library/       Biblioteca visual
├── wellbeing/     Uso, progreso y notas
├── widgets/       Widget Center y AppWidgetHost
├── usage/         Cálculo de estadísticas (lógica pura y probada)
├── focus/         Niveles de acceso e interrupción
├── settings/      Ajustes
├── storage/       DataStore y Room
├── system/        LauncherApps y permisos
└── ui/            Tema y componentes
```

## Fases

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Investigación y documentación | Completa con las limitaciones descritas |
| 1 | Base funcional: compila, se instala, seis pantallas, apps reales, preferencias | Ver `docs/FEATURES.md` |
| 2 | Interfaz premium: arrastrar, carpetas, widgets en inicio, temas, iconos | Implementada; cristal sin efecto verificado |
| 3 | Focus Mode, tipos de interrupción, filtro de notificaciones, gestos | Pendiente |
| 4 | Resumen diario propio, proyecciones, gráficas, límites | Pendiente |
| 5 | Modo estudio, perfiles, widgets académicos, Obsidian, exportar | Pendiente |
| 6 | Pruebas amplias en tu teléfono y optimización | Pendiente |

## Decisiones que requerirán tu aprobación

1. Activar el modo de límites reforzado, por el permiso de superposición (Fase 4).
2. Cualquier uso de `AccessibilityService`.
3. Cómo implementar el doble toque para bloquear (Fase 3).
