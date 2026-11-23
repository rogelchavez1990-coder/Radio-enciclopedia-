package cu.radioenciclopedia.app

import android.content.Context

/** Grupo de emisoras por lugar de origen. */
enum class Place(val label: String) {
    HABANA("La Habana"), MEXICO("México"), ESPANA("España")
}

/**
 * Una emisora que la app sabe reproducir.
 *
 * Las emisoras y sus señales provienen del archivo de datos de «Dial Habana Estudio 2.1.0» y de las
 * fuentes citadas en docs/INVESTIGACION-EMISORAS-2.1.md. Los colores NO son los de los logotipos oficiales
 * (no hay una guía de identidad pública): son una paleta de diseño inspirada en el carácter de cada emisora.
 */
class Station(
    val id: String,
    val name: String,
    /** Nombre corto para el sintonizador. */
    val short: String,
    /** Iniciales para el motivo de la tarjeta. */
    val mark: String,
    /** Indicativo oficial, si se conoce. */
    val call: String,
    val place: Place,
    val city: String,
    /** Banda de la señal que se reproduce: «94,1 FM», «AM», «Nacional»… */
    val band: String,
    /** Línea de frecuencias para la tarjeta y la ficha. */
    val dial: String,
    /** Posición en el dial FM de La Habana (88–108 MHz). Solo las emisoras habaneras en FM. */
    val fm: Float?,
    val tagline: String,
    /** Señales en orden de preferencia: si la primera falla, se prueba la siguiente. */
    val streams: List<String>,
    /** Degradado de la tarjeta (arriba y abajo) y acentos de luz. Valores ARGB. */
    val top: Int,
    val bottom: Int,
    val accent: Int,
    val accent2: Int,
    val profile: String,
    val hasGuide: Boolean,
    val site: String? = null
) {
    val flag: String get() = "$band · $city"
    /** Cómo se dice en voz alta («Radio Rebelde AM», «Radio Progreso, 90,3 FM»). */
    val spoken: String get() = if (name.endsWith(band)) name else "$name, $band"
    val stream: String get() = streams.first()
}

private val DEEP = 0xFF11091D.toInt()
private fun themeAccent(rgb: Int) = 0xFF000000.toInt() or rgb

/** Mezcla dos colores ARGB por canal (sin depender de android.graphics, para poder probarla en la JVM). */
private fun mix(a: Int, b: Int, t: Float): Int {
    fun ch(shift: Int) = (((a ushr shift) and 0xFF) + ((((b ushr shift) and 0xFF) - ((a ushr shift) and 0xFF)) * t)).toInt() and 0xFF
    return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}
private fun themeTop(rgb: Int) = mix(themeAccent(rgb), DEEP, 0.55f)
private fun themeBottom(rgb: Int) = mix(themeAccent(rgb), DEEP, 0.88f)
private fun themeSoft(rgb: Int) = mix(themeAccent(rgb), 0xFFFFFFFF.toInt(), 0.5f)

/** Segunda señal de respaldo de las emisoras cubanas (la misma que usa Dial Habana Estudio). */
private fun mirror(url: String) = "https://theonestopradio.com/stream?url=$url"

object Stations {
    val ENCICLOPEDIA = Station(
        id = "enciclopedia", name = "Radio Enciclopedia", short = "ENCICLOPEDIA", mark = "RE", call = "CMBQ",
        place = Place.HABANA, city = "La Habana", band = "94,1 FM", dial = "94,1 FM · 1260 AM", fm = 94.1f,
        tagline = "Una emisora para todos los momentos de la vida",
        streams = listOf(Links.STREAM, mirror(Links.STREAM)),
        top = 0xFF5A3568.toInt(), bottom = 0xFF241239.toInt(),
        accent = 0xFFCC66FF.toInt(), accent2 = 0xFFFFE1A3.toInt(),
        profile = "CMBQ · 94,1 FM y 530 / 1260 AM en La Habana.\n\n" +
            "Fundada el 7 de noviembre de 1962. Música instrumental de Cuba y del mundo, " +
            "voces femeninas y divulgación cultural y científica.",
        hasGuide = true, site = "https://www.radioenciclopedia.cu/"
    )

