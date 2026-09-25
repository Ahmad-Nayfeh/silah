package io.github.ahmadnayfeh.silah.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** The one person chosen for today, remembered so the app, widget and notification agree. */
data class StoredPick(val date: LocalDate, val personId: Long)

sealed interface TodayDecision {
    /** Suggest this person. */
    data class Suggest(val ranked: Ranked) : TodayDecision

    /** Today's person was already contacted. [othersAvailable] = someone else could still be suggested. */
    data class Done(val personId: Long, val othersAvailable: Boolean) : TodayDecision

    data object NoOne : TodayDecision
}

object TodayPlanner {

    /**
     * Decides what "today" shows. Returns the decision and the pick to remember.
     * Once a person is chosen for the day they stay chosen (stable across screens),
     * unless they become ineligible or someone with an occasion outranks them.
     */
    fun decide(
        candidates: List<Candidate>,
        stored: StoredPick?,
        today: LocalDate,
    ): Pair<TodayDecision, StoredPick?> {
        val eligible = SelectionEngine.eligible(candidates, today)
        val top = eligible.firstOrNull()

        if (stored != null && stored.date == today) {
            val storedCandidate = candidates.firstOrNull { it.personId == stored.personId }
            if (storedCandidate != null && storedCandidate.lastContact == today) {
                return TodayDecision.Done(stored.personId, eligible.isNotEmpty()) to stored
            }
            val kept = eligible.firstOrNull { it.personId == stored.personId }
            if (kept != null && (kept.occasion != null || top?.occasion == null)) {
                return TodayDecision.Suggest(kept) to stored
            }
        }
        if (top == null) return TodayDecision.NoOne to null
        return TodayDecision.Suggest(top) to StoredPick(today, top.personId)
    }

    /**
     * Whether today's notification should fire. An occasion always speaks;
     * otherwise only on the rhythm's suggestion days. Every other day is silent.
     */
    fun shouldNotify(
        candidates: List<Candidate>,
        today: LocalDate,
        perWeek: Int,
        allowedDays: Set<DayOfWeek>,
    ): Boolean {
        val eligible = SelectionEngine.eligible(candidates, today)
        if (eligible.isEmpty()) return false
        if (eligible.first().occasion != null) return true
        return Rhythm.isSuggestionDay(today, perWeek, allowedDays)
    }
}
