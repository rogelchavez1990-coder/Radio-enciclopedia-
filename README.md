# Radio Enciclopedia — App Android (CMBQ, La Habana)

App Android **nativa en Kotlin**, con reproducción de radio integrada, sin AndroidX, sin Google Play Services, sin publicidad ni rastreadores.
Experiencia de radio con seis pantallas (Inicio, Escuchar, Programas, Comunidad, Contacto y Frecuencias), accesos oficiales
a la web, la señal de radio, podcast, videos y redes, e información de contacto
(teléfonos que abren el marcador, correo que abre la app de correo).

- Android mínimo: **9.0 (API 28)** · compileSdk/targetSdk **35**
- Paquete: `cu.radioenciclopedia.app` · versión `1.9.1` (versionCode 12)
- Resultado: `dist/Radio_Enciclopedia.apk` (APK universal, firmado, instalación directa)

> El APK firmado se publica en *Releases*. Los resultados de cada versión están en su ejecución de Actions;
> los informes de versiones anteriores no acreditan la validación de una compilación nueva.
> En este repositorio privado la clave de firma cifrada está en `signing/radio-release.jks` y la contraseña va en el secreto `KEY_PASSWORD`.

## Versión 1.9.1

Portada con el banner de libros y partituras facilitado por el propietario,
restaurado y con el eslogan en cursiva. Se muestra entero, sin recortarlo ni
añadir un segundo logo. La franja avanza ahora de derecha a izquierda.
Inicio incorpora los accesos de pódcast, web en español y web en inglés;
se retiran los cuatro enlaces finales de Programas señalados en la captura.

[Portada, accesos y adaptación a pantalla](docs/PORTADA-REFERENCIA-1.9.md).

## Versión 1.8.0 (anterior)

Franja musical rosa y malva debajo de la cabecera, con desplazamiento continuo
de izquierda a derecha. El Capitolio nocturno conserva el micrófono, el logo y
el eslogan. El retrato y el pensamiento de Martí se muestran únicamente en
Comunidad, con el texto a la izquierda y la imagen a la derecha.

[Distribución y movimiento de la franja](docs/NOTAS-MARTI-1.8.md).

## Versión 1.7.0 (anterior)

Portada unificada con el Capitolio iluminado de noche, Martí y un micrófono
clásico, con los tonos malva/lila de la emisora. El logo y el eslogan se integran
sobre la foto y se conserva el inicio sin desplazamiento.

[Composición y adaptación de la portada](docs/PORTADA-NOCHE-1.7.md).

## Versión 1.6.0 (anterior)

Retrato de José Martí restaurado, firma como en la referencia del propietario,
fondo malva RGB y pentagrama musical. Inicio sin desplazamiento y sin acceso
duplicado de Audio Real. Reproductor nativo con controles del sistema,
reproducción en segundo plano y animaciones ligadas a su estado. Incluye las
frecuencias nacionales FM, una lista de horarios, el pensamiento de Martí en
Comunidad y el correo y la dirección corregidos.

Detalles, fuentes y permisos en [MARTI-RADIO-1.6.md](docs/MARTI-RADIO-1.6.md).

## Identidad y programación 1.5 (versión anterior)

Inicio completo sin desplazamiento vertical, amanecer luminoso del Capitolio,
firma blanca oficial integrada, colores del dossier visual, menú lateral y
tipografía complementaria Source Sans 3 / Fira Sans Condensed Italic.
La sección Programas incluye los 15 espacios documentados, sus perfiles y horas
de inicio en Cuba, con filtros Hoy / Lun–sáb / Domingo. El espacio de ahora se
calcula a partir de la parrilla publicada y la zona `America/Havana`.
Los enlaces a redes, YouTube e iVoox prueban primero sus aplicaciones asociadas;
si no hay un manejador compatible, abren la dirección oficial predeterminada.
Favoritos permite guardar accesos en el dispositivo.

Fuentes, decisiones de identidad y límites de los horarios:
[docs/IDENTIDAD-PROGRAMACION-1.5.md](docs/IDENTIDAD-PROGRAMACION-1.5.md).

## Portada 1.4 (versión anterior)

Capitolio al amanecer, logotipo obtenido directamente de la web oficial, textos
en Mulish (familia Muli de la web), tarjetas translúcidas y movimiento suave.
Procedencia de los recursos y detalles: [docs/PORTADA-1.4.md](docs/PORTADA-1.4.md).
El informe técnico 1.3.1 documenta la versión anterior; los resultados de cada
compilación nueva se encuentran en Actions y en su release.