    val PROGRESO = Station(
        id = "progreso", name = "Radio Progreso", short = "PROGRESO", mark = "RP", call = "CMBC",
        place = Place.HABANA, city = "La Habana", band = "90,3 FM", dial = "90,3 FM · 640 AM", fm = 90.3f,
        tagline = "La Onda de la Alegría",
        streams = listOf("https://icecast.teveo.cu/XjfW7qWN", mirror("https://icecast.teveo.cu/XjfW7qWN")),
        top = 0xFFD9822B.toInt(), bottom = 0xFF5B1A4A.toInt(),
        accent = 0xFFFFB347.toInt(), accent2 = 0xFFFF6B8A.toInt(),
        profile = "CMBC · 640 AM y 90,3 FM en La Habana (onda corta, 4 765 kHz).\n\n" +
            "Salió al aire el 15 de diciembre de 1929. Nació en la tienda «El Progreso Cubano», " +
            "de Domingo Fernández, y de ahí su nombre. Su lema es «La Onda de la Alegría».\n\n" +
            "Desde su Estudio Uno, con capacidad para 300 personas, programó a La Sonora Matancera, " +
            "Antonio María Romeu y Barbarito Diez, además de radioteatro y espacios de variedades.",
        hasGuide = false, site = "https://www.radioprogreso.icrt.cu/"
    )

    /** Señal de onda media (AM) de Radio Rebelde; la de FM es [REBELDE_FM]. */
    val REBELDE = Station(
        id = "rebelde", name = "Radio Rebelde AM", short = "REBELDE AM", mark = "RB", call = "CMBA",
        place = Place.HABANA, city = "La Habana", band = "AM", dial = "Onda media (AM)", fm = null,
        tagline = "Aquí Radio Rebelde",
        streams = listOf("https://icecast.teveo.cu/kHKL7tWd", mirror("https://icecast.teveo.cu/kHKL7tWd")), // señal AM publicada en radiorebelde.cu
        top = 0xFFB3121C.toInt(), bottom = 0xFF14040A.toInt(),
        accent = 0xFFFF4A4A.toInt(), accent2 = 0xFFFFD6D6.toInt(),
        profile = "CMBA · señal de onda media (AM) de Radio Rebelde. La FM de La Habana es 96,7 MHz y existe " +
            "también en onda corta (5 025 kHz).\n\n" +
            "Fundada el 24 de febrero de 1958 por Ernesto Che Guevara en Altos de Conrado, Sierra Maestra. " +
            "Su primera transmisión duró 20 minutos y abrió con el himno rebelde. Su saludo, «Aquí Radio Rebelde», " +
            "se mantiene hasta hoy.\n\n" +
            "Emite las 24 horas: noticias, deportes nacionales e internacionales y magacines en vivo.",
        hasGuide = false, site = "https://www.radiorebelde.cu/"
    )

    val COCO = Station(
        id = "coco", name = "Radio COCO", short = "COCO", mark = "CO", call = "CMCK",
        place = Place.HABANA, city = "La Habana", band = "91,7 FM", dial = "91,7 FM · 980 AM", fm = 91.7f,
        tagline = "Deporte, información y música cubana",
        streams = listOf("https://icecast.teveo.cu/fvc4RVRz", mirror("https://icecast.teveo.cu/fvc4RVRz")),
        top = 0xFF12A5A0.toInt(), bottom = 0xFF10294F.toInt(),
        accent = 0xFFC7F464.toInt(), accent2 = 0xFF7FFFE0.toInt(),
        profile = "CMCK · 980 AM y 91,7 FM en La Habana.\n\n" +
            "Planta radial con más de 80 años de historia. Su programación es sobre todo deportiva e " +
            "informativa, con espacios de análisis y opinión, y música tradicional cubana.",
        hasGuide = false, site = "https://www.radiocubana.cu/emisoras/provinciales/la-habana/radio-coco/"
    )

    val REBELDE_FM = Station(
        id = "rebelde-fm", name = "Radio Rebelde FM", short = "REBELDE FM", mark = "RB", call = "CMBA",
        place = Place.HABANA, city = "La Habana", band = "96,7 FM", dial = "96,7 FM · señal de FM", fm = 96.7f,
        tagline = "Informativos, béisbol y revistas de la señal cubana de frecuencia modulada.",
        streams = listOf(
            "https://icecast.teveo.cu/zrXXWK9F",
            "https://theonestopradio.com/stream?url=https://icecast.teveo.cu/zrXXWK9F"
        ),
        top = 0xFFB3121C.toInt(), bottom = 0xFF14040A.toInt(),
        accent = 0xFFFF4A4A.toInt(), accent2 = 0xFFFFD6D6.toInt(),
        profile = "CMBA · señal de frecuencia modulada de Radio Rebelde: 96,7 FM en La Habana. Informativos, béisbol y revistas. " +
            "La señal de onda media es «Radio Rebelde AM».\n\nFundada el 24 de febrero de 1958 por Ernesto Che Guevara en la Sierra Maestra.",
        hasGuide = false, site = "https://www.radiorebelde.cu/"
    )

