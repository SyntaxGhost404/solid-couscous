package com.wickmoth.lakekeeps.game.messages

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.case.CaseId
import com.wickmoth.lakekeeps.game.case.CaseProgress

/**
 * How far a thread has got: the replies picked, how many live lines have arrived, how many lines
 * the player has read ([read] null means "as the phone's owner left it"), how many lines there
 * were when its notification was last swiped away, and whether the contact has signed off.
 */
@Immutable
data class Progress(
    val chosen: List<Int> = emptyList(),
    val delivered: Int = 0,
    val read: Int? = null,
    val dismissed: Int = 0,
    val signedOff: Boolean = false,
)

/** An unread conversation as the notification shade shows it. */
@Immutable
data class Notice(val thread: Thread, val line: Line, val unread: Int)

/** Orders lines across days: today's latest first when sorted descending. */
val Line.sortKey: Int get() = day.index * 1440 + minutes

/**
 * The player's progress through every thread on every phone. Saved with the game state. Scripts
 * wait at their gates on, and report their marks to, the case's [flags].
 */
@Stable
class Messages(initial: Map<String, Progress> = emptyMap()) {
    private val progress = mutableStateMapOf<String, Progress>().apply { putAll(initial) }

    /** The case progress the scripts read and write; the game state shares its own. */
    var flags: CaseProgress = CaseProgress()

    operator fun get(thread: Thread): Progress = progress[thread.id] ?: Progress()

    private fun edit(thread: Thread, change: (Progress) -> Progress) {
        progress[thread.id] = change(this[thread])
    }

    fun replay(thread: Thread): Replay? = thread.live?.let { replay(it, this[thread].chosen, flags::has) }

    /** Every line visible in the conversation, oldest first. Live lines are stamped as they arrive. */
    fun lines(thread: Thread): List<Line> {
        val live = replay(thread) ?: return thread.history
        val delivered = this[thread].delivered.coerceAtMost(live.lines.size)
        return thread.history + live.lines.take(delivered).map { Line(it.mine, it.text, it.day, it.minutes, it.attachment) }
    }

    /** A live thread only appears once its first message has arrived. */
    fun hasStarted(thread: Thread): Boolean = thread.history.isNotEmpty() || this[thread].delivered > 0

    /** The contact is online from their first live line until they sign off after their last. */
    fun isLive(thread: Thread): Boolean =
        thread.live != null && this[thread].delivered > 0 && !this[thread].signedOff

    /** Every line so far has arrived and no question is open: the conversation is over, for now or for good. */
    fun isOver(thread: Thread): Boolean {
        val live = replay(thread) ?: return true
        return live.finished && this[thread].delivered >= live.lines.size
    }

    /** The next live line still to arrive. */
    fun nextLine(thread: Thread): LiveLine? = replay(thread)?.lines?.getOrNull(this[thread].delivered)

    /** The open question, once everything before it has arrived. */
    fun question(thread: Thread): Beat.Ask? {
        val live = replay(thread) ?: return null
        return if (this[thread].delivered >= live.lines.size) live.pending else null
    }

    /**
     * The next line arrives. A contact who had signed off is back for it, and the case hears of any
     * marks the conversation has now passed.
     */
    fun deliver(thread: Thread) {
        edit(thread) { it.copy(delivered = it.delivered + 1, signedOff = false) }
        val delivered = this[thread].delivered
        replay(thread)?.marks?.forEach { if (it.after <= delivered) flags.set(it.flag) }
    }

    /** The contact goes offline; only once the conversation is over. */
    fun signOff(thread: Thread) {
        if (isOver(thread)) edit(thread) { it.copy(signedOff = true) }
    }

    fun choose(thread: Thread, reply: Int) = edit(thread) { it.copy(chosen = it.chosen + reply) }

    fun markRead(thread: Thread) {
        val count = lines(thread).size
        if (this[thread].read != count) edit(thread) { it.copy(read = count) }
    }

    fun dismiss(thread: Thread) {
        val count = lines(thread).size
        edit(thread) { it.copy(dismissed = count) }
    }

    fun unread(thread: Thread): Int {
        val all = lines(thread)
        val seen = (this[thread].read ?: thread.readHistory).coerceIn(0, all.size)
        return all.drop(seen).count { !it.mine }
    }

    fun unread(owner: Owner): Int = Threads.of(owner).sumOf { unread(it) }

    /** Unread conversations not swiped away, newest first. */
    fun notices(owner: Owner): List<Notice> = Threads.of(owner)
        .filter { unread(it) > 0 && lines(it).size > this[it].dismissed }
        .map { thread -> Notice(thread, lines(thread).last { !it.mine }, unread(thread)) }
        .sortedByDescending { it.line.sortKey }

    /** Conversations in the inbox, newest first. */
    fun inbox(owner: Owner): List<Thread> =
        Threads.of(owner).filter(::hasStarted).sortedByDescending { lines(it).last().sortKey }

    /** The time of day in [case] (minutes after midnight): when its latest live line arrived today. */
    fun clock(case: CaseId): Int {
        val latest = case.owners.flatMap(Threads::of).mapNotNull { thread ->
            val live = replay(thread) ?: return@mapNotNull null
            live.lines.take(this[thread].delivered).filter { it.day == Day.Today }.maxOfOrNull { it.minutes }
        }.maxOrNull()
        return maxOf(case.opensAt, latest ?: case.opensAt)
    }

    /** Forgets all progress, as when the game is started over. */
    fun clear() = progress.clear()

    fun encode(): String = progress.entries.joinToString("\n") { (id, p) ->
        listOf(id, p.chosen.joinToString(","), p.delivered, p.read ?: "", p.dismissed, if (p.signedOff) 1 else 0).joinToString(";")
    }

    companion object {
        fun decode(saved: String?): Messages {
            val entries = saved.orEmpty().lineSequence().mapNotNull { row ->
                val f = row.split(';')
                if (f.size != 6) return@mapNotNull null
                val chosen = f[1].split(',').filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: return@mapNotNull null }
                f[0] to Progress(
                    chosen = chosen,
                    delivered = f[2].toIntOrNull() ?: return@mapNotNull null,
                    read = f[3].toIntOrNull(),
                    dismissed = f[4].toIntOrNull() ?: 0,
                    signedOff = f[5] == "1",
                )
            }.toMap()
            return Messages(entries)
        }
    }
}
