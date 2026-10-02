package com.wickmoth.lakekeeps

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.wickmoth.lakekeeps.game.GameRoot
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.Stage
import com.wickmoth.lakekeeps.game.littlebird.Flag
import com.wickmoth.lakekeeps.game.littlebird.SamsPhone
import com.wickmoth.lakekeeps.game.littlebird.Symptom
import com.wickmoth.lakekeeps.game.messages.Thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Case 1, Little Bird, played through Sam's office and phone, frame by frame to build/shots. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class LittleBirdTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var state: GameState
    private var now = 0L

    /** Sam's office, with the case already as far as [flags] (and [prepare]) take it. */
    private fun office(settled: Boolean = true, phone: Owner? = null, prepare: GameState.() -> Unit = {}) {
        compose.mainClock.autoAdvance = false
        state = GameState(Stage.Board, phone = phone, boardSettled = settled)
        state.prepare()
        compose.setContent { GameRoot(state) }
    }

    private fun at(ms: Long, name: String? = null) {
        compose.mainClock.advanceTimeBy(ms - now)
        now = ms
        if (name != null) compose.onRoot().captureRoboImage("build/shots/$name.png")
    }

    private fun until(label: String, text: Boolean = false, limitMs: Long = 20_000): Long {
        val end = now + limitMs
        fun found() = (if (text) compose.onAllNodesWithText(label, substring = true) else compose.onAllNodesWithContentDescription(label, substring = true))
            .fetchSemanticsNodes().isNotEmpty()
        while (!found()) {
            check(now < end) { "\"$label\" never appeared" }
            at(now + 16)
        }
        return now
    }

    /** Taps the one thing labelled exactly [label] (or, with [containing], the first whose label contains it). */
    private fun tap(label: String, containing: Boolean = false) {
        if (containing) {
            compose.onAllNodesWithContentDescription(label, substring = true).onFirst().performClick()
        } else {
            compose.onNodeWithContentDescription(label).performClick()
        }
        compose.waitForIdle()
    }

    /** Taps the topmost of several things labelled [label] (an app's close button over the one under it). */
    private fun tapTop(label: String) {
        compose.onAllNodesWithContentDescription(label).onLast().performClick()
        compose.waitForIdle()
    }

    /** Picks the first reply to [thread]'s open question, once it's on screen. */
    private fun answer(thread: Thread) {
        val question = state.messages.question(thread) ?: return
        val label = "Reply: ${question.replies.first().text}"
        until(label)
        tap(label)
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /** Plays [thread] as far as it goes right now, answering with [picks] in turn (the first reply otherwise). */
    private fun GameState.converse(thread: Thread, vararg picks: Int) {
        var i = 0
        while (true) {
            if (messages.nextLine(thread) != null) {
                messages.deliver(thread)
                continue
            }
            val question = messages.question(thread) ?: break
            messages.choose(thread, picks.getOrElse(i++) { 0 })
        }
        messages.markRead(thread)
        messages.signOff(thread)
    }

    private fun GameState.afterIntake() {
        progress.set(Flag.CASE_OPEN)
        converse(SamsPhone.AmyHart)
        converse(SamsPhone.DanaBrooks)
        Flag.seen("memo.intake").let(progress::set)
    }

    private fun GameState.afterSorting() {
        afterIntake()
        Symptom.entries.forEach { progress.set(Flag.sorted(it.id)) }
        progress.set(Flag.SORTED)
    }

    private fun GameState.afterBand() {
        afterSorting()
        converse(SamsPhone.AmyHart)
        progress.set(Flag.PIN_BAND)
        progress.set(Flag.seen("memo.band"))
    }

    /** Presses and holds the Lab's screenshot at ([x], [y]) in screenshot dp. */
    private fun holdShot(title: String, x: Float, y: Float) {
        compose.onNodeWithContentDescription("$title. Press and hold anything worth pinning")
            .performTouchInput { longClick(percentOffset(x / 360f, y / 780f)) }
        compose.waitForIdle()
    }

    @Test fun officeOpensAndAmyWrites() {
        office(settled = false)
        at(500, "70_office_0500")
        at(1300, "70_office_1300")
        at(2600, "70_office_2600")
        at(4000, "70_office_settled")
        assertTrue(state.progress.has(Flag.CASE_OPEN))
        until("Sam's phone, on the desk")
        while (state.messages[SamsPhone.AmyHart].delivered == 0) at(now + 16)
        at(now + 120, "71_phone_buzz_0120")
        at(now + 600, "71_phone_buzz")
        at(now + 4000, "71_phone_waiting")
    }

    @Test fun intakeByMessage() {
        office { progress.set(Flag.CASE_OPEN) }
        at(300)
        while (state.messages[SamsPhone.AmyHart].delivered == 0) at(now + 16)
        tap("Sam's phone, on the desk")
        at(now + 1500, "72_sams_phone")
        tap("Messages, ", containing = true)
        at(now + 700, "72_inbox")
        tap("Amy Hart, 1 unread message")
        at(now + 600, "73_amy_first")
        // answer everything with the first reply, watching the conversation as it goes
        var answered = 0
        while (!state.progress.has(Flag.INTAKE_DONE)) {
            if (state.messages.question(SamsPhone.AmyHart) != null) {
                if (answered == 2) at(now + 400, "73_amy_c1_choice")
                answer(SamsPhone.AmyHart)
                answered++
            }
            at(now + 200)
            check(now < 240_000) { "the intake never ended" }
        }
        at(now + 400, "74_intake_over")
        at(now + 2000, "74_amy_offline")
        assertTrue(state.progress.has(Flag.C1_STEADY))
        // Dana writes once Sam has taken the case; her banner comes and goes
        while (state.messages[SamsPhone.DanaBrooks].delivered == 0) at(now + 16)
        at(now + 300, "74_dana_banner")
        at(now + 6000)
        tap("Back")
        at(now + 600)
        tap("Close")
        at(now + 700)
        back()
        at(now + 1200, "75_board_reveal_1200")
        at(now + 1600, "75_board_reveal_2800")
        at(now + 2400, "75_board_reveal_5200")
        at(now + 6000, "75_board_after_intake")
    }

    /** A reply tapped again as it goes out must not answer the next question before it's asked. */
    @Test fun aSecondTapNeverAnswersTheNextQuestion() {
        office(phone = Owner.Sam) { progress.set(Flag.CASE_OPEN) }
        val amy = SamsPhone.AmyHart
        while (state.messages[amy].delivered == 0) at(now + 100)
        at(now + 1500)
        tap("Messages, 2 unread messages")
        at(now + 900)
        tap("Amy Hart, 1 unread message")
        until("Reply: This is Sam")
        // tapped again and again, for as long as it's on screen: on its way out too
        val reply = compose.onAllNodesWithContentDescription("Reply: This is Sam", substring = true)
        var taps = 0
        while (reply.fetchSemanticsNodes().isNotEmpty()) {
            reply.onFirst().performClick()
            compose.waitForIdle()
            taps++
            compose.mainClock.advanceTimeBy(40, ignoreFrameDuration = true)
            now += 40
        }
        assertTrue(taps > 6)
        // her next question is asked, not answered for her
        while (state.messages.question(amy)?.replies?.first()?.text?.startsWith("Slow down") != true) {
            at(now + 100)
            check(now < 60_000) { "the C1 question never came" }
        }
        assertEquals(listOf(0), state.messages[amy].chosen)
        until("Reply: Slow down")
    }

    @Test fun sortingTheSymptoms() {
        office { afterIntake() }
        at(800, "76_tray")
        // a wrong column first, then every card where it belongs
        tap("Card: silent calls at odd hours, to sort")
        at(now + 200, "76_card_lifted")
        tap("Put silent calls at odd hours under: a machine could do this")
        at(now + 160, "76_wrong_0160")
        at(now + 700, "76_wrong")
        Symptom.entries.forEach { card ->
            tap("Card: ${card.text}, to sort")
            at(now + 120)
            tap("Put ${card.text} under: ${card.column.heading}")
            at(now + 260)
        }
        at(now + 200, "77_sorted_0200")
        at(now + 1400, "77_sorted_1400")
        at(now + 4000, "77_sorted")
        assertTrue(state.progress.has(Flag.SORTED))
    }

    @Test fun batteryScreenshotPinnedInTheLab() {
        office { afterSorting() }
        at(600)
        tap("Sam's phone, on the desk")
        at(now + 1400)
        tap("Messages")
        at(now + 700)
        tap("Amy Hart")
        at(now + 600, "78_coach_choice")
        tap("Reply: Pull your per-app", containing = true)
        at(now + 2600, "78_jargon")
        while (state.messages.question(SamsPhone.AmyHart) == null) at(now + 100)
        answer(SamsPhone.AmyHart)
        while (state.messages.lines(SamsPhone.AmyHart).none { it.attachment != null }) at(now + 100)
        at(now + 900, "79_screenshot_in_chat")
        tap("Picture: Battery, last 24 hours")
        at(now + 400, "79_picture")
        back()
        while (!state.progress.has(Flag.BATTERY_SENT)) {
            answer(SamsPhone.AmyHart)
            at(now + 200)
        }
        at(now + 2200)
        tapTop("Close")
        at(now + 600)
        back()
        at(now + 1600, "80_file_in_tray")
        tap("Evidence tray, 1 file")
        at(now + 200, "80_vault_0200")
        at(now + 700, "80_vault")
        tap("Battery, last 24 hours, from Amy Hart", containing = true)
        at(now + 600, "81_lab")
        // a press on the daytime bars, then on the band
        val lab = compose.onNodeWithContentDescription("Battery, last 24 hours. Press and hold", substring = true)
        lab.performTouchInput { longClick(percentOffset(0.85f, 0.38f)) }
        at(now + 300, "81_lab_daytime")
        lab.performTouchInput { longClick(percentOffset(0.32f, 0.38f)) }
        at(now + 300, "81_lab_pinned")
        assertTrue(state.progress.has(Flag.PIN_BAND))
        at(now + 3000)
        back()
        at(now + 400)
        back()
        at(now + 1400, "82_board_band_1400")
        at(now + 4000, "82_board_band")
    }

    @Test fun impostorPickedAndTrackerTied() {
        office { afterBand() }
        at(600, "85_board_before_apps")
        tap("Sam's phone, on the desk")
        at(now + 1400)
        tap("Messages")
        at(now + 700)
        tap("Amy Hart")
        at(now + 600, "85_apps_choice")
        var pictures = 0
        while (!state.progress.has(Flag.APPS_SENT)) {
            answer(SamsPhone.AmyHart)
            val sent = state.messages.lines(SamsPhone.AmyHart).count { it.attachment != null }
            if (sent > pictures) {
                pictures = sent
                at(now + 700, "85_apps_picture_$sent")
            }
            at(now + 200)
            check(now < 120_000) { "the apps conversation never ended" }
        }
        at(now + 2000, "85_apps_over")
        tapTop("Close")
        at(now + 600)
        back()
        at(now + 1800, "86_tray_three")
        tap("Evidence tray, 3 files")
        at(now + 700, "86_vault_three")
        tap("Apps, with system apps shown, from Amy Hart", containing = true)
        at(now + 600, "87_lab_apps")
        holdShot("Apps, with system apps shown", 180f, 202f)
        at(now + 300, "87_lab_apps_belongs")
        holdShot("Apps, with system apps shown", 180f, 502f)
        at(now + 300, "87_lab_apps_pinned")
        assertTrue(state.progress.has(Flag.PIN_IMPOSTOR))
        at(now + 3000)
        back()
        at(now + 500)
        tap("System Sync Service, app info, from Amy Hart", containing = true)
        at(now + 600, "88_lab_info")
        holdShot("System Sync Service, app info", 180f, 328f)
        at(now + 300)
        holdShot("System Sync Service, app info", 180f, 586f)
        at(now + 300)
        holdShot("System Sync Service, app info", 180f, 750f)
        at(now + 300, "88_lab_info_pinned")
        at(now + 3000)
        back()
        at(now + 400)
        back()
        at(now + 1500, "89_board_tracker_1500")
        at(now + 3500, "89_board_tracker")

        // tie the tracker to what it explains; one that doesn't hold first
        tap("Thread: tie two things together")
        at(now + 300, "90_spool_out")
        tap("Tie: System Sync Service")
        at(now + 200, "90_first_picked")
        tap("Tie: silent calls at odd hours")
        at(now + 400, "90_verbs")
        tap("System Sync Service explains silent calls at odd hours")
        at(now + 300, "90_does_not_hold")
        at(now + 3000)
        tap("Tie: System Sync Service")
        tap("Tie: phone dead by noon")
        at(now + 300)
        tap("System Sync Service explains phone dead by noon")
        at(now + 300, "91_first_link_0300")
        at(now + 900, "91_first_link")
        tap("Tie: System Sync Service")
        tap("Tie: they know where she's been")
        at(now + 300)
        tap("System Sync Service explains they know where she's been")
        at(now + 400)
        assertTrue(state.progress.has(Flag.ACT1_DONE))
        at(now + 1200, "92_act1_1200")
        at(now + 2400, "92_act1_2400")
        at(now + 9000, "92_act1_done")
    }

    @Test fun aVoiceMemoNeverBlocksTheDesk() {
        office {
            progress.set(Flag.CASE_OPEN)
            converse(SamsPhone.AmyHart)
            converse(SamsPhone.DanaBrooks)
        }
        // the memo after the intake is still to play: the phone can be picked up meanwhile
        at(600)
        tap("Sam's phone, on the desk")
        at(now + 1500)
        assertEquals(Owner.Sam, state.phone)
        back()
        // back in the office it plays, and a tap on it files it
        until("Voice memo, After the intake")
        at(now + 600, "84_memo_playing")
        tap("Voice memo, After the intake", containing = true)
        at(now + 400)
        assertTrue(state.progress.has(Flag.seen("memo.intake")))
    }

    @Test fun notebookHints() {
        office { afterIntake() }
        at(600)
        tap("Notebook", containing = true)
        at(now + 200, "83_notebook_0200")
        at(now + 700, "83_notebook")
        tap("Stuck? A nudge")
        at(now + 300)
        tap("Still stuck? A pointer")
        at(now + 400, "83_notebook_hints")
        assertEquals(2, state.progress.hints("sort"))
        tap("Intake page")
        at(now + 500, "84_notebook_intake")
        tap("Memos page")
        at(now + 500, "84_notebook_memos")
    }
}