    val RELOJ = Station(
        id = "reloj", name = "Radio Reloj", short = "RELOJ", mark = "RR", call = "",
        place = Place.HABANA, city = "La Habana", band = "950 AM", dial = "950 AM", fm = null,
        tagline = "Noticias y hora exacta minuto a minuto, las 24 horas.",
        streams = listOf(
            "https://icecast.teveo.cu/b3jbfThq",
            "https://theonestopradio.com/stream?url=https://icecast.teveo.cu/b3jbfThq"
        ),
        top = themeTop(0x9CCFFF), bottom = themeBottom(0x9CCFFF),
        accent = themeAccent(0x9CCFFF), accent2 = themeSoft(0x9CCFFF),
        profile = "Noticias y hora exacta minuto a minuto, las 24 horas.\n\nLa Habana, Cuba · 950 AM.",
        hasGuide = false, site = "https://www.radioreloj.cu/"
    )

    val TAINO = Station(
        id = "taino", name = "Radio Taíno", short = "TAÍNO", mark = "RT", call = "",
        place = Place.HABANA, city = "La Habana", band = "93,3 FM", dial = "93,3 FM", fm = 93.3f,
        tagline = "Música cubana, turismo y actualidad cultural.",
        streams = listOf(
            "https://icecast.teveo.cu/3MCwWg3V"
        ),
        top = themeTop(0x99E4DF), bottom = themeBottom(0x99E4DF),
        accent = themeAccent(0x99E4DF), accent2 = themeSoft(0x99E4DF),
        profile = "Música cubana, turismo y actualidad cultural.\n\nLa Habana, Cuba · 93,3 FM.",
        hasGuide = false, site = "https://www.radiotaino.icrt.cu/"
    )

    val HABANA_CUBA = Station(
        id = "habana", name = "Radio Habana Cuba", short = "HABANA CUBA", mark = "HC", call = "",
        place = Place.HABANA, city = "La Habana", band = "102,5 FM", dial = "102,5 FM", fm = 102.5f,
        tagline = "Información internacional y cultura desde La Habana.",
        streams = listOf(
            "https://icecast.teveo.cu/McW3fLhs"
        ),
        top = themeTop(0xAAD9A1), bottom = themeBottom(0xAAD9A1),
        accent = themeAccent(0xAAD9A1), accent2 = themeSoft(0xAAD9A1),
        profile = "Información internacional y cultura desde La Habana.\n\nLa Habana, Cuba · 102,5 FM.",
        hasGuide = false, site = "https://www.radiohc.cu/"
    )

    val IPN = Station(
        id = "ipn", name = "Radio IPN", short = "IPN", mark = "IP", call = "",
        place = Place.MEXICO, city = "Ciudad de México", band = "95,7 FM", dial = "95,7 FM", fm = null,
        tagline = "Divulgación científica, innovación y vida universitaria desde el Instituto Politécnico Nacional.",
        streams = listOf(
            "http://201.124.135.243:8000/radio.mp3"
        ),
        top = themeTop(0xF2ABC6), bottom = themeBottom(0xF2ABC6),
        accent = themeAccent(0xF2ABC6), accent2 = themeSoft(0xF2ABC6),
        profile = "Divulgación científica, innovación y vida universitaria desde el Instituto Politécnico Nacional.\n\nCiudad de México, México · 95,7 FM.",
        hasGuide = false, site = "https://www.ipn.mx/radio/"
    )

