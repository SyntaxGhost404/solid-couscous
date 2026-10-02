package com.wickmoth.lakekeeps

import androidx.compose.runtime.saveable.SaverScope
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.Stage
import com.wickmoth.lakekeeps.game.case.CaseId
import com.wickmoth.lakekeeps.game.case.CaseProgress
import com.wickmoth.lakekeeps.game.littlebird.Column
import com.wickmoth.lakekeeps.game.littlebird.Evidence
import com.wickmoth.lakekeeps.game.littlebird.Flag
import com.wickmoth.lakekeeps.game.littlebird.LittleBird
import com.wickmoth.lakekeeps.game.littlebird.SamsPhone
import com.wickmoth.lakekeeps.game.littlebird.Symptom
import com.wickmoth.lakekeeps.game.littlebird.Verb
import com.wickmoth.lakekeeps.game.messages.Day
import com.wickmoth.lakekeeps.game.messages.Messages
import com.wickmoth.lakekeeps.game.messages.Thread
import com.wickmoth.lakekeeps.game.messages.Threads
import com.wickmoth.lakekeeps.game.messages.clockMinutes
import com.wickmoth.lakekeeps.game.messages.replay
import com.wickmoth.lakekeeps.game.messages.script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Little Bird's rules without the screens: sessions behind flags, marks, objectives, the board, saving. */
class CaseTest {
    /** Plays [thread] as far as it goes right now, answering every question with [pick]. */
    private fun Messages.playOut(thread: Thread, pick: Int = 0) {
        while (true) {
            if (nextLine(thread) != null) {
                deliver(thread)
                continue
            }
            question(thread) ?: break
            choose(thread, pick)
        }
    }

    private val sessions = script {
        gate("first", Day.Today, "6:00 PM")
        says("one")
        mark("said one")
        says("two")
        gate("second", Day(1), "9:30 AM")
        writes("three")
        mark("wrote three")
    }

    @Test fun aGateHoldsTheConversationUntilItsFlagAndRestampsTheClock() {
        val shut = replay(sessions, emptyList())
        assertTrue(shut.lines.isEmpty())
        assertEquals("first", shut.gate?.flag)
        assertTrue("nothing to answer while it waits", shut.finished)

        val firstOnly = replay(sessions, emptyList(), open = { it == "first" })
        assertEquals(listOf(false to "one", false to "two"), firstOnly.lines.map { it.mine to it.text })
        assertEquals(clockMinutes("6:00 PM"), firstOnly.lines.first().minutes)
        assertEquals("second", firstOnly.gate?.flag)

        val both = replay(sessions, emptyList(), open = { true })
        assertNull(both.gate)
        val three = both.lines.last()
        assertEquals(true to "three", three.mine to three.text)
        assertEquals(Day(1), three.day)
        assertEquals(clockMinutes("9:30 AM"), three.minutes)
        assertEquals(listOf("said one" to 1, "wrote three" to 3), both.marks.map { it.flag to it.after })
    }

    @Test fun marksReachTheCaseOnlyOnceTheirLinesHaveArrived() {
        val progress = CaseProgress(listOf("first"))
        val m = Messages().apply { flags = progress }
        val t = SamsPhone.AmyHart.copy(id = "test/marks", live = sessions)

        m.deliver(t)
        assertTrue(progress.has("said one"))
        m.deliver(t)
        m.signOff(t)
        assertFalse("signed off between sessions", m.isLive(t))
        assertNull(m.nextLine(t))

        // the next session opens: the scripted line is Sam's own, and the mark waits for it
        progress.set("second")
        assertTrue(m.nextLine(t)!!.mine)
        assertFalse(progress.has("wrote three"))
        m.deliver(t)
        assertTrue(m.isLive(t))
        assertTrue(progress.has("wrote three"))
    }

