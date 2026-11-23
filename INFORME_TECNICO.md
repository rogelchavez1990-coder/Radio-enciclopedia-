# Informe técnico — Radio Enciclopedia (Android) · versión 1.3.1

## Datos del proyecto

| Concepto | Valor |
|---|---|
| Versión mínima de Android | 9.0 (API 28) |
| compileSdk / targetSdk | 35 / 35 |
| Identificador de paquete | `cu.radioenciclopedia.app` |
| Versión | 1.3.1 (versionCode 5) — se instala como actualización sobre las anteriores (misma firma) |
| Lenguaje / UI | Kotlin, vistas nativas dibujadas por código (sin WebView, sin AndroidX, sin Compose) |
| Herramientas | Android Gradle Plugin 8.7.3 · Gradle 8.9 (wrapper) · Kotlin 2.0.21 · JDK 17 |
| Dependencias externas | Ninguna |
| Permisos | Ninguno |
| Firma | Release con clave propia persistente (esquema v3, válido desde Android 9). Nunca se firma con la clave de depuración |
| ABI | APK universal; sin código nativo |
| Reducción | R8 + shrinkResources |
| Tamaño del APK | 1 301 352 bytes (≈ 1,3 MB) |
| SHA-256 del APK entregado | `656428efe00ea9c0e755bc9d95aaa6a9ff9db274ac28403acaa4e9656a94b928` |
| Certificado de firma (SHA-256) | `ed88bea1214361c210e13fef88416772fdd2441619f6fa40d421aa6f1abce1ad` (CN=Radio Enciclopedia) |

## Rediseño (de botones estáticos a una experiencia de radio)

- **Cinco pantallas:** Inicio, Escuchar, Programas, Comunidad y Contacto, con barra de navegación inferior con «píldora» deslizante.
- **Sistema de color:** fondo en degradado ciruela/violeta (`#12002B` → `#2A0B4D` → `#8E318F`), acentos magenta/rosa y dorado (micrófono, anillos, botón principal).
- **Efectos:** aurora de fondo con estrellas titilantes, micrófono dorado flotante con anillo giratorio y destellos, skyline nocturno de La Habana (Capitolio) con ventanas que brillan, tarjetas de cristal con borde degradado y destello periódico, indicador «EN VIVO», ecualizador, dial con espectro radial y botón de reproducción pulsante, atardecer con bandera cubana ondeando en «Cultura que nos une».
- **Movimiento:** entrada escalonada de tarjetas, transición entre pantallas, parallax en Inicio, rebote al pulsar, vibración suave al abrir enlaces.
- **Rendimiento y accesibilidad:** un único reloj de animación (~30 fps) que se detiene cuando la app no está visible y se desactiva si el usuario apaga las animaciones del sistema (se muestra un fotograma fijo). Todos los elementos pulsables tienen descripción de accesibilidad. Los textos se reducen automáticamente en pantallas estrechas.
- Arte propio (ilustraciones vectoriales renderizadas por `tools/generar_arte.py`). Se conserva la mariposa original de la emisora en Contacto.
- **Lema** oficial conservado («una emisora para todos los momentos de la vida») más la línea «Cultura, música, historia y conocimiento». Los tamaños de nombre y lema son mayores que los 14 sp / 12 sp de la especificación original porque la nueva dirección visual lo requería.

## Portada, logotipo y tipografías (v1.3)

- **Portada de Inicio:** la foto real de La Habana (Capitolio, calle colonial, auto antiguo, trompetista, conguero y cantante) proporcionada por el propietario. Se recortó de su captura de pantalla y se le quitaron los textos y el emblema superpuestos reconstruyendo el fondo; **la resolución es la de la captura (841 px de ancho), así que en pantallas muy nítidas puede verse algo suave. Con la imagen original sin textos se vería mejor.** Con zoom lento de cámara y estrellas titilantes sobre el cielo.
- **Logotipo:** el de la mariposa/micrófono, recortado de la segunda imagen del propietario (resolución limitada, 720 px). En Inicio y Contacto se muestra completo (con «CMBQ», «Radio Enciclopedia» y el lema) sobre placa blanca; en las demás pantallas y como icono de la aplicación, solo la mariposa. Se retiraron el micrófono dorado y el skyline dibujado.
- **Tipografías** (todas con licencia SIL OFL, ver `licencias/`): Lobster para títulos (cursiva gruesa, la más parecida al rótulo «Radio Enciclopedia»), Fira Sans Condensed para el lema y subtítulos (como el lema del logotipo) y Liberation Sans para el cuerpo. Son tipografías *similares*, no las originales del logotipo, que no tengo.