    val UAM = Station(
        id = "uam", name = "UAM Radio", short = "UAM", mark = "UA", call = "",
        place = Place.MEXICO, city = "Ciudad de México", band = "94,1 FM", dial = "94,1 FM", fm = null,
        tagline = "Investigación, ciencia universitaria y conversaciones sobre el futuro.",
        streams = listOf(
            "https://stream5.mexiserver.com:1124/;"
        ),
        top = themeTop(0xF1CD86), bottom = themeBottom(0xF1CD86),
        accent = themeAccent(0xF1CD86), accent2 = themeSoft(0xF1CD86),
        profile = "Investigación, ciencia universitaria y conversaciones sobre el futuro.\n\nCiudad de México, México · 94,1 FM.",
        hasGuide = false, site = "https://uamradio.uam.mx/"
    )

    val UNAM = Station(
        id = "unam", name = "Radio UNAM", short = "UNAM", mark = "UN", call = "",
        place = Place.MEXICO, city = "Ciudad de México", band = "96,1 FM", dial = "96,1 FM", fm = null,
        tagline = "Ciencia, cultura, universidad y música desde la UNAM.",
        streams = listOf(
            "https://tv.radiohosting.online:9484/stream"
        ),
        top = themeTop(0xADC6FA), bottom = themeBottom(0xADC6FA),
        accent = themeAccent(0xADC6FA), accent2 = themeSoft(0xADC6FA),
        profile = "Ciencia, cultura, universidad y música desde la UNAM.\n\nCiudad de México, México · 96,1 FM.",
        hasGuide = false, site = "https://www.radio.unam.mx/"
    )

    val EDUCACION = Station(
        id = "educacion", name = "Radio Educación", short = "EDUCACIÓN", mark = "ED", call = "",
        place = Place.MEXICO, city = "Ciudad de México", band = "96,5 FM", dial = "96,5 FM", fm = null,
        tagline = "Radio pública mexicana de arte, educación y divulgación cultural.",
        streams = listOf(
            "https://s2.mexside.net/8174/stream"
        ),
        top = themeTop(0xFFCA89), bottom = themeBottom(0xFFCA89),
        accent = themeAccent(0xFFCA89), accent2 = themeSoft(0xFFCA89),
        profile = "Radio pública mexicana de arte, educación y divulgación cultural.\n\nCiudad de México, México · 96,5 FM.",
        hasGuide = false, site = "https://radioeducacion.edu.mx/"
    )

    val ILCE = Station(
        id = "ilce", name = "Radio ILCE", short = "ILCE", mark = "IL", call = "",
        place = Place.MEXICO, city = "Ciudad de México", band = "Por Internet", dial = "Por Internet", fm = null,
        tagline = "Radio educativa del Instituto Latinoamericano de la Comunicación Educativa. Los martes dedica sus bloques a ciencia y tecnología.",
        streams = listOf(
            "https://radio.ilce.edu.mx:8443/radioilce.mp3"
        ),
        top = themeTop(0xC9FF9C), bottom = themeBottom(0xC9FF9C),
        accent = themeAccent(0xC9FF9C), accent2 = themeSoft(0xC9FF9C),
        profile = "Radio educativa del Instituto Latinoamericano de la Comunicación Educativa. Los martes dedica sus bloques a ciencia y tecnología.\n\nCiudad de México, México · Por Internet.",
        hasGuide = false, site = "https://ilce.edu.mx/index.php/audiovisual/canales/radio-ilce"
    )

    val TEC = Station(
        id = "tec", name = "Tec Sounds Radio", short = "TEC SOUNDS", mark = "TC", call = "",
        place = Place.MEXICO, city = "Monterrey", band = "94,9 FM", dial = "94,9 FM", fm = null,
        tagline = "Radio del Tecnológico de Monterrey, con ciencia, innovación y vida universitaria.",
        streams = listOf(
            "https://mdstrm.com/live-stream-playlist/66884c4a98f03fecbe37a347.m3u8"
        ),
        top = themeTop(0x9FF2FF), bottom = themeBottom(0x9FF2FF),
        accent = themeAccent(0x9FF2FF), accent2 = themeSoft(0x9FF2FF),
        profile = "Radio del Tecnológico de Monterrey, con ciencia, innovación y vida universitaria.\n\nMonterrey, México · 94,9 FM.",
        hasGuide = false, site = "https://tecsoundsradio.tec.mx/es"
    )

