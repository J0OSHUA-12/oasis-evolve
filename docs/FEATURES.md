# Funciones implementadas y pendientes (v0.2.0)

## Implementado y probado en emulador

**Base (v0.1)**

- Launcher predeterminado, botón Inicio y Atrás.
- Seis pantallas con transición; la biblioteca se puede desactivar.
- Apps reales con `LauncherApps`, actualizadas al instalar o desinstalar.
- App Drawer solo texto: orden alfabético, búsqueda por iniciales, riel de letras.
- Cuatro niveles de acceso, búsqueda deliberada e interrupción de nivel 4 con espera y datos reales.
- Bienestar: uso en 24 h, 7 y 30 días; progreso de año, semestre y mes; notas con editar, copiar, compartir y borrar.

**Interfaz premium (v0.2)**

- Arrastrar y soltar en Inicio y Apps útiles. Mantener pulsado sin mover abre las opciones.
- Carpetas en Inicio y Apps útiles: crear, añadir, sacar, renombrar y deshacer sin perder apps.
- Widgets en Inicio: añadir, tamaños S a XL, subir, bajar y quitar. También en Widget Center.
- Biblioteca: vista compacta o expandida, crear, renombrar, ocultar y borrar categorías, y arrastrar una app a otra categoría en la vista expandida.
- Iconos: tamaño, forma (redondeado, círculo, cuadrado) y paquetes con formato `appfilter.xml`.
- Tema claro y oscuro, cinco acentos, cuatro tipografías y cuatro tamaños de texto.
- Espaciado, márgenes y columnas configurables. Reducir animaciones.
- Ajustes organizados en secciones plegables.
- Actualización desde v0.1 sin perder apps, niveles ni notas.
- Versión release (R8) instalada y ejercitada: arrastre, carpetas, notas, uso y persistencia.

## Implementado, sin prueba completa

- **Cristal esmerilado (experimental):** se probaron dos API oficiales y en el emulador **no produjo desenfoque visible**,
  aunque Android lo indica como activo. Desactivado por defecto; falta comprobarlo en tu teléfono.
- Comparación con el periodo anterior: lógica con pruebas unitarias, sin historial real que comparar.
- Proyección de tiempo en la interrupción: cálculo probado; el texto no se ha visto con datos suficientes.
- Completar la configuración de un widget: se probó cancelarla, no terminarla.
- Paquetes de iconos reales: se probó con un paquete mínimo propio, no con uno publicado.
- Perfil de trabajo: código presente, sin probar.

## Problemas conocidos

- En tema claro con fondo oscuro, los iconos de la barra de estado se ven blancos sobre la pantalla de Ajustes.
- El dock no se ordena arrastrando: usa «Mover antes/después». Tampoco se puede arrastrar una app entre pantallas ni al dock.
- Dentro de una carpeta las apps no se reordenan.
- Si se cierra el proceso mientras se configura un widget, ese identificador queda sin liberar.
- Las pulsaciones inyectadas por `adb` a veces perdieron o duplicaron una letra. Parece un efecto de la prueba, pero debe confirmarse con el teclado real.

## Pendiente por fase

**Fase 3:** Focus Mode, tipos de interrupción (matemáticas, mensaje), interrupciones en apps visibles y carpetas, lista de
esenciales, filtro de notificaciones, gestos asignables, modo inmersivo y opciones restantes del drawer.

**Fase 4:** resumen diario propio para superar la retención del sistema, gráficas por día, mensajes de mejora, límites de tiempo.

**Fase 5:** modo estudio, perfiles, widgets académicos y de proyecto, Pomodoro, calendario, enlaces a notas de Obsidian,
exportar e importar ajustes.

**Fase 6:** pruebas en tu teléfono real y optimización.
