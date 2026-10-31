# Análisis del APK de Oasis Launcher

Análisis **estático**: nada del APK se ejecutó y no se usó ningún dispositivo. Este documento describe
comportamiento; no reproduce código descompilado. El proyecto no contiene código ni recursos de Oasis.

Etiquetas: **[V]** verificado en el APK · **[I]** inferencia razonable · **[D]** requiere prueba dinámica.

## 1. Identidad del artefacto

| Dato | Valor |
|---|---|
| Archivo | `C:\Users\Yeshua\Downloads\Oasis Launcher.apk` (8.868.951 bytes) |
| SHA-256 | `b7fbf7e6482b22113c9bc008f839379a99bf57e89d7155531d974cc2f36ec19e` |
| Paquete / versión | `com.crimson.oasislauncher` 5.18 (versionCode 104) |
| SDK | min 24, target 36, compile 36 |
| Firma declarada | v2 y v3, certificado `e3f195d1…43c50c`; source stamp de Play. La firma **no** se verificó criptográficamente |
| Código | `classes.dex` único; 7.478 clases según JADX, 7.999 archivos smali según Apktool |
| Resultados por idioma | Solo `values/` en inglés; sin carpetas `values-xx` |

### Herramientas y evidencia

| Paso | Herramienta | Evidence ID de REA |
|---|---|---|
| Firmas | REA `inspect_apk_signature` | `ev_eb847aff5db35efb8c5f15b144953d15e81f7ab2f95742eacb54999d7a4ff51c` |
| Inventario ZIP (477 entradas) | REA `inspect_artifact` | `ev_45feba42e9d8c09b2904984fa50f07b89aebfb0e1e159a95ab3dc8379291e193` |
| Paquete y manifest | REA + JADX bridge 0.7.1 `inspect_android_package` | `ev_8e65cec088db76ef14cba1858e9ddf509adfe63e9c14f915c7bc0e94a71c5d65` |
| Manejador de eventos de uso (`lm3.e`) | REA + JADX `inspect_android_method` | `ev_62b747ea6309e5f8fa2e7bd8330d283ab8b1fc844aa60e96d08a5981fab3e00c` |
| Servicio de accesibilidad | REA + JADX `inspect_android_class` | `ev_eb52b19d419fc644b98b752149e95dafad49271126463a617aa9377402f796f2` |
| Recursos, manifest, smali | Apktool 3.0.3 | Salida local, fuera del repositorio |

Copias locales de la evidencia JSON: `research/evidence/` (excluida de Git porque incluye texto descompilado).

## 2. Limitaciones de este análisis

- **Ofuscación [V]:** R8 renombró casi todo. Solo unas pocas clases conservan su nombre (`MainActivity`,
  `AppDatabase`, los servicios y receptores). Los nombres como `t60` o `lm3` son del ofuscador.
- **APK parcial [V]:** el manifest declara `requiredSplitTypes="base__abi,base__density"` y
  `com.android.vending.splits.required=true`. Este archivo es solo la parte base: faltan los splits de ABI y densidad.
- **Sin bibliotecas nativas [V]:** el APK no contiene ningún `.so`. Si existen, están en el split de ABI que no se proporcionó.
- **Ghidra no aporta aquí [V]:** no hay binarios nativos que abrir. Además, la integración Windows de REA con Ghidra
  solo admite PE y NMOS 6502 en bruto, así que tampoco abriría un `.so` ELF.
- **Protección PairIP [V]:** `com.pairip.application.Application` es la clase Application y hay un flujo de licencia
  en `com.pairip.licensecheck`. No se analizó para eludirla ni se intentó ejecutar la app; queda fuera del alcance.
- **Compose ofuscado [V]:** la navegación y la composición se ven solo por recursos y llamadas, no por nombres de pantalla.
- **Claves de ajustes [I]:** se usa DataStore Preferences (hay metadatos de `androidx.datastore`), pero la búsqueda
  de claves no las recuperó. Sus nombres quedan sin determinar.

## 3. Componentes

| Componente | Función | Estado |
|---|---|---|
| `MainActivity` | Launcher (`HOME` + `LAUNCHER` + `DEFAULT`), `singleTask`, `excludeFromRecents`, `taskAffinity=""` | [V] |
| `PinShortcutConfirmActivity` | Recibe `CONFIRM_PIN_SHORTCUT`: atajos web desde el navegador | [V] |
| `NotificationListenerService` | Filtro de notificaciones con servicio en primer plano `specialUse` | [V] |
| `OasisAccessibilityService` | Abrir recientes con deslizar hacia arriba | [V] |
| `OasisAdminReceiver` | Administrador de dispositivo con política única `force-lock` | [V] |
| `AppDatabase` (Room) | Datos locales, ver sección 5 | [V] |
| Facebook SDK, Firebase Analytics, Crashlytics, Play Billing, AD_ID, AdServices | Terceros declarados | [V] |

`com.facebook.sdk.AutoLogAppEventsEnabled` y `AdvertiserIDCollectionEnabled` están en `true` en el manifest. **[V]**
Qué datos se envían realmente requiere captura de red. **[D]** Nuestro launcher no incluirá nada de esto.

## 4. Cómo funciona internamente

