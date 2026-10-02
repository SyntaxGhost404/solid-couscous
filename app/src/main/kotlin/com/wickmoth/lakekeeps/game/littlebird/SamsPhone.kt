package com.wickmoth.lakekeeps.game.littlebird

import androidx.compose.ui.graphics.Color
import com.wickmoth.lakekeeps.game.gallery.Photo
import com.wickmoth.lakekeeps.game.mail.Email
import com.wickmoth.lakekeeps.game.messages.Contact
import com.wickmoth.lakekeeps.game.messages.Day
import com.wickmoth.lakekeeps.game.messages.Face
import com.wickmoth.lakekeeps.game.messages.Thread
import com.wickmoth.lakekeeps.game.messages.clockMinutes
import com.wickmoth.lakekeeps.game.messages.me
import com.wickmoth.lakekeeps.game.messages.script
import com.wickmoth.lakekeeps.game.messages.them
import com.wickmoth.lakekeeps.game.phone.Call
import com.wickmoth.lakekeeps.game.phone.CallKind
import com.wickmoth.lakekeeps.game.phone.PhoneContact
import com.wickmoth.lakekeeps.game.phone.dialable

/**
 * Sam Novak's phone in Little Bird: his conversations (Amy's plays out as the case goes), his
 * address book and calls, his mail and his photos. Numbers are in the 555-01xx range set aside
 * for fiction, and addresses use the reserved .example domain. Photos are placeholders until the
 * case's art is made.
 */
object SamsPhone {
    private val Dana = Contact("Dana Brooks", Face.Initial("D", Color(0xFF5B6CA8)), "Desert Legal Aid", "Work · 555-0134")
    private val Amy = Contact("Amy Hart", Face.Initial("A", Color(0xFFA0525E)), "mobile", "Mobile · 555-0181")
    private val Rosa = Contact("Rosa Delgado", Face.Initial("R", Color(0xFF8C6A4F)), "next door", "Mobile · 555-0156")
    private val Shaw = Contact("Erin Shaw", Face.Initial("E", Color(0xFF6A8A4C)), "Verano County Sheriff", "Work · 555-0199")

    private val Thursday = Day(-4)

