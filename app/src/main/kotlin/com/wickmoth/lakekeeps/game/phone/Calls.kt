package com.wickmoth.lakekeeps.game.phone

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.wickmoth.lakekeeps.game.Owner

/** Which way a call went. */
enum class CallKind { Outgoing, Incoming, Missed }

/** Someone in a phone's address book. [label] is the kind of number, as a call log shows it. */
@Immutable
data class PhoneContact(val name: String, val number: String, val label: String)

/**
 * One entry in a phone's call log: the [number] (digits only), and when the call was, as calendar
 * days before the game's "now" ([daysAgo], 0 = today) and the time of day in minutes.
 */
@Immutable
data class Call(val number: String, val kind: CallKind, val daysAgo: Int, val minutes: Int)

/** How long ago something was, in the largest whole unit a call log would use. */
sealed interface Ago {
    data object Now : Ago
    data class Minutes(val n: Int) : Ago
    data class Hours(val n: Int) : Ago
    data class Days(val n: Int) : Ago
}

/** How long before [now] (minutes after midnight, today) this call was. */
fun Call.ago(now: Int): Ago {
    if (daysAgo > 0) return Ago.Days(daysAgo)
    val minutes = (now - this.minutes).coerceAtLeast(0)
    return when {
        minutes < 1 -> Ago.Now
        minutes < 60 -> Ago.Minutes(minutes)
        else -> Ago.Hours(minutes / 60)
    }
}

/** The keys a number can be dialled with: digits, star and hash. */
fun dialable(number: String): String = number.filter { it.isDigit() || it == '*' || it == '#' }

/** Groups a dialled number for display as it is typed: 555-0142, or (555) 010-4477 for ten digits. */
fun formatNumber(digits: String): String = when {
    digits.any { !it.isDigit() } -> digits
    digits.length in 4..7 -> digits.substring(0, 3) + "-" + digits.substring(3)
    digits.length == 10 -> "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
    else -> digits
}

/**
 * The calls the player has made on each phone, saved with the game state. A phone's log is these,
 * newest first, followed by its history from [PhoneBook].
 */
@Stable
class CallLog(initial: List<Placed> = emptyList()) {
    /** A call the player placed: on whose phone, to which number, at what in-game time of day. */
    @Immutable
    data class Placed(val owner: Owner, val number: String, val minutes: Int)

    private val placed = mutableStateListOf<Placed>().apply { addAll(initial) }

    fun place(owner: Owner, number: String, minutes: Int) {
        placed += Placed(owner, dialable(number), minutes)
    }

    /** Every call on [owner]'s phone, newest first. */
    fun calls(owner: Owner): List<Call> =
        placed.filter { it.owner == owner }.asReversed().map { Call(it.number, CallKind.Outgoing, 0, it.minutes) } +
            PhoneBook.history(owner)

    fun encode(): String = placed.joinToString("\n") { "${it.owner.name};${it.number};${it.minutes}" }

    companion object {
        fun decode(saved: String?): CallLog {
            val rows = saved.orEmpty().lineSequence().mapNotNull { row ->
                val f = row.split(';')
                if (f.size != 3) return@mapNotNull null
                val owner = Owner.entries.firstOrNull { it.name == f[0] } ?: return@mapNotNull null
                val number = f[1].takeIf { it.isNotEmpty() && it == dialable(it) } ?: return@mapNotNull null
                Placed(owner, number, f[2].toIntOrNull() ?: return@mapNotNull null)
            }
            return CallLog(rows.toList())
        }
    }
}
