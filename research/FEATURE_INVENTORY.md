# Inventario de funciones y tabla comparativa

Clasificación pedida:

- **ID**: función identificada y verificable en el APK.
- **PC**: parcialmente comprendida.
- **NV**: no verificable con el material disponible.
- **NUEVA**: función propuesta para nuestro launcher.

Estado en Oasis Evolve: ✅ en v0.1 · 🟡 parcial en v0.1 · ⏳ fase posterior · ❌ descartada.

## Pantallas y organización

| Función | Oasis original | Clase | Decisión | Oasis Evolve | Estado |
|---|---|---|---|---|---|
| Launcher predeterminado | `HOME`, abre ajustes de inicio | ID | Conservar | `RoleManager`, con alternativa a ajustes | ✅ |
| Lista textual de apps | App Drawer | ID | Conservar | Siempre textual, sin iconos | ✅ |
| Búsqueda en el drawer | Sí, con iniciales | ID | Conservar | Coincidencia por iniciales y subcadena, sin acentos | ✅ |
| Fast scroll por letras | Sí, también a la izquierda | ID | Conservar | Riel de letras | ✅ (lado derecho) |
| Búsqueda abajo / ocultar búsqueda | Sí | ID | Conservar | Ajuste | ⏳ |
| Abrir si queda una sola app | Instant App | ID | Conservar | Ajuste | ⏳ |
| Teclado automático | Sí | ID | Conservar | Ajuste | ⏳ |
| Instaladas recientemente (3 días) | Sí | ID | Conservar | Sección opcional | ⏳ |
| Apps web y atajos del navegador | Sí | ID | Conservar | `CONFIRM_PIN_SHORTCUT` | ⏳ |
| Renombrar apps | `RenamedApp` | ID | Conservar | Pendiente | ⏳ |
| Carpetas | `Folder`, `FolderApp` | ID | Modificar | Carpetas en Inicio y Apps útiles | ✅ |
| Paquetes de iconos | Sí | ID | Conservar | Formato `appfilter.xml` | ✅ |
| Pantalla principal | Reloj, apps en lista, iconos laterales | ID | Modificar | Cuadrícula de iconos, dock, widgets | 🟡 |
| Selección manual de apps en inicio | Sí, con límite Pro | ID | Conservar | Sin límite, sin autopoblar | ✅ |
| Reordenar con arrastrar | Mover arriba/abajo | PC | Modificar | Cuadrícula libre, dock y carpetas | ✅ |
| Seis pantallas | Número exacto incierto | NV | Cambiar | 2 izquierda, inicio, 3 derecha | ✅ |
| Apps que aportan valor | No existe | NUEVA | Nueva | Selección manual y categoría «Quiero usar más» | ✅ |
| App Library visual | No existe | NUEVA | Nueva | Carpetas automáticas, búsqueda, desactivable | 🟡 |
| Edición manual de categorías | No existe | NUEVA | Nueva | Mover app de categoría | ✅ |
| Crear, renombrar y ocultar categorías | No existe | NUEVA | Nueva | Editor de categorías y arrastre entre ellas | ✅ |
| Vista compacta o expandida | No existe | NUEVA | Nueva | Selector en la biblioteca | ✅ |
| Fondo real del dispositivo | Opción | ID | Cambiar | Siempre activo, sin opción | ✅ |
| Fondos Lottie y temas de color | Sí | ID | Descartar | Sin fondos animados, con tema propio | ❌ |
| Modo inmersivo / ocultar barra de estado | Sí | ID | Conservar | Immersive Mode en Customization | ✅ |

## Bienestar y distracciones

