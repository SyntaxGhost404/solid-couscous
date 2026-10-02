package com.wickmoth.lakekeeps

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.wickmoth.lakekeeps.game.GameRoot
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.Stage
import com.wickmoth.lakekeeps.game.case.CaseId
import com.wickmoth.lakekeeps.game.littlebird.Flag
import com.wickmoth.lakekeeps.game.littlebird.LittleBird
import com.wickmoth.lakekeeps.game.littlebird.SamsPhone
import com.wickmoth.lakekeeps.game.littlebird.Symptom
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.game.messages.Thread
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The fixed design frame letterboxes cleanly on other aspect ratios. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AspectTest {
    @get:Rule val compose = createComposeRule()

    private fun board(name: String, phone: Owner? = null) {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameRoot(GameState(Stage.Board, phone = phone, boardSettled = true, case = CaseId.Prototype)) }
        compose.mainClock.advanceTimeBy(1500)
        compose.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun wide16x9Board() = board("9_aspect_16x9_board")

    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun wide16x9Phone() = board("9_aspect_16x9_phone", Owner.Mira)

    @Config(sdk = [36], qualifiers = "w360dp-h860dp-xhdpi")
    @Test fun tallBoard() = board("9_aspect_tall_board")

    /** On a short screen the keys give up some height so the number keeps its room. */
    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun wide16x9Keypad() {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameRoot(GameState(Stage.Board, phone = Owner.Mira, boardSettled = true, case = CaseId.Prototype)) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithContentDescription("Phone").performClick()
        compose.mainClock.advanceTimeBy(700)
        compose.onNodeWithContentDescription("Keypad").performClick()
        compose.mainClock.advanceTimeBy(600)
        for (k in "5550142") {
            compose.onNode(hasContentDescription(k.toString()) and hasClickAction()).performClick()
            compose.mainClock.advanceTimeBy(100)
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("build/shots/9_aspect_16x9_keypad.png")
        compose.onNodeWithContentDescription("Call").performClick()
        compose.mainClock.advanceTimeBy(2000)
        compose.onRoot().captureRoboImage("build/shots/9_aspect_16x9_calling.png")
    }

    /** Plays [thread] as far as it goes right now, with the first reply to everything. */
    private fun Messages.playOut(thread: Thread) {
        while (true) {
            if (nextLine(thread) != null) {
                deliver(thread)
                continue
            }
            question(thread) ?: break
            choose(thread, 0)
        }
        markRead(thread)
        signOff(thread)
    }

    /** Sam's office at the end of Little Bird's first act: everything on the board and in the tray. */
    private fun office(name: String, then: () -> Unit = {}) {
        compose.mainClock.autoAdvance = false
        val state = GameState(Stage.Board, phone = null, boardSettled = true).apply {
            progress.set(Flag.CASE_OPEN)
            messages.playOut(SamsPhone.AmyHart)
            messages.playOut(SamsPhone.DanaBrooks)
            Symptom.entries.forEach { progress.set(Flag.sorted(it.id)) }
            progress.set(Flag.SORTED)
            messages.playOut(SamsPhone.AmyHart)
            listOf(Flag.PIN_BAND, Flag.PIN_SYNC).forEach(progress::set)
            messages.playOut(SamsPhone.AmyHart)
            listOf(Flag.PIN_IMPOSTOR, Flag.PIN_PERMISSIONS, Flag.PIN_INSTALLED, Flag.PIN_PACKAGE).forEach(progress::set)
            LittleBird.links.forEach { progress.set(Flag.linked(it.id)) }
            progress.set(Flag.ACT1_DONE)
            LittleBird.memos.forEach { progress.set(Flag.seen("memo.${it.id}")) }
        }
        compose.setContent { GameRoot(state) }
        compose.mainClock.advanceTimeBy(1500)
        then()
        compose.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun wide16x9Office() = office("9_aspect_16x9_office")

    @Config(sdk = [36], qualifiers = "w360dp-h860dp-xhdpi")
    @Test fun tallOffice() = office("9_aspect_tall_office")

    /** The Lab keeps the whole screenshot in view on a short screen. */
    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun wide16x9Lab() = office("9_aspect_16x9_lab") {
        compose.onNodeWithContentDescription("Evidence tray, 3 files").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(800)
        compose.onNode(hasContentDescription("Battery, last 24 hours, from Amy Hart", substring = true)).performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(800)
    }

    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun wide16x9Notebook() = office("9_aspect_16x9_notebook") {
        compose.onNode(hasContentDescription("Notebook", substring = true) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(900)
    }

    /** On a short screen the settings scroll, down to the version line. */
    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun wide16x9Settings() {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameRoot(GameState(Stage.Board, phone = Owner.Mira, boardSettled = true, case = CaseId.Prototype)) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.mainClock.advanceTimeBy(900)
        compose.onRoot().captureRoboImage("build/shots/9_aspect_16x9_settings.png")
        compose.onNodeWithText("Version 0.1.0").performScrollTo()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Version 0.1.0").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/shots/9_aspect_16x9_settings_end.png")
    }
}