## Enlaces y datos de contacto (revisados)

Las 8 URL de la especificación coinciden carácter por carácter y se abren con `ACTION_VIEW`:
SITIO WEB, AUDIO REAL EN INTERNET, PODCAST, VIDEOS (YouTube), FACEBOOK, X, TELEGRAM, INSTAGRAM.
Novena dirección, indicada después por el propietario: SITIO WEB EN INGLÉS → https://www.radioenciclopedia.cu/cultural-news/tag/cuba/ (la página consultada está en inglés, con enlace «ESPAÑOL»; es una página de etiqueta «Cuba» de la sección de noticias en inglés, no una portada).
**YouTube:** el botón VIDEOS intenta abrir directamente la aplicación oficial (`com.google.android.youtube`); si no está instalada, usa el navegador.
Contacto: CMBQ Radio Enciclopedia; Edificio ENE, calle N.º 266, 7mo. piso, entre 21 y 23, El Vedado, La Habana, Cuba; CP 10400; Directora Ing. Luisa Márquez Echevarría; teléfonos (+53) 78384587, 78361882 y 78361883 (`ACTION_DIAL`: abre el marcador, **nunca llama solo**); correo radio.enciclopedia@icrt.cu (`ACTION_SENDTO`).
«EN VIVO» y «Escuchar ahora» abren la página oficial de audio real; la app no incluye ninguna dirección de streaming inventada y no reproduce audio por sí misma.

## Resultado de las comprobaciones (compilación nº 11 en GitHub Actions, commit 411f432)

| Comprobación | Resultado |
|---|---|
| Compilación `assembleRelease` | ✔ |
| Manifiesto: paquete, minSdk 28, targetSdk 35, sin permisos, sin código nativo | ✔ (aapt2) |
| Firma válida (v3), no usa clave de depuración, alineación zip | ✔ (apksigner, zipalign) |
| Instalación y reinstalación sobre la misma app, en Android 9, 14 y 15 (emuladores) | ✔ |
| Arranque sin conexión y pantalla principal directa | ✔ en los tres |
| Las 5 pantallas accesibles; 9 enlaces, 3 teléfonos y correo presentes | ✔ en los tres |
| Los 13 elementos pulsables probados | ✔ en los tres |
| Pantalla pequeña (480×800): todo accesible | ✔ en los tres |
| Las animaciones se mueven (dos capturas distintas con animaciones activas) | ✔ en los tres |
| Sin cierres inesperados; ninguna llamada se inicia sola | ✔ en los tres |
| Aspecto en pantalla | ✔ Revisadas las capturas reales de Android 9 y 15 (y 14 en pantalla pequeña) |

### Avisos y limitaciones (honestidad)
- **No se pudo comprobar en los emuladores que YouTube abra la app de YouTube** (el botón VIDEOS dejó la app en primer plano, probablemente porque los emuladores no traen YouTube). Hay que verificarlo en un teléfono real con YouTube instalado.
- Al pulsar algunos botones, en los emuladores la app siguió en primer plano (VIDEOS y SITIO WEB en los tres; AUDIO REAL en Android 15; los 3 teléfonos en Android 9 sí abrieron el marcador pero hubo que reabrir la app, y en 14/15 no se vio abrir). Probablemente el emulador carece de esas aplicaciones o la pulsación no llegó; la causa **no está confirmada**. Hay que comprobarlo en un teléfono real.
- Solo se probó en emuladores x86_64 de Google (Android 9, 14 y 15). **No se probó en dispositivos físicos (incluido Samsung) ni en Android 10–13 ni 16.**
- No se probó la reproducción de audio ni el contenido de las páginas externas; la app solo las abre.
- En la compilación nº 10 la prueba de Android 15 falló una vez por tiempos del emulador (los botones de la barra no respondieron a tiempo); la repetición pasó completa y la compilación nº 11 pasó a la primera en los tres emuladores. Antes, una ejecución en Android 14 había fallado por una pantalla negra de arranque del emulador.
