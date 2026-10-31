package com.oasisevolve.launcher

import com.oasisevolve.launcher.storage.GridTarget

/** Lugares donde se coloca una app a mano. */
enum class AppList(val label: String, val target: GridTarget?) {
    HOME("Pantalla principal", GridTarget.HOME),
    USEFUL("Apps útiles", GridTarget.USEFUL),
    DOCK("Dock", null),
}