| Función | Oasis original | Clase | Decisión | Oasis Evolve | Estado |
|---|---|---|---|---|---|
| Uso por app | Hoy y 7 días | ID | Ampliar | 24 h, 7 y 30 días con cobertura real | ✅ |
| Comparar con el periodo anterior | No observado | NUEVA | Nueva | Solo con periodos completos y equivalentes | ✅ |
| Gráficas | No observado | NUEVA | Nueva | Barras por app y por día | ✅ |
| Year Progress | Día, semana, mes, año | ID | Ampliar | Día, semana, mes, semestre y año | ✅ |
| Notas | Sí | ID | Conservar | Escribir, editar, eliminar, copiar, compartir | ✅ |
| To-do, calendario | Sí | ID | Evaluar | Calendario en Widget Center, Fase 5 | ⏳ |
| Minijuegos, trivia, chistes | Sí | ID | Descartar | Excluidos por requisito | ❌ |
| Pomodoro | Sí | ID | Conservar | Widget opcional | ⏳ |
| Apps ocultas | Tabla y filtro local | ID | Ampliar | Cuatro niveles | ✅ |
| Interrupción previa | 5 tipos, desde launcher | ID | Modificar | Datos reales, espera, Continuar/Volver | 🟡 |
| Espera configurable | Fija en 10 s | ID | Ampliar | 5, 10, 15 s o más | ✅ |
| Matemáticas / mensaje | Sí | ID | Conservar | Tipos opcionales, Fase 3 | ⏳ |
| Interrumpir carpetas y al salir de Focus | Sí | ID | Conservar | Fase 3 | ⏳ |
| Excluir apps esenciales | No observado | NUEVA | Nueva | Lista de esenciales | ⏳ |
| Proyección de tiempo | No observado | NUEVA | Nueva | En tarjetas, detalle e interrupción | ✅ |
| Mensajes de mejora | No observado | NUEVA | Nueva | Comparación semanal y de 30 días; objetivos en Fase 4 | 🟡 |
| Límites de tiempo | No existe | NUEVA | Nueva | Ver `TECHNICAL_LIMITATIONS.md` | ⏳ |
| Focus Mode | Interfaz reducida | ID/PC | Conservar | Fase 3 | ⏳ |
| Filtro de notificaciones | Listener, cancelar y guardar | ID | Conservar | Fase 3 | ⏳ |

## Widgets, gestos, ajustes y universidad

| Función | Oasis original | Clase | Decisión | Oasis Evolve | Estado |
|---|---|---|---|---|---|
| Widgets de terceros | `AppWidgetHost` | ID | Conservar | En Widget Center con API oficial | 🟡 |
| Redimensionar widgets | Sí | ID | Conservar | Por celdas con asa en edición | ✅ |
| Galería de widgets con vista previa | Solo nombre | ID | Modificar | Vista previa real, tamaños y detalle | ✅ |
| Widgets en inicio | Sí | ID | Conservar | Añadir, ordenar y quitar | ✅ |
| Doble toque para bloquear | Device admin | ID | Evaluar | Fase 3; alternativa en limitaciones | ⏳ |
| Deslizar arriba/abajo | Sí | ID | Conservar | Fase 3 con acciones asignables | ⏳ |
| Show App Icons | Sí | ID | Modificar | Solo afecta a pantallas con iconos; drawer siempre textual | ✅ |
| Tipografías | 15 fuentes | ID | Modificar | Tres fuentes OFL y la del sistema; no se copian las de Oasis | ✅ |
| Facebook, Firebase, AD_ID | Sí | ID | Descartar | Sin analítica ni permiso `INTERNET` | ❌ |
| Compra y licencia | Billing, PairIP | ID | Descartar | No aplica | ❌ |
| Modo estudio, perfiles | No existe | NUEVA | Nueva | Fase 5 | ⏳ |
| Widget de proyecto activo | No existe | NUEVA | Nueva | Fase 5 | ⏳ |
| Obsidian | No existe | NUEVA | Nueva | Compartir nota en v0.1; enlaces en Fase 5 | 🟡 |
| Exportar/importar ajustes | No observado | NUEVA | Nueva | Archivo local, Fase 5 | ⏳ |

## No verificable con el material disponible

- Transiciones exactas entre pantallas y su número total.
- Nombres de claves y valores por defecto de los ajustes.
- Tráfico real de los SDK.
- Si existe código relevante en el split nativo que falta.
