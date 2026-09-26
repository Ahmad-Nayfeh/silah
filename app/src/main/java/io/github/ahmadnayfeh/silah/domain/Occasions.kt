package io.github.ahmadnayfeh.silah.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object Occasions {

    /** The next date (today or later) this occasion happens, or null for a one-time occasion that has passed. */
    fun nextOccurrence(occasion: OccasionInfo, today: LocalDate): LocalDate? {
        if (!occasion.repeatsYearly) {
            return occasion.date.takeIf { !it.isBefore(today) }
        }
        val thisYear = inYear(occasion.date, today.year)
        return if (thisYear.isBefore(today)) inYear(occasion.date, today.year + 1) else thisYear
    }

    /** Feb 29 falls back to Feb 28 in years that are not leap years. */
    private fun inYear(date: LocalDate, year: Int): LocalDate {
        val day = minOf(date.dayOfMonth, date.month.length(java.time.Year.isLeap(year.toLong())))
        return LocalDate.of(year, date.month, day)
    }

    fun upcoming(occasion: OccasionInfo, today: LocalDate): UpcomingOccasion? {
        val next = nextOccurrence(occasion, today) ?: return null
        val days = ChronoUnit.DAYS.between(today, next).toInt()
        return if (days in 0..occasion.remindDaysBefore) UpcomingOccasion(occasion, next, days) else null
    }

    /** All occasions (from anyone) that are coming within their reminder window, nearest first. */
    fun upcomingAll(occasions: List<OccasionInfo>, today: LocalDate): List<UpcomingOccasion> =
        occasions.mapNotNull { upcoming(it, today) }.sortedBy { it.daysUntil }
}
