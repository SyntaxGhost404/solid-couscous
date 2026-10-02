package com.wickmoth.lakekeeps.game.messages

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A day of the story, counted from the day the case opens on ([index] 0, "today"): -1 is the day
 * before, and so on. Chips and the inbox name it from the case's calendar.
 */
@JvmInline
value class Day(val index: Int) {
    companion object {
        val Today = Day(0)
        val Yesterday = Day(-1)
    }
}

/**
 * One message. [minutes] is the time of day (minutes after midnight) on [day]. An [attachment]
 * is a picture or file sent with it, named by its evidence id.
 */
@Immutable
data class Line(val mine: Boolean, val text: String, val day: Day, val minutes: Int, val attachment: String? = null)

/** How a contact is pictured: an existing board photo, a lettered placeholder, or no photo at all. */
sealed interface Face {
    /** A board photo; [closeUp] crops onto the face of a drawn portrait instead of showing it whole. */
    data class Photo(@DrawableRes val res: Int, val closeUp: Boolean = true) : Face
    data class Initial(val letter: String, val tint: Color) : Face
    data object None : Face
}

@Immutable
data class Contact(val name: String, val face: Face, val subtitle: String, val detail: String)

/**
 * A scripted exchange: the contact says something (perhaps sending a picture), the player picks one
 * of a few replies, or the phone's owner writes a line of their own. A [Gate] holds the script
 * until the case reaches a point; a [Mark] tells the case that the conversation got this far.
 */
sealed interface Beat {
    data class Says(val text: String, val attachment: String? = null) : Beat
    data class Ask(val replies: List<Reply>) : Beat

    /** A line the phone's owner sends without a choice to make. */
    data class Writes(val text: String) : Beat

    /** Waits until the case sets [flag]; the lines after it are stamped from [day] at [minutes]. */
    data class Gate(val flag: String, val day: Day, val minutes: Int) : Beat

    /** Sets the case flag [flag] once every line before it has arrived. */
    data class Mark(val flag: String) : Beat
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

/** A line of a live script, stamped with when it arrives: from the player ([mine]) or the contact. */
@Immutable
data class LiveLine(val mine: Boolean, val text: String, val day: Day, val minutes: Int, val attachment: String? = null)

/** A [Beat.Mark]'s flag, to be set once [after] lines have arrived. */
@Immutable
data class MarkAt(val flag: String, val after: Int)

/**
 * The live part of a thread as far as the player's choices and the case go: its lines, the question
 * it waits on ([pending]) or the [gate] it waits at, and the marks passed on the way.
 */
class Replay(val lines: List<LiveLine>, val pending: Beat.Ask?, val gate: Beat.Gate?, val marks: List<MarkAt>) {
    /** No question is open: the script has run out, for now or for good. */
    val finished: Boolean get() = pending == null
}

/**
 * Walks [beats] following [chosen] (one reply index per question) until the next open question,
 * or a gate whose flag isn't [open] yet. Lines are stamped [Threads.SECONDS_PER_LIVE_LINE] apart from
 * [startMinutes] on [startDay], or from the time of the last gate passed.
 */
fun replay(
    beats: List<Beat>,
    chosen: List<Int>,
    open: (String) -> Boolean = { false },
    startDay: Day = Day.Today,
    startMinutes: Int = Threads.NOW_MINUTES,
): Replay {
    val lines = mutableListOf<LiveLine>()
    val marks = mutableListOf<MarkAt>()
    var next = 0
    var day = startDay
    var base = startMinutes
    var count = 0
    fun add(mine: Boolean, text: String, attachment: String? = null) {
        count++
        lines += LiveLine(mine, text, day, base + count * Threads.SECONDS_PER_LIVE_LINE / 60, attachment)
    }
    // Returns where the walk stopped: an open question, a closed gate, or null at the end.
    fun walk(list: List<Beat>): Beat? {
        for (beat in list) {
            when (beat) {
                is Beat.Says -> add(false, beat.text, beat.attachment)
                is Beat.Writes -> add(true, beat.text)
                is Beat.Mark -> marks += MarkAt(beat.flag, lines.size)
                is Beat.Gate -> {
                    if (!open(beat.flag)) return beat
                    day = beat.day
                    base = beat.minutes
                    count = 0
                }
                is Beat.Ask -> {
                    val pick = chosen.getOrNull(next) ?: return beat
                    next++
                    val reply = beat.replies[pick.coerceIn(beat.replies.indices)]
                    add(true, reply.text)
                    walk(reply.then)?.let { return it }
                }
            }
        }
        return null
    }
    val stop = walk(beats)
    return Replay(lines, stop as? Beat.Ask, stop as? Beat.Gate, marks)
}

@DslMarker
annotation class ScriptDsl

@ScriptDsl
class ScriptBuilder {
    internal val beats = mutableListOf<Beat>()

    fun says(text: String) {
        beats += Beat.Says(text)
    }

    /** The contact sends the evidence [attachment], with [text] under it (or nothing). */
    fun sends(attachment: String, text: String = "") {
        beats += Beat.Says(text, attachment)
    }

    fun ask(vararg replies: Reply) {
        beats += Beat.Ask(replies.toList())
    }

    fun writes(text: String) {
        beats += Beat.Writes(text)
    }

    fun gate(flag: String, day: Day, time: String) {
        beats += Beat.Gate(flag, day, clockMinutes(time))
    }

    fun mark(flag: String) {
        beats += Beat.Mark(flag)
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
