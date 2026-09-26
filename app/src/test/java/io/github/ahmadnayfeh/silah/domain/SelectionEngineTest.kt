package io.github.ahmadnayfeh.silah.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SelectionEngineTest {
    private val today = LocalDate.of(2026, 9, 25)

    private fun person(
        id: Long,
        target: Int = 7,
        lastDaysAgo: Long? = null,
        paused: Boolean = false,
        skippedDaysAgo: Long? = null,
        createdDaysAgo: Long = 100,
        occasions: List<OccasionInfo> = emptyList(),
    ) = Candidate(
        personId = id,
        targetDays = target,
        isPaused = paused,
        createdOn = today.minusDays(createdDaysAgo),
        lastContact = lastDaysAgo?.let { today.minusDays(it) },
        skippedOn = skippedDaysAgo?.let { today.minusDays(it) },
        occasions = occasions,
    )

    private fun birthdayIn(days: Long, remind: Int = 2, id: Long = 1) =
        OccasionInfo(id, "ميلاد", today.plusDays(days).minusYears(30), repeatsYearly = true, remindDaysBefore = remind)

    @Test
    fun `ratio is days since last contact over target`() {
        assertEquals(2.0, SelectionEngine.ratio(person(1, target = 7, lastDaysAgo = 14), today), 1e-9)
        assertEquals(0.5, SelectionEngine.ratio(person(1, target = 30, lastDaysAgo = 15), today), 1e-9)
    }

    @Test
    fun `people are ordered by ratio, highest first`() {
        val people = listOf(
            person(1, target = 7, lastDaysAgo = 7), // 1.0
            person(2, target = 14, lastDaysAgo = 28), // 2.0
            person(3, target = 30, lastDaysAgo = 45), // 1.5
        )
        assertEquals(listOf(2L, 3L, 1L), SelectionEngine.eligible(people, today).map { it.personId })
    }

    @Test
    fun `never contacted comes first, oldest added first`() {
        val people = listOf(
            person(1, target = 7, lastDaysAgo = 70), // 10.0
            person(2, lastDaysAgo = null, createdDaysAgo = 1),
            person(3, lastDaysAgo = null, createdDaysAgo = 5),
        )
        assertEquals(listOf(3L, 2L, 1L), SelectionEngine.eligible(people, today).map { it.personId })
    }

    @Test
    fun `nobody under 0_7 is suggested`() {
        val people = listOf(
            person(1, target = 10, lastDaysAgo = 6), // 0.6
            person(2, target = 10, lastDaysAgo = 7), // 0.7 exactly
        )
        assertEquals(listOf(2L), SelectionEngine.eligible(people, today).map { it.personId })
        assertNull(SelectionEngine.pick(listOf(person(1, target = 10, lastDaysAgo = 6)), today))
    }

    @Test
    fun `paused people are never suggested`() {
        val people = listOf(person(1, lastDaysAgo = 100, paused = true), person(2, lastDaysAgo = 8))
        assertEquals(listOf(2L), SelectionEngine.eligible(people, today).map { it.personId })
    }

    @Test
    fun `someone contacted today is not suggested`() {
        assertNull(SelectionEngine.pick(listOf(person(1, target = 1, lastDaysAgo = 0)), today))
    }

    @Test
    fun `upcoming occasion jumps to the top even below the threshold`() {
        val people = listOf(
            person(1, target = 7, lastDaysAgo = 70), // 10.0
            person(2, target = 30, lastDaysAgo = 3, occasions = listOf(birthdayIn(1))), // 0.1 but birthday tomorrow
        )
        val ranked = SelectionEngine.eligible(people, today)
        assertEquals(listOf(2L, 1L), ranked.map { it.personId })
        assertEquals(1, ranked.first().occasion?.daysUntil)
    }

    @Test
    fun `occasion outside its reminder window does not count`() {
        val people = listOf(person(2, target = 30, lastDaysAgo = 3, occasions = listOf(birthdayIn(5, remind = 2))))
        assertNull(SelectionEngine.pick(people, today))
    }

    @Test
    fun `nearest occasion wins between two`() {
        val people = listOf(
            person(1, target = 30, lastDaysAgo = 40, occasions = listOf(birthdayIn(2))),
            person(2, target = 30, lastDaysAgo = 3, occasions = listOf(birthdayIn(0))),
        )
        assertEquals(listOf(2L, 1L), SelectionEngine.eligible(people, today).map { it.personId })
    }

    @Test
    fun `occasion stops pushing once contacted inside the window`() {
        // Birthday in 2 days, window started today; contact made today → done.
        val contactedToday = person(1, target = 30, lastDaysAgo = 0, occasions = listOf(birthdayIn(2)))
        assertNull(SelectionEngine.activeOccasion(contactedToday, today))
        // Contact before the window → still reminded.
        val contactedBefore = person(1, target = 30, lastDaysAgo = 3, occasions = listOf(birthdayIn(2)))
        assertTrue(SelectionEngine.activeOccasion(contactedBefore, today) != null)
    }

    @Test
    fun `not today hides a person for 3 days`() {
        fun eligibleAfterSkip(daysAgo: Long) =
            SelectionEngine.eligible(listOf(person(1, lastDaysAgo = 30, skippedDaysAgo = daysAgo)), today).isNotEmpty()
        assertEquals(false, eligibleAfterSkip(0))
        assertEquals(false, eligibleAfterSkip(1))
        assertEquals(false, eligibleAfterSkip(2))
        assertEquals(true, eligibleAfterSkip(3))
    }

    @Test
    fun `not today with an occasion returns the next day, not the same day`() {
        fun eligible(skipDaysAgo: Long) = SelectionEngine.eligible(
            listOf(person(1, target = 30, lastDaysAgo = 5, skippedDaysAgo = skipDaysAgo, occasions = listOf(birthdayIn(1)))),
            today,
        ).isNotEmpty()
        assertEquals(false, eligible(0))
        assertEquals(true, eligible(1))
    }

    @Test
    fun `yearly occasion on 29 February falls on 28 February in common years`() {
        val o = OccasionInfo(1, "ميلاد", LocalDate.of(2000, 2, 29), repeatsYearly = true, remindDaysBefore = 2)
        assertEquals(LocalDate.of(2027, 2, 28), Occasions.nextOccurrence(o, LocalDate.of(2027, 2, 1)))
        assertEquals(LocalDate.of(2028, 2, 29), Occasions.nextOccurrence(o, LocalDate.of(2028, 2, 1)))
    }

    @Test
    fun `one-time occasion in the past is ignored, yearly one rolls to next year`() {
        val once = OccasionInfo(1, "سفر", today.minusDays(1), repeatsYearly = false, remindDaysBefore = 2)
        assertNull(Occasions.nextOccurrence(once, today))
        val yearly = OccasionInfo(2, "ميلاد", today.minusDays(1), repeatsYearly = true, remindDaysBefore = 2)
        assertEquals(today.minusDays(1).plusYears(1), Occasions.nextOccurrence(yearly, today))
    }
}
