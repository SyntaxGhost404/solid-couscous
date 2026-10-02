package com.wickmoth.lakekeeps.game

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The game's calendar. "Now" is Saturday 4 November, 4:12 PM. */
object GameTime {
    /** Today in the game. Only day and month are ever shown; the year just puts the weekdays right. */
    val today: LocalDate = LocalDate.of(2023, 11, 4)

    fun date(daysAgo: Int): LocalDate = today.minusDays(daysAgo.toLong())

    /** "2 November" */
    fun longDate(daysAgo: Int): String = date(daysAgo).format(LongDate)

    /** "2 Nov" */
    fun shortDate(daysAgo: Int): String = date(daysAgo).format(ShortDate)

    private val LongDate = DateTimeFormatter.ofPattern("d MMMM", Locale.UK)
    private val ShortDate = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
}
