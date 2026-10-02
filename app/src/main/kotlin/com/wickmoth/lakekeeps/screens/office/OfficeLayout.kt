package com.wickmoth.lakekeeps.screens.office

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.wickmoth.lakekeeps.game.littlebird.Column
import com.wickmoth.lakekeeps.game.littlebird.Symptom

/**
 * Where everything sits in Sam's office, in design dp on the 360 x 800 frame: the board above
 * [DESK_TOP], the desk below. The board starts almost bare and fills in these places as the
 * case goes.
 */
internal object OfficeLayout {
    const val DESK_TOP = 648f

    /** What Sam is after right now, on a manila tag in the corner. */
    val tag = Rect(10f, 10f, 196f, 68f)

    /** A photograph turned to face the board. He won't take it down. */
    val photo = Rect(300f, 18f, 346f, 72f)
    const val PHOTO_TILT = 6f

    /** The case's name in chalk, and the client's photo beside it. */
    val title = Offset(18f, 90f)
    val client = Rect(152f, 84f, 220f, 166f)
    const val CLIENT_TILT = -2.5f

    /** The ex, in faint chalk over everything: not pinned, because nothing's proven. */
    val shadow = Offset(236f, 112f)

    /** The three columns the symptoms sort into, and their headings. */
    private val columnCenters = mapOf(Column.Machine to 62f, Column.Person to 180f, Column.House to 298f)
    const val COLUMN_W = 112f
    const val HEAD_Y = 180f
    const val SLOTS_TOP = 222f
    const val SLOT_STEP = 50f
    val cardSize = Size(104f, 44f)

    fun columnCenter(column: Column): Float = columnCenters.getValue(column)

    /** Where a dragged card counts as dropped in [column]. */
    fun dropZone(column: Column): Rect {
        val c = columnCenter(column)
        return Rect(c - COLUMN_W / 2f, HEAD_Y - 14f, c + COLUMN_W / 2f, 540f)
    }

    /** The place of [symptom] once sorted: the next free slot down its column, in card order. */
    fun slot(symptom: Symptom): Rect {
        val index = Symptom.entries.filter { it.column == symptom.column }.indexOf(symptom)
        val c = columnCenter(symptom.column)
        val top = SLOTS_TOP + index * SLOT_STEP
        return Rect(c - cardSize.width / 2f, top, c + cardSize.width / 2f, top + cardSize.height)
    }

    /** Where the cards wait to be sorted: two rows along the bottom of the board, a little smaller. */
    val traySize = Size(84f, 40f)

    fun traySlot(symptom: Symptom): Rect {
        val i = symptom.ordinal
        val (x, y) = if (i < 4) (4f + i * 88f) to 552f else (48f + (i - 4) * 88f) to 600f
        return Rect(x, y, x + traySize.width, y + traySize.height)
    }

    /** Evidence pinned under the machine column as it comes in, with Sam's chalk beside it. */
    val bandPrint = Rect(8f, 330f, 116f, 394f)
    val syncNote = Rect(14f, 400f, 110f, 428f)
    val trackerPrint = Rect(8f, 438f, 116f, 514f)
    val permissionsNote = Rect(14f, 522f, 110f, 550f)
    val installedNote = Rect(14f, 556f, 110f, 584f)
    val packageNote = Rect(14f, 590f, 110f, 618f)

    val bandDeduction = Offset(126f, 336f)
    val trackerDeduction = Offset(126f, 446f)
    val actDeduction = Offset(126f, 604f)

    /** The spool of red thread: tap it to tie two things on the board together. */
    val spool = Rect(306f, 548f, 350f, 592f)

    /** Where Sam's second thoughts appear, on a strip of paper. */
    val note = Rect(36f, 494f, 324f, 538f)

    // The desk

    val notebook = Rect(20f, 664f, 90f, 756f)
    const val NOTEBOOK_TILT = -6f
    val tray = Rect(104f, 670f, 204f, 746f)
    const val TRAY_TILT = 2f
    val mug = Rect(214f, 728f, 264f, 784f)
    val steam = Offset(211f, 678f)
}
