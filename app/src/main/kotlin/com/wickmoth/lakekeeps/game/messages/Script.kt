package com.wickmoth.lakekeeps.game.messages

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.wickmoth.lakekeeps.R

/** Days relative to the game's "now" (a Saturday), as shown on date chips and in the inbox. */
enum class Day(@StringRes val chip: Int, @StringRes val short: Int) {
    Today(R.string.day_today, R.string.day_today),
    Yesterday(R.string.day_yesterday, R.string.day_yesterday),
    Thursday(R.string.day_thursday, R.string.day_thu),
    Wednesday(R.string.day_wednesday, R.string.day_wed),
}

/** One message. [minutes] is the time of day (minutes after midnight) on [day]. */
@Immutable
data class Line(val mine: Boolean, val text: String, val day: Day, val minutes: Int)

/** How a contact is pictured: an existing board photo, a lettered placeholder, or no photo at all. */
sealed interface Face {
    /** A board photo; [closeUp] crops onto the face of a drawn portrait instead of showing it whole. */
    data class Photo(@DrawableRes val res: Int, val closeUp: Boolean = true) : Face
    data class Initial(val letter: String, val tint: Color) : Face
    data object None : Face
}

@Immutable
data class Contact(val name: String, val face: Face, val subtitle: String, val detail: String)

/** A scripted exchange: the contact says something, or the player picks one of a few replies. */
sealed interface Beat {
    data class Says(val text: String) : Beat
    data class Ask(val replies: List<Reply>) : Beat
}

/** A reply the player can pick, and what follows from it before the script rejoins. */
@Immutable
data class Reply(val text: String, val then: List<Beat>)

/**
 * A conversation on one phone. [history] was there before the game began; the phone's owner had
 * read the first [readHistory] lines. A [live] script plays out in the chat, driven by the player.
 */
@Immutable
data class Thread(
    val id: String,
    val contact: Contact,
    val history: List<Line> = emptyList(),
    val readHistory: Int = history.size,
    val live: List<Beat>? = null,
)

/** The live part of a thread as far as the player's choices go. */
class Replay(val lines: List<Pair<Boolean, String>>, val pending: Beat.Ask?) {
    val finished: Boolean get() = pending == null
}

/** Walks [beats] following [chosen] (one reply index per question) until the next open question. */
fun replay(beats: List<Beat>, chosen: List<Int>): Replay {
    val lines = mutableListOf<Pair<Boolean, String>>()
    var next = 0
    fun walk(list: List<Beat>): Beat.Ask? {
        for (beat in list) {
            when (beat) {
                is Beat.Says -> lines += false to beat.text
                is Beat.Ask -> {
                    val pick = chosen.getOrNull(next) ?: return beat
                    next++
                    val reply = beat.replies[pick.coerceIn(beat.replies.indices)]
                    lines += true to reply.text
                    walk(reply.then)?.let { return it }
                }
            }
        }
        return null
    }
    return Replay(lines, walk(beats))
}

@DslMarker
annotation class ScriptDsl

@ScriptDsl
class ScriptBuilder {
    internal val beats = mutableListOf<Beat>()

    fun says(text: String) {
        beats += Beat.Says(text)
    }

    fun ask(vararg replies: Reply) {
        beats += Beat.Ask(replies.toList())
    }

    fun reply(text: String, then: ScriptBuilder.() -> Unit = {}) = Reply(text, script(then))
}

fun script(block: ScriptBuilder.() -> Unit): List<Beat> = ScriptBuilder().apply(block).beats.toList()

/** History lines from the contact ([them]) or the phone's owner ([me]). */
fun them(text: String, day: Day, time: String) = Line(false, text, day, clockMinutes(time))
fun me(text: String, day: Day, time: String) = Line(true, text, day, clockMinutes(time))

/** Parses "9:05 PM" style times. */
fun clockMinutes(time: String): Int {
    val (clock, half) = time.trim().split(' ')
    val (h, m) = clock.split(':').map(String::toInt)
    val hour = (h % 12) + if (half.equals("PM", ignoreCase = true)) 12 else 0
    return hour * 60 + m
}

fun formatClock(minutes: Int, withHalf: Boolean = true): String {
    val m = ((minutes % 1440) + 1440) % 1440
    val h = (m / 60) % 12
    val clock = "${if (h == 0) 12 else h}:${(m % 60).toString().padStart(2, '0')}"
    return if (withHalf) "$clock ${if (m < 720) "AM" else "PM"}" else clock
}
