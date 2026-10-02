package com.wickmoth.lakekeeps.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** One-shot effects. Every sound is synthesised by tools/audio, so none comes from a library. */
enum class Sfx {
    Ignite, Tap, Paper, Pin, Pickup, Putdown, Denied, Scribble,
    MessageIn, MessageOut, Notify, AppOpen, AppClose, Offline,
    Key1, Key2, Key3, Key4, Key5, Key6, Key7, Key8, Key9, KeyStar, Key0, KeyHash, Ringback, CallEnd,
    Toggle, Reveal, Buzz, Thread,
    ;

    companion object {
        /** The tone of a keypad key: a digit, star or hash. */
        fun key(key: Char): Sfx = when (key) {
            '*' -> KeyStar
            '#' -> KeyHash
            '0' -> Key0
            else -> entries[Key1.ordinal + (key - '1').coerceIn(0, 8)]
        }
    }
}

interface GameAudio {
    fun play(sfx: Sfx, volume: Float = 1f)

    /** Plays [sfx] and returns an id that [stop] can cut it short with (0 if nothing played). */
    fun start(sfx: Sfx, volume: Float = 1f): Int

    fun stop(id: Int)

    /** Fades the lake ambience in or out. */
    fun ambience(on: Boolean)

    /** Whether the player has switched the game's sound off (observable from composition). */
    var muted: Boolean

    /** Whether the lake ambience plays at all; a player setting (observable from composition). */
    var ambienceOn: Boolean
}

object SilentAudio : GameAudio {
    override fun play(sfx: Sfx, volume: Float) = Unit
    override fun start(sfx: Sfx, volume: Float) = 0
    override fun stop(id: Int) = Unit
    override fun ambience(on: Boolean) = Unit

    override var muted by mutableStateOf(false)

    override var ambienceOn by mutableStateOf(true)
}

val LocalAudio = staticCompositionLocalOf<GameAudio> { SilentAudio }
