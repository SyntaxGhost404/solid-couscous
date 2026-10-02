package com.wickmoth.lakekeeps.screens.phone

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.game.StoryCalendar
import com.wickmoth.lakekeeps.game.messages.Day
import com.wickmoth.lakekeeps.game.messages.Line
import java.time.DayOfWeek

/** A conversation's day chip: Today, Yesterday, a weekday within the week, or the date. */
@Composable
internal fun dayChip(day: Day, calendar: StoryCalendar): String {
    val ago = -day.index
    return when (ago) {
        0 -> stringResource(R.string.day_today)
        1 -> stringResource(R.string.day_yesterday)
        in 2..6 -> stringResource(weekday(calendar.date(ago).dayOfWeek, short = false))
        else -> calendar.longDate(ago)
    }
}

/** A day in a list (inbox, notifications): Yesterday, Thu, or 26 Jun. Today's lines show their time instead. */
@Composable
internal fun dayShort(day: Day, calendar: StoryCalendar): String {
    val ago = -day.index
    return when (ago) {
        0 -> stringResource(R.string.day_today)
        1 -> stringResource(R.string.day_yesterday)
        in 2..6 -> stringResource(weekday(calendar.date(ago).dayOfWeek, short = true))
        else -> calendar.shortDate(ago)
    }
}

@StringRes
private fun weekday(day: DayOfWeek, short: Boolean): Int = when (day) {
    DayOfWeek.MONDAY -> if (short) R.string.day_mon else R.string.day_monday
    DayOfWeek.TUESDAY -> if (short) R.string.day_tue else R.string.day_tuesday
    DayOfWeek.WEDNESDAY -> if (short) R.string.day_wed else R.string.day_wednesday
    DayOfWeek.THURSDAY -> if (short) R.string.day_thu else R.string.day_thursday
    DayOfWeek.FRIDAY -> if (short) R.string.day_fri else R.string.day_friday
    DayOfWeek.SATURDAY -> if (short) R.string.day_sat else R.string.day_saturday
    DayOfWeek.SUNDAY -> if (short) R.string.day_sun else R.string.day_sunday
}

/** What a list or banner shows for a line: its text, or that a picture came with it. */
@Composable
internal fun preview(line: Line): String = line.text.ifEmpty { stringResource(R.string.sent_picture) }