### Tiempo de uso
- **[V]** Consulta `UsageStatsManager.queryEvents` y recorre los eventos con una máquina de estados.
- **[V]** Procesa los tipos 1 (`ACTIVITY_RESUMED`), 2 (`ACTIVITY_PAUSED`), 23 (`ACTIVITY_STOPPED`),
  15 (`SCREEN_INTERACTIVE`), 17 (`KEYGUARD_SHOWN`), 18 (`KEYGUARD_HIDDEN`) y 26 (`DEVICE_SHUTDOWN`).
  Separa las sesiones por `paquete/clase`, de modo que un cambio entre actividades no corta el tiempo.
- **[V]** Si una sesión sigue abierta al final, suma hasta el instante actual.
- **[V]** El total de hoy parte de la medianoche local: pone a cero campos de `Calendar` (hora, minuto, segundo, ms).
- **[V]** Los «últimos 7 días» restan 6 días y parten de medianoche, así que son 6 días completos más hoy.
- **[I]** El recurso se llama `app_usage_24_hours` pero su texto es «Today»: es «hoy», no las últimas 24 horas.
- **[V]** También llama a `queryUsageStats` en una rama separada; su uso exacto no se determinó.

### Detección de apertura de apps
- **[V]** Las aperturas pasan por `LauncherApps.startMainActivity`.
- **[V]** `accessibility_service_config.xml` declara `accessibilityEventTypes=""` y `canRetrieveWindowContent="false"`.
- **[V]** El manifest no declara `SYSTEM_ALERT_WINDOW`, ni existe un servicio de monitorización en primer plano.
- **[I]** Oasis **no detecta** aperturas de apps desde fuera del launcher. Sus interrupciones actúan solo si abres
  la app desde Oasis. Abrirla desde una notificación o desde otra app la evitaría. **[D]** Confirmar en un dispositivo.

### Interrupciones
- **[V]** Tipos: confirmación, temporizador de 10 s, matemáticas (opción múltiple), matemáticas difícil (escribir la
  respuesta) y mensaje personalizado. La tabla `InterruptApp` guarda el tipo por app.
- **[V]** Se aplican a apps, carpetas (`InterruptFolder`), apps ocultas y a la salida de Focus Mode.
- **[V]** Los textos muestran el uso de hoy y de los últimos 7 días.

### Aplicaciones ocultas
- **[V]** Tabla `HiddenApp` y filtro dentro del launcher; se listan en Ajustes > App Drawer. No ocultan nada a nivel de sistema.

### Focus Mode
- **[V]** Ajustes: elegir apps (etiquetas `app_1` a `app_4`), botón en la pantalla principal, iconos inferiores,
  interrupción al salir y filtro de notificaciones específico.
- **[I]** Es una interfaz reducida dentro del launcher, sin bloqueo a nivel de sistema.
- **[D]** El límite exacto de cuatro apps debe confirmarse en uso real.

### Filtro de notificaciones
- **[V]** En `onNotificationPosted` comprueba si el paquete está en `NotificationFilteredApp`.
- **[V]** Si lo está, lee `android.title`, `android.text` y `android.bigText`, lo guarda en `FilteredNotification` y
  llama a `cancelNotification`. Las notificaciones quedan en la base de datos local para verlas después.

### Pantallas y transiciones
- **[V]** Hay página de widgets desactivable («Hide Widgets Page… Will restart App») y App Drawer aparte.
- **[V]** La captura muestra pestañas «Widgets | Oasis» en esa página.
- **[I]** El número exacto de páginas y el tipo de transición no se pudieron determinar por la ofuscación. **[D]**

### Listado de apps y atajos
- **[V]** `LauncherApps`: `getActivityList`, `registerCallback`, `startMainActivity`, `getShortcuts`, `startShortcut`,
  `pinShortcuts`. Declara `QUERY_ALL_PACKAGES`. El campo `user` en las tablas indica soporte de perfil de trabajo.

### Widgets
- **[V]** `AppWidgetHost` y `AppWidgetManager.bindAppWidgetIdIfAllowed`, con configuración y redimensionado.

### Gestos y sistema
- **[V]** Doble toque para bloquear: `DevicePolicyManager.lockNow`.
- **[V]** Deslizar arriba: desactivado, búsqueda de Google, recientes (accesibilidad, `performGlobalAction`) o una app.
- **[V]** Deslizar abajo: abre notificaciones mediante reflexión sobre `StatusBarManager.expandNotificationsPanel`.

## 5. Modelo de datos declarado

**[V]** Tablas Room observadas por sus sentencias SQL: `DefaultApp`, `FilteredNotification`, `Folder`, `FolderApp`,
`HiddenApp`, `HomeScreenApp`, `InterruptApp`, `InterruptFolder`, `NotificationFilteredApp`, `RenamedApp`, `Todo`.
Conclusión: casi toda la organización vive en tablas locales, y no hay tablas de estadísticas: el uso se recalcula.

## 6. Conexiones externas declaradas en el código

**[V]** Además de los SDK: `opentdb.com` (trivia) e `icanhazdadjoke.com` (chistes), que alimentan funciones que
no conservaremos. Política de privacidad alojada en flycricket.io.

## 7. Qué no se puede afirmar

- Qué datos envían los SDK de analítica en la práctica.
- Las reglas de negocio de funciones Pro, que no se investigaron a propósito.
- La jerarquía exacta de pantallas de ajustes más allá de lo que conectan los recursos.
- El código del split nativo.
