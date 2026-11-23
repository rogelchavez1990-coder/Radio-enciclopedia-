# Investigación de emisoras e integración (versiones 2.1 a 2.3)

Emisoras añadidas junto a Radio Enciclopedia: **Radio Progreso**, **Radio Rebelde** (AM/FM) y **Radio COCO** («la COCO»).

## 1. Datos verificados

| | Indicativo | La Habana | Fundación | Lema / saludo | Señal en la app |
|---|---|---|---|---|---|
| Radio Enciclopedia | CMBQ | 94,1 FM · 530 y 1260 AM | 7-nov-1962 | «Una emisora para todos los momentos de la vida» | `icecast.teveo.cu/9Rnrbjzq` (ya existía) |
| Radio Progreso | CMBC | 90,3 FM · 640 AM (OC 4 765 kHz) | 15-dic-1929 | «La Onda de la Alegría» | `icecast.teveo.cu/XjfW7qWN` |
| Radio Rebelde | CMBA | 670 AM · 96,7 FM (OC 5 025 kHz) | 24-feb-1958 | «Aquí Radio Rebelde» | **AM**: `icecast.teveo.cu/kHKL7tWd` (FM: `zrXXWK9F`, no usada) |
| Radio COCO | CMCK | 91,7 FM · 980 AM | «más de 80 años» (sin fecha exacta publicada) | no se documenta (la app muestra una descripción) | `icecast.teveo.cu/fvc4RVRz` |

**Historia en una línea**
- *Progreso*: nació en la tienda «El Progreso Cubano», de Domingo Fernández (Máximo Gómez); pasó por el Centro Gallego y desde 1953 emite desde Infanta. Su Estudio Uno tiene 300 butacas. Programó a La Sonora Matancera, Antonio María Romeu y Barbarito Diez, y radioteatro.
- *Rebelde*: la fundó el Che Guevara en Altos de Conrado (Sierra Maestra); primera emisión de 20 minutos que abrió con el himno rebelde; el 1-ene-1959 Fidel habló por ella; a fines de la guerra enlazaba 32 emisoras (Cadena de la Libertad). Hoy: 24 h de noticias, deportes y magacines; sede en el edificio del ICRT (Vedado).
- *COCO*: planta con más de 80 años; perfil deportivo e informativo, con análisis y opinión, y música tradicional cubana.

**Señal de Rebelde (v2.1.2):** la página oficial de audio en vivo de Radio Rebelde publica dos señales distintas, una para AM (`kHKL7tWd`) y otra para FM (`zrXXWK9F`). La app reproduce la **AM**, a petición del propietario; la FM figura solo como dato.

## 2. Identidad visual: qué se encontró y qué no

No se halló una guía de identidad pública (manual de marca, paleta o logotipo vectorial) de estas emisoras. Las fuentes consultadas no describen colores ni logotipos. Por eso:

- **No se copian ni imitan logotipos.** Cada emisora tiene una paleta y un motivo animado **de diseño propio**, inspirados en su carácter documentado, y la ficha de la app lo aclara.
- Enciclopedia conserva el malva de siempre.

| Emisora | Paleta | Motivo animado | Razón |
|---|---|---|---|
| Enciclopedia | malva y lila + dorado | disco de vinilo que gira, arcos de sonido, notas | música instrumental y cultura |
| Progreso | ámbar a ciruela, coral | sol con rayos, olas y confeti | «La Onda de la Alegría» |
| Rebelde | rojo y negro, blanco | franjas diagonales, estrella con ondas, línea de morse | radio clandestina de la Sierra Maestra, noticias y deporte |
| COCO | turquesa a azul marino, lima | pelota que rebota y ecualizador | deporte, juventud y música |

Los datos de la tarjeta (frecuencias, indicativo, lema) sí son los verificados arriba.

## 3. Cómo funciona (versión 2.3.0)

