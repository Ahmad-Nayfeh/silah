package io.github.ahmadnayfeh.silah.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil

/**
 * How many nudges per week are needed, and on which days they fall.
 * Days that are not suggestion days are completely silent.
 */
object Rhythm {
    const val MAX_PER_WEEK = 7

    /** The week as it is lived in Saudi Arabia: Saturday first. */
    val WEEK_ORDER: List<DayOfWeek> = listOf(
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    )

    /** Σ (7 ÷ targetDays) over active people, rounded up, at most one per day. */
    fun weeklyNeeded(activeTargetDays: List<Int>): Int {
        if (activeTargetDays.isEmpty()) return 0
        val sum = activeTargetDays.sumOf { 7.0 / it.coerceAtLeast(1) }
        // The small epsilon keeps 3.0000000004 (floating point noise) from becoming 4.
        return ceil(sum - 1e-9).toInt().coerceIn(0, MAX_PER_WEEK)
    }

    fun perWeek(candidates: List<Candidate>, autoRhythm: Boolean, manualPerWeek: Int): Int =
        if (autoRhythm) weeklyNeeded(candidates.filterNot { it.isPaused }.map { it.targetDays })
        else manualPerWeek.coerceIn(1, MAX_PER_WEEK)

    /** Spreads [perWeek] suggestion days as evenly as possible over the allowed days. */
    fun suggestionDays(perWeek: Int, allowed: Set<DayOfWeek>): Set<DayOfWeek> {
        val days = WEEK_ORDER.filter { it in allowed }
        val n = minOf(perWeek, days.size)
        if (n <= 0) return emptySet()
        return (0 until n).map { days[it * days.size / n] }.toSet()
    }

    fun isSuggestionDay(date: LocalDate, perWeek: Int, allowed: Set<DayOfWeek>): Boolean =
        date.dayOfWeek in suggestionDays(perWeek, allowed)
}
