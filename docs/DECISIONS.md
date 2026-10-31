# Registro de decisiones técnicas

| # | Fecha | Decisión | Motivo |
|---|---|---|---|
| 1 | 2026-10-31 | Base propia en Kotlin y Compose | Encaja con seis pantallas; ver `research/IMPLEMENTATION_PLAN.md` |
| 2 | 2026-10-31 | Sin código de launchers GPL-3.0 | Obligaría a licenciar el proyecto bajo GPL-3.0 si se distribuye |
| 3 | 2026-10-31 | Sin permiso `INTERNET`, eliminado también en el manifest final | Garantía de privacidad impuesta por el sistema |
| 4 | 2026-10-31 | `<queries>` en lugar de `QUERY_ALL_PACKAGES` | Basta para listar apps abribles |
| 5 | 2026-10-31 | `applicationId` `com.oasisevolve.launcher`, con sufijo `.debug` | Se instala como app distinta de Oasis |
| 6 | 2026-10-31 | Nivel 4 también oculta la app, además de interrumpir | Los niveles son de dificultad creciente; interpretación razonable de tu petición |
| 7 | 2026-10-31 | Búsqueda deliberada con mínimo de 3 letras | Hace que encontrar una app oculta requiera intención |
| 8 | 2026-10-31 | «24 horas», «7 días» y «30 días» son ventanas móviles | Solo así son comparables con el periodo anterior |
| 9 | 2026-10-31 | No comparar si el periodo anterior no tiene datos suficientes | Requisito de no comparar periodos incompletos |
| 10 | 2026-10-31 | Color sobre el fondo según el fondo, con sombra y atenuación | El tema no garantizaba contraste: se detectó en la prueba real |
| 11 | 2026-10-31 | Orientación vertical fija | Simplifica la v0.1; puede revisarse |
| 12 | 2026-10-31 | Textos en español directamente en el código | Proyecto personal; migrar a `strings.xml` si se traduce |
| 13 | 2026-10-31 | Nombre «Oasis Evolve» conservado por ser el nombre que pediste | Antes de publicarla, conviene otro nombre para evitar confusión con Oasis |

## Decisiones pendientes de tu aprobación

1. Modo de límites reforzado, con permiso de superposición (Fase 4).
2. Cualquier uso de `AccessibilityService`.
3. Método para el doble toque de bloqueo (Fase 3).
