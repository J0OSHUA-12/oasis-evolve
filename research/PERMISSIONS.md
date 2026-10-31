# Permisos

## Oasis 5.18 (declarados en el manifest)

| Permiso | Uso observado o probable |
|---|---|
| `PACKAGE_USAGE_STATS` | Estadísticas de uso |
| `QUERY_ALL_PACKAGES` | Listado de paquetes |
| `READ_CALENDAR` | Widget de calendario |
| `SET_WALLPAPER` | Fijar color de tema como fondo |
| `EXPAND_STATUS_BAR` | Deslizar abajo para ver notificaciones |
| `REQUEST_DELETE_PACKAGES` | Desinstalar desde el launcher |
| `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Notificación del filtro |
| `BIND_NOTIFICATION_LISTENER_SERVICE`, `BIND_ACCESSIBILITY_SERVICE`, `BIND_DEVICE_ADMIN` | Protegen los servicios correspondientes |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` | Red, SDK y trabajo en segundo plano |
| `AD_ID`, `ACCESS_ADSERVICES_*`, `BIND_GET_INSTALL_REFERRER_SERVICE` | Publicidad y atribución de SDK |
| `BILLING`, `CHECK_LICENSE` | Compra y licencia |

## Oasis Evolve

Principio: cada permiso se pide solo cuando una función activada lo necesita.

### Declarados en v0.1

| Permiso | Motivo | Cómo se concede | Si se rechaza |
|---|---|---|---|
| `<queries>` con `MAIN`/`LAUNCHER` | Ver apps abribles; evita `QUERY_ALL_PACKAGES` | Automático | No aplica |
| `PACKAGE_USAGE_STATS` | Uso real en bienestar e interrupciones | Manual, en Acceso a datos de uso | El panel lo explica; lo demás funciona |
| `REQUEST_DELETE_PACKAGES` | Abrir el diálogo de desinstalación | Automático; el sistema confirma | No aplica |

**No hay permiso `INTERNET`.** El propio sistema impide que el launcher envíe tus estadísticas a cualquier servidor.

El vínculo de widgets usa `ACTION_APPWIDGET_BIND`, con confirmación del sistema, y no requiere un permiso propio.

### Previstos en fases posteriores (todos opcionales)

| Permiso o servicio | Función | Motivo y riesgo |
|---|---|---|
| Notification Listener | Filtro de notificaciones | Permite leer notificaciones; se guardarán solo en local |
| `READ_CALENDAR` | Widget de calendario | Solo si se añade ese widget |
| `POST_NOTIFICATIONS`, servicio en primer plano | Monitor de límites | Se muestra una notificación visible mientras esté activo |
| `SYSTEM_ALERT_WINDOW` | Pantalla de límite agotado sobre otra app | Sensible; solo si activas el modo de límites reforzado |

### Evaluados y no adoptados por ahora

| Permiso | Motivo |
|---|---|
| `AccessibilityService` | Da mucha más capacidad de la necesaria. No se usará como control genérico. Solo se reconsiderará con una finalidad concreta y tu aprobación |
| Administrador de dispositivo | Dificulta la desinstalación, y su API de bloqueo es limitada |
| Propietario de dispositivo | Exigiría quitar cuentas del teléfono; riesgo desproporcionado |
| `AD_ID`, analítica | Contrarios al requisito de privacidad |
