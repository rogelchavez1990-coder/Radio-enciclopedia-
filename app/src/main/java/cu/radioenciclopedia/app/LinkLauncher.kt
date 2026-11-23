package cu.radioenciclopedia.app

import java.net.URLEncoder

data class LinkRequest(val uri: String, val packageName: String? = null)

/** Prueba primero las aplicaciones asociadas, después el manejador predeterminado del enlace. */
object LinkLauncher {
    fun open(link: Link, launch: (LinkRequest) -> Boolean): Boolean {
        for (request in requests(link)) if (launch(request)) return true
        return false
    }

    internal fun requests(link: Link): List<LinkRequest> {
        val packages = when (link.app) {
            LinkApp.YOUTUBE -> listOf("com.google.android.youtube")
            LinkApp.FACEBOOK -> listOf("com.facebook.katana", "com.facebook.lite")
            LinkApp.INSTAGRAM -> listOf("com.instagram.android", "com.instagram.lite")
            LinkApp.TELEGRAM -> listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram")
            LinkApp.X -> listOf("com.twitter.android")
            LinkApp.IVOOX -> listOf("com.ivoox.app")
            LinkApp.BROWSER -> emptyList()
        }
        val native = when (link.app) {
            LinkApp.FACEBOOK -> "fb://facewebmodal/f?href=" + URLEncoder.encode(link.url, "UTF-8")
            LinkApp.INSTAGRAM -> "instagram://user?username=radio_enciclopedia"
            LinkApp.TELEGRAM -> "tg://resolve?domain=radio_enciclopedia7_cu"
            LinkApp.X -> "twitter://user?screen_name=enciclopedia_cu"
            else -> null
        }
        val result = ArrayList<LinkRequest>()
        for (pkg in packages) {
            // Facebook resuelve la página dentro de su aplicación mediante su enlace universal.
            // El esquema antiguo queda como alternativa para versiones que aún lo requieren.
            if (link.app == LinkApp.FACEBOOK) result.add(LinkRequest(link.url, pkg))
            if (native != null) result.add(LinkRequest(native, pkg))
            if (link.app != LinkApp.FACEBOOK) result.add(LinkRequest(link.url, pkg))
        }
        // Compatible también con otros clientes de Telegram elegidos por el usuario.
        if (link.app == LinkApp.TELEGRAM && native != null) result.add(LinkRequest(native))
        result.add(LinkRequest(link.url))
        return result
    }
}
