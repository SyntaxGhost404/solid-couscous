package com.wickmoth.lakekeeps.game

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A case's calendar: [today] is the day it opens on, and dates count back from there. Only the
 * weekday, day and month are ever shown; the year just puts the weekdays right.
 */
@Immutable
class StoryCalendar(val today: LocalDate) {
    fun date(daysAgo: Int): LocalDate = today.minusDays(daysAgo.toLong())

    /** "2 November" */
    fun longDate(daysAgo: Int): String = date(daysAgo).format(LongDate)

    /** "2 Nov" */
    fun shortDate(daysAgo: Int): String = date(daysAgo).format(ShortDate)

    /** "Saturday, 4 November" */
    fun fullDate(daysAgo: Int): String = date(daysAgo).format(FullDate)

    private companion object {
        val LongDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.UK)
        val ShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
        val FullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK)
    }
}
