package com.wickmoth.lakekeeps

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.wickmoth.lakekeeps.game.GameRoot
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.GameSurface
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.Stage
import com.wickmoth.lakekeeps.game.case.CaseId
import com.wickmoth.lakekeeps.screens.advisory.HeadphonesAdvisory
import com.wickmoth.lakekeeps.screens.studio.StudioSplash
import com.wickmoth.lakekeeps.screens.title.TitleScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders each screen (and frames of its animations) at the 720 x 1600 reference size, to
 * build/shots. Run with: ./gradlew :app:recordRoborazziDebug
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class ScreensTest {
    @get:Rule val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameSurface(content) }
    }

    /** Advances the frame clock to [ms] since the first frame and captures the screen. */
    private var now = 0L

    private fun at(ms: Long, name: String) {
        compose.mainClock.advanceTimeBy(ms - now)
        now = ms
        compose.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test fun studio() {
        show { StudioSplash(onFinished = {}) }
        at(600, "1_studio_0600")
        at(1340, "1_studio_1340")
        at(2200, "1_studio_2200_hold")
        at(3200, "1_studio_3200_out")
    }

    @Test fun advisory() {
        show { HeadphonesAdvisory(onFinished = {}) }
        at(700, "2_advisory_0700")
        at(2600, "2_advisory_2600_hold")
    }

    @Test fun title() {
        show { TitleScreen(onFinished = {}) }
        at(1200, "3_title_1200")
        at(2800, "3_title_2800")
        at(4400, "3_title_4400_shimmer")
        at(6200, "3_title_6200_hold")
    }

    @Test fun boardIntro() {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameRoot(GameState(Stage.Board, phone = null, boardSettled = false, case = CaseId.Prototype)) }
        at(500, "4_board_0500")
        at(1300, "4_board_1300")
        at(2100, "4_board_2100")
        at(2900, "4_board_2900")
        at(4200, "4_board_4200_settled")
    }

    @Test fun theoPhone() {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameRoot(GameState(Stage.Board, phone = null, boardSettled = true, case = CaseId.Prototype)) }
        at(100, "5_theo_0000_desk")
        compose.onNodeWithContentDescription("Theo's phone, on the desk").performClick()
        at(250, "5_theo_0150_lift")
        at(400, "5_theo_0300_lift")
        at(600, "5_theo_0500_lift")
        at(1400, "5_theo_1300_open")
    }

    @Test fun miraPhone() {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameRoot(GameState(Stage.Board, phone = Owner.Mira, boardSettled = true, case = CaseId.Prototype)) }
        at(300, "6_mira_open")
        compose.onNodeWithContentDescription("Back to the case board").performClick()
        at(500, "6_mira_close_0200")
        at(750, "6_mira_close_0450")
        at(1400, "6_mira_closed")
    }

    @Test fun polaroidSwing() {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameRoot(GameState(Stage.Board, phone = null, boardSettled = true, case = CaseId.Prototype)) }
        at(100, "7_swing_0000")
        compose.onNodeWithContentDescription("Photo of Owen").performClick()
        at(180, "7_swing_0080")
        at(300, "7_swing_0200")
        at(520, "7_swing_0420")
    }

    /** Settings, once a locked app on Theo's phone, opens out of its icon like the others. */
    @Test fun settingsOpens() {
        compose.mainClock.autoAdvance = false
        compose.setContent { GameRoot(GameState(Stage.Board, phone = Owner.Theo, boardSettled = true, case = CaseId.Prototype)) }
        at(300, "8_settings_0000")
        compose.onNodeWithContentDescription("Settings").performClick()
        at(360, "8_settings_0060")
        at(430, "8_settings_0130")
        at(900, "8_settings_open")
    }
}
