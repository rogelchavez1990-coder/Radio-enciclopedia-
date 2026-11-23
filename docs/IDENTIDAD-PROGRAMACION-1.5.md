# Identidad y programación — versión 1.5.0

## Referencias

Se usa el PDF aportado por el propietario, **Radio_Enciclopedia_Dossier_Visual-1.pdf**,
fechado el 9 de noviembre de 2026, y se contrasta la parrilla pública el 10 de noviembre.
El dossier es una investigación visual y documental, no un manual normativo con
archivos maestros. La imagen de referencia del propietario guía la distribución:
firma integrada arriba, ciudad, lema, escucha en vivo, cuatro tarjetas, redes y
navegación inferior. La petición posterior conserva un **amanecer vivo y alegre**.

## Marca y recursos visuales

- Malva `#9E5FA3`, azul de firma `#394086`, lila `#CC66FF`, blanco y fondos oscuros
  de apoyo. El dossier distingue los valores exactos del CSS de las muestras
  aproximadas de imágenes JPEG.
- Firma blanca oficial de la página 2 del dossier:
  https://www.radioenciclopedia.cu/wp-content/uploads/2022/11/cmbq-radio-enciclopedia-bw.jpg
  Se conserva el archivo original, sus proporciones y su lettering. Android
  compone el mate oscuro mediante una matriz de alfa para integrarlo en el fondo.
- El dossier no identifica la fuente maestra del logotipo. La interfaz usa
  **Source Sans 3** (cuerpo) y **Fira Sans Condensed SemiBold Italic** (títulos)
  como tipografías complementarias, sin atribuirles carácter de fuentes oficiales.
  Licencias SIL OFL incluidas. Los pesos de Source Sans se instancian en TTF
  estáticos para mantener el aspecto entre versiones de Android.
- `art_amanecer_alegre.webp` es una ilustración fotográfica generada para esta
  aplicación. Representa el Capitolio, fachadas coloniales, palmeras y luz cálida
  de amanecer. Se usa en portada y banners; no es una fotografía documental de
  la emisora. La luz dorada es un acento de la imagen, no un color institucional.

## Portada y navegación

El inicio usa un `ViewGroup` que reparte la altura útil entre cabecera, lema,
tarjeta de escucha, cuadrícula y redes. No contiene un `ScrollView`. En horizontal
reparte los mismos bloques en dos columnas. Respeta las barras del sistema.
El menú y las páginas de detalle pueden desplazarse. La navegación inferior
ofrece Inicio, Escuchar, Programas y Contacto; Comunidad está en la cuadrícula y
el menú. Todos los destinos anteriores siguen accesibles.

Los enlaces prueban los paquetes de sus aplicaciones oficiales y sus variantes
compatibles (incluidas Lite). Si no hay manejador, usan el enlace web original.
No se consultan listas de aplicaciones instaladas ni se añaden permisos.
Los teléfonos abren el marcador, y el correo su aplicación correspondiente.

## Parrilla

Fuente principal: https://www.radioenciclopedia.cu/programacion-general/
Perfiles: páginas 5 y 6 del PDF y enlaces que documenta. Se incluyen los **15
títulos** semanales, con retransmisiones y los espacios exclusivos del domingo.
La interfaz presenta **horas de inicio** y explica R como retransmisión.

La indicación de ahora se calcula con `America/Havana`, independientemente de
la zona horaria del teléfono. Es una referencia a la parrilla publicada, no
metadatos de emisión en tiempo real. El contenido está disponible sin conexión;
la página oficial permite contrastar cambios posteriores.

Se toma 00:00 como inicio del primer Interludio de lunes a sábado, de acuerdo con
la secuencia de la parrilla y la explicación del dossier. El error 09:69 del
domingo no se muestra: el inicio es 09:00 y el siguiente espacio comienza a las
10:00. Para resaltar ahora se usa esa secuencia. Los minutos de identificación
de lunes a sábado se mantienen fuera de los programas principales.

Se muestra **94.1 FM · La Habana** como referencia documentada. El dossier recoge
discrepancias para 1260 AM y no se presenta esa frecuencia como vigente.

## Comprobación

- Pruebas de horario cubano, cambio de día UTC/Cuba, programas del domingo,
  retransmisiones y minutos de identificación.
- Pruebas de apertura directa, variantes Lite y alternativa al enlace original.
- Comprobación Android de los accesos completos del inicio, encima de la barra,
  con pantalla pequeña y texto ampliado; menú, cinco secciones y enlaces.
- Firma e identificador de la versión anterior conservados; versionCode 7.
