# Oasis Evolve

Launcher Android personal, nativo y local, inspirado en la filosofía minimalista de Oasis. Es una aplicación
independiente, con su propio paquete, código y firma. No contiene código ni recursos de Oasis.

**Estado:** v0.3.0, con cuadrícula libre, dock, galería de widgets y Bienestar rediseñado. Consulta [qué funciona](docs/FEATURES.md) y
[qué se probó realmente](docs/TEST_RESULTS.md).

## Las seis pantallas

`Widgets ← Bienestar ← INICIO → Apps útiles → App Drawer → Biblioteca`

- Mantén pulsada una app para ver sus opciones, o arrástrala para moverla.
- Suelta una app sobre otra para crear una carpeta.
- Mantén pulsado un espacio libre para editar: mover, redimensionar, añadir apps y widgets.
- Los ajustes están en la última pantalla a la derecha y en el modo edición.

## Requisitos para compilar

- JDK 21 (Temurin). Define `JAVA_HOME` en la terminal donde compiles.
- Android SDK con la plataforma 36 y build-tools 36.
- `local.properties` en la raíz con la ruta del SDK; Android Studio lo crea solo. Ejemplo:
  `sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk`

## Compilar

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

El APK queda en `app\build\outputs\apk\debug\app-debug.apk`.

## Instalar en tu teléfono

1. En el teléfono, activa Opciones de desarrollador y Depuración USB, y conéctalo.
2. Instala. La versión de depuración convive con cualquier otro launcher, incluido Oasis:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
   ```
3. Abre Oasis Evolve desde tu launcher actual, entra en Ajustes (engranaje) y pulsa **Establecer como predeterminado**.
4. Para Bienestar, concede **Acceso a datos de uso** desde la misma pantalla.

**Volver atrás:** en Android, ve a Ajustes > Aplicaciones > Aplicaciones predeterminadas > Pantalla de inicio y elige otro
launcher. Tus apps no se modifican en ningún caso.

## Actualizar sin perder datos

Instala la versión nueva encima de la anterior con `adb install -r`. Tus apps, carpetas, widgets y notas se conservan. Volver
a una versión anterior exige desinstalar, lo que borra los datos. Por eso `dist/` conserva cada APK y Git cada versión con una
etiqueta (`v0.2.0`, `v0.3.0`).

## Versión release firmada

Crea tu propia clave y guárdala fuera del repositorio. Nunca uses la firma de otra app.

```powershell
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore C:\ruta\privada\evolve.jks -alias evolve -keyalg RSA -keysize 4096 -validity 10000
.\gradlew.bat :app:assembleRelease
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\zipalign.exe" -p -f 4 app\build\outputs\apk\release\app-release-unsigned.apk evolve-aligned.apk
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" sign --ks C:\ruta\privada\evolve.jks --out evolve-release.apk evolve-aligned.apk
```

Si pierdes esa clave no podrás actualizar la app instalada sin desinstalarla.

## Documentación

- [Arquitectura](docs/ARCHITECTURE.md)
- [Decisiones técnicas](docs/DECISIONS.md)
- [Funciones implementadas y pendientes](docs/FEATURES.md)
- [Resultados de pruebas](docs/TEST_RESULTS.md)
- [Investigación](research/) del APK, permisos, limitaciones y diseño
