package com.wickmoth.lakekeeps.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.wickmoth.lakekeeps.R

/** Bundled OFL typefaces (licences in assets/licenses). */
object Fonts {
    /** In-game phone UI and the advisory line. */
    val Quicksand = FontFamily(
        Font(R.font.quicksand_medium, FontWeight.Medium),
        Font(R.font.quicksand_semibold, FontWeight.SemiBold),
        Font(R.font.quicksand_bold, FontWeight.Bold),
    )

    /** Handwritten notes on the case board. */
    val Caveat = FontFamily(
        Font(R.font.caveat_regular, FontWeight.Normal),
        Font(R.font.caveat_medium, FontWeight.Medium),
    )

    /** Felt-tip names on the polaroids. */
    val Marker = FontFamily(Font(R.font.covered_by_your_grace))

    /** The game title. */
    val Fell = FontFamily(
        Font(R.font.im_fell_english, FontWeight.Normal),
        Font(R.font.im_fell_english_italic, FontWeight.Normal, FontStyle.Italic),
    )

    /** The studio wordmark. */
    val Josefin = FontFamily(Font(R.font.josefin_sans_light, FontWeight.Light))
}
