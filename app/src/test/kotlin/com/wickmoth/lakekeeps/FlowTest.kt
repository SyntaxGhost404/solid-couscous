package com.wickmoth.lakekeeps

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
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

    /** Taps through the three intro screens to the settled case board. */
    private fun skipToBoard() {
        compose.mainClock.autoAdvance = false
        repeat(3) {
            compose.mainClock.advanceTimeBy(500)
            compose.onRoot().performClick()
            compose.mainClock.advanceTimeBy(1_400)
        }
        compose.mainClock.advanceTimeBy(1_000)
    }

    /**
     * Plays frames until something with [label] (or [text]) is in the UI, then [settleMs] more so
     * whatever brought it in has finished moving.
     */
    private fun until(label: String? = null, text: String? = null, settleMs: Long = 800, limitMs: Long = 15_000) {
        var waited = 0L
        fun found() = (if (text != null) compose.onAllNodesWithText(text) else compose.onAllNodesWithContentDescription(label!!))
            .fetchSemanticsNodes().isNotEmpty()
        while (!found()) {
            check(waited < limitMs) { "${label ?: text} never appeared" }
            compose.mainClock.advanceTimeBy(16)
            waited += 16
        }
        compose.mainClock.advanceTimeBy(settleMs)
    }

    @Test fun phoneOpensAndClosesFromTheBoard() {
        skipToBoard()
        compose.onNodeWithContentDescription("Theo's phone, on the desk").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("Theo's phone").assertExists()
        compose.onNodeWithContentDescription("Loose Ends").assertExists()
        compose.onNodeWithContentDescription("Back to the case board").performClick()
        compose.mainClock.advanceTimeBy(1_200)
        compose.onNodeWithContentDescription("Loose Ends").assertDoesNotExist()
    }

    @Test fun conversationPlaysThroughAndIsSavedWithTheGame() {
        skipToBoard()
        compose.onNodeWithContentDescription("Theo's phone, on the desk").performClick()
        until(label = "Private number: Theo. 1 unread message")
        compose.onNodeWithContentDescription("Private number: Theo. 1 unread message").performClick()
        until(label = "Reply: Do I know you?")
        compose.onNodeWithContentDescription("Reply: Do I know you?").performClick()
        until(label = "Reply: How do you know that?")
        compose.onNodeWithContentDescription("Reply: How do you know that?").performClick()
        until(text = "Private number is offline")

        // Recreated (as after the process is reclaimed): the phone comes back on its home screen
        // with the conversation kept, and the private number does not start over.
        compose.activityRule.scenario.recreate()
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithContentDescription("Private number: Theo. 1 unread message").assertDoesNotExist()
        compose.onNodeWithContentDescription("Messages, 1 unread message").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("And keep the police out of this.").assertExists()
        compose.onNodeWithContentDescription("Private number").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("You've walked past me a hundred times.").assertExists()
        compose.onNodeWithText("Small town. People watch the pier.").assertExists()
        compose.onNodeWithText("Private number is offline").assertExists()
    }

    @Test fun callIsLoggedAndSavedWithTheGame() {
        skipToBoard()
        compose.onNodeWithContentDescription("Mira's phone, on the desk").performClick()
        compose.mainClock.advanceTimeBy(1_600)
        compose.onNodeWithContentDescription("Phone").performClick()
        until(label = "Keypad")
        compose.onNodeWithContentDescription("Keypad").performClick()
        compose.mainClock.advanceTimeBy(600)
        for (k in "5550142") {
            compose.onNode(hasContentDescription(k.toString()) and hasClickAction()).performClick()
            compose.mainClock.advanceTimeBy(100)
        }
        compose.onNodeWithContentDescription("Call").performClick()
        until(text = "calling…", settleMs = 1_500)
        compose.onNodeWithText("Priya").assertExists()
        compose.onNodeWithContentDescription("End call").performClick()
        until(text = "ended", settleMs = 2_000)

        // Recreated: the phone comes back on its home screen, and the call is still in the log.
        compose.activityRule.scenario.recreate()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithContentDescription("Phone").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithContentDescription("Outgoing call to Priya, now").assertExists()
    }

    @Test fun resetFromSettingsStartsTheGameOver() {
        skipToBoard()
        compose.onNodeWithContentDescription("Theo's phone, on the desk").performClick()
        until(label = "Private number: Theo. 1 unread message")
        compose.onNodeWithContentDescription("Settings").performClick()
        until(label = "Reset progress")
        compose.onNodeWithContentDescription("Reset progress").performClick()
        until(label = "Reset")
        compose.onNodeWithContentDescription("Reset").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithContentDescription("wickmoth").assertExists()

        // From the top again: the private number has never written, so it writes again.
        skipToBoard()
        compose.onNodeWithContentDescription("Theo's phone, on the desk").performClick()
        until(label = "Private number: Theo. 1 unread message")
    }
}
