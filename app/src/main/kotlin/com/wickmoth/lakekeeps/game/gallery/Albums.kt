package com.wickmoth.lakekeeps.game.gallery

import androidx.compose.runtime.Immutable
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.littlebird.SamsPhone

/**
 * A photo on a phone. Until its art is made it shows as a placeholder, and [caption] says what it
 * will be. It was taken [daysAgo] days before its case's "today".
 */
@Immutable
data class Photo(val id: String, val caption: String, val daysAgo: Int)

/** Every phone's photos: the prototype's placeholders for Theo and Mira, and Sam's from [SamsPhone]. */
object Albums {
    private val miraPhotos = listOf(
        Photo("mira/bonfire", "Bonfire at the cove", 2),
        Photo("mira/sparklers", "Priya with sparklers", 2),
        Photo("mira/marshmallows", "Marshmallows over the fire", 2),
        Photo("mira/pier", "The pier lights at dusk", 3),
        Photo("mira/wheel", "The big wheel from below", 7),
        Photo("mira/candyfloss", "Candyfloss at the fair", 7),
        Photo("mira/heron", "A heron on the jetty", 14),
        Photo("mira/boathouse", "The boathouse sign", 14),
        Photo("mira/sketch", "A page from her sketchbook", 21),
    )

    private val miraHidden = listOf(
        Photo("mira/hidden-night", "The shore at night, taken from the water", 2),
        Photo("mira/hidden-map", "A map with one spot circled", 3),
    )

    private val theoPhotos = listOf(
        Photo("theo/poster", "Missing poster, first draft", 0),
        Photo("theo/flyer", "Search party flyer", 1),
        Photo("theo/screenshot", "Screenshot of Mira's last message", 1),
        Photo("theo/arcade", "Arcade night with Mira", 9),
        Photo("theo/sunrise", "The lake at sunrise", 20),
        Photo("theo/boat", "Mira laughing on the boat", 27),
    )

    /** [owner]'s photos, newest first. */
    fun photos(owner: Owner): List<Photo> = when (owner) {
        Owner.Theo -> theoPhotos
        Owner.Mira -> miraPhotos
        Owner.Sam -> SamsPhone.photos
    }.sortedBy { it.daysAgo }

    /** The photos [owner] hid from the main album, newest first. */
    fun hidden(owner: Owner): List<Photo> = when (owner) {
        Owner.Theo, Owner.Sam -> emptyList()
        Owner.Mira -> miraHidden
    }.sortedBy { it.daysAgo }
}