    val MARCA = Station(
        id = "marca", name = "Radio MARCA", short = "MARCA", mark = "RM", call = "",
        place = Place.ESPANA, city = "Madrid", band = "Nacional", dial = "Cobertura nacional", fm = null,
        tagline = "Actualidad deportiva, fútbol en directo, entrevistas y tertulias.",
        streams = listOf(
            "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIOMARCA_NACIONAL.mp3",
            "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIOMARCA_NACIONALAAC.aac"
        ),
        top = themeTop(0xFFA699), bottom = themeBottom(0xFFA699),
        accent = themeAccent(0xFFA699), accent2 = themeSoft(0xFFA699),
        profile = "Actualidad deportiva, fútbol en directo, entrevistas y tertulias.\n\nMadrid, España · Cobertura nacional.",
        hasGuide = false, site = "https://www.marca.com/radio.html"
    )

    val SER = Station(
        id = "ser", name = "Cadena SER", short = "SER", mark = "SE", call = "",
        place = Place.ESPANA, city = "Madrid", band = "Nacional", dial = "Cobertura nacional", fm = null,
        tagline = "Actualidad, Carrusel Deportivo y El Larguero.",
        streams = listOf(
            "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASER.mp3",
            "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASERAAC.aac"
        ),
        top = themeTop(0xC0A9FF), bottom = themeBottom(0xC0A9FF),
        accent = themeAccent(0xC0A9FF), accent2 = themeSoft(0xC0A9FF),
        profile = "Actualidad, Carrusel Deportivo y El Larguero.\n\nMadrid, España · Cobertura nacional.",
        hasGuide = false, site = "https://cadenaser.com/"
    )

    val RNE5 = Station(
        id = "rne5", name = "Radio 5 · RNE", short = "RADIO 5", mark = "R5", call = "",
        place = Place.ESPANA, city = "Madrid", band = "Nacional", dial = "Cobertura nacional", fm = null,
        tagline = "Información continua y espacios de divulgación científica, salud y tecnología.",
        streams = listOf(
            "https://dispatcher.rndfnk.com/crtve/rne5/main/mp3/high",
            "https://dispatcher.rndfnk.com/crtve/rne5/main/aac/low"
        ),
        top = themeTop(0x94DFBC), bottom = themeBottom(0x94DFBC),
        accent = themeAccent(0x94DFBC), accent2 = themeSoft(0x94DFBC),
        profile = "Información continua y espacios de divulgación científica, salud y tecnología.\n\nMadrid, España · Cobertura nacional.",
        hasGuide = false, site = "https://www.rtve.es/play/radio/radio-5/"
    )

    /** Orden de las tarjetas. */
    val all: List<Station> = listOf(
        ENCICLOPEDIA, PROGRESO, REBELDE, REBELDE_FM, COCO, RELOJ, TAINO, HABANA_CUBA,
        IPN, UAM, UNAM, EDUCACION, ILCE, TEC, MARCA, SER, RNE5
    )

    /** Emisoras con frecuencia en el dial FM de La Habana. */
    val onDial: List<Station> = all.filter { it.fm != null }

    fun inPlace(place: Place): List<Station> = all.filter { it.place == place }

    var current: Station = ENCICLOPEDIA
        private set

    private var started = false
    private val listeners = LinkedHashSet<(Station) -> Unit>()

    fun byId(id: String?): Station = all.firstOrNull { it.id == id } ?: ENCICLOPEDIA

    fun indexOf(s: Station): Int = all.indexOf(s).coerceAtLeast(0)

    fun init(context: Context) {
        if (started) return
        started = true
        current = byId(context.getSharedPreferences("station", Context.MODE_PRIVATE).getString("emisora", null))
    }

    /** Cambia la emisora elegida y la recuerda. Devuelve true si realmente cambió. */
    fun select(context: Context, s: Station): Boolean {
        init(context)
        if (s === current) return false
        current = s
        context.getSharedPreferences("station", Context.MODE_PRIVATE).edit().putString("emisora", s.id).apply()
        listeners.toList().forEach { it(s) }
        return true
    }

    fun next(): Station = all[(indexOf(current) + 1) % all.size]
    fun previous(): Station = all[(indexOf(current) + all.size - 1) % all.size]

    fun listen(l: (Station) -> Unit) { listeners.add(l) }
    fun unlisten(l: (Station) -> Unit) { listeners.remove(l) }
}

/** Lo que suena ahora: el programa de la parrilla (Enciclopedia) o el lema de la emisora. */
fun Station.nowTitle(): String = if (hasGuide) ProgramSchedule.currentTitle() else tagline
