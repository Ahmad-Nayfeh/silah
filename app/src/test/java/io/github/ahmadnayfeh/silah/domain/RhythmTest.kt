package io.github.ahmadnayfeh.silah.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class RhythmTest {
    private val allDays = DayOfWeek.entries.toSet()

    @Test
    fun `14 people with mixed targets need 6 nudges a week`() {
        // 2 weekly (2.0) + 4 fortnightly (2.0) + 8 monthly (8 × 7/30 = 1.87) = 5.87 → 6
        val targets = List(2) { 7 } + List(4) { 14 } + List(8) { 30 }
        assertEquals(14, targets.size)
        assertEquals(6, Rhythm.weeklyNeeded(targets))
    }

    @Test
    fun `14 people all monthly need 4 nudges a week`() {
        // 14 × 7/30 = 3.27 → 4
        assertEquals(4, Rhythm.weeklyNeeded(List(14) { 30 }))
    }

    @Test
    fun `14 people fortnightly need exactly 7`() {
        // 14 × 0.5 = 7.0 — floating point noise must not push it over.
        assertEquals(7, Rhythm.weeklyNeeded(List(14) { 14 }))
    }

    @Test
    fun `whole numbers are not rounded up by floating point noise`() {
        // 3 × 7/21 = 1.0 exactly in maths, 0.9999… or 1.0000…1 in doubles
        assertEquals(1, Rhythm.weeklyNeeded(List(3) { 21 }))
        assertEquals(3, Rhythm.weeklyNeeded(List(9) { 21 }))
    }

    @Test
    fun `never more than one a day`() {
        val targets = List(10) { 3 } + List(4) { 1 } // way more than 7
        assertEquals(7, Rhythm.weeklyNeeded(targets))
    }

    @Test
    fun `nobody means no nudges`() {
        assertEquals(0, Rhythm.weeklyNeeded(emptyList()))
    }

    @Test
    fun `paused people do not count in the automatic rhythm`() {
        val today = LocalDate.of(2026, 9, 25)
        val people = listOf(
            Candidate(1, 7, false, today, null, null),
            Candidate(2, 1, true, today, null, null),
        )
        assertEquals(1, Rhythm.perWeek(people, autoRhythm = true, manualPerWeek = 5))
        assertEquals(5, Rhythm.perWeek(people, autoRhythm = false, manualPerWeek = 5))
    }

    @Test
    fun `suggestion days are spread evenly from Saturday`() {
        assertEquals(setOf(SATURDAY, MONDAY, WEDNESDAY), Rhythm.suggestionDays(3, allDays))
        assertEquals(allDays, Rhythm.suggestionDays(7, allDays))
        assertEquals(emptySet<DayOfWeek>(), Rhythm.suggestionDays(0, allDays))
    }

    @Test
    fun `suggestion days respect the allowed days`() {
        val allowed = setOf(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        assertEquals(allowed, Rhythm.suggestionDays(6, allowed))
        assertEquals(1, Rhythm.suggestionDays(1, allowed).size)
    }

    @Test
    fun `a week has exactly as many notification days as needed, the rest are silent`() {
        val targets = List(2) { 7 } + List(4) { 14 } + List(8) { 30 } // needs 6
        val start = LocalDate.of(2026, 9, 26) // a Saturday
        // Everyone is due, so only the rhythm decides.
        val people = targets.mapIndexed { i, t -> Candidate(i + 1L, t, false, start.minusYears(1), start.minusDays(60), null) }
        val perWeek = Rhythm.perWeek(people, autoRhythm = true, manualPerWeek = 0)
        val week = (0L until 7).map { start.plusDays(it) }
        val notifyDays = week.filter { TodayPlanner.shouldNotify(people, it, perWeek, allDays) }
        assertEquals(6, notifyDays.size)
        val silentDay = week.single { it !in notifyDays }
        assertFalse(TodayPlanner.shouldNotify(people, silentDay, perWeek, allDays))
    }

    @Test
    fun `a silent day stays silent even when people are due`() {
        val day = LocalDate.of(2026, 9, 27) // Sunday — not in {Sat, Mon, Wed}
        val people = listOf(Candidate(1, 7, false, day.minusYears(1), day.minusDays(30), null))
        assertFalse(TodayPlanner.shouldNotify(people, day, perWeek = 3, allowedDays = allDays))
        assertTrue(TodayPlanner.shouldNotify(people, day.minusDays(1), perWeek = 3, allowedDays = allDays)) // Saturday
    }

    @Test
    fun `an occasion breaks the silence`() {
        val day = LocalDate.of(2026, 9, 27) // silent Sunday
        val birthday = OccasionInfo(1, "ميلاد", day.plusDays(1).minusYears(20), true, 2)
        val people = listOf(Candidate(1, 30, false, day.minusYears(1), day.minusDays(2), null, listOf(birthday)))
        assertTrue(TodayPlanner.shouldNotify(people, day, perWeek = 3, allowedDays = allDays))
    }

    @Test
    fun `no notification on a suggestion day when nobody is due`() {
        val day = LocalDate.of(2026, 9, 26) // Saturday
        val people = listOf(Candidate(1, 30, false, day.minusYears(1), day.minusDays(2), null))
        assertFalse(TodayPlanner.shouldNotify(people, day, perWeek = 7, allowedDays = allDays))
    }
}
