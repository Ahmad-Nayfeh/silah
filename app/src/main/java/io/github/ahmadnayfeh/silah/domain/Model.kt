package io.github.ahmadnayfeh.silah.domain

import java.time.LocalDate

enum class Tag { FAMILY, FRIENDS, OTHER }

enum class Direction { ME, THEM }

enum class Channel { WHATSAPP, CALL, VISIT, OTHER }

enum class Source { AUTO, MANUAL }

/** Everything the selection logic needs to know about one person, free of Android types. */
data class Candidate(
    val personId: Long,
    val targetDays: Int,
    val isPaused: Boolean,
    val createdOn: LocalDate,
    val lastContact: LocalDate?,
    val skippedOn: LocalDate?,
    val occasions: List<OccasionInfo> = emptyList(),
)

data class OccasionInfo(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val repeatsYearly: Boolean,
    val remindDaysBefore: Int,
)

data class UpcomingOccasion(
    val occasion: OccasionInfo,
    val nextDate: LocalDate,
    val daysUntil: Int,
)

data class Ranked(
    val candidate: Candidate,
    /** Days since last contact ÷ target days. [Double.POSITIVE_INFINITY] if never contacted. */
    val ratio: Double,
    /** An occasion inside its reminder window that has not been acknowledged by a contact yet. */
    val occasion: UpcomingOccasion?,
) {
    val personId: Long get() = candidate.personId
}
