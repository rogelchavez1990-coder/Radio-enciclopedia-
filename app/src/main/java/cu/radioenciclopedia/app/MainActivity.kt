package cu.radioenciclopedia.app

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.graphics.RenderEffect
import android.graphics.Shader
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast

/**
 * Radio Enciclopedia: Inicio, Escuchar, Programas, Comunidad, Contacto y Frecuencias.
 *
 * Radio nativa en servicio multimedia y enlaces que priorizan las aplicaciones asociadas.
 * Las animaciones se detienen cuando la app no está visible y respetan el ajuste del sistema.
 */
class MainActivity : Activity() {
    companion object {
        /** Acción de los atajos del icono. */
        const val TUNE = "cu.radioenciclopedia.TUNE"
    }

    private lateinit var host: FrameLayout
    private lateinit var nav: NavBar
    private lateinit var drawer: StationDrawer
    private lateinit var burst: BurstView
    private var predictiveBack: PredictiveBack? = null
    private var drawerOpen = false
    private var blurAnim: ValueAnimator? = null
    private var currentBlur = 0f
    private val navScreens = listOf(0, 1, 2, 4)
    private val minuteHandler = Handler(Looper.getMainLooper())
    private val minuteRefresh = object : Runnable {
        override fun run() {
            screens.getOrNull(current)?.refresh?.invoke()
            minuteHandler.postDelayed(this, 60_000L)
        }
    }
    private val screens = ArrayList<Screen>()
    private var current = -1
    private var insetL = 0
    private var insetT = 0
    private var insetR = 0
    private var insetB = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fonts.init(this)
        Stations.init(this)
        configurarVentana()

