package com.wickmoth.lakekeeps.game.littlebird

/**
 * The case flags of Little Bird. Each is set once, when the thing it names has happened, and
 * everything on the desk, the board and Sam's phone follows from which are set.
 */
object Flag {
    /** The office is laid out and the case can begin: Amy writes. */
    const val CASE_OPEN = "case.open"

    /** Amy's first conversation is over, and Sam has taken the case. */
    const val INTAKE_DONE = "amy.intake"

    /** How Sam took the case (choice C1): steadied her, took the facts, or promised to fix it. */
    const val C1_STEADY = "c1.steady"
    const val C1_FACTS = "c1.facts"
    const val C1_PROMISE = "c1.promise"

    /** Every symptom card is in its column on the board. */
    const val SORTED = "board.sorted"

    /** Amy sent her battery screen, and the conversation about it is over. */
    const val BATTERY_SENT = "amy.battery"

    /** Pinned in the Lab: the overnight band on the battery screen, and (optionally) its odd line. */
    const val PIN_BAND = "pin.band"
    const val PIN_SYNC = "pin.sync"

    /** Amy sent her app list and the app's permissions, and that conversation is over. */
    const val APPS_SENT = "amy.apps"

    /** Pinned in the Lab: the impostor in the app list, and (optionally) its permissions and install date. */
    const val PIN_IMPOSTOR = "pin.impostor"
    const val PIN_PERMISSIONS = "pin.permissions"
    const val PIN_INSTALLED = "pin.installed"

    /** Pinned in the Lab (optional): the package name in the app info's fine print, for later. */
    const val PIN_PACKAGE = "pin.package"

    /** The tracker is tied to both things it explains: the end of Act 1. */
    const val ACT1_DONE = "act1.done"

    /** A symptom card sorted into its column. */
    fun sorted(card: String) = "sort.$card"

    /** A link on the board that held. */
    fun linked(link: String) = "link.$link"

    /** A memo or notebook page the player has already been shown. */
    fun seen(id: String) = "seen.$id"
}
