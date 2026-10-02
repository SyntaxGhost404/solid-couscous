package com.wickmoth.lakekeeps.game.messages

import androidx.compose.ui.graphics.Color
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.game.Owner

/**
 * Placeholder conversations for the prototype. The story is not final: edit freely. Board
 * characters reuse their polaroid portraits; everyone else gets a placeholder face.
 */
object Threads {
    /** The game's "now": Saturday, 4:12 PM. Each live message moves the clock on 20 seconds. */
    const val NOW_MINUTES = 16 * 60 + 12
    const val SECONDS_PER_LIVE_LINE = 20

    private val Mum = Contact("Mum", Face.Initial("M", Color(0xFF8C6A4F)), "mobile", "Mobile · 555-0118")

    val PrivateNumber = Thread(
        id = "theo/private",
        contact = Contact("Private number", Face.None, "number withheld", "No caller ID"),
        live = script {
            says("Theo.")
            ask(
                reply("Who is this?") { says("Someone who wants her found as much as you do.") },
                reply("Do I know you?") { says("You've walked past me a hundred times.") },
            )
            says("You have her phone. Read it before someone else does.")
            ask(
                reply("How do you know that?") { says("Small town. People watch the pier.") },
                reply("Why are you helping me?") { says("Who said I was helping?") },
            )
            says("Start with the night of the bonfire.")
            says("And keep the police out of this.")
        },
    )

    private val theo = listOf(
        PrivateNumber,
        Thread(
            id = "theo/priya",
            contact = Contact("Priya", Face.Photo(R.drawable.portrait_priya), "mobile", "Mobile · 555-0142"),
            history = listOf(
                them("any news?", Day.Yesterday, "9:14 PM"),
                me("nothing yet. the police say it's too early", Day.Yesterday, "9:20 PM"),
                them("she wouldn't just leave. not without telling me", Day.Yesterday, "9:21 PM"),
                me("I know", Day.Yesterday, "9:23 PM"),
                them("call me if you hear anything", Day.Yesterday, "9:24 PM"),
            ),
        ),
        Thread(
            id = "theo/rosa",
            contact = Contact("Rosa", Face.Photo(R.drawable.portrait_rosa), "boathouse", "Work · 555-0160"),
            history = listOf(
                me("Did you see Mira on the pier that night?", Day.Yesterday, "10:02 AM"),
                them("I told the police everything I saw.", Day.Yesterday, "11:40 AM"),
                them("The boathouse stays shut until they're done. Come by Monday.", Day.Yesterday, "11:41 AM"),
            ),
        ),
        Thread(
            id = "theo/mum",
            contact = Mum,
            history = listOf(
                them("Did you eat anything today?", Day.Today, "9:05 AM"),
                them("Mira's mother called again. Go and see her.", Day.Today, "9:06 AM"),
            ),
            readHistory = 1,
        ),
    )

    private val mira = listOf(
        Thread(
            id = "mira/priya",
            contact = Contact("Priya", Face.Photo(R.drawable.portrait_priya), "mobile", "Mobile · 555-0142"),
            history = listOf(
                them("you coming tonight?", Day.Thursday, "6:58 PM"),
                me("obviously. save me a spot by the fire", Day.Thursday, "7:02 PM"),
                them("where did you go??", Day.Thursday, "10:06 PM"),
                them("mira answer me", Day.Thursday, "10:31 PM"),
            ),
            readHistory = 2,
        ),
        Thread(
            id = "mira/felix",
            contact = Contact("Felix", Face.Photo(R.drawable.portrait_felix), "mobile", "Mobile · 555-0175"),
            history = listOf(them("you up?", Day.Thursday, "11:52 PM")),
            readHistory = 0,
        ),
        Thread(
            id = "mira/theo",
            contact = Contact("Theo", Face.Initial("T", Color(0xFF2F7F7A)), "mobile", "Mobile · 555-0103"),
            history = listOf(
                them("need a ride to the bonfire?", Day.Thursday, "5:40 PM"),
                me("marcus is driving me. see you there", Day.Thursday, "5:52 PM"),
                them("save me a marshmallow", Day.Thursday, "5:53 PM"),
            ),
        ),
        Thread(
            id = "mira/mum",
            contact = Mum.copy(detail = "Mobile · 555-0121"),
            history = listOf(them("Text me when you're home.", Day.Thursday, "8:47 PM")),
        ),
        Thread(
            id = "mira/heron",
            // the board's photo for this account is its no-photo placeholder, so it shows whole
            contact = Contact("night_heron", Face.Photo(R.drawable.portrait_heron, closeUp = false), "online contact", "Username · night_heron"),
            history = listOf(
                them("did you tell anyone?", Day.Wednesday, "11:14 PM"),
                me("no. not even priya", Day.Wednesday, "11:20 PM"),
                them("good. thursday then", Day.Wednesday, "11:21 PM"),
            ),
        ),
    )

    fun of(owner: Owner): List<Thread> = when (owner) {
        Owner.Theo -> theo
        Owner.Mira -> mira
    }

    val all: List<Thread> = theo + mira

    fun byId(id: String): Thread? = all.firstOrNull { it.id == id }
}