    @Test fun amyWritesFirstOnceTheCaseOpensAndEachSessionWaitsForTheBoard() {
        val progress = CaseProgress()
        val m = Messages().apply { flags = progress }
        val amy = SamsPhone.AmyHart
        assertFalse(m.hasStarted(amy))
        assertNull(m.nextLine(amy))

        progress.set(Flag.CASE_OPEN)
        val first = m.nextLine(amy)!!
        assertFalse("Amy writes first, by message", first.mine)
        assertEquals("Is this Sam Novak? Dana Brooks gave me this number.", first.text)
        assertEquals(clockMinutes("5:46 PM"), first.minutes)

        m.playOut(amy)
        assertTrue(progress.has(Flag.INTAKE_DONE))
        assertTrue(progress.has(Flag.C1_STEADY))
        assertFalse(progress.has(Flag.C1_FACTS) || progress.has(Flag.C1_PROMISE))
        assertTrue(m.isOver(amy))
        assertTrue(m.clock(CaseId.LittleBird) >= clockMinutes("5:46 PM"))
        // nothing more from Amy until her symptoms are sorted on the board
        m.signOff(amy)
        assertNull(m.nextLine(amy))
        assertNull(m.question(amy))

        progress.set(Flag.SORTED)
        assertNotNull("Sam asks for the battery screen in his own words", m.question(amy))
        m.playOut(amy)
        assertTrue(progress.has(Flag.BATTERY_SENT))
        assertEquals(listOf(Evidence.Battery.id), m.lines(amy).mapNotNull { it.attachment })
        assertTrue(m.clock(CaseId.LittleBird) >= clockMinutes("8:14 PM"))

        m.signOff(amy)
        progress.set(Flag.PIN_BAND)
        m.playOut(amy)
        assertTrue(progress.has(Flag.APPS_SENT))
        assertEquals(listOf(Evidence.Battery.id, Evidence.Apps.id, Evidence.Permissions.id), m.lines(amy).mapNotNull { it.attachment })
    }

    @Test fun danaWritesOnceSamHasTakenTheCase() {
        val progress = CaseProgress(listOf(Flag.CASE_OPEN))
        val m = Messages().apply { flags = progress }
        val dana = SamsPhone.DanaBrooks
        assertTrue("her referral is there from the afternoon", m.hasStarted(dana))
        assertEquals(1, m.unread(dana))
        assertNull(m.nextLine(dana))
        progress.set(Flag.INTAKE_DONE)
        assertFalse(m.nextLine(dana)!!.mine)
    }

    @Test fun onlySamsPhoneBelongsToLittleBird() {
        assertEquals(listOf(Owner.Sam), CaseId.LittleBird.owners)
        assertEquals(listOf(Owner.Mira, Owner.Theo), CaseId.Prototype.owners)
        assertTrue(Threads.of(Owner.Sam).all { it.id.startsWith("sam/") })
    }

    @Test fun objectivesFollowTheCase() {
        val p = CaseProgress()
        assertEquals("intake", LittleBird.objective(p).id)
        listOf(
            Flag.INTAKE_DONE to "sort", Flag.SORTED to "battery", Flag.BATTERY_SENT to "band", Flag.PIN_BAND to "apps",
            Flag.APPS_SENT to "impostor", Flag.PIN_IMPOSTOR to "connect", Flag.ACT1_DONE to "act2",
        ).forEach { (flag, next) ->
            p.set(flag)
            assertEquals(next, LittleBird.objective(p).id)
        }
        LittleBird.objectives.forEach { assertEquals("three hints each: ${it.id}", 3, it.hints.size) }
    }

    @Test fun progressAndHintsSurviveSaving() {
        val p = CaseProgress()
        p.set(Flag.CASE_OPEN)
        p.set(Flag.sorted(Symptom.Noon.id))
        p.set(Flag.CASE_OPEN)
        p.showHint("sort")
        repeat(5) { p.showHint("band") }
        val restored = CaseProgress.decode(p.encode())
        assertEquals(listOf(Flag.CASE_OPEN, "sort.noon"), restored.flags)
        assertEquals(1, restored.hints("sort"))
        assertEquals(CaseProgress.MAX_HINTS, restored.hints("band"))
        assertTrue(CaseProgress.decode(null).flags.isEmpty())
        assertTrue(CaseProgress.decode("h:sort=x\nh:broken\nf:").flags.isEmpty())
        assertEquals(0, CaseProgress.decode("h:sort=x").hints("sort"))
    }

