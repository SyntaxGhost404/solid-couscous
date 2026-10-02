package com.wickmoth.lakekeeps.game.littlebird

import androidx.compose.runtime.Immutable
import com.wickmoth.lakekeeps.game.case.CaseProgress

/** A file someone sends Sam: [id] names it in scripts, [title] is how the Vault lists it. */
enum class Evidence(val id: String, val title: String, val fileName: String, val from: String) {
    Battery("battery", "Battery, last 24 hours", "Screenshot_20250908_2014.png", "Amy Hart"),
    Apps("apps", "Apps, with system apps shown", "Screenshot_20250908_2040.png", "Amy Hart"),
    Permissions("permissions", "System Sync Service, app info", "Screenshot_20250908_2041.png", "Amy Hart"),
    ;

    companion object {
        fun byId(id: String): Evidence? = entries.firstOrNull { it.id == id }
    }
}

/**
 * What the player is working towards. It is current until [done] is set; [hints] are Sam's own
 * margin notes, given one at a time: a nudge, a pointer, then the answer.
 */
@Immutable
data class Objective(val id: String, val text: String, val done: String?, val hints: List<String>)

/** A line in Sam's case notes, written once [flag] is set. */
@Immutable
data class Finding(val flag: String, val text: String)

/** A voice memo Sam records to himself, filed to the notebook once [flag] is set. */
@Immutable
data class Memo(val id: String, val flag: String, val title: String, val text: String)

/** The three ways a symptom can be read when Amy's symptoms are sorted on the board. */
enum class Column(val heading: String, val thread: String) {
    Machine("a machine could do this", "what's on her phone?"),
    Person("a person is doing this", "who calls? who takes the pictures?"),
    House("the house is doing this", "who runs the house?"),
    ;

    /** The heading as chalked on the board, over two lines. */
    val chalked: String get() = heading.split(' ').let { words -> words.take(2).joinToString(" ") + "\n" + words.drop(2).joinToString(" ") }
}

/** One of Amy's symptoms, as a card for the board; [notes] are Sam's second thoughts on a wrong column. */
enum class Symptom(val id: String, val text: String, val column: Column, val notes: Map<Column, String>) {
    Noon(
        "noon", "phone dead by noon", Column.Machine,
        mapOf(Column.Person to "Nobody needs to touch a phone to drain it.", Column.House to "That's her phone, not her house."),
    ),
    Whereabouts(
        "where", "they know where she's been", Column.Machine,
        mapOf(Column.Person to "Somebody knows. Something is telling them.", Column.House to "The house doesn't follow her to the pharmacy."),
    ),
    Calls(
        "calls", "silent calls at odd hours", Column.Person,
        mapOf(Column.Machine to "Somebody picks those hours. Somebody stays on the line.", Column.House to "Those come to her phone, wherever she is."),
    ),
    Photos(
        "photos", "photos of her own house", Column.Person,
        mapOf(Column.Machine to "Somebody stood there and took them.", Column.House to "Somebody took those of the house. The house didn't."),
    ),
    Lights(
        "lights", "den lights switch on", Column.House,
        mapOf(Column.Machine to "Not her phone. Her house.", Column.Person to "Nobody was in the room."),
    ),
    Thermostat(
        "thermostat", "thermostat swings", Column.House,
        mapOf(Column.Machine to "Not her phone. Her house.", Column.Person to "Nobody was at the dial."),
    ),
    Lock(
        "lock", "front lock clicks over", Column.House,
        mapOf(Column.Machine to "Not her phone. Her door.", Column.Person to "She was home. Nobody was at the door."),
    ),
    ;

    companion object {
        fun byId(id: String): Symptom? = entries.firstOrNull { it.id == id }
    }
}

/** The words a link on the board can say. Only true links hold. */
enum class Verb(val text: String) {
    Explains("explains"),
    Sent("sent"),
    SameAs("is the same as"),
}

/** A link that holds: [a] [verb] [b], between two things on the board (either way round). */
@Immutable
data class Link(val id: String, val a: String, val verb: Verb, val b: String)

/** Little Bird's Act 1: objectives, notes and memos, and what the board can hold. */
object LittleBird {
    const val TITLE = "Little Bird"

    /** Board pieces the player can link (besides the symptom cards, which are linkable once sorted). */
    const val TRACKER = "tracker"
    const val BAND = "band"
    const val AMY = "amy"