    /** Amy's conversation with Sam, a session at a time as the case moves on. */
    val AmyHart = Thread(
        id = "sam/amy",
        contact = Amy,
        live = script {
            // Event 1, the intake: she reaches out by message, not a call
            gate(Flag.CASE_OPEN, Day.Today, "5:46 PM")
            says("Is this Sam Novak? Dana Brooks gave me this number.")
            ask(
                reply("This is Sam. Dana said you'd write."),
                reply("Speaking. Take your time."),
            )
            says("Sorry. I've typed this out ten times and deleted it.")
            says("Someone is watching me. I know how that sounds.")
            says("My phone is dead by noon, even on days I barely touch it.")
            says("People know where I've been. Nick brought up a parking lot I never told him about.")
            says("The pharmacy said my husband picked up my refill. I don't have a husband anymore.")
            says("I get calls from numbers I don't know. Nobody talks. Sometimes I can hear a room for a second, then nothing.")
            says("Someone texted me pictures of my own house.")
            says("And the house does things. The den lights come on by themselves. The thermostat swings. The front lock clicked over twice while I was home.")
            ask(
                reply("Slow down. I believe you. We'll go one thing at a time.") {
                    mark(Flag.C1_STEADY)
                    says("Okay.")
                    says("Okay. Thank you. Nobody says that.")
                },
                reply("Dates and times. Start with the calls.") {
                    mark(Flag.C1_FACTS)
                    says("A few a week. Late. One, three in the morning.")
                    says("I didn't write them down. I'm sorry.")
                },
                reply("I'll make this stop. You have my word.") {
                    mark(Flag.C1_PROMISE)
                    says("You can do that?")
                    says("Please. I can't keep living like this.")
                },
            )
            says("It's Ryan. My ex-husband. He's out on parole.")
            says("He hurt me. Years ago. I know it's him, and I know he won't stop at me.")
            says("I have a little boy. Toby. He's seven.")
            ask(
                reply("Who's Nick?"),
                reply("Is anyone helping you?"),
            )
            says("Nick is my boyfriend. Five months. He's the only thing holding me together.")
            says("He put in cameras and smart locks for us. He helps me log everything.")
            says("He's the one who said I should get a protective order.")
            says("I need proof a judge will accept, and I can't pay much. Dana said she'd cover part.")
            ask(
                reply("We'll work it out. First, how this goes."),
                reply("Money later. Three rules first."),
            )
            writes("Don't reset your phone. Don't delete anything. Don't hand it to anyone.")
            writes("If someone is listening, I don't want them to know we're looking.")
            writes("Keep it with you and use it like normal. For the order, it has to stay yours and untouched.")
            writes("We do this together. I tell you where to look, you send me what you find. I never need to hold it.")
            says("Okay. I can do that.")
            ask(
                reply("I'll text you tonight."),
                reply("Keep the phone on you. I'll be in touch."),
            )
            says("Okay. Thank you, Mr. Novak.")
            mark(Flag.INTAKE_DONE)

            // Event 3, coached capture: the battery screen
            gate(Flag.SORTED, Day.Today, "8:14 PM")
            ask(
                reply("Open Settings, then Battery, then 'Last 24 hours'. Screenshot it and send it to me."),
                reply("Pull your per-app battery attribution for the last 24 hours and export it.") {
                    says("I'm sorry. I don't know what that means.")
                    ask(reply("Open Settings, then Battery, then 'Last 24 hours'. Screenshot it and send it to me."))
                },
            )
            says("Okay. One second.")
            sends(Evidence.Battery.id, "Is this it?")
            ask(
                reply("That's it. Don't change anything."),
                reply("Good. Leave everything as it is."),
            )
            says("Okay. Toby's asleep. I'm keeping it on me.")
            mark(Flag.BATTERY_SENT)

            // Event 4, coached capture: the app that doesn't belong
            gate(Flag.PIN_BAND, Day.Today, "8:39 PM")
            ask(
                reply("One more. Settings, then Apps. Show system apps and send me the list."),
                reply("Settings, Apps, show system apps. Screenshot the list for me."),
            )
            says("There are so many.")
            sends(Evidence.Apps.id)
            writes("Now open System Sync Service and send me its permissions.")
            says("Okay.")
            sends(Evidence.Permissions.id, "It says it can use my microphone?")
            says("Why would a system thing need my microphone?")
            ask(
                reply("Don't touch it. Leave it exactly as it is."),
                reply("Leave it alone. That part matters."),
            )
            says("Okay. I won't.")
            says("Is it him?")
            ask(
                reply("I don't know yet. I will."),
                reply("Not yet. Get some sleep."),
            )
            says("Okay. Goodnight.")
            mark(Flag.APPS_SENT)
        },
    )

    /** Dana sent Amy here, and writes again once Sam has taken the case. */
    val DanaBrooks = Thread(
        id = "sam/dana",
        contact = Dana,
        history = listOf(
            them("Coffee next week? I might have one for you.", Thursday, "3:20 PM"),
            me("Always.", Thursday, "3:41 PM"),
            them("Sending you someone. Her name is Amy. Please be gentle, she is barely holding on.", Day.Today, "4:51 PM"),
        ),
        readHistory = 2,
        live = script {
            gate(Flag.INTAKE_DONE, Day.Today, "6:02 PM")
            says("She called me after. She's going to try. Thank you for taking her, Sam.")
        },
    )

