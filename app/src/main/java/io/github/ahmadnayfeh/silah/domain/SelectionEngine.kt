package io.github.ahmadnayfeh.silah.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The heart of the app: decides who is worth a nudge today.
 *
 * Rules (from SPEC.md):
 * - ratio = days since last contact ÷ targetDays; never contacted = highest.
 * - An occasion inside its reminder window moves the person to the top.
 * - "Not today" hides a person for 3 days, unless they have an occasion
 *   (but never on the same day they were skipped).
 * - Paused people are never suggested.
 * - Nobody under 0.7 is suggested unless they have an occasion.
 */
object SelectionEngine {
    const val THRESHOLD = 0.7
    const val SKIP_DAYS = 3

    fun ratio(c: Candidate, today: LocalDate): Double {
        val last = c.lastContact ?: return Double.POSITIVE_INFINITY
        val days = ChronoUnit.DAYS.between(last, today).coerceAtLeast(0)
        return days.toDouble() / c.targetDays.coerceAtLeast(1)
    }

    /**
     * The nearest occasion inside its reminder window, ignoring it once the person
     * was contacted inside that window (the reminder has done its job).
     */
    fun activeOccasion(c: Candidate, today: LocalDate): UpcomingOccasion? =
        Occasions.upcomingAll(c.occasions, today).firstOrNull { up ->
            val windowStart = up.nextDate.minusDays(up.occasion.remindDaysBefore.toLong())
            c.lastContact == null || c.lastContact.isBefore(windowStart)
        }

    private val order = compareBy<Ranked>(
        { if (it.occasion != null) 0 else 1 },
        { it.occasion?.daysUntil ?: 0 },
        { -it.ratio },
        { it.candidate.createdOn },
        { it.candidate.personId },
    )

    /** Everyone who is not paused, most due first. Used by the people list. */
    fun rank(candidates: List<Candidate>, today: LocalDate): List<Ranked> =
        candidates.filterNot { it.isPaused }
            .map { Ranked(it, ratio(it, today), activeOccasion(it, today)) }
            .sortedWith(order)

    /** People who may be suggested today, best first. */
    fun eligible(candidates: List<Candidate>, today: LocalDate): List<Ranked> =
        rank(candidates, today).filter { r -> isEligible(r, today) }

    fun isEligible(r: Ranked, today: LocalDate): Boolean {
        val c = r.candidate
        if (c.isPaused) return false
        if (c.lastContact == today) return false
        val skipped = c.skippedOn
        if (skipped != null) {
            val sinceSkip = ChronoUnit.DAYS.between(skipped, today)
            if (sinceSkip in 0 until 1) return false
            if (sinceSkip in 1 until SKIP_DAYS && r.occasion == null) return false
        }
        return r.occasion != null || r.ratio >= THRESHOLD
    }

    fun pick(candidates: List<Candidate>, today: LocalDate): Ranked? = eligible(candidates, today).firstOrNull()
}
