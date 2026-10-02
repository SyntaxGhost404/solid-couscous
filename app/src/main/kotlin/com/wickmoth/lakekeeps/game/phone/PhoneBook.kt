package com.wickmoth.lakekeeps.game.phone

import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.littlebird.SamsPhone
import com.wickmoth.lakekeeps.game.messages.clockMinutes

/**
 * Every phone's address book and call history: the prototype's placeholders for Theo and Mira,
 * and Sam's from [SamsPhone]. Numbers are in the 555-01xx range set aside for fiction, and match
 * the contact details shown in Messages.
 */
object PhoneBook {
    private val theoContacts = listOf(
        PhoneContact("Mira", "555-0127", "Mobile"),
        PhoneContact("Priya", "555-0142", "Mobile"),
        PhoneContact("Rosa", "555-0160", "Work"),
        PhoneContact("Mum", "555-0118", "Mobile"),
    )

    private val miraContacts = listOf(
        PhoneContact("Theo", "555-0103", "Mobile"),
        PhoneContact("Priya", "555-0142", "Mobile"),
        PhoneContact("Felix", "555-0175", "Mobile"),
        PhoneContact("Mum", "555-0121", "Mobile"),
    )

    private fun call(kind: CallKind, number: String, daysAgo: Int, time: String) =
        Call(dialable(number), kind, daysAgo, clockMinutes(time))

    private fun out(number: String, daysAgo: Int, time: String) = call(CallKind.Outgoing, number, daysAgo, time)
    private fun answered(number: String, daysAgo: Int, time: String) = call(CallKind.Incoming, number, daysAgo, time)
    private fun missed(number: String, daysAgo: Int, time: String) = call(CallKind.Missed, number, daysAgo, time)

    private val theoHistory = listOf(
        out("555-0127", 0, "11:40 AM"),
        missed("555-0118", 0, "9:01 AM"),
        out("555-0127", 0, "8:05 AM"),
        out("555-0142", 1, "9:45 PM"),
        out("555-0160", 1, "9:58 AM"),
        out("555-0127", 1, "7:30 AM"),
        out("555-0127", 2, "11:15 PM"),
        answered("555-0127", 2, "8:48 PM"),
        answered("555-0118", 3, "6:20 PM"),
        out("555-0142", 5, "4:35 PM"),
    )

    private val miraHistory = listOf(
        missed("555-0121", 0, "2:30 PM"),
        missed("555-0103", 0, "11:40 AM"),
        missed("555-0103", 0, "8:05 AM"),
        missed("555-0121", 1, "6:15 PM"),
        missed("555-0142", 1, "1:20 PM"),
        missed("555-0103", 1, "7:30 AM"),
        missed("555-0103", 2, "11:15 PM"),
        missed("555-0142", 2, "10:33 PM"),
        out("555-0187", 2, "9:12 PM"),
        out("555-0103", 2, "8:48 PM"),
        answered("555-0175", 3, "7:02 PM"),
    )

    fun contacts(owner: Owner): List<PhoneContact> = when (owner) {
        Owner.Theo -> theoContacts
        Owner.Mira -> miraContacts
        Owner.Sam -> SamsPhone.contacts
    }

    /** The phone's calls from before the game began, newest first. */
    fun history(owner: Owner): List<Call> = when (owner) {
        Owner.Theo -> theoHistory
        Owner.Mira -> miraHistory
        Owner.Sam -> SamsPhone.history
    }.sortedWith(compareBy<Call> { it.daysAgo }.thenByDescending { it.minutes })

    /** The saved contact with this number on [owner]'s phone, however the number is written. */
    fun find(owner: Owner, number: String): PhoneContact? {
        val digits = dialable(number)
        return contacts(owner).firstOrNull { dialable(it.number) == digits }
    }
}