        val root = FrameLayout(this)
        root.setBackgroundColor(Pal.DEEP)
        // Android 13+: fondo con shader AGSL. Si no compila o es una versión anterior, AuroraView.
        val backdrop: View = (if (Build.VERSION.SDK_INT >= 33) LiveBackdropView(this).takeIf { it.ok } else null)
            ?: AuroraView(this)
        root.addView(backdrop, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(AccentWashView(this), FrameLayout.LayoutParams(MATCH, MATCH))

        host = FrameLayout(this)
        root.addView(host, FrameLayout.LayoutParams(MATCH, MATCH))

        val factory = ScreenFactory(this)
        screens.add(factory.inicio())
        screens.add(factory.escuchar())
        screens.add(factory.programas())
        screens.add(factory.comunidad())
        screens.add(factory.contacto())
        screens.add(factory.frecuencias())
        for (s in screens) {
            s.scroll.visibility = View.GONE
            host.addView(s.scroll, FrameLayout.LayoutParams(MATCH, MATCH))
        }

        nav = NavBar(
            this,
            listOf("Inicio", "Escuchar", "Programas", "Contacto"),
            listOf(
                R.drawable.ic_nav_home, R.drawable.ic_nav_listen, R.drawable.ic_nav_programs,
                R.drawable.ic_nav_contact
            )
        ) { goTo(navScreens[it]) }
        root.addView(nav, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        burst = BurstView(this)
        root.addView(burst, FrameLayout.LayoutParams(MATCH, MATCH))

        drawer = StationDrawer(this)
        drawer.visibility = View.GONE
        root.addView(drawer, FrameLayout.LayoutParams(MATCH, MATCH))

        setContentView(root)
        root.setOnApplyWindowInsetsListener { _, insets ->
            val m = margenesDelSistema(insets)
            insetL = m[0]; insetT = m[1]; insetR = m[2]; insetB = m[3]
            aplicarMargenes()
            insets
        }
        aplicarMargenes()
        root.requestApplyInsets()

        if (Build.VERSION.SDK_INT >= 33) predictiveBack = PredictiveBack(this)

        val requestedScreen = if (intent.hasExtra("screen")) intent.getIntExtra("screen", 0)
            else if (intent.action == TUNE) 1 else null
        intent.removeExtra("screen")
        val tab = requestedScreen ?: savedInstanceState?.getInt("tab", 0) ?: 0
        show(tab.coerceIn(0, screens.size - 1), savedInstanceState == null)
        handleShortcut(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val shortcut = intent.action == TUNE
        handleShortcut(intent)
        if (shortcut) goTo(1)
        if (intent.hasExtra("screen")) {
            val screen = intent.getIntExtra("screen", 0)
            intent.removeExtra("screen")
            goTo(screen)
        }
    }

    override fun onResume() {
        super.onResume()
        Announcer.uiVisible = true
        Clock.start()
        minuteHandler.removeCallbacks(minuteRefresh)
        minuteHandler.post(minuteRefresh)
    }

    override fun onPause() {
        Announcer.uiVisible = false
        // Las animaciones decorativas se detienen cuando la app no está visible (ahorro de batería).
        Clock.stop()
        minuteHandler.removeCallbacks(minuteRefresh)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", current)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (drawerOpen) closeMenu()
        else if (current > 0) goTo(0) else super.onBackPressed()
    }

    private fun updateBack() {
        predictiveBack?.update(drawerOpen || current > 0)
    }

    /** Acción de «atrás» cuando hay algo que cerrar: menú, o volver a Inicio. */
    fun handleBack() {
        if (drawerOpen) closeMenu() else if (current > 0) goTo(0) else finish()
    }

    // Vista previa del gesto «atrás» (Android 14+): la pantalla se encoge y se desplaza con el dedo.
    fun backProgress(p: Float, edge: Int) {
        if (drawerOpen) {
            drawer.backPreview(p)
            return
        }
        val v = screens.getOrNull(current)?.scroll ?: return
        val dir = if (edge == android.window.BackEvent.EDGE_LEFT) 1f else -1f
        val s = 1f - 0.09f * p
        v.cancelMotion()
        v.animate().cancel()
        v.scaleX = s
        v.scaleY = s
        v.translationX = dir * dp(28f) * p
        v.alpha = 1f - 0.3f * p
    }

    fun backCancel() {
        if (drawerOpen) {
            drawer.backReset()
            return
        }
        screens.getOrNull(current)?.scroll?.animate()?.scaleX(1f)?.scaleY(1f)?.translationX(0f)?.alpha(1f)?.setDuration(200L)?.start()
    }

    fun backCommit() {
        if (!drawerOpen) {
            screens.getOrNull(current)?.scroll?.let {
                it.animate().cancel()
                it.scaleX = 1f; it.scaleY = 1f; it.translationX = 0f; it.alpha = 1f
            }
        } else {
            drawer.backReset()
        }
        handleBack()
    }

    // ------------------------------------------------------------------ ventana y barras del sistema

    private fun configurarVentana() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    /** Devuelve [izquierda, arriba, derecha, abajo] ocupados por las barras del sistema y recortes. */
    @Suppress("DEPRECATION")
    private fun margenesDelSistema(insets: WindowInsets): IntArray =
        if (Build.VERSION.SDK_INT >= 30) {
            val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            intArrayOf(i.left, i.top, i.right, i.bottom)
        } else {
            intArrayOf(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom
            )
        }

    /** El contenido queda entre la barra de estado y la barra de navegación inferior. */
    private fun aplicarMargenes() {
        val navH = dpi(66)
        for (s in screens) {
            s.scroll.setPadding(insetL, insetT, insetR, navH + insetB + dpi(10))
        }
        nav.setPadding(insetL, 0, insetR, insetB)
        drawer.setSafeInsets(insetL, insetT, insetR, insetB)
    }

    // ------------------------------------------------------------------ navegación entre pantallas

    fun goTo(index: Int) {
        if (index !in screens.indices) return
        closeMenu()
        if (index != current) show(index, true)
    }

    private fun show(index: Int, animate: Boolean) {
        val from = if (current >= 0) screens[current] else null
        val to = screens[index]
        val dir = if (current < 0 || index > current) 1 else -1
        current = index
        nav.select(navScreens.indexOf(index), animate)
        to.refresh?.invoke()

        val anim = animate && Motion.enabled()

        for (s in screens) {
            if (s !== to && s !== from) ocultar(s)
        }
        if (from != null) {
            from.scroll.cancelMotion()
            from.scroll.animate().cancel()
            if (anim) {
                // Eje compartido: la pantalla que sale se aparta y se encoge un poco.
                from.scroll.animate().alpha(0f).translationX(-dir * dp(28f)).scaleX(0.95f).scaleY(0.95f)
                    .setDuration(150L).withEndAction { ocultar(from) }.start()
            } else {
                ocultar(from)
            }
        }

        restablecer(to)
        to.scroll.visibility = View.VISIBLE
        if (anim) {
            // La que entra llega con un resorte desde el lado de la navegación.
            to.scroll.springIn(30L, dx = dir * dp(54f), fromScale = 0.94f, ratio = 0.8f, stiffness = 260f)
            entradaEscalonada(to)
        }
        to.onShow?.invoke()
        updateBack()
    }

    /** Entrada suave y escalonada; sin rebotes que dificulten leer el contenido. */
    private fun entradaEscalonada(s: Screen) {
        var i = 0
        for (v in s.items) {
            v.springIn(60L + minOf(i, 5) * 50L, dy = dp(26f), fromScale = 0.94f, ratio = 0.68f)
            i++
        }
    }

    private fun restablecer(s: Screen) {
        s.scroll.cancelMotion()
        s.scroll.animate().cancel()
        s.scroll.alpha = 1f
        s.scroll.translationX = 0f
        s.scroll.scaleX = 1f
        s.scroll.scaleY = 1f
        for (v in s.items) {
            v.cancelMotion()
            v.animate().cancel()
            v.alpha = 1f
            v.translationY = 0f
            v.scaleX = 1f
            v.scaleY = 1f
        }
    }

    private fun ocultar(s: Screen) {
        if (current >= 0 && s === screens[current]) return
        restablecer(s)
        s.scroll.visibility = View.GONE
    }

    private fun dp(v: Float): Float = resources.displayMetrics.density * v
    private fun dpi(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    // ------------------------------------------------------------------ acciones externas

    fun openLink(link: Link) {
        if (link === Links.AUDIO) { goTo(1); playRadio(); return }
        Haptics.tap(window.decorView)
        val opened = LinkLauncher.open(link) { request ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.uri)).apply { request.packageName?.let { setPackage(it) } })
                true
            } catch (e: ActivityNotFoundException) {
                false
            } catch (e: SecurityException) {
                false
            }
        }
        if (!opened) Toast.makeText(this, R.string.error_no_browser, Toast.LENGTH_LONG).show()
    }

    fun playRadio() {
        requestPlayerNotification()
        if (!RadioPlayback.info.active) RadioPlayback.play(this)
    }

    /** Al empezar a sonar, [from] lanza una explosión de notas y destellos desde su centro. */
    fun toggleRadio(from: View? = null) {
        val starting = !RadioPlayback.info.active
        if (starting) {
            requestPlayerNotification()
            if (from != null) burstFrom(from)
        }
        RadioPlayback.toggle(this)
    }

    fun burstFrom(v: View) {
        val a = IntArray(2)
        val b = IntArray(2)
        v.getLocationInWindow(a)
        burst.getLocationInWindow(b)
        burst.burst(a[0] - b[0] + v.width / 2f, a[1] - b[1] + v.height / 2f)
        Haptics.tap(v)
    }

    private fun requestPlayerNotification() {
        val prefs = getSharedPreferences("station", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            && !prefs.getBoolean("notificationAsked", false)) {
            prefs.edit().putBoolean("notificationAsked", true).apply()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 62)
        }
    }

    // ------------------------------------------------------------------ emisoras

    /** Cambia de emisora (con haptica, ráfaga de color y ondas). Con [play] la señal arranca sola. */
    fun chooseStation(s: Station, play: Boolean = false) {
        if (play) requestPlayerNotification()
        val changed = RadioPlayback.switchStation(this, s, startIfStopped = play)
        if (changed) {
            // Con TalkBack se dice la emisora nueva; sin él, la pantalla ya lo muestra.
            window.decorView.announceForAccessibility(Announcer.stationLine(s))
            Haptics.lock(window.decorView)
            if (host.width > 0) burst.burstTinted(host.width / 2f, host.height * 0.30f, s.accent, s.accent2)
        }
    }

    fun showStationInfo(s: Station) {
        val dialog = AlertDialog.Builder(this).setTitle(s.name + " · " + s.dial)
            .setMessage("«" + s.tagline + "»\n\n" + s.profile +
                "\n\nLos colores y las animaciones de la tarjeta son una interpretación de diseño de la app, no el logotipo oficial.")
            .setPositiveButton("Escuchar") { _, _ -> chooseStation(s, true) }
            .setNegativeButton("Cerrar", null)
        s.site?.let { url -> dialog.setNeutralButton("Sitio web") { _, _ -> openLink(Link("Sitio de " + s.name, url)) } }
        dialog.show()
    }

    /** Atajo del icono («Radio Rebelde», etc.): abre Escuchar y sintoniza la emisora. */
    private fun handleShortcut(i: Intent?) {
        val shortcut = i ?: return
        if (shortcut.action != TUNE) return
        shortcut.action = null
        chooseStation(Stations.byId(shortcut.getStringExtra("emisora")), true)
    }

    fun openMenu() {
        if (drawerOpen) return
        drawerOpen = true
        drawer.animate().cancel()
        drawer.alpha = 1f
        drawer.markCurrent(current)
        drawer.visibility = View.VISIBLE
        host.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        nav.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        drawer.requestFocus()
        drawer.animateIn()
        blurBackground(true)
        updateBack()
    }

    fun closeMenu() {
        if (!::drawer.isInitialized || !drawerOpen) return
        drawerOpen = false
        host.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        nav.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        drawer.animateOut { if (!drawerOpen) drawer.visibility = View.GONE }
        blurBackground(false)
        updateBack()
    }

    /** Android 12+: el contenido detrás del menú se desenfoca con suavidad. */
    private fun blurBackground(on: Boolean) {
        if (Build.VERSION.SDK_INT < 31) return
        blurAnim?.cancel()
        val to = if (on) dp(14f) else 0f
        if (!Motion.enabled()) {
            aplicarDesenfoque(to)
            return
        }
        blurAnim = ValueAnimator.ofFloat(currentBlur, to).apply {
            duration = 260L
            addUpdateListener { aplicarDesenfoque(it.animatedValue as Float) }
            start()
        }
    }

    @TargetApi(31)
    private fun aplicarDesenfoque(radio: Float) {
        currentBlur = radio
        val fx = if (radio < 0.5f) null else RenderEffect.createBlurEffect(radio, radio, Shader.TileMode.CLAMP)
        host.setRenderEffect(fx)
        nav.setRenderEffect(fx)
    }

    fun showAbout() {
        closeMenu()
        AlertDialog.Builder(this).setTitle("CMBQ Radio Enciclopedia")
            .setMessage("Una emisora para todos los momentos de la vida.\n\n" +
                "Fundada el 7 de noviembre de 1962 en La Habana. Música instrumental de Cuba y del mundo, voces femeninas y divulgación cultural y científica.\n\n" +
                "La programación semanal reúne 15 espacios. Frecuencias nacionales en FM disponibles desde Inicio y el menú.\n\n" +
                "Idea de la aplicación: Gilberto González García\n" +
                "Desarrollador principal: Rogelio Rivero Chávez\n\nVersión " + BuildConfig.VERSION_NAME)
            .setPositiveButton("Web oficial") { _, _ -> openLink(Links.ABOUT) }
            .setNegativeButton("Cerrar", null).show()
    }

    private fun favorites(): Set<String> = getSharedPreferences("station", MODE_PRIVATE).getStringSet("favorites", emptySet())!!.toSet()

    fun toggleFavorite(link: Link) {
        val values = favorites().toMutableSet()
        val added = values.add(link.url)
        if (!added) values.remove(link.url)
        getSharedPreferences("station", MODE_PRIVATE).edit().putStringSet("favorites", values).apply()
        Toast.makeText(this, if (added) "Guardado en Favoritos" else "Eliminado de Favoritos", Toast.LENGTH_SHORT).show()
    }

    fun showFavorites() {
        closeMenu()
        val saved = Links.ALL.filter { it.url in favorites() }
        val dialog = AlertDialog.Builder(this).setTitle("Favoritos")
        if (saved.isEmpty()) dialog.setMessage("Guarda aquí tus accesos preferidos de la emisora.")
        else dialog.setItems(saved.map { it.name }.toTypedArray()) { _, i -> openLink(saved[i]) }
        dialog.setPositiveButton("Elegir enlaces") { _, _ -> editFavorites() }
            .setNegativeButton("Cerrar", null).show()
    }

    private fun editFavorites() {
        val chosen = favorites().toMutableSet()
        AlertDialog.Builder(this).setTitle("Elegir favoritos")
            .setMultiChoiceItems(Links.ALL.map { it.name }.toTypedArray(), Links.ALL.map { it.url in chosen }.toBooleanArray()) { _, i, on ->
                if (on) chosen.add(Links.ALL[i].url) else chosen.remove(Links.ALL[i].url)
            }.setPositiveButton("Guardar") { _, _ -> getSharedPreferences("station", MODE_PRIVATE).edit().putStringSet("favorites", chosen).apply() }
            .setNegativeButton("Cancelar", null).show()
    }

    /** Abre el marcador con el número escrito. NUNCA inicia la llamada por sí solo. */
    fun dial(numero: String) {
        lanzar(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", numero, null)), R.string.error_no_dialer)
    }

    fun sendMail() {
        lanzar(Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", Links.EMAIL, null)), R.string.error_no_mail)
    }

    private fun lanzar(intent: Intent, mensajeSinApp: Int) {
        Haptics.tap(window.decorView)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, mensajeSinApp, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show()
        }
    }
}
