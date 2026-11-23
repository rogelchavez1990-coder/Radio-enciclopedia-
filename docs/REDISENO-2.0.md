# Rediseño de movimiento — 2.0.0

Colores, tipografías, contenido, enlaces y estructura de la app se mantienen. Cambia
cómo se mueve todo: física de resorte en lugar de curvas fijas, efectos propios de
Android 13/14/15 y animaciones que cuentan la radio, la música y la cultura.
Todo respeta el ajuste del sistema que desactiva las animaciones y se detiene con
la app fuera de pantalla.

## Motor (Motion.kt)
- **Resortes físicos** (`Spring`, `SpringAnim`) sin AndroidX: entradas con rebote, barra inferior,
  menú, sintonizador. Probados en `SpringTest`.
- **Háptica moderna**: confirmaciones en Android 11+, «clics» de sintonía en Android 14+.

## Radio
- **Sintonizador FM** en Escuchar: la aguja barre la banda y se engancha en 94,1 MHz con rebote y
  chasquido háptico. Se puede arrastrar; fuera de la emisora aparece «estática» y al soltar vuelve sola.
- **Fondo vivo con shader AGSL** (Android 13+): ondas de sonido que fluyen y un vinilo difuso que gira;
  su energía sigue a la radio (calmas en reposo, amplias y rápidas al sonar). En Android 9–12 se
  mantiene el fondo anterior.
- **Dial** con halo que respira al sonar (1.9.2) y anillo dorado que invita a pulsar en reposo (1.9.3).

## Música
- **Explosión de notas, destellos y ondas** al pulsar play, desde el propio botón.
- **Notas que suben** por el fondo, más rápido si suena la radio; el pentagrama del banner también
  corre más deprisa.
- **Destello de luz** diagonal sobre el banner cada 9 s (solo luz: la imagen no se toca).

## Cultura
- **Cita de Martí** que se escribe palabra por palabra al abrir Comunidad; retrato con zoom lento
  tipo Ken Burns, esquinas redondeadas y filete dorado.
- **Programas**: «● AL AIRE» que late, barra de progreso que barre al entrar y filas que suben con resorte.

## Android 13–15
- **Atrás predictivo**: la pantalla se encoge y se desplaza con el dedo (progreso en Android 14+).
  Solo se registra cuando hay algo que cerrar; en Inicio manda la animación del sistema.
- **Menú con desenfoque** del contenido de fondo (RenderEffect, Android 12+).
- **Barra inferior líquida**: la píldora se estira según su velocidad y el icono activo salta.
- **Transiciones de eje compartido** entre pantallas, con resorte.
- **Tarjetas con inclinación 3D** hacia el dedo y foco de luz bajo él.
- El teléfono de Contacto «suena» de vez en cuando.

## Notas
- minSdk sigue en 28: en Android 9–12 se omiten el shader, el desenfoque y el atrás predictivo,
  el resto funciona igual.
- Los efectos AGSL fallan de forma segura: si el shader no compila, se usa el fondo anterior.
- Versión 2.0.0 (versionCode 16), mismo paquete y misma firma.
