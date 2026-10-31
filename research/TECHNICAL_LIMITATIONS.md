# Limitaciones técnicas y viabilidad en Android

Las afirmaciones de plataforma proceden de la documentación de Android tal como la conozco; las marcadas con
**[Probar]** deben confirmarse en tu teléfono, un Tecno LJ7 con HiOS 16.3 y Android 16. El ahorro de batería
del fabricante puede agravar los puntos sobre segundo plano. **[Inferencia]**

## Clima en el widget de reloj

No está incluido. Consultar el clima exige Internet, y esta app no tiene ese permiso para garantizar que tus
datos no salen del teléfono. Añadirlo es una decisión tuya, no un detalle técnico: requiere pedir `INTERNET` y,
para el clima local, una ubicación. Mientras tanto, la galería permite colocar el widget de clima de una app instalada.

## Resumen de viabilidad

| Requisito | ¿Viable? | Cómo | Garantía |
|---|---|---|---|
| Ocultar apps en el launcher | Sí | Filtro propio | Total dentro del launcher; nunca en Ajustes u otros launchers |
| Interrupción al abrir desde el launcher | Sí | Pantalla previa | Total para aperturas desde el launcher |
| Interrupción al abrir desde otra vía | Parcial | Monitor en primer plano | Mejor esfuerzo, con retraso |
| Medir uso real | Sí | `UsageEvents` con Usage Access | Buena, dentro de la retención del sistema |
| Límite diario estricto | No garantizado | Monitor y pantalla de bloqueo | Mejor esfuerzo; se etiquetará así |
| Cerrar otra app a la fuerza | **No** | Android no lo permite a apps normales | No se prometerá |
| Filtrar notificaciones | Sí | Notification Listener | Buena, con excepciones |

## Límites de tiempo: alternativas

1. **Puerta del launcher (recomendada como base).** Si la app agotó su límite, el launcher no la abre sin la
   interrupción. Fiable, sin permisos especiales, pero se evita abriendo la app por una notificación o un enlace.
2. **Monitor con servicio en primer plano.** Consulta eventos de uso cada pocos segundos mientras una app limitada
   está abierta y lleva al usuario al launcher. Requiere Usage Access, notificación visible y, para mostrarse sobre
   otra app, `SYSTEM_ALERT_WINDOW`, porque Android restringe iniciar actividades desde segundo plano desde la API 29.
   Hay retraso, gasta algo más de batería y el sistema o el fabricante pueden detenerlo. **[Probar]**
3. **AccessibilityService.** Reacciona antes a los cambios de ventana, pero da una capacidad muy amplia. No se usará
   salvo decisión explícita tuya. Play restringe este uso; para una app personal el riesgo principal es de seguridad.
4. **Suspender paquetes o propietario de dispositivo.** Es lo único realmente estricto, pero la API requiere privilegios
   de sistema o dispositivo administrado. Descartado.

**Decisión:** alternativa 1 por defecto y alternativa 2 como «modo reforzado» opcional en la Fase 4. Es una decisión de
alto impacto por el permiso de superposición, así que se te presentará antes de activarla.

Llamadas, emergencias y Ajustes quedarán siempre fuera de todo bloqueo, y debe existir una forma inmediata de desactivarlo.

## Estadísticas de uso

- `queryUsageStats` conserva datos diarios unos 7 días, semanales 4 semanas, mensuales 6 meses y anuales 2 años.
- Por eso «últimos 30 días» puede no tener detalle completo. La app muestra los **días realmente cubiertos** y no
  inventa el resto. La solución definitiva es guardar un resumen diario propio con WorkManager desde la instalación (Fase 4).
- Un periodo no se compara con el anterior si este no tiene cobertura suficiente.
- Las proyecciones se calculan sobre el promedio real, con su periodo indicado, y se expresan como estimación.
- El reloj de hoy parte de medianoche; «24 horas» es una ventana móvil. Se muestran como cosas distintas, a diferencia de Oasis.

## Instalación fuera de Play

- Desde Android 13, las apps instaladas fuera de una tienda pueden tener bloqueados Accesibilidad y Notification Listener
  hasta activar «Permitir ajustes restringidos» en la información de la app. **[Probar]** en tu versión.
- La app no comprueba ninguna licencia ni depende de Play.

## Launcher y sistema

- **Gestos:** la navegación por gestos reserva los bordes laterales e inferior para el sistema. Se evita depender de ellos.
- **Botón Inicio:** llega como `onNewIntent` y debe devolver a la pantalla central. Implementado.
- **Atrás:** vuelve a la pantalla principal; un launcher no debe cerrarse.
- **Deslizar para notificaciones:** Oasis usa reflexión sobre una API oculta, que puede fallar según versión y fabricante. Se evaluará en la Fase 3.
- **Doble toque para bloquear:** sin device admin ni accesibilidad, no existe una API pública directa para bloquear la pantalla. Se decidirá en la Fase 3.
- **Widgets:** `AppWidgetHost` con Compose requiere `AndroidView`. Algunos widgets de terceros se comportan mal fuera de launchers conocidos. **[Probar]**
- **Efecto cristal:** el fondo pertenece a otra ventana, y Android restringe leerlo a apps normales, así que no se pueden desenfocar solo las tarjetas. La única vía es desenfocar lo que hay detrás de toda la ventana (API 31+). Se probaron `setBackgroundBlurRadius` y `FLAG_BLUR_BEHIND`: en el emulador ninguna produjo un efecto visible, aunque el sistema lo indicaba activo. **[Probar]** en un dispositivo físico.
- **Notificaciones:** Android 15 puede ocultar contenido sensible, como códigos, a los listeners no confiables.

## Limitaciones de la verificación actual

- No hay teléfono conectado por ADB en este equipo en el momento del análisis.
- El análisis del APK es estático. El SDK no incluye `cmdline-tools`, pero sí emulador e imagen Android 36.
- Los resultados de pruebas reales están en `docs/TEST_RESULTS.md`.
