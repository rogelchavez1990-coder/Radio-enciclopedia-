# Portada viva — 1.9.3

La portada (Inicio) era casi estática: solo se movían la franja de pentagramas
y un destello poco frecuente en las tarjetas. Esta versión le añade movimiento
sin tocar nada de lo que el propietario fijó: el banner de partituras sigue
completo, sin filtros, recorte ni superposición; el eslogan, los enlaces, los
textos, las descripciones de accesibilidad y la distribución sin desplazamiento
vertical quedan igual. Todo el movimiento son transformaciones (no cambian el
diseño) y no cambia el área pulsable de los accesos.

## Qué se mueve

- **Entrada con rebote.** Cada vez que se muestra Inicio, los accesos (cuatro
  tarjetas, tres sitios y los iconos de redes) crecen desde pequeños con un
  ligero rebote, escalonados.
- **Iconos que flotan.** En las cuatro tarjetas grandes, el icono dorado sube y
  baja un par de dp, cada uno con su desfase, sobre un resplandor dorado que respira.
- **Botón de reproducir que invita.** Con la radio parada, un anillo dorado se
  expande y se desvanece alrededor del botón de «Contigo siempre». Con la radio
  sonando siguen las ondas rosas de siempre.
- **Redes que saludan.** Cada 6 s los cinco iconos de redes y correo dan un
  saltito en cadena.
- **Fondo con notas.** Siete notas musicales suben despacio por el fondo,
  se mecen y se desvanecen. Aparecen en todas las pantallas.

## Cómo se controla

Todo usa el reloj único de animaciones (`Clock`): se detiene cuando la app no
está visible y, si el usuario desactiva las animaciones del sistema, la entrada
con rebote no se ejecuta y el resto queda en un fotograma fijo.

## Versión

`versionCode` 14, `versionName` 1.9.3. Mismo paquete y firma; se instala sobre
la 1.9.2 o la 1.9.1.

## A mirar en el teléfono

El movimiento no se aprecia en las capturas del flujo, que son estáticas.
Conviene verlo en pantalla y decir si algo resulta excesivo (velocidad del
rebote, amplitud de los iconos, frecuencia del saludo de las redes).
