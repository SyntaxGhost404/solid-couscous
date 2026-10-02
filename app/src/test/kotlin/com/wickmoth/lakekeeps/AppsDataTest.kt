package com.wickmoth.lakekeeps

import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.GameTime
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.Stage
import com.wickmoth.lakekeeps.game.gallery.Albums
import com.wickmoth.lakekeeps.game.mail.Inboxes
import com.wickmoth.lakekeeps.game.mail.MailBox
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.game.phone.PhoneBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class AppsDataTest {
    @Test fun theGameCalendarIsSaturdayFourthOfNovember() {
        assertEquals(DayOfWeek.SATURDAY, GameTime.today.dayOfWeek)
        assertEquals("4 November", GameTime.longDate(0))
        assertEquals("2 Nov", GameTime.shortDate(2))
        assertEquals("28 October", GameTime.longDate(7))
    }

    @Test fun inboxesAreNewestFirstWithUniqueIds() {
        for (owner in Owner.entries) {
            val inbox = Inboxes.of(owner)
            assertTrue(inbox.isNotEmpty())
            inbox.zipWithNext().forEach { (a, b) ->
                assertTrue("${a.id} before ${b.id}", a.daysAgo < b.daysAgo || (a.daysAgo == b.daysAgo && a.minutes >= b.minutes))
            }
        }
        assertEquals(Inboxes.all.size, Inboxes.all.map { it.id }.toSet().size)
        assertTrue(Inboxes.all.all { it.address.endsWith(".example") })
    }

    @Test fun openedMailIsSavedAndCounted() {
        val box = MailBox()
        assertEquals(2, box.unread(Owner.Mira))
        assertEquals(1, box.unread(Owner.Theo))
        box.open(Inboxes.byId("mira/picnook")!!)
        box.open(Inboxes.byId("mira/picnook")!!)
        assertEquals(1, box.unread(Owner.Mira))

        val restored = MailBox.decode(box.encode())
        assertEquals(1, restored.unread(Owner.Mira))
        assertEquals(1, restored.unread(Owner.Theo))
        assertEquals(2, MailBox.decode("nonsense\nmira/unknown").unread(Owner.Mira))
    }

    @Test fun albumsAreNewestFirst() {
        for (owner in Owner.entries) {
            for (album in listOf(Albums.photos(owner), Albums.hidden(owner))) {
                album.zipWithNext().forEach { (a, b) -> assertTrue(a.daysAgo <= b.daysAgo) }
            }
        }
        assertTrue(Albums.hidden(Owner.Theo).isEmpty())
        assertTrue(Albums.hidden(Owner.Mira).isNotEmpty())
    }

    @Test fun resetStartsTheGameOver() {
        val state = GameState(Stage.Board, phone = Owner.Theo, boardSettled = true)
        state.messages.deliver(Threads.PrivateNumber)
        state.calls.place(Owner.Theo, "5550142", 975)
        state.mail.open(Inboxes.of(Owner.Theo).first())

        state.reset()
        assertEquals(Stage.Studio, state.stage)
        assertNull(state.phone)
        assertFalse(state.boardSettled)
        assertEquals(0, state.messages[Threads.PrivateNumber].delivered)
        assertEquals(PhoneBook.history(Owner.Theo), state.calls.calls(Owner.Theo))
        assertEquals(1, state.mail.unread(Owner.Theo))
    }
}
