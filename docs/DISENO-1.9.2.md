# Pulido de diseño — 1.9.2

Primera ronda de mejoras visuales sobre la 1.9.1. No cambia ninguna decisión de
marca ni de contenido: el banner de partituras, el eslogan, los enlaces, los
textos y las descripciones de accesibilidad quedan exactamente como estaban,
y Inicio conserva su distribución sin desplazamiento vertical.

## Cambios

- **Títulos de sección sin partir palabras.** «Comunidad», «Escuchar»,
  «Programas» y «Contacto» usan ahora una sola línea que reduce su tamaño
  (de 32–34 sp hasta 20 sp) en vez de partirse en «Comunida / d» con letra
  ampliada o en pantallas estrechas. Función común: `Context.sectionTitle()` en `Ui.kt`.
- **Programas: emisión actual más clara.** La tarjeta «Ahora, según la parrilla»
  muestra una línea dorada con lo que va de la emisión y «Termina a las HH:MM».
  En la lista, el programa en emisión lleva el distintivo dorado «● AL AIRE».
  Se calcula con la misma parrilla y la hora de La Habana de siempre.
- **Escuchar: halo que respira.** Mientras suena la radio, el dial tiene un
  resplandor cálido que pulsa detrás de las barras. Usa el reloj único de
  animaciones: se detiene fuera de la app y con las animaciones del sistema desactivadas.
- **Barra inferior:** vibración breve al cambiar de pestaña, como en los accesos de Inicio.

## Versión

`versionCode` 13 y `versionName` 1.9.2: mismo paquete y misma firma, de modo que
se instala encima de la 1.9.1 conservando la app.

## Pendiente de comprobar

Estos cambios se escribieron sin Android SDK, así que **no se han compilado ni
visto en pantalla** todavía. El flujo `compilar-apk.yml` los compila, firma y
prueba en emuladores Android 9, 14 y 15 y guarda capturas; hasta que termine en
verde no debe darse por comprobado. Puntos a mirar en las capturas:

1. «Comunidad» en una sola línea en pantalla pequeña y con letra grande.
2. «● AL AIRE» y la línea dorada en Programas (depende de la hora del emulador).
3. El halo del dial no tapa las barras ni el botón central.