    @Test fun cardsBelongUnderOneColumnAndSayWhyNotTheOthers() {
        Symptom.entries.forEach { card ->
            assertEquals(Column.entries.toSet() - card.column, card.notes.keys)
        }
        assertEquals(listOf(Symptom.Noon, Symptom.Whereabouts), Symptom.entries.filter { it.column == Column.Machine })
        assertEquals(Symptom.Calls, Symptom.byId("calls"))
        assertNull(Symptom.byId("nope"))
    }

    @Test fun onlyTrueLinksHold() {
        val tracker = LittleBird.TRACKER
        assertNotNull(LittleBird.holds(tracker, Symptom.Noon.id, Verb.Explains))
        assertNotNull("either way round", LittleBird.holds(Symptom.Whereabouts.id, tracker, Verb.Explains))
        assertNull(LittleBird.holds(tracker, Symptom.Noon.id, Verb.SameAs))
        assertNull(LittleBird.holds(tracker, Symptom.Calls.id, Verb.Explains))
        assertNull(LittleBird.holds(LittleBird.AMY, Symptom.Photos.id, Verb.Sent))

        assertEquals("Right pins. Wrong word.", LittleBird.linkNote(tracker, Symptom.Noon.id, Verb.Sent))
        assertEquals("A tracker listens. It doesn't dial.", LittleBird.linkNote(Symptom.Calls.id, tracker, Verb.Explains))
        assertEquals("That's the house, not her phone. Not yet.", LittleBird.linkNote(tracker, Symptom.Lock.id, Verb.Explains))
        assertEquals("Does not hold.", LittleBird.linkNote(LittleBird.BAND, LittleBird.AMY, Verb.SameAs))

        // a symptom picked first still reads as the thing explained
        assertEquals(tracker to Symptom.Noon.id, LittleBird.sentence(Symptom.Noon.id, tracker))
        assertEquals(tracker to Symptom.Noon.id, LittleBird.sentence(tracker, Symptom.Noon.id))
        assertEquals(LittleBird.AMY to LittleBird.BAND, LittleBird.sentence(LittleBird.AMY, LittleBird.BAND))

        val p = CaseProgress()
        assertFalse(LittleBird.act1Done(p))
        p.set(Flag.linked("tracker-noon"))
        assertFalse(LittleBird.act1Done(p))
        p.set(Flag.linked("tracker-where"))
        assertTrue(LittleBird.act1Done(p))
    }

    @Test fun theCaseIsSavedWithTheGameAndForgottenOnReset() {
        val state = GameState(Stage.Board, phone = Owner.Sam, boardSettled = true)
        state.progress.set(Flag.CASE_OPEN)
        state.messages.playOut(SamsPhone.AmyHart)
        state.progress.showHint("sort")

        val saved = with(GameState.Saver) { SaverScope { true }.save(state) } as List<*>
        val restored = GameState.Saver.restore(saved)!!
        assertEquals(CaseId.LittleBird, restored.case)
        assertEquals(Owner.Sam, restored.phone)
        assertEquals(state.progress.flags.toList(), restored.progress.flags.toList())
        assertEquals(1, restored.progress.hints("sort"))
        assertEquals(state.messages.lines(SamsPhone.AmyHart), restored.messages.lines(SamsPhone.AmyHart))
        assertTrue("the restored conversation writes to the restored case", restored.messages.flags === restored.progress)

        // a phone from another case never comes back in this one
        val stray = saved.toMutableList().also { it[1] = Owner.Theo.name }
        assertNull(GameState.Saver.restore(stray)!!.phone)
        // state saved before cases existed starts Little Bird
        assertEquals(CaseId.LittleBird, GameState.Saver.restore(saved.take(3))!!.case)

        restored.reset()
        assertTrue(restored.progress.flags.isEmpty())
        assertFalse(restored.messages.hasStarted(SamsPhone.AmyHart))
        assertNull(restored.phone)
        assertEquals(Stage.Studio, restored.stage)
    }
}