    val threads = listOf(
        AmyHart,
        DanaBrooks,
        Thread(
            id = "sam/rosa",
            contact = Rosa,
            history = listOf(
                them("Your air conditioner is screaming again. I can hear it through the wall.", Day.Yesterday, "9:12 PM"),
                me("It's fine.", Day.Yesterday, "9:30 PM"),
                them("It's 104 at night, Sam. It is not fine.", Day.Yesterday, "9:31 PM"),
                them("Tamales on your step. Eat them before the heat does.", Day.Today, "12:10 PM"),
            ),
        ),
        Thread(
            id = "sam/shaw",
            contact = Shaw,
            history = listOf(me("Saw the news. Congratulations, Detective.", Day(-74), "7:05 PM")),
        ),
    )

    val contacts = listOf(
        PhoneContact("Dana Brooks", "555-0134", "Work"),
        PhoneContact("Amy Hart", "555-0181", "Mobile"),
        PhoneContact("Rosa Delgado", "555-0156", "Mobile"),
        PhoneContact("Erin Shaw", "555-0199", "Work"),
    )

    private fun call(kind: CallKind, number: String, daysAgo: Int, time: String) =
        Call(dialable(number), kind, daysAgo, clockMinutes(time))

    /** Sam's calls from before the case, newest first. */
    val history = listOf(
        call(CallKind.Incoming, "555-0134", 1, "2:05 PM"),
        call(CallKind.Outgoing, "555-0156", 1, "9:34 PM"),
        call(CallKind.Missed, "555-0110", 2, "10:14 AM"),
        call(CallKind.Outgoing, "555-0134", 4, "3:44 PM"),
        call(CallKind.Incoming, "555-0156", 5, "1:20 PM"),
    )

    private fun mail(id: String, sender: String, address: String, subject: String, daysAgo: Int, time: String, body: String, unread: Boolean = false) =
        Email(id, sender, address, subject, body.trimIndent(), daysAgo, clockMinutes(time), unread = unread)

    val inbox = listOf(
        mail(
            "sam/referral", "Dana Brooks", "dana.brooks@desertlegalaid.example", "Referral: Amy Hart", 0, "4:53 PM",
            """
            Sam,

            As we discussed. Amy Hart, 34, dental hygienist, one son, Toby, who is 7. She believes her ex-husband, Ryan Hart, is watching her. He is out on parole.

            She is frightened, and whatever this turns out to be, she is not imagining it.

            We can cover part of your fee from the protective order fund. Send anything you find to me first, so it holds up.

            Dana Brooks
            Advocate, Desert Legal Aid
            """,
            unread = true,
        ),
        mail(
            "sam/heat", "Verano County Alerts", "alerts@verano-county.example", "Excessive heat warning through Thursday", 0, "6:00 AM",
            """
            Highs near 110°F. Overnight lows near 88°F. Limit time outdoors and check on neighbours who live alone.

            Monsoon storms are possible late in the week, with blowing dust and flash flooding in washes and low crossings.
            """,
        ),
        mail(
            "sam/plaza", "Ocotillo Plaza Management", "office@ocotilloplaza.example", "Unit 114: air conditioning service", 1, "11:02 AM",
            """
            Hello,

            Our technician can look at the rooftop unit for Unit 114 on Thursday between 8 AM and 4 PM. Please make sure the back room is accessible.

            Ocotillo Plaza Management
            """,
        ),
        mail(
            "sam/licence", "State Licensing Board", "noreply@pi-licensing.example", "Your licence renewal is due", 5, "9:00 AM",
            """
            Your private investigator licence expires on 30 September. Renew online before then to avoid a lapse in your licence.
            """,
        ),
    )

    val photos = listOf(
        Photo("sam/tamales", "Rosa's tamales on the step", 0),
        Photo("sam/roof", "The rooftop air conditioner, open", 1),
        Photo("sam/plaza", "Ocotillo Plaza at dusk", 3),
        Photo("sam/dust", "A wall of dust over Thunderbird Road", 9),
        Photo("sam/door", "The card on the office door", 30),
    )
}
