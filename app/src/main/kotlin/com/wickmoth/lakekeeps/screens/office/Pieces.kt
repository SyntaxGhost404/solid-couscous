package com.wickmoth.lakekeeps.screens.office

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.wickmoth.lakekeeps.game.case.CaseProgress
import com.wickmoth.lakekeeps.game.littlebird.Flag
import com.wickmoth.lakekeeps.game.littlebird.LittleBird
import com.wickmoth.lakekeeps.game.littlebird.Symptom

/** The things on the board a thread can be tied to: where their pins are and what they're called. */
internal object Pieces {
    private fun symptom(id: String) = Symptom.byId(id)

    /** Whether [id] is on the board yet. */
    fun present(progress: CaseProgress, id: String): Boolean = when (id) {
        LittleBird.AMY -> progress.has(Flag.INTAKE_DONE)
        LittleBird.BAND -> progress.has(Flag.PIN_BAND)
        LittleBird.TRACKER -> progress.has(Flag.PIN_IMPOSTOR)
        else -> symptom(id)?.let { progress.has(Flag.sorted(it.id)) } ?: false
    }

    fun all(progress: CaseProgress): List<String> =
        (listOf(LittleBird.AMY, LittleBird.BAND, LittleBird.TRACKER) + Symptom.entries.map { it.id }).filter { present(progress, it) }

    fun bounds(id: String): Rect = when (id) {
        LittleBird.AMY -> OfficeLayout.client
        LittleBird.BAND -> OfficeLayout.bandPrint
        LittleBird.TRACKER -> OfficeLayout.trackerPrint
        else -> OfficeLayout.slot(symptom(id) ?: Symptom.Noon)
    }

    /** The pin at the top of [id], where its threads are tied. */
    fun anchor(id: String): Offset = bounds(id).let { Offset(it.center.x, it.top + PIN_DROP) }

    fun name(id: String): String = when (id) {
        LittleBird.AMY -> "Amy Hart"
        LittleBird.BAND -> "the 2 to 4 AM band"
        LittleBird.TRACKER -> "System Sync Service"
        else -> symptom(id)?.text ?: id
    }

    /** How far below a piece's top edge its pin goes in. */
    const val PIN_DROP = 4f
}
