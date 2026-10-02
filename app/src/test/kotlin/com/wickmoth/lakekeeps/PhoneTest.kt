package com.wickmoth.lakekeeps

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.wickmoth.lakekeeps.game.GameRoot
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.Stage
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.game.phone.CallKind
import com.wickmoth.lakekeeps.game.phone.CallLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The held phones: status bar, notification shade, heads-up banner and the Messages app, played
 * through and rendered frame by frame to build/shots. Run with: ./gradlew :app:recordRoborazziDebug
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class PhoneTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val messages = Messages()
    private val calls = CallLog()
    private var now = 0L

    /** The case board with [owner]'s phone already held up and awake. */
    private fun holding(owner: Owner) {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameRoot(GameState(Stage.Board, phone = owner, boardSettled = true, messages = messages, calls = calls)) }
    }

    /** Advances the frame clock to [ms] since the first frame, capturing the screen if [name] is given. */
    private fun at(ms: Long, name: String? = null) {
        compose.mainClock.advanceTimeBy(ms - now)
        now = ms
        if (name != null) compose.onRoot().captureRoboImage("build/shots/$name.png")
    }

    /** Plays frames until [label] is on screen, then returns the time it appeared. */
    private fun until(label: String, text: Boolean = false, limitMs: Long = 15_000): Long {
        val end = now + limitMs
        fun found() = (if (text) compose.onAllNodesWithText(label) else compose.onAllNodesWithContentDescription(label))
            .fetchSemanticsNodes().isNotEmpty()
        while (!found()) {
            check(now < end) { "\"$label\" never appeared" }
            at(now + 16)
        }
        return now
    }

    private fun tap(label: String) = compose.onNodeWithContentDescription(label).performClick()

    /** A keypad key (the number display can carry the same text, but isn't a button). */
    private fun key(k: Char) = compose.onNode(hasContentDescription(k.toString()) and hasClickAction()).performClick()

    private fun back() = compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

    @Test fun liveConversationFromTheBanner() {
        holding(Owner.Theo)
        at(1100, "10_theo_before_message")
        val banner = "Private number: Theo. 1 unread message"
        val arrived = until(banner)
        at(arrived + 100, "10_theo_banner_0100")
        at(arrived + 220, "10_theo_banner_0220")
        at(arrived + 700, "10_theo_banner")

        tap(banner)
        at(now + 120, "11_open_from_banner_0120")
        at(now + 130, "11_open_from_banner_0250")
        at(now + 450, "11_chat_first_question")

        // No leaving while the contact is online: back only nudges the replies.
        back()
        at(now + 60, "11_back_nudge_0060")
        at(now + 500)
        compose.onNodeWithContentDescription("Reply: Who is this?").assertExists()
        compose.onNodeWithContentDescription("Contact details for Private number").assertExists()

        tap("Reply: Who is this?")
        at(now + 150, "12_reply_picked")
        at(now + 150, "12_reply_sent_0070")
        at(now + 400, "12_reply_sent")
        until("Private number is typing…", text = true)
        at(now + 500, "13_typing")
        until("Someone who wants her found as much as you do.", text = true)
        at(now + 180, "13_reply_arrives_0180")
        val second = until("Reply: Why are you helping me?")
        at(second + 120, "14_second_question_0120")
        at(second + 700, "14_second_question")

        tap("Reply: Why are you helping me?")
        until("And keep the police out of this.", text = true)
        at(now + 100, "15_last_line_0100")
        // The contact lingers online for a moment, then signs off.
        compose.onNodeWithText("Private number is online").assertExists()
        until("Private number is offline", text = true)
        at(now + 120, "15_signing_off_0120")
        at(now + 580, "15_chat_ended")
        assertEquals(0, messages.unread(Threads.PrivateNumber))
        assertFalse(messages.isLive(Threads.PrivateNumber))

        // The conversation is over, so back leaves it: to the inbox, then the home screen.
        back()
        at(now + 150, "16_back_to_inbox_0150")
        at(now + 450, "16_inbox_after_chat")
        compose.onNodeWithText("And keep the police out of this.").assertExists()
        back()
        at(now + 170, "16_app_closing_0170")
        at(now + 400, "16_home_after_chat")
    }

    @Test fun inboxConversationAndContactCard() {
        holding(Owner.Theo)
        at(500)
        tap("Messages, 1 unread message")
        at(600, "20_inbox_open_0100")
        at(720, "20_inbox_open_0220")
        at(900, "20_inbox_open_0400")
        at(1150, "20_theo_inbox")

        // The private number's first message lands while the inbox is open.
        val banner = "Private number: Theo. 1 unread message"
        until(banner)
        at(now + 500, "21_inbox_new_thread")
        compose.onNodeWithContentDescription(banner).performTouchInput { swipeUp() }
        at(now + 400, "21_banner_dismissed")
        compose.onNodeWithContentDescription(banner).assertDoesNotExist()

        tap("Priya")
        at(now + 120, "22_open_chat_0120")
        at(now + 400, "22_priya_chat")

        tap("Contact details for Priya")
        at(now + 120, "23_contact_0120")
        at(now + 500, "23_contact")
        compose.onNodeWithText("Mobile · 555-0142").assertExists()

        // Back closes the card first, then the conversation.
        back()
        at(now + 500, "23_contact_closed")
        compose.onNodeWithText("Mobile · 555-0142").assertDoesNotExist()
        compose.onNodeWithContentDescription("Contact details for Priya").assertExists()
        back()
        at(now + 150, "24_back_to_inbox_0150")
        at(now + 400)
        compose.onNodeWithContentDescription("Contact details for Priya").assertDoesNotExist()
    }

    @Test fun shadeOpensANotification() {
        holding(Owner.Mira)
        at(600, "30_mira_home")
        tap("Notifications and quick settings")
        at(700, "30_shade_0100")
        at(820, "30_shade_0220")
        at(1300, "30_mira_shade")

        tap("Felix: you up?")
        at(now + 150, "31_open_from_shade_0150")
        at(now + 500, "31_felix_chat")
        assertEquals(2, messages.unread(Owner.Mira))

        back()
        at(now + 450, "32_mira_inbox_after_felix")

        tap("Notifications and quick settings")
        at(now + 500, "33_shade_one_left")
        compose.onNodeWithContentDescription("Felix: you up?").assertDoesNotExist()
        compose.onNodeWithContentDescription("Priya: mira answer me").performTouchInput { swipeLeft() }
        at(now + 500, "33_shade_cleared")
        compose.onNodeWithText("No new notifications").assertExists()

        tap("Sound on")
        at(now + 300, "34_sound_off")
        compose.onNodeWithContentDescription("Sound off").assertExists()
        tap("Sound off")
        at(now + 300)
        compose.onNodeWithContentDescription("Sound on").assertExists()

        back()
        at(now + 500, "35_shade_closed")
        compose.onNodeWithText("No new notifications").assertDoesNotExist()
    }

    @Test fun miraInbox() {
        holding(Owner.Mira)
        at(500)
        tap("Messages, 3 unread messages")
        at(1100, "36_mira_inbox")
        tap("night_heron")
        at(1600, "37_heron_chat")
    }

    @Test fun phoneAppDialsAndHangsUp() {
        holding(Owner.Mira)
        at(500)
        tap("Phone")
        at(620, "40_phone_open_0120")
        at(1100, "40_mira_recents")

        tap("Keypad")
        at(now + 150, "41_keypad_0150")
        at(now + 500, "41_keypad")
        for (k in "5550103") {
            key(k)
            at(now + 90)
        }
        at(now + 40, "42_number_typed_0040")
        at(now + 300, "42_number_typed")
        compose.onNodeWithContentDescription("555-0103").assertExists()
        tap("Delete")
        at(now + 250, "42_digit_deleted")
        compose.onNodeWithContentDescription("555-010").assertExists()
        key('3')
        at(now + 250)

        tap("Call")
        at(now + 120, "43_calling_0120")
        at(now + 500, "43_calling")
        compose.onNodeWithText("Theo").assertExists()
        compose.onNodeWithText("calling…").assertExists()
        at(now + 1100, "43_ringing")
        // No leaving mid-call: back only shakes the call.
        back()
        at(now + 60, "43_back_refused_0060")
        at(now + 600)
        compose.onNodeWithContentDescription("End call").assertExists()

        tap("End call")
        at(now + 120, "44_ended_0120")
        at(now + 700, "44_ended")
        compose.onNodeWithText("ended").assertExists()
        at(now + 1300, "44_back_to_keypad")
        compose.onNodeWithContentDescription("Call").assertExists()
        compose.onNodeWithContentDescription("Delete").assertDoesNotExist()

        tap("Recent calls")
        at(now + 500, "45_recents_after_call")
        compose.onNodeWithContentDescription("Outgoing call to Theo, now").assertExists()
        assertEquals(CallKind.Outgoing, calls.calls(Owner.Mira).first().kind)

        back()
        at(now + 170, "45_phone_closing_0170")
        at(now + 400, "45_mira_home_after_call")
    }

    @Test fun callRingsOutWhileTheBannerWaits() {
        holding(Owner.Theo)
        at(300)
        tap("Phone")
        at(800, "46_theo_recents")
        compose.onAllNodesWithContentDescription("Call Mira").onFirst().performClick()
        // The private number's first message lands mid-call; its banner can't take the player away.
        val banner = "Private number: Theo. 1 unread message"
        until(banner)
        at(now + 600, "46_banner_during_call")
        tap(banner)
        at(now + 80, "46_banner_refused_0080")
        at(now + 500)
        compose.onNodeWithContentDescription("End call").assertExists()
        compose.onNodeWithText("Mira").assertExists()

        until("ended", text = true)
        at(now + 120, "47_rang_out_0120")
        at(now + 1700)
        at(now + 400, "47_recents_after_ring_out")
        compose.onNodeWithContentDescription("Outgoing call to Mira, now").assertExists()
    }
}
