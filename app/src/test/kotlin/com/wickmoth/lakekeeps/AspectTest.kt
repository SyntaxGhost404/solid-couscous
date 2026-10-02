package com.wickmoth.lakekeeps

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.wickmoth.lakekeeps.game.GameRoot
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.Stage
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
        compose.setContent { GameRoot(GameState(Stage.Board, phone = phone, boardSettled = true)) }
        compose.mainClock.advanceTimeBy(1500)
        compose.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun wide16x9Board() = board("9_aspect_16x9_board")

    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    @Test fun wide16x9Phone() = board("9_aspect_16x9_phone", Owner.Mira)

    @Config(sdk = [36], qualifiers = "w360dp-h860dp-xhdpi")
    @Test fun tallBoard() = board("9_aspect_tall_board")
}
