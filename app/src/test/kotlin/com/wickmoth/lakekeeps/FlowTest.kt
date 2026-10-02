package com.wickmoth.lakekeeps

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Launches the real activity (splash theme, immersive window, sound bank) and plays the intro. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class FlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun introPlaysThroughToTheBoard() {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("wickmoth").assertExists()
        compose.mainClock.advanceTimeBy(4_200)
        compose.onNodeWithText("better experience").assertExists()
        compose.mainClock.advanceTimeBy(4_500)
        compose.onNodeWithText("Lake Keeps").assertExists()
        compose.mainClock.advanceTimeBy(8_200)
        compose.onNodeWithContentDescription("Photo of Mira").assertExists()
    }

    @Test fun tapsSkipEachIntroScreen() {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(400)
        compose.onRoot().performClick()
        compose.mainClock.advanceTimeBy(1_300)
        compose.onNodeWithText("better experience").assertExists()
        compose.onRoot().performClick()
        compose.mainClock.advanceTimeBy(900)
        compose.mainClock.advanceTimeBy(800)
        compose.onRoot().performClick()
        compose.mainClock.advanceTimeBy(1_300)
        compose.onNodeWithContentDescription("Photo of Mira").assertExists()
    }

    @Test fun phoneOpensAndClosesFromTheBoard() {
        compose.mainClock.autoAdvance = false
        repeat(3) {
            compose.mainClock.advanceTimeBy(500)
            compose.onRoot().performClick()
            compose.mainClock.advanceTimeBy(1_400)
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithContentDescription("Theo's phone, on the desk").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("Theo's phone").assertExists()
        compose.onNodeWithContentDescription("Loose Ends").assertExists()
        compose.onNodeWithContentDescription("Back to the case board").performClick()
        compose.mainClock.advanceTimeBy(1_200)
        compose.onNodeWithContentDescription("Loose Ends").assertDoesNotExist()
    }
}
