package io.github.ahmadnayfeh.silah.ui

import io.github.ahmadnayfeh.silah.data.Person
import io.github.ahmadnayfeh.silah.data.Settings
import io.github.ahmadnayfeh.silah.data.Snapshot
import io.github.ahmadnayfeh.silah.data.Thread
import io.github.ahmadnayfeh.silah.data.toInfo
import io.github.ahmadnayfeh.silah.domain.ArabicText
import io.github.ahmadnayfeh.silah.domain.Candidate
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.MonthCoverage
import io.github.ahmadnayfeh.silah.domain.Occasions
import io.github.ahmadnayfeh.silah.domain.SelectionEngine
import io.github.ahmadnayfeh.silah.domain.Source
import io.github.ahmadnayfeh.silah.domain.StatContact
import io.github.ahmadnayfeh.silah.domain.StatPerson
import io.github.ahmadnayfeh.silah.domain.Stats
import io.github.ahmadnayfeh.silah.domain.Tag
import io.github.ahmadnayfeh.silah.domain.TodayDecision
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class TodayCard(
    val personId: Long,
    val name: String,
    val tag: Tag,
    val tagName: String,
    val phone: String?,
    val lastContactText: String,
    val occasionText: String?,
    val threads: List<Thread>,
)

sealed interface TodayUi {
    data object Loading : TodayUi
    data object NoPeople : TodayUi
    data class Suggest(val card: TodayCard) : TodayUi
    data class Done(val name: String, val othersAvailable: Boolean) : TodayUi
    data object NoOne : TodayUi
}

data class PersonRow(
    val id: Long,
    val name: String,
    val tag: Tag,
    val tagName: String,
    val lastText: String,
    val everyText: String,
    val isPaused: Boolean,
    val occasionText: String?,
)

data class OccasionRow(
    val id: Long,
    val title: String,
    val dateText: String,
    val repeatsYearly: Boolean,
    val nextText: String?,
)

data class ContactRow(
    val id: Long,
    val dateText: String,
    val direction: Direction,
    val channel: Channel,
    val source: Source,
)

data class PersonDetailUi(
    val person: Person,
    val tagName: String,
    val lastContactText: String,
    val everyText: String,
    val openThreads: List<Thread>,
    val doneThreads: List<Thread>,
    val occasions: List<OccasionRow>,
    val timeline: List<ContactRow>,
)

data class StatRow(
    val personId: Long,
    val name: String,
    val tag: Tag,
    val intervalText: String,
    val byMe: Int,
    val byThem: Int,
)

data class StatsUi(
    val current: MonthCoverage,
    val months: List<MonthCoverage>,
    val rows: List<StatRow>,
)

/** Turns stored data into what the screens show. Pure: easy to test and to preview. */
class UiBuilder(private val today: LocalDate, private val zone: ZoneId) {

