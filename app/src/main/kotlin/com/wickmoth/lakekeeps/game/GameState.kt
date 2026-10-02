package com.wickmoth.lakekeeps.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wickmoth.lakekeeps.game.case.CaseId
import com.wickmoth.lakekeeps.game.case.CaseProgress
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.game.mail.MailBox
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.game.phone.CallLog

/** The intro plays once, in order, and lands on the case board. */
enum class Stage { Studio, Advisory, Title, Board }

/** Every phone in the game, by whose it is. Each belongs to one case, and only its case's are on the desk. */
enum class Owner { Theo, Mira, Sam }

/**
 * The game being played: which [case], how far the intro has got, the phone in hand, and the
 * player's progress through conversations, calls, mail and the case itself.
 */
@Stable
class GameState(
    stage: Stage,
    phone: Owner?,
    boardSettled: Boolean,
    val messages: Messages = Messages(),
    val calls: CallLog = CallLog(),
    val mail: MailBox = MailBox(),
    val case: CaseId = CaseId.LittleBird,
    val progress: CaseProgress = CaseProgress(),
) {
    init {
        messages.flags = progress
    }

    var stage by mutableStateOf(stage)
        private set

    /** The phone currently held up, if any. */
    var phone by mutableStateOf(phone)

    /** Whether the board has already been laid out once (its intro never replays). */
    var boardSettled by mutableStateOf(boardSettled)

    /** The conversation open on the phone in hand, if any (not saved: it closes with the phone). */
    var openThread by mutableStateOf<String?>(null)

    fun next() {
        stage = Stage.entries[(stage.ordinal + 1).coerceAtMost(Stage.entries.lastIndex)]
    }

    /** Starts the game over from the intro: phones back on the desk, nothing read, sent or called. */
    fun reset() {
        messages.clear()
        calls.clear()
        mail.clear()
        progress.clear()
        phone = null
        boardSettled = false
        stage = Stage.Studio
    }

    companion object {
        val Saver = listSaver<GameState, Any?>(
            save = {
                listOf(
                    it.stage.name, it.phone?.name, it.boardSettled, it.messages.encode(), it.calls.encode(), it.mail.encode(),
                    it.case.name, it.progress.encode(),
                )
            },
            restore = {
                // state saved by earlier builds has no messages, calls, mail or case entries
                val case = (it.getOrNull(6) as String?)?.let { name -> CaseId.entries.firstOrNull { c -> c.name == name } } ?: CaseId.LittleBird
                GameState(
                    stage = Stage.valueOf(it[0] as String),
                    phone = (it[1] as String?)?.let(Owner::valueOf)?.takeIf { owner -> owner.case == case },
                    boardSettled = it[2] as Boolean,
                    messages = Messages.decode(it.getOrNull(3) as String?),
                    calls = CallLog.decode(it.getOrNull(4) as String?),
                    mail = MailBox.decode(it.getOrNull(5) as String?),
                    case = case,
                    progress = CaseProgress.decode(it.getOrNull(7) as String?),
                )
            },
        )
    }
}

@Composable
fun rememberGameState(start: Stage = Stage.Studio): GameState =
    rememberSaveable(saver = GameState.Saver) { GameState(start, phone = null, boardSettled = false) }
