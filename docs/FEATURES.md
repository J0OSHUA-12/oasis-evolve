# Funciones implementadas y pendientes (v0.1.0)

## Implementado y probado en emulador

- Launcher predeterminado, botón Inicio y Atrás.
- Seis pantallas con transición; la biblioteca se puede desactivar.
- Apps reales con `LauncherApps`, actualizadas al instalar o desinstalar.
- Inicio: reloj y fecha, selección manual, reordenar con «Mover antes/después», dock.
- Apps útiles: secciones «Quiero usar más» y «Útiles».
- App Drawer solo texto: orden alfabético, búsqueda por iniciales, riel de letras.
- Cuatro niveles de acceso, búsqueda deliberada y filtrado en todas las pantallas.
- Interrupción de nivel 4 con espera y datos de uso reales.
- Biblioteca: categorías automáticas, mover app de categoría, búsqueda.
- Bienestar: uso en 24 h, 7 y 30 días; progreso de año, semestre y mes; notas con editar, copiar, compartir y borrar.
- Widgets externos por la API oficial: añadir, conservar y quitar.
- Ajustes: iconos, nombres, columnas, dock, reloj, espera, tema, reducir animaciones.
- Ajustes y notas persistentes tras reiniciar el proceso.

## Implementado, sin prueba completa

- Comparación con el periodo anterior y mensaje de cobertura: lógica cubierta por pruebas unitarias, pero el emulador
  recién creado no tenía historial que comparar.
- Proyección de tiempo en la interrupción: cálculo probado; el texto no se ha visto con datos reales suficientes.
- Widgets con pantalla de configuración: se probó cancelar, no completar la configuración.
- Perfil de trabajo: código presente, sin probar.

## Pendiente por fase

**Fase 2:** arrastrar y soltar, carpetas, widgets en inicio, tamaños y espaciado configurables, efecto cristal real,
paquetes de iconos, fuentes, crear/renombrar/ocultar categorías, vista compacta o expandida.

**Fase 3:** Focus Mode, tipos de interrupción (matemáticas, mensaje), interrupciones en apps visibles y carpetas, lista de
esenciales, filtro de notificaciones, gestos asignables, modo inmersivo y opciones restantes del drawer.

**Fase 4:** resumen diario propio para superar la retención del sistema, gráficas por día, mensajes de mejora, límites de tiempo.

**Fase 5:** modo estudio, perfiles, widgets académicos y de proyecto, Pomodoro, calendario, enlaces a notas de Obsidian,
exportar e importar ajustes.

**Fase 6:** pruebas en tu teléfono real y optimización.
