package com.oasisevolve.launcher.focus

/** Dificultad de acceso. Solo afecta a la visibilidad dentro de este launcher, nunca al sistema. */
enum class AccessLevel(val level: Int, val title: String, val description: String) {
    VISIBLE(1, "Nivel 1: visible", "Aparece normalmente donde la coloques."),
    DRAWER_ONLY(2, "Nivel 2: solo en la lista", "Solo aparece en el App Drawer."),
    HIDDEN(3, "Nivel 3: oculta", "Solo se encuentra con la búsqueda deliberada."),
    GUARDED(4, "Nivel 4: protegida", "Oculta, y además con interrupción y espera antes de abrirse.");

    /** ¿Se muestra en inicio, apps útiles y biblioteca? */
    val inVisualScreens: Boolean get() = this == VISIBLE

    /** ¿Se muestra en la lista normal del App Drawer? */
    val inDrawer: Boolean get() = this == VISIBLE || this == DRAWER_ONLY

    companion object {
        fun of(level: Int?): AccessLevel = entries.firstOrNull { it.level == level } ?: VISIBLE
    }
}
