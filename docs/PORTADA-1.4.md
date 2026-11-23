# Radio Enciclopedia 1.4.0 — Amanecer en La Habana

La portada muestra el Capitolio al amanecer con un movimiento de cámara lento,
una placa de marca clara, tarjetas translúcidas, reflejos discretos y transiciones
sin rebotes. El contenido y los enlaces de las cinco pantallas se conservan.

## Recursos y procedencia

- `art_amanecer.webp`: ilustración generada para esta aplicación el 9 de noviembre
  de 2026. Representa el Capitolio de La Habana al amanecer; no es una fotografía
  documental. El original se conserva fuera del código de la aplicación.
- `logo_oficial.jpg`: archivo intacto de la cabecera de la web oficial:
  https://www.radioenciclopedia.cu/wp-content/uploads/2023/05/cropped-cmbq-radio-enciclopedia.jpg
  (315 × 107 px). Se conserva su fondo y sus proporciones. El rótulo exacto es
  parte de esa imagen; no se ha sustituido por una tipografía parecida.
- La web declara `Muli` en su CSS y no publica un archivo de fuente para el
  lettering de su logotipo. Los textos de la aplicación usan **Mulish**, nombre
  actual de la familia Muli, incluida localmente, con pesos 400, 500, 600 y 700.
  Los pesos se instancian en TTF estáticos para mantener la misma apariencia
  en las distintas versiones de Android.
  Fuente: https://github.com/google/fonts/tree/main/ofl/mulish
  Licencia SIL OFL en `licencias/OFL_Mulish.txt`.
  CSS consultado: https://www.radioenciclopedia.cu/wp-content/themes/cream-magazine/assets/dist/css/main.css

## Movimiento y rendimiento

- Un reloj compartido actualiza solo las vistas visibles y se detiene al salir
  de la aplicación. Las animaciones decorativas respetan el ajuste del sistema.
- Los gradientes de la portada y las tarjetas se preparan al cambiar de tamaño.
  El reflejo reutiliza su shader y matriz; no crea gradientes en cada fotograma.
- No se añaden permisos, conexiones de red ni dependencias para efectos de blur.
  La translucidez se obtiene por composición de colores con alfa.

## Compilación

Versión 1.4.0, versionCode 6. Se mantiene `cu.radioenciclopedia.app` y la clave de
firma existente para permitir actualizaciones. GitHub Actions compila, verifica
la firma y ejecuta las pruebas de humo en Android 9, 14 y 15; cada ejecución
publica el APK y las capturas en su release. Una ejecución correcta no sustituye
la prueba de los enlaces externos en un teléfono físico.
