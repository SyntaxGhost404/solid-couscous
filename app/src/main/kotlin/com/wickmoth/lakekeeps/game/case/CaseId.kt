package com.wickmoth.lakekeeps.game.case

import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.StoryCalendar
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.game.messages.clockMinutes
import java.time.LocalDate

/**
 * A case the game can be played as: the phones it puts on the desk, its calendar, and the time
 * of day it opens at ([opensAt], minutes after midnight).
 */
enum class CaseId(val owners: List<Owner>, val calendar: StoryCalendar, val opensAt: Int) {
    /** Case 1, Sam Novak's first case: one phone, his own, on a Monday at the end of the monsoon. */
    LittleBird(listOf(Owner.Sam), StoryCalendar(LocalDate.of(2025, 9, 8)), clockMinutes("5:44 PM")),

    /**
     * The prototype's lake story, with Theo's phone and the secondary phone, Mira's. Nothing in the
     * game opens it any more; it is kept for that second phone and its tests.
     */
    Prototype(listOf(Owner.Mira, Owner.Theo), StoryCalendar(LocalDate.of(2023, 11, 4)), Threads.NOW_MINUTES),
}

/** The case a phone belongs to. */
val Owner.case: CaseId get() = CaseId.entries.first { this in it.owners }
