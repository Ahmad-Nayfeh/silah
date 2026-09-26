package io.github.ahmadnayfeh.silah.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TodayPlannerTest {
    private val today = LocalDate.of(2026, 9, 25)

    private fun c(id: Long, target: Int, lastDaysAgo: Long?, occasions: List<OccasionInfo> = emptyList(), skipped: LocalDate? = null) =
        Candidate(id, target, false, today.minusYears(1), lastDaysAgo?.let { today.minusDays(it) }, skipped, occasions)

    @Test
    fun `picks the most due person and remembers them`() {
        val people = listOf(c(1, 7, 10), c(2, 7, 20))
        val (decision, pick) = TodayPlanner.decide(people, null, today)
        assertEquals(2L, (decision as TodayDecision.Suggest).ranked.personId)
        assertEquals(StoredPick(today, 2), pick)
    }

    @Test
    fun `the chosen person stays chosen for the day`() {
        val people = listOf(c(1, 7, 30), c(2, 7, 20)) // 1 is now more due, but 2 was picked earlier today
        val (decision, _) = TodayPlanner.decide(people, StoredPick(today, 2), today)
        assertEquals(2L, (decision as TodayDecision.Suggest).ranked.personId)
    }

    @Test
    fun `a pick from yesterday is replaced`() {
        val people = listOf(c(1, 7, 30), c(2, 7, 20))
        val (decision, pick) = TodayPlanner.decide(people, StoredPick(today.minusDays(1), 2), today)
        assertEquals(1L, (decision as TodayDecision.Suggest).ranked.personId)
        assertEquals(StoredPick(today, 1), pick)
    }

    @Test
    fun `once contacted, today is done`() {
        val people = listOf(c(1, 7, 0), c(2, 7, 20))
        val (decision, _) = TodayPlanner.decide(people, StoredPick(today, 1), today)
        assertEquals(TodayDecision.Done(1, othersAvailable = true), decision)
    }

    @Test
    fun `not today moves on to the next person`() {
        val people = listOf(c(1, 7, 30, skipped = today), c(2, 7, 20))
        val (decision, _) = TodayPlanner.decide(people, StoredPick(today, 1), today)
        assertEquals(2L, (decision as TodayDecision.Suggest).ranked.personId)
    }

    @Test
    fun `an occasion takes over a regular pick`() {
        val bday = OccasionInfo(1, "ميلاد", today.plusDays(1).minusYears(40), true, 2)
        val people = listOf(c(1, 7, 30), c(2, 30, 3, listOf(bday)))
        val (decision, pick) = TodayPlanner.decide(people, StoredPick(today, 1), today)
        assertEquals(2L, (decision as TodayDecision.Suggest).ranked.personId)
        assertTrue(decision.ranked.occasion != null)
        assertEquals(StoredPick(today, 2), pick)
    }

    @Test
    fun `nobody due means no one today`() {
        val (decision, pick) = TodayPlanner.decide(listOf(c(1, 30, 2)), null, today)
        assertEquals(TodayDecision.NoOne, decision)
        assertNull(pick)
    }
}
