package com.wickmoth.lakekeeps

import androidx.compose.ui.test.assertCountEquals
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

/**
 * Launches the real activity (splash theme, immersive window, sound bank) and plays the intro into
 * Little Bird: Sam's office, his phone, and the case saved with the game.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class FlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val OPENING = "Is this Sam Novak? Dana Brooks gave me this number."
        const val FIRST_REPLY = "This is Sam. Dana said you'd write."
    }

    @Test fun introPlaysThroughToTheOffice() {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("wickmoth").assertExists()
        compose.mainClock.advanceTimeBy(4_200)
        compose.onNodeWithText("better experience").assertExists()
        compose.mainClock.advanceTimeBy(4_500)
        compose.onNodeWithText("Lake Keeps").assertExists()
        compose.mainClock.advanceTimeBy(8_200)
        compose.onNodeWithContentDescription("A photograph, turned to face the board").assertExists()
        compose.onNodeWithContentDescription("Sam's phone, on the desk").assertExists()
        // the single-device rule: the prototype's phones are nowhere on the desk
        compose.onNodeWithContentDescription("Theo's phone, on the desk").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mira's phone, on the desk").assertDoesNotExist()
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
        compose.onNodeWithContentDescription("A photograph, turned to face the board").assertExists()
    }

    /** Taps through the three intro screens to the settled office. */
    private fun skipToOffice() {
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

    /** Picks Sam's phone up from the desk and waits for its home screen. */
    private fun pickUpThePhone() {
        compose.onNodeWithContentDescription("Sam's phone, on the desk").performClick()
        until(text = "Sam's phone")
    }

    @Test fun phoneOpensAndClosesFromTheOffice() {
        skipToOffice()
        pickUpThePhone()
        compose.onNodeWithContentDescription("Settings").assertExists()
        compose.onNodeWithContentDescription("Back to the case board").performClick()
        compose.mainClock.advanceTimeBy(1_200)
        compose.onNodeWithContentDescription("Settings").assertDoesNotExist()
        compose.onNodeWithContentDescription("Sam's phone, on the desk").assertExists()
    }

    @Test fun amyWritesFirstAndTheConversationIsSavedWithTheGame() {
        skipToOffice()
        // the case opens with Amy's message, not a call: it waits in Messages, beside Dana's
        // referral from earlier in the afternoon
        pickUpThePhone()
        until(label = "Messages, 2 unread messages")
        compose.onNodeWithContentDescription("Messages, 2 unread messages").performClick()
        until(label = "Amy Hart, 1 unread message")
        compose.onNodeWithContentDescription("Amy Hart, 1 unread message").performClick()
        until(text = OPENING)
        until(label = "Reply: $FIRST_REPLY")
        compose.onNodeWithContentDescription("Reply: $FIRST_REPLY").performClick()
        until(text = "Sorry. I've typed this out ten times and deleted it.")

        // Recreated (as after the process is reclaimed): the phone comes back on its home screen
        // with the conversation kept, and Amy doesn't start it over.
        compose.activityRule.scenario.recreate()
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithContentDescription("Messages, 1 unread message").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithContentDescription("Amy Hart").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.onAllNodesWithText(OPENING).assertCountEquals(1)
        compose.onNodeWithText(FIRST_REPLY).assertExists()
    }

    @Test fun callIsLoggedAndSavedWithTheGame() {
        skipToOffice()
        pickUpThePhone()
        compose.onNodeWithContentDescription("Phone").performClick()
        until(label = "Keypad")
        compose.onNodeWithContentDescription("Keypad").performClick()
        compose.mainClock.advanceTimeBy(600)
        for (k in "5550134") {
            compose.onNode(hasContentDescription(k.toString()) and hasClickAction()).performClick()
            compose.mainClock.advanceTimeBy(100)
        }
        compose.onNodeWithContentDescription("Call").performClick()
        until(text = "calling…", settleMs = 1_500)
        compose.onNodeWithText("Dana Brooks").assertExists()
        compose.onNodeWithContentDescription("End call").performClick()
        until(text = "ended", settleMs = 2_000)

        // Recreated: the phone comes back on its home screen, and the call is still in the log.
        compose.activityRule.scenario.recreate()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithContentDescription("Phone").performClick()
        compose.mainClock.advanceTimeBy(800)
        // the one from four days ago, and this one
        compose.onAllNodes(hasContentDescription("Outgoing call to Dana Brooks", substring = true)).assertCountEquals(2)
    }

    @Test fun resetFromSettingsStartsTheCaseOver() {
        skipToOffice()
        pickUpThePhone()
        until(label = "Messages, 2 unread messages")
        compose.onNodeWithContentDescription("Settings").performClick()
        until(label = "Reset progress")
        compose.onNodeWithContentDescription("Reset progress").performClick()
        until(label = "Reset")
        compose.onNodeWithText("the case starts again", substring = true).assertExists()
        compose.onNodeWithContentDescription("Reset").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithContentDescription("wickmoth").assertExists()

        // From the top again: Amy has never written, so she writes again.
        skipToOffice()
        pickUpThePhone()
        until(label = "Messages, 2 unread messages")
    }
}
