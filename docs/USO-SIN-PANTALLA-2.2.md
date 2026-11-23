# Uso sin pantalla y con TalkBack (versión 2.2.0; actualizado en 2.3.0)

> **Actualización 2.3.0:** se retiraron el micrófono de la app, las órdenes por voz («ayuda», «qué suena», cambiar de emisora hablando) y la entrada de «Hey Google». Siguen: el locutor, los botones de auriculares y notificación, TalkBack y los atajos del icono.

Objetivo: que una persona ciega pueda manejar la radio solo con la voz, también con la pantalla apagada.

## Qué hace la app

- **Locutor**: dice en voz alta cada cambio («Radio Rebelde, 670 AM»), la pausa, los errores y las respuestas a «qué suena» y «ayuda». Usa la voz en español del teléfono. La radio baja de volumen mientras habla.
  - Habla cuando la orden llega de fuera de la pantalla (auriculares, notificación, asistente) y la app no se está viendo, o si TalkBack está activo.
  - Con la app a la vista y TalkBack activo, TalkBack lee la interfaz y el locutor no la repite.
- **Asistente** («Hey Google, pon Radio Rebelde en Radio Enciclopedia»): entra por una puerta sin ventana, ejecuta la orden y la confirma en voz alta. No abre la app ni enciende la pantalla.
- **Botones de auriculares**: dos pulsaciones = emisora siguiente, tres = anterior (comportamiento estándar de Android, la app lo recibe como siguiente/anterior).
- **Órdenes nuevas** (también en el micrófono de la app): «ayuda», «qué suena», «qué emisora estoy escuchando».
- **TalkBack**: en el carrusel de emisoras y en el dial, deslizar con dos dedos hacia adelante o atrás cambia de emisora. Las descripciones indican la banda que realmente suena (Rebelde: 670 AM).
- **Atajos del icono**: al mantener pulsado el icono, una entrada por emisora (también desde las acciones de TalkBack).

## Límites

- No se pudo probar con el asistente ni con el micrófono reales desde el entorno de desarrollo.
- Gemini, que en muchos teléfonos sustituye a Google Assistant, no siempre pasa órdenes a apps de terceros.
- La app no escucha de forma continua: Android 14 y 15 lo limitan con la pantalla apagada y gasta mucha batería.
