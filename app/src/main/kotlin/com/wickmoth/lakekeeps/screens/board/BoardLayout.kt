package com.wickmoth.lakekeeps.screens.board

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.wickmoth.lakekeeps.R
import kotlin.math.cos
import kotlin.math.sin

/** Everything on the board is placed in design dp on the 360 x 800 frame. */
internal const val DESK_TOP = 648f

/** Distance from a card's top edge down to its pin. */
internal const val PIN_INSET = 4f

internal val CardSize = Size(78f, 92f)
internal val BigCardSize = Size(94f, 112f)

enum class Person(@StringRes val label: Int, @DrawableRes val photo: Int) {
    Mira(R.string.name_mira, R.drawable.portrait_mira),
    Jonah(R.string.name_jonah, R.drawable.portrait_jonah),
    Priya(R.string.name_priya, R.drawable.portrait_priya),
    Owen(R.string.name_owen, R.drawable.portrait_owen),
    Celia(R.string.name_celia, R.drawable.portrait_celia),
    Heron(R.string.name_heron, R.drawable.portrait_heron),
    Marcus(R.string.name_marcus, R.drawable.portrait_marcus),
    Ines(R.string.name_ines, R.drawable.portrait_ines),
    Felix(R.string.name_felix, R.drawable.portrait_felix),
    Rosa(R.string.name_rosa, R.drawable.portrait_rosa),
    Unknown(R.string.name_unknown, R.drawable.portrait_unknown),
}

/**
 * A polaroid hung from a pin at [pin]. The card rotates about its pin, so threads tied to the
 * pin stay put while the card tilts or swings. A [doublePinned] card is also pinned at the bottom.
 */
@Immutable
data class Card(val person: Person, val pin: Offset, val tilt: Float, val doublePinned: Boolean = false) {
    val size: Size get() = if (doublePinned) BigCardSize else CardSize
    val topLeft: Offset get() = Offset(pin.x - size.width / 2f, pin.y - PIN_INSET)

    /** The lower pin of a double-pinned card, following the card's tilt. */
    val lowerPin: Offset
        get() {
            val d = size.height - PIN_INSET * 2f
            val a = Math.toRadians(tilt.toDouble())
            return Offset(pin.x - (d * sin(a)).toFloat(), pin.y + (d * cos(a)).toFloat())
        }

    val center: Offset
        get() {
            val d = size.height / 2f - PIN_INSET
            val a = Math.toRadians(tilt.toDouble())
            return Offset(pin.x - (d * sin(a)).toFloat(), pin.y + (d * cos(a)).toFloat())
        }
}

/** A red thread between two people's pins; [fromLower] ties it to the lower pin of the first card. */
@Immutable
data class Thread(val from: Person, val to: Person, val fromLower: Boolean = false)

/** A handwritten note centred on [at]. [about] is who it refers to, so it can light up with them. */
@Immutable
data class Note(
    @StringRes val text: Int,
    val at: Offset,
    val tilt: Float,
    val red: Boolean,
    val about: Person?,
    val size: Float = 16f,
)

/** A chalked dashed line. */
@Immutable
data class Dash(val from: Offset, val to: Offset)

internal object Board {
    val cards = listOf(
        Card(Person.Mira, Offset(180f, 248f), tilt = -2.5f, doublePinned = true),
        Card(Person.Jonah, Offset(60f, 62f), tilt = -5f),
        Card(Person.Priya, Offset(164f, 44f), tilt = 3f),
        Card(Person.Owen, Offset(272f, 60f), tilt = -3f),
        Card(Person.Celia, Offset(312f, 196f), tilt = 5f),
        Card(Person.Heron, Offset(50f, 228f), tilt = 2f),
        Card(Person.Marcus, Offset(312f, 330f), tilt = -4f),
        Card(Person.Ines, Offset(52f, 362f), tilt = 4f),
        Card(Person.Felix, Offset(262f, 464f), tilt = 3f),
        Card(Person.Rosa, Offset(160f, 516f), tilt = -2f),
        Card(Person.Unknown, Offset(54f, 506f), tilt = -3f),
    )

    fun card(person: Person) = cards.first { it.person == person }

    val threads = listOf(
        Thread(Person.Mira, Person.Priya),
        Thread(Person.Mira, Person.Jonah),
        Thread(Person.Mira, Person.Owen),
        Thread(Person.Mira, Person.Heron),
        Thread(Person.Mira, Person.Celia),
        Thread(Person.Mira, Person.Marcus, fromLower = true),
        Thread(Person.Mira, Person.Ines, fromLower = true),
        Thread(Person.Mira, Person.Rosa, fromLower = true),
        Thread(Person.Mira, Person.Felix, fromLower = true),
        Thread(Person.Mira, Person.Unknown, fromLower = true),
    )

    val notes = listOf(
        Note(R.string.note_jonah, Offset(42f, 40f), tilt = -6f, red = false, about = Person.Jonah),
        Note(R.string.note_priya, Offset(234f, 29f), tilt = 2f, red = false, about = Person.Priya),
        Note(R.string.note_priya_doubt, Offset(166f, 152f), tilt = -2f, red = true, about = Person.Priya),
        Note(R.string.note_owen, Offset(272f, 167f), tilt = -3f, red = false, about = Person.Owen),
        Note(R.string.note_handle_doubt, Offset(150f, 176f), tilt = 4f, red = false, about = Person.Heron, size = 15f),
        Note(R.string.note_celia, Offset(305f, 301f), tilt = 3f, red = false, about = Person.Celia),
        Note(R.string.note_heron, Offset(66f, 335f), tilt = -2f, red = true, about = Person.Heron),
        Note(R.string.note_mark, Offset(212f, 404f), tilt = -3f, red = false, about = null),
        Note(R.string.note_marcus, Offset(302f, 437f), tilt = -4f, red = true, about = Person.Marcus),
        Note(R.string.note_ines, Offset(54f, 469f), tilt = 2f, red = false, about = Person.Ines),
        Note(R.string.note_felix, Offset(268f, 571f), tilt = -2f, red = false, about = Person.Felix),
        Note(R.string.note_unknown, Offset(60f, 616f), tilt = -3f, red = true, about = Person.Unknown),
        Note(R.string.note_rosa, Offset(186f, 621f), tilt = 2f, red = false, about = Person.Rosa),
    )

    /** The username tag pinned above the faceless account, and the clues tied in with chalk. */
    val handleTag = Offset(88f, 186f)
    const val HANDLE_TILT = -4f
    val handleSize = Size(108f, 26f)
    val mark = Offset(124f, 412f)
    const val MARK_SIZE = 46f
    val crumpledNote = Offset(116f, 474f)
    const val CRUMPLED_TILT = -10f

    val dashes = listOf(
        Dash(Offset(70f, 200f), Offset(53f, 225f)),
        Dash(Offset(134f, 436f), Offset(156f, 510f)),
        Dash(Offset(99f, 487f), Offset(63f, 507f)),
    )

    /** Order in which cards are pinned up during the intro: outward from the missing girl. */
    val pinOrder: List<Card> = cards.sortedBy { (it.pin - cards.first().pin).getDistance() }
}
