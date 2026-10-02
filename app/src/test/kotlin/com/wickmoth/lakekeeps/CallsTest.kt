package com.wickmoth.lakekeeps

import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.game.phone.Ago
import com.wickmoth.lakekeeps.game.phone.Call
import com.wickmoth.lakekeeps.game.phone.CallKind
import com.wickmoth.lakekeeps.game.phone.CallLog
import com.wickmoth.lakekeeps.game.phone.PhoneBook
import com.wickmoth.lakekeeps.game.phone.ago
import com.wickmoth.lakekeeps.game.phone.dialable
import com.wickmoth.lakekeeps.game.phone.formatNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallsTest {
    @Test fun historyIsNewestFirst() {
        for (owner in Owner.entries) {
            val history = PhoneBook.history(owner)
            assertTrue(history.isNotEmpty())
            history.zipWithNext().forEach { (a, b) ->
                assertTrue("$a before $b", a.daysAgo < b.daysAgo || (a.daysAgo == b.daysAgo && a.minutes >= b.minutes))
            }
        }
    }

    @Test fun numbersMatchTheContactsInMessages() {
        val number = Regex("""\d{3}-\d{4}""")
        for (owner in Owner.entries) {
            for (thread in Threads.of(owner)) {
                val shown = number.find(thread.contact.detail)?.value ?: continue
                val saved = PhoneBook.find(owner, shown)
                assertEquals("${thread.id} is saved under the same name", thread.contact.name, saved?.name)
            }
        }
    }

    @Test fun findMatchesNumbersHoweverTheyAreWritten() {
        assertEquals("Priya", PhoneBook.find(Owner.Theo, "5550142")?.name)
        assertEquals("Priya", PhoneBook.find(Owner.Mira, "555-0142")?.name)
        assertNull(PhoneBook.find(Owner.Theo, "555-0103"))
        assertEquals("*31#5550142", dialable("*31# 555-0142"))
    }

    @Test fun relativeTimes() {
        val now = 16 * 60 + 12
        assertEquals(Ago.Now, Call("1", CallKind.Outgoing, 0, now).ago(now))
        assertEquals(Ago.Minutes(5), Call("1", CallKind.Outgoing, 0, now - 5).ago(now))
        assertEquals(Ago.Hours(2), Call("1", CallKind.Missed, 0, now - 130).ago(now))
        assertEquals(Ago.Days(2), Call("1", CallKind.Incoming, 2, now + 300).ago(now))
    }

    @Test fun numbersAreGroupedAsTheyAreTyped() {
        assertEquals("555", formatNumber("555"))
        assertEquals("555-0", formatNumber("5550"))
        assertEquals("555-0142", formatNumber("5550142"))
        assertEquals("(555) 010-4477", formatNumber("5550104477"))
        assertEquals("*31#", formatNumber("*31#"))
    }

    @Test fun placedCallsComeFirstAndAreSaved() {
        val log = CallLog()
        log.place(Owner.Theo, "555-0142", 975)
        log.place(Owner.Theo, "5550187", 976)
        val theo = log.calls(Owner.Theo)
        assertEquals(Call("5550187", CallKind.Outgoing, 0, 976), theo[0])
        assertEquals(Call("5550142", CallKind.Outgoing, 0, 975), theo[1])
        assertEquals(PhoneBook.history(Owner.Theo), theo.drop(2))
        assertEquals(PhoneBook.history(Owner.Mira), log.calls(Owner.Mira))

        val restored = CallLog.decode(log.encode())
        assertEquals(theo, restored.calls(Owner.Theo))
        assertEquals(PhoneBook.history(Owner.Theo), CallLog.decode("Nobody;555;1\nTheo;;1\nTheo;12a;1\nTheo;1;x").calls(Owner.Theo))
    }
}
