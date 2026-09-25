package io.github.ahmadnayfeh.silah.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StatsAndTextTest {
    private val today = LocalDate.of(2026, 9, 25)

    @Test
    fun `coverage counts people contacted this month out of active people`() {
        val people = listOf(
            StatPerson(1, today.minusYears(1), false, 7),
            StatPerson(2, today.minusYears(1), false, 7),
            StatPerson(3, today.minusYears(1), true, 7), // paused: not counted
            StatPerson(4, today.plusDays(1), false, 7), // not added yet
        )
        val contacts = listOf(
            StatContact(1, today.minusDays(2), Direction.ME),
            StatContact(1, today.minusDays(1), Direction.THEM), // same person twice → counted once
            StatContact(2, today.minusMonths(1), Direction.ME), // last month
            StatContact(3, today, Direction.ME), // paused
        )
        val c = Stats.coverage(people, contacts, YearMonth.from(today), today)
        assertEquals(1, c.contacted)
        assertEquals(2, c.total)
    }

    @Test
    fun `six months of coverage end with the current month`() {
        val people = listOf(StatPerson(1, today.minusYears(1), false, 7))
        val contacts = listOf(StatContact(1, today.minusMonths(2), Direction.ME))
        val months = Stats.lastMonths(people, contacts, today)
        assertEquals(6, months.size)
        assertEquals(YearMonth.from(today), months.last().month)
        assertEquals(listOf(0, 0, 0, 1, 0, 0), months.map { it.contacted })
    }

    @Test
    fun `average interval uses distinct contact days`() {
        val d = LocalDate.of(2026, 1, 1)
        assertEquals(19.0, Stats.averageInterval(listOf(d, d.plusDays(19), d.plusDays(38), d.plusDays(38)))!!, 1e-9)
        assertNull(Stats.averageInterval(listOf(d)))
    }

    @Test
    fun `initiation split per person`() {
        val people = listOf(StatPerson(1, today, false, 14))
        val contacts = listOf(
            StatContact(1, today, Direction.ME),
            StatContact(1, today.minusDays(14), Direction.THEM),
            StatContact(1, today.minusDays(28), Direction.ME),
        )
        val s = Stats.perPerson(people, contacts).single()
        assertEquals(2, s.byMe)
        assertEquals(1, s.byThem)
        assertEquals(14.0, s.averageInterval!!, 1e-9)
        assertEquals("كل 14 يوماً / الهدف 14", ArabicText.intervalVsTarget(s.averageInterval, 14))
    }

    @Test
    fun `arabic phrasing is neutral and grammatical`() {
        assertEquals("لم تتواصلا بعد", ArabicText.lastContact(null))
        assertEquals("آخر تواصل اليوم", ArabicText.lastContact(0))
        assertEquals("آخر تواصل أمس", ArabicText.lastContact(1))
        assertEquals("آخر تواصل قبل يومين", ArabicText.lastContact(2))
        assertEquals("آخر تواصل قبل 5 أيام", ArabicText.lastContact(5))
        assertEquals("آخر تواصل قبل 12 يوماً", ArabicText.lastContact(12))
        assertEquals("غداً ميلاد سارة", ArabicText.occasion("ميلاد", "سارة", 1))
        assertEquals("كل أسبوعين", ArabicText.every(14))
        assertEquals("كل 45 يوماً", ArabicText.every(45))
        assertEquals("تواصلت مع 11 من 14 هذا الشهر", ArabicText.coverage(11, 14))
        assertEquals("7:30 م", ArabicText.time(19 * 60 + 30))
        assertEquals("12:05 ص", ArabicText.time(5))
    }

    @Test
    fun `phone numbers are normalised to international form`() {
        assertEquals("+966501234567", PhoneNumbers.normalize("050 123 4567"))
        assertEquals("+966501234567", PhoneNumbers.normalize("501234567"))
        assertEquals("+966501234567", PhoneNumbers.normalize("00966501234567"))
        assertEquals("+962791234567", PhoneNumbers.normalize("+962 79 123 4567"))
        assertEquals("+966501234567", PhoneNumbers.normalize("٠٥٠١٢٣٤٥٦٧"))
        assertNull(PhoneNumbers.normalize("123"))
        assertNull(PhoneNumbers.normalize(""))
        assertEquals("966501234567", PhoneNumbers.waDigits("+966501234567"))
    }
}
