package com.wickmoth.lakekeeps.game.case

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

/**
 * How far the player has got through the case, as named flags set in the order things happened:
 * conversations reached, cards sorted, pins made, links that held, memos heard. Also how many
 * hints each objective has had. Saved with the game state.
 */
@Stable
class CaseProgress(flags: Collection<String> = emptyList(), hints: Map<String, Int> = emptyMap()) {
    private val raised = mutableStateListOf<String>().apply { addAll(flags.distinct()) }
    private val hintsShown = mutableStateMapOf<String, Int>().apply { putAll(hints) }

    /** Every flag set so far, oldest first. */
    val flags: List<String> get() = raised

    fun has(flag: String): Boolean = flag in raised

    fun set(flag: String) {
        if (flag !in raised) raised += flag
    }

    /** How many of [objective]'s hints the player has asked for (0 to 3). */
    fun hints(objective: String): Int = hintsShown[objective] ?: 0

    fun showHint(objective: String) {
        hintsShown[objective] = (hints(objective) + 1).coerceAtMost(MAX_HINTS)
    }

    /** Forgets everything, as when the game is started over. */
    fun clear() {
        raised.clear()
        hintsShown.clear()
    }

    fun encode(): String = (raised.map { "f:$it" } + hintsShown.map { (k, v) -> "h:$k=$v" }).joinToString("\n")

    companion object {
        const val MAX_HINTS = 3

        fun decode(saved: String?): CaseProgress {
            val rows = saved.orEmpty().lines()
            val flags = rows.filter { it.startsWith("f:") }.map { it.removePrefix("f:") }.filter { it.isNotEmpty() }
            val hints = rows.filter { it.startsWith("h:") }.mapNotNull { row ->
                val (key, n) = row.removePrefix("h:").split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
                key to (n.toIntOrNull() ?: return@mapNotNull null).coerceIn(0, MAX_HINTS)
            }.toMap()
            return CaseProgress(flags, hints)
        }
    }
}
