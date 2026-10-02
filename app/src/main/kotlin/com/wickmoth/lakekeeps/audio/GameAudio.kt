package com.wickmoth.lakekeeps.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** One-shot effects. Every sound is synthesised by tools/audio, so none comes from a library. */
enum class Sfx {
    Ignite, Tap, Paper, Pin, Pickup, Putdown, Denied, Scribble,
    MessageIn, MessageOut, Notify, AppOpen, AppClose, Offline,
}

interface GameAudio {
    fun play(sfx: Sfx, volume: Float = 1f)

    /** Fades the lake ambience in or out. */
    fun ambience(on: Boolean)

    /** Whether the player has switched the game's sound off (observable from composition). */
    var muted: Boolean
}

object SilentAudio : GameAudio {
    override fun play(sfx: Sfx, volume: Float) = Unit
    override fun ambience(on: Boolean) = Unit

    override var muted by mutableStateOf(false)
}

val LocalAudio = staticCompositionLocalOf<GameAudio> { SilentAudio }
