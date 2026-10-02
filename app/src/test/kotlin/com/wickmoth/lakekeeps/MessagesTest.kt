package com.wickmoth.lakekeeps

import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.case.CaseId
import com.wickmoth.lakekeeps.game.messages.Beat
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.game.messages.clockMinutes
import com.wickmoth.lakekeeps.game.messages.formatClock
import com.wickmoth.lakekeeps.game.messages.replay
import com.wickmoth.lakekeeps.game.messages.script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagesTest {
    private val sample = script {
        says("one")
        ask(
            reply("a") { says("after a") },
            reply("b") { says("after b") },
        )
        says("two")
    }

    @Test fun replayStopsAtTheFirstOpenQuestion() {
        val r = replay(sample, emptyList())
        assertEquals(listOf(false to "one"), r.lines.map { it.mine to it.text })
        assertNotNull(r.pending)
        assertFalse(r.finished)
    }

    @Test fun replayFollowsTheChosenBranchAndRejoins() {
        val r = replay(sample, listOf(1))
        assertEquals(listOf(false to "one", true to "b", false to "after b", false to "two"), r.lines.map { it.mine to it.text })
        assertTrue(r.finished)
    }

    @Test fun liveThreadStartsHiddenThenArrivesUnread() {
        val m = Messages()
        val t = Threads.PrivateNumber
        assertFalse(m.hasStarted(t))
        assertFalse(t in m.inbox(Owner.Theo))
        assertEquals(Threads.NOW_MINUTES, m.clock(CaseId.Prototype))

        m.deliver(t)
        assertTrue(m.hasStarted(t))
        assertTrue(m.isLive(t))
        assertEquals(1, m.unread(t))
        assertEquals(t, m.inbox(Owner.Theo).first())
        assertEquals(t, m.notices(Owner.Theo).first().thread)

        m.markRead(t)
        assertEquals(0, m.unread(t))
        assertTrue(m.notices(Owner.Theo).none { it.thread == t })
    }

    @Test fun answeringEveryQuestionEndsTheConversation() {
        val m = Messages()
        val t = Threads.PrivateNumber
        m.deliver(t)
        m.signOff(t)
        assertTrue("signing off mid-conversation is ignored", m.isLive(t))
        while (true) {
            val next = m.nextLine(t)
            if (next != null) {
                m.deliver(t)
                continue
            }
            val q: Beat.Ask = m.question(t) ?: break
            m.choose(t, q.replies.lastIndex)
        }
        assertNull(m.question(t))
        assertEquals("And keep the police out of this.", m.lines(t).last().text)
        assertTrue(m.clock(CaseId.Prototype) > Threads.NOW_MINUTES)
        assertTrue("still online right after the last line", m.isLive(t))
        assertTrue(m.isOver(t))

        m.signOff(t)
        assertFalse(m.isLive(t))
        assertTrue(Messages.decode(m.encode())[t].signedOff)
    }

    @Test fun mirasUnreadMessagesWaitInTheShadeNewestFirst() {
        val m = Messages()
        assertEquals(3, m.unread(Owner.Mira))
        val notices = m.notices(Owner.Mira)
        assertEquals(listOf("mira/felix", "mira/priya"), notices.map { it.thread.id })
        m.dismiss(notices.first().thread)
        assertEquals(listOf("mira/priya"), m.notices(Owner.Mira).map { it.thread.id })
        assertEquals(3, m.unread(Owner.Mira))
    }

    @Test fun progressSurvivesSaving() {
        val m = Messages()
        val t = Threads.PrivateNumber
        m.deliver(t)
        m.choose(t, 1)
        m.markRead(t)
        val restored = Messages.decode(m.encode())
        assertEquals(m[t], restored[t])
        assertEquals(m.lines(t), restored.lines(t))
        assertEquals(Messages()[t], Messages.decode("garbage;;")[t])
    }

    @Test fun clockFormatting() {
        assertEquals(16 * 60 + 12, clockMinutes("4:12 PM"))
        assertEquals(0, clockMinutes("12:00 AM"))
        assertEquals("4:12 PM", formatClock(16 * 60 + 12))
        assertEquals("12:05", formatClock(5, withHalf = false))
    }
}
