pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OasisEvolve"
include(":app")
// Paquete de iconos mínimo: solo sirve para probar la carga de paquetes.
include(":fixtures:iconpack")