    val objectives = listOf(
        Objective(
            "intake", "Answer Amy in Messages", Flag.INTAKE_DONE,
            listOf(
                "The phone's going. Somebody needs a detective.",
                "Pick up my phone from the desk and open Messages.",
                "Open Amy Hart's conversation and answer her.",
            ),
        ),
        Objective(
            "sort", "Sort Amy's symptoms on the board", Flag.SORTED,
            listOf(
                "What could a program do on its own, and what takes somebody choosing to do it?",
                "A battery can drain without a hand on it. A photo can't take itself.",
                "Machine: dead by noon, where she's been. Person: calls, photos. House: lights, thermostat, lock.",
            ),
        ),
        Objective(
            "battery", "Ask Amy for her battery screen", Flag.BATTERY_SENT,
            listOf(
                "Ask her for something a scared person can find in a minute.",
                "Open Amy's conversation. Plain steps, no jargon.",
                "Send her the message that starts 'Open Settings'.",
            ),
        ),
        Objective(
            "band", "Find what drains Amy's battery", Flag.PIN_BAND,
            listOf(
                "Overnight, with the phone idle. What has a reason to wake up at 2 a.m.?",
                "Open her battery screenshot from the tray on the desk. Look at the hours she was asleep.",
                "In the Lab, press and hold the bars between 2 and 4 AM.",
            ),
        ),
        Objective(
            "apps", "Ask Amy about System Sync Service", Flag.APPS_SENT,
            listOf(
                "Find out what that thing is without touching her phone.",
                "Open Amy's conversation and walk her to her app list.",
                "Send either message about the system apps, then let her send what she finds.",
            ),
        ),
        Objective(
            "impostor", "Find the impostor in Amy's apps", Flag.PIN_IMPOSTOR,
            listOf(
                "Compare that grey gear to the other system apps. What does a real one never show you?",
                "Real system apps don't show a version number or a data bill.",
                "Open the app list from the tray, then press and hold System Sync Service.",
            ),
        ),
        Objective(
            "connect", "Connect the tracker to what it explains", Flag.ACT1_DONE,
            listOf(
                "Which of her symptoms could a hidden app cause all by itself?",
                "Tap the spool of thread on the board, then the tracker and a symptom card.",
                "Link System Sync Service to 'phone dead by noon' and to 'they know where she's been', with 'explains'.",
            ),
        ),
        Objective(
            "act2", "Find out who sells a tracker like this", null,
            listOf(
                "The rest of the case isn't in this build yet.",
                "Act 2 picks up from the tracker's real name.",
                "That's where Act 1 ends.",
            ),
        ),
    )

    /** The first objective whose flag isn't set yet. */
    fun objective(progress: CaseProgress): Objective =
        objectives.firstOrNull { it.done == null || !progress.has(it.done) } ?: objectives.last()

    val findings = listOf(
        Finding(Flag.INTAKE_DONE, "Amy Hart, 34. Dental hygienist. One son, Toby, 7."),
        Finding(Flag.INTAKE_DONE, "Ex-husband Ryan Hart, out on parole. She is sure it's him."),
        Finding(Flag.INTAKE_DONE, "Nick, her boyfriend of five months. Put in her cameras and smart locks."),
        Finding(Flag.INTAKE_DONE, "Phone dead by noon. People know where she's been. Silent calls. Photos of her house. Lights, thermostat and lock with a mind of their own."),
        Finding(Flag.SORTED, "Three threads: what's on her phone, who calls and takes the pictures, who runs the house."),
        Finding(Flag.PIN_BAND, "Something on her phone runs from 2 to 4 AM every night, screen off."),
        Finding(Flag.PIN_IMPOSTOR, "System Sync Service is not a system app. It shows a version and a data bill. Installed 9 April, five months ago."),
        Finding(Flag.PIN_PERMISSIONS, "It can see her location all the time, read her messages, use her microphone."),
        Finding(Flag.PIN_PACKAGE, "In its fine print, a package name: com.keypr.familysafe. Worth a search."),
        Finding(Flag.ACT1_DONE, "Someone put a tracker on her phone and dressed it up as part of the phone."),
    )

    /** How the intake went, by the way Sam took the case. */
    fun intakeNote(progress: CaseProgress): String = when {
        progress.has(Flag.C1_STEADY) -> "She steadied once I slowed down. Let her set the pace."
        progress.has(Flag.C1_FACTS) -> "Got the facts. She's still brittle."
        progress.has(Flag.C1_PROMISE) -> "I promised to make it stop. Careful. She leans hard."
        else -> ""
    }

    val memos = listOf(
        Memo(
            "intake", Flag.INTAKE_DONE, "After the intake",
            "Everybody thinks the phone is the evidence. The phone is the alibi. The evidence is who needed her to believe it.",
        ),
        Memo(
            "band", Flag.PIN_BAND, "The battery",
            "Two to four in the morning, every night, phone face down on a nightstand. Nothing that belongs on a phone wakes up like that.",
        ),
        Memo(
            "act1", Flag.ACT1_DONE, "The tracker",
            "Five months ago somebody put this on her phone and dressed it up as part of the furniture. Not a glitch. Not a man rattling a door. A leash. Who's holding the other end?",
        ),
    )

    val links = listOf(
        Link("tracker-noon", TRACKER, Verb.Explains, Symptom.Noon.id),
        Link("tracker-where", TRACKER, Verb.Explains, Symptom.Whereabouts.id),
    )

    /** Why a link between [a] and [b] doesn't hold, when there's more to say than that. */
    fun linkNote(a: String, b: String, verb: Verb): String {
        val pair = setOf(a, b)
        if (links.any { setOf(it.a, it.b) == pair }) return "Right pins. Wrong word."
        return when {
            TRACKER in pair && Symptom.Calls.id in pair -> "A tracker listens. It doesn't dial."
            TRACKER in pair && Symptom.Photos.id in pair -> "Somebody stood in her yard for those."
            TRACKER in pair && pair.any { Symptom.byId(it)?.column == Column.House } -> "That's the house, not her phone. Not yet."
            else -> "Does not hold."
        }
    }

    /**
     * Two picked pieces in the order a sentence about them reads, whichever was picked first: a
     * person or a find before one of Amy's symptoms.
     */
    fun sentence(a: String, b: String): Pair<String, String> =
        if (Symptom.byId(a) != null && Symptom.byId(b) == null) b to a else a to b

    /** Whether [a] [verb] [b] holds, either way round. */
    fun holds(a: String, b: String, verb: Verb): Link? =
        links.firstOrNull { it.verb == verb && setOf(it.a, it.b) == setOf(a, b) }

    /** Act 1 closes once every link that holds has been made. */
    fun act1Done(progress: CaseProgress): Boolean = links.all { progress.has(Flag.linked(it.id)) }
}
