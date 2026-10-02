package com.wickmoth.lakekeeps.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** The intro plays once, in order, and lands on the case board. */
enum class Stage { Studio, Advisory, Title, Board }

/** The two phones on the desk. */
enum class Owner { Theo, Mira }

@Stable
class GameState(stage: Stage, phone: Owner?, boardSettled: Boolean) {
    var stage by mutableStateOf(stage)
        private set

    /** The phone currently held up, if any. */
    var phone by mutableStateOf(phone)

    /** Whether the board has already been laid out once (its intro never replays). */
    var boardSettled by mutableStateOf(boardSettled)

    fun next() {
        stage = Stage.entries[(stage.ordinal + 1).coerceAtMost(Stage.entries.lastIndex)]
    }

    companion object {
        val Saver = listSaver<GameState, Any?>(
            save = { listOf(it.stage.name, it.phone?.name, it.boardSettled) },
            restore = {
                GameState(
                    stage = Stage.valueOf(it[0] as String),
                    phone = (it[1] as String?)?.let(Owner::valueOf),
                    boardSettled = it[2] as Boolean,
                )
            },
        )
    }
}

@Composable
fun rememberGameState(start: Stage = Stage.Studio): GameState =
    rememberSaveable(saver = GameState.Saver) { GameState(start, phone = null, boardSettled = false) }