---

## Opción A (recomendada, sin Android Studio): compilar en GitHub Actions

1. Crea un repositorio **privado** en GitHub y sube todo el contenido de esta carpeta (incluida `.github/`).
2. **Crea la clave de firma una sola vez** (necesita JDK/keytool; en Termux: `pkg install openjdk-17`):
   ```bash
   bash tools/crear_clave.sh
   ```
   Genera `radio-release.jks`, `keystore.properties` y `radio-release.jks.b64.txt`. **Guarda una copia segura del `.jks` y de la contraseña.**
   Si pierdes la clave no podrás actualizar la app sobre las instalaciones existentes.
3. En GitHub → *Settings → Secrets and variables → Actions → New repository secret*, crea:

   | Secreto | Valor |
   |---|---|
   | `KEYSTORE_BASE64` | contenido completo de `radio-release.jks.b64.txt` |
   | `KEYSTORE_PASSWORD` | la contraseña que elegiste |
   | `KEY_ALIAS` | `radioenciclopedia` |
   | `KEY_PASSWORD` | la misma contraseña |

4. En la pestaña **Actions → "Compilar y probar APK" → Run workflow** (o simplemente haz un *push*).
5. Al terminar, descarga los artefactos:
   - **`apk`** → `Radio_Enciclopedia.apk` + `verificacion_apk.txt` (paquete, SDK, firma, alineación)
   - **`pruebas-android-28 / 34 / 35`** → informe de pruebas y **capturas de pantalla reales** en cada versión de Android

## Opción B: compilar en tu PC

Requisitos: JDK 17+, Android SDK (`ANDROID_HOME`) con `platforms;android-35` y `build-tools;34.0.0`.

```bash
bash tools/crear_clave.sh        # solo la primera vez
bash tools/construir.sh          # compila, copia a dist/ y verifica
```
En Windows: `gradlew.bat assembleRelease` (con `keystore.properties` creado) y el APK queda en `app/build/outputs/apk/release/`.
También puedes abrir la carpeta con **Android Studio** (Ladybug o posterior) y usar *Build → Generate Signed App Bundle / APK*.

`assembleRelease` **falla a propósito** si no hay clave de firma: nunca produce un APK sin firmar ni firmado con la clave de depuración.

## Probar en un teléfono o emulador con adb

```bash
bash tools/probar_en_emulador.sh dist/Radio_Enciclopedia.apk resultados
bash tools/probar_radio_nativa.sh resultados/radio-nativa
```

## Actualizar la app en el futuro

1. Cambia `versionCode` (+1) y `versionName` en `app/build.gradle.kts`.
2. Compila con **la misma clave** (mismos secretos). El `applicationId` no debe cambiar.
3. Instala el nuevo APK encima: Android lo trata como actualización y conserva la app.

## Estructura

```
app/src/main/java/cu/radioenciclopedia/app/   MainActivity, Screens, NavBar, Clock, vistas animadas (Aurora, Skyline, Emblem, Dial…)
app/src/main/res/                             arte (drawable-nodpi), iconos, fuentes
app/build.gradle.kts                          minSdk 28, firma release por variables/archivo
tools/                                        crear_clave, construir, verificar_apk, probar_en_emulador, generar_arte, generar_iconos
.github/workflows/compilar-apk.yml            compilación firmada + pruebas en emuladores
```

## Cambiar enlaces, textos o colores

- Enlaces y datos: `Links.kt` (las direcciones son exactamente las de la especificación).
- Textos: `res/values/strings.xml` · Colores: `res/values/colors.xml` (fondo `#8E318F`).
- Arte e iconos: `tools/generar_arte.py` y `tools/generar_iconos.py` (o reemplaza los archivos de `res/drawable-nodpi/`).

## Notas de diseño

- Tipografía de interfaz: **Source Sans 3** y **Fira Sans Condensed** en cursiva. Son familias de apoyo elegidas para el diseño, con licencias SIL OFL en `licencias/`.
- Inicio, menú y Contacto usan la firma blanca publicada en el pie de la web, con sus formas originales. El mate oscuro del JPG se compone con el fondo en el renderizado Android.
- «Sitio web en inglés» conserva la dirección facilitada por el propietario en la versión anterior.
- Las animaciones (aurora, micrófono, ecualizador, bandera, destellos) son decorativas, se detienen cuando la app no está visible y se desactivan si el usuario desactiva las animaciones del sistema.
