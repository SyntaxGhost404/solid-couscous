package com.wickmoth.lakekeeps.audio

import androidx.compose.runtime.staticCompositionLocalOf

/** One-shot effects. Every sound is synthesised by tools/audio, so none comes from a library. */
enum class Sfx { Ignite, Tap, Paper, Pin, Pickup, Putdown, Denied, Scribble }

interface GameAudio {
    fun play(sfx: Sfx, volume: Float = 1f)

    /** Fades the lake ambience in or out. */
    fun ambience(on: Boolean)
}

object SilentAudio : GameAudio {
    override fun play(sfx: Sfx, volume: Float) = Unit
    override fun ambience(on: Boolean) = Unit
}

val LocalAudio = staticCompositionLocalOf<GameAudio> { SilentAudio }
