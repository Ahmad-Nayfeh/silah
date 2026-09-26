package io.github.ahmadnayfeh.silah.domain

import java.time.LocalDate
import java.time.YearMonth

data class StatPerson(val id: Long, val createdOn: LocalDate, val isPaused: Boolean, val targetDays: Int)

data class StatContact(val personId: Long, val date: LocalDate, val direction: Direction)

data class MonthCoverage(val month: YearMonth, val contacted: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else contacted.toFloat() / total
}

data class PersonStat(
    val personId: Long,
    /** Mean days between distinct contact days, or null with fewer than two. */
    val averageInterval: Double?,
    val targetDays: Int,
    val byMe: Int,
    val byThem: Int,
)

/** Small, honest statistics computed straight from the contact log. */
object Stats {

    /**
     * People counted in a month = not paused and added on/before the month's last day
     * (or today, for the current month). Contacted = at least one contact that month.
     */
    fun coverage(
        people: List<StatPerson>,
        contacts: List<StatContact>,
        month: YearMonth,
        today: LocalDate,
    ): MonthCoverage {
        val end = minOf(month.atEndOfMonth(), today)
        val counted = people.filter { !it.isPaused && !it.createdOn.isAfter(end) }.map { it.id }.toSet()
        val contacted = contacts
            .filter { it.personId in counted && YearMonth.from(it.date) == month }
            .map { it.personId }
            .toSet()
        return MonthCoverage(month, contacted.size, counted.size)
    }

    fun lastMonths(
        people: List<StatPerson>,
        contacts: List<StatContact>,
        today: LocalDate,
        count: Int = 6,
    ): List<MonthCoverage> {
        val current = YearMonth.from(today)
        return (count - 1 downTo 0).map { coverage(people, contacts, current.minusMonths(it.toLong()), today) }
    }

    fun averageInterval(dates: List<LocalDate>): Double? {
        val days = dates.map { it.toEpochDay() }.distinct().sorted()
        if (days.size < 2) return null
        return (days.last() - days.first()).toDouble() / (days.size - 1)
    }

    fun perPerson(people: List<StatPerson>, contacts: List<StatContact>): List<PersonStat> {
        val byPerson = contacts.groupBy { it.personId }
        return people.map { p ->
            val mine = byPerson[p.id].orEmpty()
            PersonStat(
                personId = p.id,
                averageInterval = averageInterval(mine.map { it.date }),
                targetDays = p.targetDays,
                byMe = mine.count { it.direction == Direction.ME },
                byThem = mine.count { it.direction == Direction.THEM },
            )
        }
    }
}
