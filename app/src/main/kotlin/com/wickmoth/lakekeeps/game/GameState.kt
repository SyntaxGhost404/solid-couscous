package com.wickmoth.lakekeeps.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wickmoth.lakekeeps.game.mail.MailBox
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.game.phone.CallLog

/** The intro plays once, in order, and lands on the case board. */
enum class Stage { Studio, Advisory, Title, Board }

/** The two phones on the desk. */
enum class Owner { Theo, Mira }

@Stable
class GameState(
    stage: Stage,
    phone: Owner?,
    boardSettled: Boolean,
    val messages: Messages = Messages(),
    val calls: CallLog = CallLog(),
    val mail: MailBox = MailBox(),
) {
    var stage by mutableStateOf(stage)
        private set

    /** The phone currently held up, if any. */
    var phone by mutableStateOf(phone)

    /** Whether the board has already been laid out once (its intro never replays). */
    var boardSettled by mutableStateOf(boardSettled)

    fun next() {
        stage = Stage.entries[(stage.ordinal + 1).coerceAtMost(Stage.entries.lastIndex)]
    }

    /** Starts the game over from the intro: phones back on the desk, nothing read, sent or called. */
    fun reset() {
        messages.clear()
        calls.clear()
        mail.clear()
        phone = null
        boardSettled = false
        stage = Stage.Studio
    }

    companion object {
        val Saver = listSaver<GameState, Any?>(
            save = { listOf(it.stage.name, it.phone?.name, it.boardSettled, it.messages.encode(), it.calls.encode(), it.mail.encode()) },
            restore = {
                GameState(
                    stage = Stage.valueOf(it[0] as String),
                    phone = (it[1] as String?)?.let(Owner::valueOf),
                    boardSettled = it[2] as Boolean,
                    // state saved by earlier builds has no messages, calls or mail entries
                    messages = Messages.decode(it.getOrNull(3) as String?),
                    calls = CallLog.decode(it.getOrNull(4) as String?),
                    mail = MailBox.decode(it.getOrNull(5) as String?),
                )
            },
        )
    }
}

@Composable
fun rememberGameState(start: Stage = Stage.Studio): GameState =
    rememberSaveable(saver = GameState.Saver) { GameState(start, phone = null, boardSettled = false) }
