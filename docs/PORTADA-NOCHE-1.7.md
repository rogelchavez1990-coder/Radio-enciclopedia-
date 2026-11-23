# Portada nocturna — 1.7.0

La indicación posterior del propietario sustituye la portada dividida de 1.6
por una única composición nocturna viva del Capitolio. Se integran el retrato
de José Martí, un micrófono clásico y la iluminación malva/lila de la emisora.
La composición se genera a partir del retrato restaurado anterior; es un
montaje conmemorativo, no una fotografía histórica documental.

Recurso: `portada_capitolio_noche.png`. El original generado se conserva en
el espacio de trabajo. La firma existente se compone sobre la foto en su
variante clara: lettering blanco y símbolo lila, con su alfa original.
No se incluye una placa rosa en la cabecera de Inicio.

El eslogan «Una emisora para todos los momentos de la vida», la frase de
Martí y los textos de apoyo se dibujan con las vistas Android sobre la foto.
Así siguen siendo legibles, accesibles y adaptables al tamaño de pantalla.
El eslogan deja de ocupar una tarjeta separada. Inicio conserva sus accesos
visibles sin desplazamiento, tanto en vertical como en horizontal.

Se conserva el reproductor, las frecuencias y los datos de 1.6. La firma de
publicación es la misma; versionCode 9 permite actualizar 1.6.0.

La comprobación de controles usa la utilidad `media dispatch` en Android 9 y
`cmd media_session dispatch` en versiones posteriores. La aplicación conserva
la misma MediaSession y sus controles; la diferencia está en la interfaz de
pruebas que ofrece cada versión del sistema.
Referencia AOSP Android 9:
https://android.googlesource.com/platform/frameworks/base/+/android-9.0.0_r61/cmds/media/src/com/android/commands/media/Media.java