    private fun date(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    private fun daysSince(millis: Long?): Long? = millis?.let { ChronoUnit.DAYS.between(date(it), today).coerceAtLeast(0) }

    fun candidates(s: Snapshot): List<Candidate> = s.candidates(zone)

    fun today(s: Snapshot, settings: Settings, decision: TodayDecision): TodayUi {
        if (s.people.isEmpty()) return TodayUi.NoPeople
        return when (decision) {
            is TodayDecision.Suggest -> {
                val p = s.person(decision.ranked.personId) ?: return TodayUi.NoOne
                val occ = decision.ranked.occasion
                TodayUi.Suggest(
                    TodayCard(
                        personId = p.id,
                        name = p.name,
                        tag = p.tag,
                        tagName = settings.tagName(p.tag),
                        phone = p.phone,
                        lastContactText = ArabicText.lastContact(daysSince(s.lastContactMillis(p.id))),
                        occasionText = occ?.let { ArabicText.occasion(it.occasion.title, p.name, it.daysUntil) },
                        threads = s.openThreads(p.id).take(3),
                    ),
                )
            }
            is TodayDecision.Done -> TodayUi.Done(s.person(decision.personId)?.name.orEmpty(), decision.othersAvailable)
            TodayDecision.NoOne -> TodayUi.NoOne
        }
    }

    /** Most due first; paused people at the end. */
    fun people(s: Snapshot, settings: Settings): List<PersonRow> {
        val cands = candidates(s)
        val ranked = SelectionEngine.rank(cands, today).map { it.personId }
        val paused = s.people.filter { it.isPaused }.sortedBy { it.name }.map { it.id }
        return (ranked + paused).mapNotNull { id ->
            val p = s.person(id) ?: return@mapNotNull null
            val since = daysSince(s.lastContactMillis(id))
            val occ = Occasions.upcomingAll(s.occasionsOf(id).map { it.toInfo() }, today).firstOrNull()
            PersonRow(
                id = id,
                name = p.name,
                tag = p.tag,
                tagName = settings.tagName(p.tag),
                lastText = since?.let { ArabicText.ago(it) } ?: "لم تتواصلا بعد",
                everyText = ArabicText.every(p.targetDays),
                isPaused = p.isPaused,
                occasionText = occ?.let { "${it.occasion.title} ${ArabicText.inDays(it.daysUntil)}" },
            )
        }
    }

    fun person(s: Snapshot, settings: Settings, id: Long): PersonDetailUi? {
        val p = s.person(id) ?: return null
        val threads = s.threads.filter { it.personId == id }
        return PersonDetailUi(
            person = p,
            tagName = settings.tagName(p.tag),
            lastContactText = ArabicText.lastContact(daysSince(s.lastContactMillis(id))),
            everyText = ArabicText.every(p.targetDays),
            openThreads = threads.filter { !it.isDone }.sortedByDescending { it.createdAt },
            doneThreads = threads.filter { it.isDone }.sortedByDescending { it.createdAt },
            occasions = s.occasionsOf(id).map { o ->
                val info = o.toInfo()
                val next = Occasions.nextOccurrence(info, today)
                OccasionRow(
                    id = o.id,
                    title = o.title,
                    dateText = if (o.repeatsYearly) ArabicText.dayMonth(info.date) else ArabicText.date(info.date),
                    repeatsYearly = o.repeatsYearly,
                    nextText = next?.let { ArabicText.inDays(ChronoUnit.DAYS.between(today, it).toInt()) },
                )
            }.sortedBy { it.nextText == null },
            timeline = s.contactsOf(id).map { c ->
                val at = Instant.ofEpochMilli(c.dateTime).atZone(zone)
                ContactRow(
                    id = c.id,
                    dateText = "${ArabicText.date(at.toLocalDate())} · ${ArabicText.time(at.hour * 60 + at.minute)}",
                    direction = c.direction,
                    channel = c.channel,
                    source = c.source,
                )
            },
        )
    }

    fun stats(s: Snapshot): StatsUi {
        val people = s.people.map { StatPerson(it.id, date(it.createdAt), it.isPaused, it.targetDays) }
        val contacts = s.contacts.map { StatContact(it.personId, date(it.dateTime), it.direction) }
        val months = Stats.lastMonths(people, contacts, today)
        val perPerson = Stats.perPerson(people.filterNot { it.isPaused }, contacts).associateBy { it.personId }
        val rows = s.people.filterNot { it.isPaused }.mapNotNull { p ->
            val st = perPerson[p.id] ?: return@mapNotNull null
            StatRow(
                personId = p.id,
                name = p.name,
                tag = p.tag,
                intervalText = ArabicText.intervalVsTarget(st.averageInterval, p.targetDays),
                byMe = st.byMe,
                byThem = st.byThem,
            )
        }.sortedBy { it.name }
        return StatsUi(current = months.last(), months = months, rows = rows)
    }
}

fun channelName(c: Channel): String = when (c) {
    Channel.WHATSAPP -> "واتساب"
    Channel.CALL -> "اتصال"
    Channel.VISIT -> "زيارة"
    Channel.OTHER -> "أخرى"
}
