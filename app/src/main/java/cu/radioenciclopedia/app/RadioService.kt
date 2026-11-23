package cu.radioenciclopedia.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager

/** Reproductor de la señal oficial. No abre un navegador ni descarga grabaciones. */
class RadioService : Service() {
    companion object {
        const val PLAY = "cu.radioenciclopedia.PLAY"
        const val PAUSE = "cu.radioenciclopedia.PAUSE"
        const val STOP = "cu.radioenciclopedia.STOP"
        const val RESTART = "reiniciar"
        const val PREV = "cu.radioenciclopedia.PREV"
        const val NEXT = "cu.radioenciclopedia.NEXT"
        private const val CHANNEL = "radio_en_directo"
        private const val NOTIFICATION = 62
    }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private lateinit var session: MediaSession
    private lateinit var audio: AudioManager
    private lateinit var focus: AudioFocusRequest
    private var focusHeld = false
    private var resumeOnFocus = false
    /** Señal de la emisora que se está probando (0 = la principal, 1 = la de respaldo). */
    private var attempt = 0
    private val connectTimeout = Runnable {
        if (RadioPlayback.info.status == PlaybackStatus.CONNECTING && !tryBackup()) fail("No se pudo conectar · toca para reintentar")
    }
    private val refreshProgram = object : Runnable {
        override fun run() {
            updateSession()
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification())
            handler.postDelayed(this, 60_000L)
        }
    }
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Stations.init(this)
        audio = getSystemService(AudioManager::class.java)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener({ change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> pause()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        if (RadioPlayback.info.active) {
                            pause(releaseFocus = false)
                            resumeOnFocus = true
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player?.setVolume(0.2f, 0.2f)
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        player?.setVolume(1f, 1f)
                        if (resumeOnFocus) { resumeOnFocus = false; play() }
                    }
                }
            }, handler).build()
        session = MediaSession(this, "Radio Enciclopedia").apply {
            @Suppress("DEPRECATION")
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setSessionActivity(openPlayerIntent())
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    val wasActive = RadioPlayback.info.active
                    play()
                    if (!wasActive) Announcer.say(this@RadioService, Announcer.stationLine(Stations.current))
                }
                override fun onPause() { pause(); Announcer.say(this@RadioService, "Radio en pausa.") }
                override fun onStop() { stop(); Announcer.say(this@RadioService, "Radio detenida.") }
                // Botones de auriculares, Bluetooth y asistente: cambian de emisora.
                override fun onSkipToNext() = switchBy(1)
                override fun onSkipToPrevious() = switchBy(-1)
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    Stations.all.firstOrNull { it.id == mediaId }?.let { RemoteControl.tune(this@RadioService, it) }
                }
            }, handler)
            isActive = true
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Radio en directo", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Controles de Radio Enciclopedia"
                setShowBadge(false)
            })
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(noisy, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(noisy, filter)
        handler.postDelayed(refreshProgram, 60_000L)
        // Mientras el locutor habla, la radio baja de volumen para que se le entienda.
        Announcer.onSpeaking = { speaking -> val v = if (speaking) 0.25f else 1f; player?.setVolume(v, v) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PLAY -> {
                // Se establece el primer plano antes de solicitar foco de audio (Android 15).
                startForeground(NOTIFICATION, notification())
                // Cambio de emisora: se descarta la señal anterior antes de abrir la nueva.
                if (intent.getBooleanExtra(RESTART, false)) releasePlayer()
                play()
            }
            PAUSE -> { pause(); Announcer.say(this, "Radio en pausa.") }
            STOP -> { stop(); Announcer.say(this, "Radio detenida.") }
            NEXT -> switchBy(1)
            PREV -> switchBy(-1)
            else -> stopSelf()
        }
        // Una interrupción del sistema nunca inicia sonido por su cuenta.
        return START_NOT_STICKY
    }

    private fun play() {
        if (RadioPlayback.info.active && player != null) return
        resumeOnFocus = false
        attempt = 0
        open()
    }

    /** Abre la señal número [attempt] de la emisora elegida. */
    private fun open() {
        releasePlayer()
        if (!focusHeld) {
            focusHeld = audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!focusHeld) { fail("Otra aplicación está usando el audio · toca para reintentar"); return }
        }
        setState(PlaybackInfo(PlaybackStatus.CONNECTING))
        try {
            val next = MediaPlayer()
            player = next
            next.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            next.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            next.setOnPreparedListener { prepared ->
                if (player !== prepared) return@setOnPreparedListener
                handler.removeCallbacks(connectTimeout)
                try {
                    prepared.start()
                    setState(PlaybackInfo(PlaybackStatus.PLAYING))
                } catch (_: IllegalStateException) { fail("La señal se interrumpió · toca para reintentar") }
            }
            next.setOnInfoListener { current, what, _ ->
                if (current === player) {
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                        setState(PlaybackInfo(PlaybackStatus.CONNECTING))
                        handler.removeCallbacks(connectTimeout)
                        handler.postDelayed(connectTimeout, 30_000L)
                    } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                        handler.removeCallbacks(connectTimeout)
                        setState(PlaybackInfo(PlaybackStatus.PLAYING))
                    }
                }
                false
            }
            next.setOnErrorListener { failed, _, _ ->
                if (player === failed && !tryBackup()) fail("No se pudo conectar con la emisora · toca para reintentar")
                true
            }
            next.setOnCompletionListener { ended ->
                if (player === ended && !tryBackup()) fail("La señal se interrumpió · toca para reintentar")
            }
            next.setDataSource(Stations.current.streams[attempt.coerceIn(0, Stations.current.streams.size - 1)])
            next.prepareAsync()
            handler.postDelayed(connectTimeout, 30_000L)
        } catch (_: Exception) { if (!tryBackup()) fail("No se pudo conectar con la emisora · toca para reintentar") }
    }

    /** Si la emisora tiene otra señal, la prueba. Devuelve false cuando ya no quedan más. */
    private fun tryBackup(): Boolean {
        if (attempt + 1 >= Stations.current.streams.size) return false
        attempt++
        handler.post { open() }
        return true
    }

    private fun switchBy(direction: Int) {
        RemoteControl.step(this, direction)
    }

    private fun pause(releaseFocus: Boolean = true) {
        resumeOnFocus = false
        releasePlayer()
        if (releaseFocus) abandonFocus()
        setState(PlaybackInfo(PlaybackStatus.PAUSED))
    }

    private fun stop() {
        resumeOnFocus = false
        releasePlayer()
        abandonFocus()
        setState(PlaybackInfo(PlaybackStatus.IDLE))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fail(message: String) {
        Announcer.say(this, message.substringBefore(" ·") + ". Di siguiente para probar otra emisora.")
        resumeOnFocus = false
        releasePlayer()
        abandonFocus()
        setState(PlaybackInfo(PlaybackStatus.ERROR, message))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayer() {
        handler.removeCallbacks(connectTimeout)
        val old = player
        player = null
        old?.release()
    }

    private fun abandonFocus() {
        if (focusHeld) audio.abandonAudioFocusRequest(focus)
        focusHeld = false
    }

    private fun setState(info: PlaybackInfo) {
        RadioPlayback.update(info)
        updateSession()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification())
    }

    private fun updateSession() {
        val status = when (RadioPlayback.info.status) {
            PlaybackStatus.PLAYING -> PlaybackState.STATE_PLAYING
            PlaybackStatus.CONNECTING -> PlaybackState.STATE_BUFFERING
            PlaybackStatus.PAUSED -> PlaybackState.STATE_PAUSED
            PlaybackStatus.ERROR -> PlaybackState.STATE_ERROR
            PlaybackStatus.IDLE -> PlaybackState.STATE_STOPPED
        }
        val state = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP or
                PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
            .setState(status, PlaybackState.PLAYBACK_POSITION_UNKNOWN, if (status == PlaybackState.STATE_PLAYING) 1f else 0f)
        if (status == PlaybackState.STATE_ERROR) state.setErrorMessage(RadioPlayback.info.label)
        session.setPlaybackState(state.build())
        session.setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, Stations.current.nowTitle())
            .putString(MediaMetadata.METADATA_KEY_ARTIST, Stations.current.name)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "En directo · La Habana")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L).build())
    }

    private fun command(action: String): PendingIntent {
        val request = when (action) { PLAY -> 1; PAUSE -> 2; PREV -> 4; NEXT -> 5; else -> 3 }
        return PendingIntent.getService(this, request, Intent(this, RadioService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun notification(): Notification {
        val info = RadioPlayback.info
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_radio_notification)
            .setContentTitle(Stations.current.name)
            .setContentText(info.label)
            .setSubText(Stations.current.nowTitle())
            .setContentIntent(openPlayerIntent())
            .setColor(if (Stations.current === Stations.ENCICLOPEDIA) Pal.BRAND else Stations.current.accent)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true).setOngoing(info.active).setShowWhen(false)
            .addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                "Anterior", command(PREV)).build())
            .addAction(Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, if (info.active) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
                if (info.active) "Pausar" else "Reproducir", command(if (info.active) PAUSE else PLAY)).build())
            .addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_next),
                "Siguiente", command(NEXT)).build())
            .addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                "Detener", command(STOP)).build())
            .setDeleteIntent(command(STOP))
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()
    }

    private fun openPlayerIntent(): PendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
        .putExtra("screen", 1).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        Announcer.onSpeaking = null
        releasePlayer()
        abandonFocus()
        unregisterReceiver(noisy)
        session.release()
        if (RadioPlayback.info.active) RadioPlayback.update(PlaybackInfo(PlaybackStatus.IDLE))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
