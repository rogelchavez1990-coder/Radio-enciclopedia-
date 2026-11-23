# Radio Enciclopedia 1.6.0

## Identidad y contenido

La referencia aportada por el propietario el 11 de noviembre de 2026 sustituye
el amanecer del Capitolio por el retrato de José Martí. El retrato se restaura
con asistencia de imagen generativa para mejorar su nitidez; no se presenta
como una fotografía histórica de archivo sin intervención. La firma se limpia
a partir de la referencia: símbolo a la izquierda y «Radio / Enciclopedia» a la
derecha, en dos líneas. Se conserva como imagen para mantener su lettering.
Los originales generados están conservados en el espacio de trabajo.

El fondo mantiene malva RGB (158, 95, 163), azul RGB (57, 64, 134) y lila
RGB (204, 102, 255), documentados en el dossier. El rosa RGB (244, 215, 237)
de apoyo permite leer la firma oscura como en la imagen enviada. El pentagrama
es un motivo gráfico inspirado en la tira musical adjunta.

Comunidad reúne el retrato con «Ser culto es el único modo de ser libre», de
*Maestros ambulantes*, La América, Nueva York, mayo de 1884. El botón permite
consultar el texto: https://es.wikisource.org/wiki/Maestros_ambulantes .

Las 15 entradas de frecuencias FM reproducen la relación facilitada por el
propietario. No se atribuye una comprobación independiente de cobertura.
Se encuentran en Inicio, Escuchar y el menú. Son información para un receptor
FM; la aplicación reproduce la señal por Internet.

La dirección confirmada se normaliza como «Edificio ENE, calle N número 266,
entre 21 y 23, Vedado, La Habana». Correo solicitado: radio.enciclo.62@gmail.com.
Facebook prioriza su enlace universal dentro de la aplicación oficial o Lite;
Telegram prioriza el esquema nativo del canal y sus clientes compatibles.
Se mantiene una alternativa web cuando no existe aplicación asociada.

La parrilla de 15 programas se contrasta con la página oficial el 11 de
noviembre. Conserva los filtros de lunes a sábado y domingo y añade un acceso
directo a la lista de horarios por programa. Las horas son de Cuba y la
indicación de programa actual sigue siendo una referencia a la parrilla.

## Reproducción nativa

Fuente exacta del elemento audio/mpeg de la página oficial:
https://icecast.teveo.cu/9Rnrbjzq

Página de origen: https://www.radioenciclopedia.cu/audio-real/ .
En la comprobación HTTP del 11 de noviembre entregó audio MP3 estéreo,
44,1 kHz y 64 kb/s, identificado como Radio Enciclopedia.

MediaPlayer reproduce la señal dentro de un servicio multimedia de primer
plano. MediaSession ofrece los controles del sistema y la notificación.
La señal continúa al cambiar de sección, salir de la aplicación o apagar la
pantalla. Pausar libera el reproductor y su conexión; reanudar regresa al
directo. Detener finaliza el servicio. No hay reproducción automática al
abrir la aplicación ni al reiniciar el proceso.

Estados explícitos: inactivo, conectando, reproduciendo, en pausa y error.
La conexión tiene un límite de 30 segundos. Cancelar durante la conexión
descarta el reproductor pendiente. Un error ofrece reintentar y conserva
accesible la página oficial. La animación del dial y el ecualizador sigue
el estado de reproducción, sin presentarse como análisis del sonido real.
Se respeta la reducción de movimiento del sistema.

El foco de audio permite pausar ante llamadas u otras interrupciones;
la desconexión de auriculares pausa la señal. Las interrupciones temporales
permiten volver al directo al recuperar el foco. Una pérdida permanente
requiere una nueva acción del usuario.

Permisos: INTERNET para la señal, FOREGROUND_SERVICE y
FOREGROUND_SERVICE_MEDIA_PLAYBACK para el servicio, WAKE_LOCK para continuar
con la pantalla apagada y POST_NOTIFICATIONS para sus controles. La solicitud
de notificaciones se hace al primer intento de reproducción en Android 13+;
denegarla no impide escuchar. Los teléfonos continúan abriendo el marcador.

Se conserva la firma de publicación y se aumenta versionCode a 8.
