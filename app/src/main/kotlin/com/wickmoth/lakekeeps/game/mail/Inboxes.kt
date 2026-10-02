package com.wickmoth.lakekeeps.game.mail

import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.messages.clockMinutes

/**
 * Placeholder inboxes for the prototype. The story is not final: edit freely. Senders and
 * businesses are invented, and addresses use the reserved .example domain.
 */
object Inboxes {
    private fun mail(
        id: String,
        sender: String,
        address: String,
        subject: String,
        daysAgo: Int,
        time: String,
        body: String,
        picture: String? = null,
        attachment: String? = null,
        unread: Boolean = false,
    ) = Email(id, sender, address, subject, body.trimIndent(), daysAgo, clockMinutes(time), picture, attachment, unread)

    private val mira = listOf(
        mail(
            "mira/picnook", "Picnook", "hello@picnook.example", "Priya tagged you in 3 photos", 0, "8:12 AM",
            """
            Priya tagged you in 3 photos from bonfire night at the cove.

            Open Picnook to see them, or to untag yourself.

            You're getting this email because Picnook notifications are on.
            """,
            unread = true,
        ),
        mail(
            "mira/college", "Silverpine College", "attendance@silverpine.example", "Absence recorded: Friday 3 November", 1, "9:30 AM",
            """
            Dear Mira,

            You were marked absent from all of your classes on Friday 3 November. If this is a mistake, or you were unwell, please reply to this email or call the attendance office.

            Attendance Office
            Silverpine College
            """,
            unread = true,
        ),
        mail(
            "mira/priya", "Priya", "priya.n@post.example", "bonfire playlist", 2, "5:47 PM",
            """
            made the playlist!! it's mostly your songs anyway

            bring the speaker and a blanket, Owen says it's going to be freezing by the water

            and do NOT let Marcus near the music this time
            """,
        ),
        mail(
            "mira/fair", "Pier Lights Fair", "tickets@pierlights.example", "Your ride passes for Saturday", 3, "2:15 PM",
            """
            Thanks for your order!

            Show this email at the gate on Saturday 4 November. Each pass is good for one ride on the big wheel after dark.

            Pier Lights Fair
            """,
            picture = "Two ride passes for the big wheel",
        ),
        mail(
            "mira/arden", "Ms Arden", "h.arden@silverpine.example", "Portfolio feedback", 4, "4:05 PM",
            """
            Hi Mira,

            I've attached my notes on your portfolio. The lake series is the strongest work you've done this year. Keep going with it.

            See you on Thursday,
            H. Arden
            """,
            attachment = "portfolio_notes.pdf",
        ),
        mail(
            "mira/ledger", "Lantern Ledger", "news@lanternledger.example", "This week by the lake", 5, "7:00 AM",
            """
            The boathouse is open again after the summer repairs, and the pier lights go back up for the fair this weekend.

            The council is asking for volunteers to clear the shore path before the first frost.

            You're receiving this because you subscribed to the Lantern Ledger weekly.
            """,
        ),
        mail(
            "mira/mooncrush", "Mooncrush", "no-reply@mooncrush.example", "Someone new likes your profile", 6, "10:44 PM",
            """
            You have a new admirer.

            Open Mooncrush to find out who.
            """,
        ),
    )

    private val theo = listOf(
        mail(
            "theo/ledger", "Lantern Ledger", "news@lanternledger.example", "Search volunteers wanted on Sunday", 0, "7:00 AM",
            """
            Volunteers are meeting at the boathouse at 9 am on Sunday to search the shore path and the woods behind the pier.

            Bring warm clothes, a torch and a charged phone. Rosa will be handing out maps.

            You're receiving this because you subscribed to the Lantern Ledger weekly.
            """,
            picture = "Map of the search area along the shore",
            unread = true,
        ),
        mail(
            "theo/police", "Lakeshore Police", "enquiries@lakeshore-police.example", "Re: Mira (reference LK-1102)", 1, "9:52 PM",
            """
            Thank you for getting in touch about Mira. We have noted your concerns under reference LK-1102.

            Most people who go missing come home within a day or two. If she has not been in touch by Saturday evening, please call us back and quote this reference.

            Lakeshore Police
            """,
        ),
        mail(
            "theo/rosa", "Rosa", "rosa@boathouse.example", "Saturday shift", 2, "3:12 PM",
            """
            Theo,

            You're on with Owen on Saturday morning. Doors open at nine, so be there by half eight.

            Rosa
            """,
        ),
        mail(
            "theo/library", "Silverpine College Library", "library@silverpine.example", "1 item overdue", 3, "8:00 AM",
            """
            This is a reminder that the following item is now overdue:

            Birds of the Lake Country

            Please return it to the library desk, or renew it online.
            """,
        ),
        mail(
            "theo/prints", "Pressbox Prints", "orders@pressbox.example", "Your prints are ready to collect", 6, "1:20 PM",
            """
            Good news: your order of 12 prints is ready to collect from the shop on Harbour Street.

            Your receipt is attached.

            Pressbox Prints
            """,
            attachment = "receipt_4471.pdf",
        ),
    )

    /** [owner]'s inbox, newest first. */
    fun of(owner: Owner): List<Email> = when (owner) {
        Owner.Theo -> theo
        Owner.Mira -> mira
    }

    val all: List<Email> = mira + theo

    fun byId(id: String): Email? = all.firstOrNull { it.id == id }
}
