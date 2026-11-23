package cu.radioenciclopedia.app

enum class LinkApp { BROWSER, YOUTUBE, FACEBOOK, INSTAGRAM, TELEGRAM, X, IVOOX }

class Link(val name: String, val url: String, val app: LinkApp = LinkApp.BROWSER)

/**
 * Direcciones EXACTAS de la especificación. No añadir ni modificar sin autorización.
 * ENGLISH («Sitio web en inglés») fue indicada después por el propietario del proyecto.
 */
object Links {
    val WEB = Link("SITIO WEB", "https://www.radioenciclopedia.cu/")
    val ENGLISH = Link("SITIO WEB EN INGLÉS", "https://www.radioenciclopedia.cu/cultural-news/tag/cuba/")
    val AUDIO = Link("AUDIO REAL EN INTERNET", "https://www.radioenciclopedia.cu/audio-real/")
    // Fuente audio/mpeg publicada en el reproductor de la página oficial.
    const val STREAM = "https://icecast.teveo.cu/9Rnrbjzq"
    val MARTI = Link("PENSAMIENTO DE JOSÉ MARTÍ", "https://es.wikisource.org/wiki/Maestros_ambulantes")
    val PODCAST = Link("PODCAST", "https://www.ivoox.com/podcast-programas-radio-enciclopedia_sq_f1600680_1.html", LinkApp.IVOOX)
    val VIDEOS = Link("VIDEOS", "https://www.youtube.com/channel/UCB6TNeZ87tAjagFrS1u1ySw", LinkApp.YOUTUBE)
    val FACEBOOK = Link("FACEBOOK", "https://www.facebook.com/radioenciclopedia/", LinkApp.FACEBOOK)
    val X = Link("X", "https://x.com/enciclopedia_cu", LinkApp.X)
    val TELEGRAM = Link("TELEGRAM", "https://t.me/radio_enciclopedia7_cu", LinkApp.TELEGRAM)
    val INSTAGRAM = Link(
        "INSTAGRAM",
        "https://www.instagram.com/radio_enciclopedia/", LinkApp.INSTAGRAM
    )

    val SCHEDULE = Link("PROGRAMACIÓN OFICIAL", "https://www.radioenciclopedia.cu/programacion-general/")
    val ABOUT = Link("QUIÉNES SOMOS", "https://www.radioenciclopedia.cu/acerca-de/")
    val ALL = listOf(WEB, ENGLISH, AUDIO, PODCAST, VIDEOS, FACEBOOK, X, TELEGRAM, INSTAGRAM)

    // Texto mostrado y número para el marcador (formato internacional de Cuba)
    val PHONES = listOf(
        "(+53) 78384587" to "+5378384587",
        "78361882" to "+5378361882",
        "78361883" to "+5378361883"
    )
    const val EMAIL = "radio.enciclo.62@gmail.com"
}
