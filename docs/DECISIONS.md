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
| 14 | 2026-10-31 | Arrastre propio sobre la cuadrícula, sin biblioteca externa | Son pocas líneas y evitan una dependencia |
| 15 | 2026-10-31 | El orden provisional solo se guarda al soltar | Evita una escritura por cada elemento que cruza el dedo |
| 16 | 2026-10-31 | Una app vive en una sola carpeta; borrar una carpeta devuelve sus apps | Comportamiento predecible y sin pérdida de datos |
| 17 | 2026-10-31 | Iconos adaptativos dibujados sin la máscara del sistema | Permite aplicar la forma elegida; los de un paquete no se recortan |
| 18 | 2026-10-31 | Fuentes Manrope, Outfit y JetBrains Mono, bajo licencia OFL | Libres de incluir; licencias en `docs/licenses` y ninguna tomada del APK de Oasis |
| 19 | 2026-10-31 | Las páginas no visibles rechazan el foco | Con teclado físico, el foco hacía saltar el carrusel |
| 20 | 2026-10-31 | Ocultar una categoría también la excluye de la búsqueda de la biblioteca | «Ocultar» debe ser coherente; sus apps siguen en el App Drawer |
| 21 | 2026-10-31 | Excluir todos los launchers del tiempo de uso | Estar en la pantalla de inicio no es usar una app |
| 22 | 2026-10-31 | Cristal esmerilado experimental y desactivado | No se pudo comprobar el efecto; Android no permite leer el fondo para desenfocar solo las tarjetas |
| 23 | 2026-10-31 | Módulo `fixtures/iconpack` solo para pruebas | Permite probar paquetes de iconos reales; nunca forma parte del launcher |
| 24 | 2026-10-31 | Inicio y Apps útiles comparten una cuadrícula libre de celdas | Es lo que permite mover y redimensionar; sustituye a las listas ordenadas |
| 25 | 2026-10-31 | Migración automática desde 0.2, sin borrar nada | Tus apps y widgets actuales van a los huecos libres de la composición |
| 26 | 2026-10-31 | La composición de referencia se aplica una sola vez | Después la pantalla es tuya; se puede restaurar desde Ajustes |
| 27 | 2026-10-31 | Nombres bajo los iconos desactivados en la composición | La referencia no los muestra; se reactivan en Home Screen |
| 28 | 2026-10-31 | Tema oscuro al aplicar la composición si seguía el del sistema | Las referencias son oscuras; un tema elegido a mano se respeta |
| 29 | 2026-10-31 | «Quiero usar más» pasa a ser una carpeta | La pantalla ya no tiene títulos, y así no se pierde esa agrupación |
| 30 | 2026-10-31 | Sin clima en el reloj | Exigiría Internet, en contra del requisito de privacidad |
| 31 | 2026-10-31 | Fotos copiadas y reducidas a almacenamiento privado | No requiere permisos de galería y no depende de que la foto siga allí |
| 32 | 2026-10-31 | Una sola pasada por los eventos para todas las ventanas de uso | Mucho menos costoso que consultar Android una vez por ventana |
| 33 | 2026-10-31 | `EvolveWidgetHostView` intercepta la pulsación larga | Sin ello, mantener pulsado un widget abría su app |
| 34 | 2026-10-31 | Focus Mode, Notification Filter y Gestures visibles como pendientes | Pediste las secciones, pero son de una fase posterior: no se finge que funcionan |
| 35 | 2026-10-31 | El dock busca la actividad exacta que atiende marcar o la cámara | En HiOS, Teléfono y Contactos son el mismo paquete |
| 36 | 2026-10-31 | Una apertura es volver a una app desde otra, como en Android | Desbloquear el teléfono inflaba la cifra: 57 frente a 16 |
| 37 | 2026-10-31 | Cada ventana de uso recibe también los eventos anteriores a ella | Así se sabe qué estaba abierto sin suponerlo; suponerlo inventó 20 días de uso |
| 38 | 2026-10-31 | Lo que no cabe en la composición va a Widgets o Apps útiles | Una fila oculta hacía que Inicio se desplazara sin que se viera nada |
| 39 | 2026-10-31 | Avisar de widgets sin contenido en lugar de ocultar los de otro launcher | Solo se comprobó en un widget de HiOS; otros podrían funcionar |
| 40 | 2026-11-03 | Servicio de accesibilidad limitado para el bloqueo y el temporizador | Lo elegiste frente a superposición con vigilancia continua y frente a no añadir permisos |
| 41 | 2026-11-03 | El servicio solo escucha las apps vigiladas y no lee la pantalla | Lista de paquetes fijada en tiempo real; `canRetrieveWindowContent=false` |
| 42 | 2026-11-03 | Un minuto fuera de la app basta para volver a pedir la interrupción | Cubre volver desde Recientes sin estorbar al contestar un mensaje rápido |
| 43 | 2026-11-03 | Al acabarse el temporizador, la siguiente apertura pasa por la interrupción | Si no, reabrir la app reiniciaría el tiempo sin esfuerzo |
| 44 | 2026-11-03 | Filtro con la función de posponer de Android, sin guardar el contenido | Mínimos datos personales; contrapartida: un reinicio puede soltarlas antes |
| 45 | 2026-11-03 | Versión «personal» optimizada con el identificador y la firma de depuración | Se instala encima sin perder datos; la de depuración era bastante más lenta |
| 46 | 2026-11-03 | Exportar e importar copia desde la app | Con la versión optimizada ya no se pueden copiar los datos por USB |
| 47 | 2026-11-03 | Al volver a Inicio solo se recalcula el día de hoy | Recalcular 60 días en cada vuelta coincidía con la animación |
| 48 | 2026-11-04 | Los accesos a Ajustes de Android abren una tarea nueva | Si no, Android mostraba la última pantalla de Ajustes abierta, no la pedida |
| 49 | 2026-11-04 | Tras cerrar una app por tiempo se ignoran sus avisos durante 3 s | Su último cambio de ventana se tomaba como una nueva apertura |

## Decisiones pendientes de tu aprobación

1. Modo de límites reforzado, con permiso de superposición (Fase 4).
2. Ampliar el servicio de accesibilidad más allá de detectar qué app se abre. La versión limitada ya la aprobaste.
3. Método para el doble toque de bloqueo (Fase 3).
4. Añadir clima, que exigiría permiso de Internet.