- **Tarjetas** (Escuchar): carrusel con física; deslizar cambia de emisora, tocar la central reproduce o pausa. Debajo, un selector **La Habana · México · España** salta al primer grupo de emisoras, y una pista de puntos muestra la posición.
- **Dial FM** (Escuchar): muestra las emisoras habaneras que tienen frecuencia FM (90,3 · 91,7 · 93,3 · 94,1 · 96,7 · 102,5). Arrastrar la aguja y soltar cerca de una la sintoniza. Si la emisora elegida no tiene FM en La Habana (AM, México, España), la aguja se atenúa y el dial muestra su banda.
- **Señales de respaldo**: cada emisora cubana tiene dos señales (la oficial del ICRT y una de respaldo); si la primera falla, la app prueba la segunda sola.
- **Sin pantalla**: los botones siguiente/anterior de los auriculares, del Bluetooth y de la notificación cambian de emisora, y el locutor del teléfono dice la emisora nueva (ver `USO-SIN-PANTALLA-2.2.md`). En TalkBack, deslizar con dos dedos cambia de emisora.
- **Atajos del icono**: una entrada por cada una de las cuatro emisoras principales.
- **Voz (retirada en 2.3.0)**: el cambio de emisora por comandos de voz (micrófono, pantalla de voz, «Hey Google») se quitó a petición del propietario. La app ya no pide permiso de micrófono.
- Al cambiar, el fondo se tiñe con el color de la emisora, salen ondas de radio y una ráfaga de notas, y el servicio y la notificación pasan a la emisora nueva.

## 3b. Emisoras añadidas en 2.3.0 (origen: «Dial Habana Estudio 2.1.0»)

Datos tomados del archivo `stations.json` de ese APK (nombre, frecuencia, descripción, sitio y señales):

| Lugar | Emisoras |
|---|---|
| La Habana | Radio Enciclopedia, Radio Progreso, Radio Rebelde AM, **Radio Rebelde FM** (96,7), Radio COCO, **Radio Reloj** (950 AM), **Radio Taíno** (93,3), **Radio Habana Cuba** (102,5) |
| México | **Radio IPN** (95,7), **UAM Radio** (94,1), **Radio UNAM** (96,1), **Radio Educación** (96,5), **Radio ILCE** (por Internet), **Tec Sounds Radio** (Monterrey, 94,9) |
| España | **Radio MARCA**, **Cadena SER**, **Radio 5 · RNE** (cobertura nacional) |

- Las tarjetas nuevas usan un motivo común (ondas que salen de un disco con las iniciales) con la paleta derivada del color de acento de cada emisora; Radio Reloj tiene una esfera animada con la hora de Cuba. No son logotipos oficiales.
- No se incorporó la programación horaria de ninguna emisora nueva: Programas sigue siendo solo de Radio Enciclopedia; en las demás se muestra su descripción.
- **Frecuencia de Radio Rebelde AM**: el APK Dial Habana indica 1180 AM y la investigación previa indicaba 670 AM. Wikipedia lista ambas entre las frecuencias AM de Rebelde sin asignar ninguna a La Habana, así que la app muestra solo «AM» hasta confirmarlo con la emisora.
- **Señales fuera de Cuba**: Radio IPN se publica solo por HTTP sin cifrar (la app lo permite únicamente para esa dirección), Tec Sounds usa HLS (.m3u8) y MARCA/SER/Radio 5 pasan por redirecciones de sus servidores. No se pudieron probar desde el entorno de desarrollo.

## 4. Límites conocidos

- Las señales son las publicadas por el ICRT en `icecast.teveo.cu`; **no se pudieron probar desde el entorno de desarrollo** (red restringida). Pueden tener cortes breves. Un sitio de terceros mostraba un aviso de «sin emisión» para Rebelde; no se ha confirmado.
- La programación horaria de la app (Programas) es solo de Radio Enciclopedia; en las otras emisoras se muestra su lema.

## Fuentes

- Wikipedia: Radio Progreso, Radio Rebelde, Lista de emisoras de Cuba (indicativos y frecuencias de La Habana)
- EcuRed: Radio Progreso (historia)
- radiorebelde.cu: historia y frecuencias FM
- Cubaperiodistas, caso de estudio «Emisora COCO» (2020): perfil de la emisora
- Directorio Cubano: señales de streaming del ICRT
