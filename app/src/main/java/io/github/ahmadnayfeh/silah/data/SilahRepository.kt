package io.github.ahmadnayfeh.silah.data

import androidx.room.withTransaction
import io.github.ahmadnayfeh.silah.domain.Candidate
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.OccasionInfo
import io.github.ahmadnayfeh.silah.domain.Source
import io.github.ahmadnayfeh.silah.domain.StoredPick
import io.github.ahmadnayfeh.silah.domain.TodayDecision
import io.github.ahmadnayfeh.silah.domain.TodayPlanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A consistent view of all stored data. The data set is small, so it is loaded whole. */
data class Snapshot(
    val people: List<Person> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val threads: List<Thread> = emptyList(),
    val occasions: List<Occasion> = emptyList(),
) {
    private val lastByPerson: Map<Long, Long> by lazy {
        contacts.groupBy { it.personId }.mapValues { (_, list) -> list.maxOf { it.dateTime } }
    }

    fun lastContactMillis(personId: Long): Long? = lastByPerson[personId]
    fun person(id: Long): Person? = people.firstOrNull { it.id == id }
    fun openThreads(personId: Long): List<Thread> = threads.filter { it.personId == personId && !it.isDone }
    fun occasionsOf(personId: Long): List<Occasion> = occasions.filter { it.personId == personId }
    fun contactsOf(personId: Long): List<Contact> =
        contacts.filter { it.personId == personId }.sortedByDescending { it.dateTime }

    /** The selection logic's view of everyone, with dates in the given time zone. */
    fun candidates(zone: ZoneId): List<Candidate> {
        fun date(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return people.map { p ->
            Candidate(
                personId = p.id,
                targetDays = p.targetDays,
                isPaused = p.isPaused,
                createdOn = date(p.createdAt),
                lastContact = lastContactMillis(p.id)?.let(::date),
                skippedOn = p.skippedOn?.let(LocalDate::ofEpochDay),
                occasions = occasionsOf(p.id).map { it.toInfo() },
            )
        }
    }
}

class SilahRepository(
    private val db: SilahDatabase,
    val store: SettingsStore,
    val clock: Clock,
) {
    private val dao = db.dao()

    val snapshot: Flow<Snapshot> =
        combine(dao.people(), dao.contacts(), dao.threads(), dao.occasions()) { p, c, t, o -> Snapshot(p, c, t, o) }

    suspend fun currentSnapshot(): Snapshot =
        Snapshot(dao.allPeople(), dao.allContacts(), dao.allThreads(), dao.allOccasions())

    fun now(): Long = clock.millis()
    fun today(): LocalDate = LocalDate.now(clock)
    fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(clock.zone).toLocalDate()

    fun daysSince(millis: Long?): Long? =
        millis?.let { java.time.temporal.ChronoUnit.DAYS.between(dateOf(it), today()).coerceAtLeast(0) }

    fun candidates(s: Snapshot): List<Candidate> = s.candidates(clock.zone)

    /** Pure decision for today given a snapshot and the remembered pick. */
    fun decide(s: Snapshot, state: AppState): Pair<TodayDecision, StoredPick?> {
        val stored = if (state.pickDay != null && state.pickPerson != null) {
            StoredPick(LocalDate.ofEpochDay(state.pickDay), state.pickPerson)
        } else {
            null
        }
        return TodayPlanner.decide(candidates(s), stored, today())
    }

    /** Decides today's person and remembers the choice. */
    suspend fun decideToday(): TodayDecision {
        val (decision, pick) = decide(currentSnapshot(), store.currentState())
        rememberPick(pick)
        return decision
    }

    suspend fun rememberPick(pick: StoredPick?) {
        store.updateState { st ->
            val keepDay = pick?.date?.toEpochDay()
            if (st.pickDay == keepDay && st.pickPerson == pick?.personId) st
            else st.copy(pickDay = keepDay, pickPerson = pick?.personId)
        }
    }

    /** Forget today's pick so the next decision picks afresh ("suggest someone else"). */
    suspend fun clearPick() = store.updateState { it.copy(pickDay = null, pickPerson = null) }

    // ---- people ----

    suspend fun addPerson(person: Person, approxLastContactDaysAgo: Int? = null): Long {
        val id = dao.insert(person.copy(id = 0, createdAt = now()))
        if (approxLastContactDaysAgo != null) {
            val at = now() - approxLastContactDaysAgo * DAY_MILLIS
            dao.insert(Contact(personId = id, dateTime = at, direction = Direction.ME, channel = Channel.OTHER, source = Source.MANUAL))
        }
        return id
    }

    suspend fun updatePerson(person: Person) = dao.update(person)
    suspend fun deletePerson(person: Person) = dao.delete(person)
    suspend fun person(id: Long): Person? = dao.person(id)

    suspend fun setPaused(id: Long, paused: Boolean) {
        dao.person(id)?.let { dao.update(it.copy(isPaused = paused)) }
    }

    suspend fun setNote(id: Long, note: String) {
        dao.person(id)?.let { dao.update(it.copy(note = note)) }
    }

    /** "Not today": hide for today (and 3 days unless an occasion is near). */
    suspend fun skipToday(id: Long) {
        dao.setSkipped(id, today().toEpochDay())
        store.updateState { st -> if (st.pickPerson == id) st.copy(pickDay = null, pickPerson = null) else st }
    }

    // ---- contacts ----

    suspend fun logContact(personId: Long, direction: Direction, channel: Channel, source: Source): Long =
        dao.insert(Contact(personId = personId, dateTime = now(), direction = direction, channel = channel, source = source))

    suspend fun deleteContact(id: Long) = dao.deleteContact(id)

    // ---- threads ----

    suspend fun addThread(personId: Long, text: String) {
        val clean = text.trim().replace('\n', ' ')
        if (clean.isNotEmpty()) dao.insert(Thread(personId = personId, text = clean, createdAt = now()))
    }

    suspend fun setThreadDone(thread: Thread, done: Boolean) = dao.update(thread.copy(isDone = done))
    suspend fun deleteThread(id: Long) = dao.deleteThread(id)

    // ---- occasions ----

    suspend fun addOccasion(occasion: Occasion) = dao.insert(occasion.copy(id = 0))
    suspend fun deleteOccasion(id: Long) = dao.deleteOccasion(id)

    // ---- whole database ----

    suspend fun replaceAll(people: List<Person>, contacts: List<Contact>, threads: List<Thread>, occasions: List<Occasion>) {
        db.withTransaction {
            clearTables()
            dao.insertPeople(people)
            dao.insertContacts(contacts)
            dao.insertThreads(threads)
            dao.insertOccasions(occasions)
        }
    }

    suspend fun eraseAll() {
        db.withTransaction { clearTables() }
        store.clear()
    }

    private suspend fun clearTables() {
        dao.clearContacts()
        dao.clearThreads()
        dao.clearOccasions()
        dao.clearPeople()
    }

    companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}

fun Occasion.toInfo() = OccasionInfo(id, title, LocalDate.ofEpochDay(date), repeatsYearly, remindDaysBefore)
