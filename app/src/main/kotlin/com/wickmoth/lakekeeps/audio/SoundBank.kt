package com.wickmoth.lakekeeps.audio

import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.wickmoth.lakekeeps.R

/**
 * Plays the effects through a SoundPool and loops the ambience through a MediaPlayer. Effects only
 * play while the game is visible and not muted; the ambience also needs audio focus.
 */
class SoundBank(context: Context) : GameAudio, DefaultLifecycleObserver {
    private val app = context.applicationContext
    private val audioManager = app.getSystemService(AudioManager::class.java)
    private val settings = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private var mutedState by mutableStateOf(settings.getBoolean(KEY_MUTED, false))

    override var muted: Boolean
        get() = mutedState
        set(value) {
            mutedState = value
            settings.edit { putBoolean(KEY_MUTED, value) }
            update()
        }

    private val effectAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val ambienceAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(effectAttributes).build()
    private val effects: Map<Sfx, Int> = Sfx.entries.associateWith { pool.load(app, it.resource(), 1) }

    private var player: MediaPlayer? = null
    private var fade: ValueAnimator? = null
    private var level = 0f
    private var wanted = false
    private var visible = false
    private var hasFocus = false

    private val onFocusChange = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                update()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> fadeTo(AMBIENCE_LEVEL * 0.3f, 300)
            else -> {
                hasFocus = change != AudioManager.AUDIOFOCUS_LOSS
                pauseAmbience()
            }
        }
    }

    // Focus callbacks drive the fade animator, so they are delivered on the main thread.
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(ambienceAttributes)
        .setOnAudioFocusChangeListener(onFocusChange, Handler(Looper.getMainLooper()))
        .build()

    override fun play(sfx: Sfx, volume: Float) {
        start(sfx, volume)
    }

    override fun start(sfx: Sfx, volume: Float): Int {
        // nothing sounds from the background, such as a message arriving or a call still ringing
        if (muted || !visible) return 0
        val id = effects[sfx] ?: return 0
        val v = (volume * EFFECTS_LEVEL).coerceIn(0f, 1f)
        return pool.play(id, v, v, 1, 0, 1f)
    }

    override fun stop(id: Int) {
        if (id != 0) pool.stop(id)
    }

    override fun ambience(on: Boolean) {
        wanted = on
        update()
    }

    override fun onStart(owner: LifecycleOwner) {
        visible = true
        update()
    }

    override fun onStop(owner: LifecycleOwner) {
        visible = false
        update()
    }

    fun release() {
        fade?.cancel()
        player?.release()
        player = null
        pool.release()
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private fun update() {
        if (wanted && visible && !muted) startAmbience() else stopAmbience()
    }

    private fun startAmbience() {
        if (!hasFocus) {
            hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!hasFocus) return
        }
        val p = player ?: MediaPlayer.create(app, R.raw.amb_lake, ambienceAttributes, audioManager.generateAudioSessionId())
            ?.also {
                it.isLooping = true
                player = it
            } ?: return
        if (!p.isPlaying) {
            p.setVolume(level, level)
            p.start()
        }
        fadeTo(AMBIENCE_LEVEL, 2600)
    }

    private fun stopAmbience() {
        pauseAmbience()
        if (hasFocus) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            hasFocus = false
        }
    }

    private fun pauseAmbience() {
        fade?.cancel()
        player?.takeIf { it.isPlaying }?.pause()
        level = 0f
    }

    private fun fadeTo(target: Float, durationMs: Long) {
        val p = player ?: return
        fade?.cancel()
        fade = ValueAnimator.ofFloat(level, target).apply {
            duration = durationMs
            addUpdateListener {
                level = it.animatedValue as Float
                p.setVolume(level, level)
            }
            start()
        }
    }

    private fun Sfx.resource(): Int = when (this) {
        Sfx.Ignite -> R.raw.sfx_ignite
        Sfx.Tap -> R.raw.sfx_tap
        Sfx.Paper -> R.raw.sfx_paper
        Sfx.Pin -> R.raw.sfx_pin
        Sfx.Pickup -> R.raw.sfx_pickup
        Sfx.Putdown -> R.raw.sfx_putdown
        Sfx.Denied -> R.raw.sfx_denied
        Sfx.Scribble -> R.raw.sfx_scribble
        Sfx.MessageIn -> R.raw.sfx_msg_in
        Sfx.MessageOut -> R.raw.sfx_msg_out
        Sfx.Notify -> R.raw.sfx_notify
        Sfx.AppOpen -> R.raw.sfx_app_open
        Sfx.AppClose -> R.raw.sfx_app_close
        Sfx.Offline -> R.raw.sfx_offline
        Sfx.Key1 -> R.raw.sfx_key_1
        Sfx.Key2 -> R.raw.sfx_key_2
        Sfx.Key3 -> R.raw.sfx_key_3
        Sfx.Key4 -> R.raw.sfx_key_4
        Sfx.Key5 -> R.raw.sfx_key_5
        Sfx.Key6 -> R.raw.sfx_key_6
        Sfx.Key7 -> R.raw.sfx_key_7
        Sfx.Key8 -> R.raw.sfx_key_8
        Sfx.Key9 -> R.raw.sfx_key_9
        Sfx.KeyStar -> R.raw.sfx_key_star
        Sfx.Key0 -> R.raw.sfx_key_0
        Sfx.KeyHash -> R.raw.sfx_key_hash
        Sfx.Ringback -> R.raw.sfx_ringback
        Sfx.CallEnd -> R.raw.sfx_call_end
    }

    private companion object {
        const val AMBIENCE_LEVEL = 0.55f
        const val EFFECTS_LEVEL = 0.9f
        const val KEY_MUTED = "muted"
    }
}
