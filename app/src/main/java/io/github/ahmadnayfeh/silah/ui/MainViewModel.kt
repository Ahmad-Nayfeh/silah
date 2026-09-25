package io.github.ahmadnayfeh.silah.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ahmadnayfeh.silah.AppContainer
import io.github.ahmadnayfeh.silah.data.AppState
import io.github.ahmadnayfeh.silah.data.Occasion
import io.github.ahmadnayfeh.silah.data.Person
import io.github.ahmadnayfeh.silah.data.Settings
import io.github.ahmadnayfeh.silah.data.Snapshot
import io.github.ahmadnayfeh.silah.data.Thread
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.Rhythm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel as EventChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiEvent {
    /** A contact was just recorded; offer "undo" for 10 seconds. */
    data class Logged(val contactId: Long, val message: String) : UiEvent
    data class Message(val text: String) : UiEvent
}

class MainViewModel(private val c: AppContainer) : ViewModel() {
    private val repo = c.repo
    private val store = c.store

    /** Bumped when the app comes back to the foreground, so "today" is re-evaluated. */
    private val tick = MutableStateFlow(0)

    private val events = EventChannel<UiEvent>(EventChannel.BUFFERED)
    val uiEvents = events.receiveAsFlow()

    val snapshot: StateFlow<Snapshot?> = repo.snapshot.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings: StateFlow<Settings> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val appState: StateFlow<AppState?> = store.state.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun builder() = UiBuilder(repo.today(), repo.clock.zone)

    fun todayDate(): java.time.LocalDate = repo.today()

    val today: StateFlow<TodayUi> =
        combine(repo.snapshot, store.state, store.settings, tick) { s, st, set, _ ->
            val (decision, pick) = repo.decide(s, st)
            if (pick?.date?.toEpochDay() != st.pickDay || pick?.personId != st.pickPerson) {
                viewModelScope.launch { repo.rememberPick(pick) }
            }
            builder().today(s, set, decision)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, TodayUi.Loading)

    val people: StateFlow<List<PersonRow>?> =
        combine(repo.snapshot, store.settings, tick) { s, set, _ -> builder().people(s, set) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val stats: StateFlow<StatsUi?> =
        combine(repo.snapshot, tick) { s, _ -> builder().stats(s) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val perWeekAuto: StateFlow<Int> = repo.snapshot
        .map { s -> Rhythm.weeklyNeeded(s.people.filterNot { it.isPaused }.map { it.targetDays }) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun personDetail(id: Long) = combine(repo.snapshot, store.settings, tick) { s, set, _ -> builder().person(s, set, id) }

    fun onResume() {
        tick.value++
        viewModelScope.launch { c.reminders.onSystemEvent() }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    // ---- contacting ----

    fun whatsApp(personId: Long) = launch {
        val id = c.contactedViaWhatsApp(personId)
        events.send(UiEvent.Logged(id, "سُجّل تواصل عبر واتساب"))
    }

    fun call(personId: Long) = launch {
        val id = c.contactedViaCall(personId)
        events.send(UiEvent.Logged(id, "سُجّل اتصال"))
    }

    fun done(personId: Long, direction: Direction, channel: Channel) = launch {
        val id = c.markDone(personId, direction, channel)
        events.send(UiEvent.Logged(id, "سُجّل التواصل"))
    }

    fun theyReachedOut(personId: Long) = done(personId, Direction.THEM, Channel.WHATSAPP)

    fun undo(contactId: Long) = launch { c.undoContact(contactId) }

    fun notToday(personId: Long) = launch { c.notToday(personId) }

    fun suggestAnother() = launch { c.suggestAnother() }

    fun deleteContact(contactId: Long) = launch {
        repo.deleteContact(contactId)
        c.afterChange()
    }

    // ---- threads ----

    fun addThread(personId: Long, text: String) = launch {
        repo.addThread(personId, text)
        c.afterChange()
    }

    fun answerThreadPrompt(personId: Long, text: String) = launch {
        if (text.isNotBlank()) repo.addThread(personId, text)
        c.dismissThreadPrompt()
        c.afterChange()
    }

    fun dismissThreadPrompt() = launch { c.dismissThreadPrompt() }

    fun setThreadDone(thread: Thread, done: Boolean) = launch {
        repo.setThreadDone(thread, done)
        c.afterChange()
    }

    fun deleteThread(id: Long) = launch {
        repo.deleteThread(id)
        c.afterChange()
    }

    // ---- people ----

    fun savePerson(person: Person, approxLastContactDaysAgo: Int?, onSaved: (Long) -> Unit = {}) = launch {
        val id = if (person.id == 0L) {
            repo.addPerson(person, approxLastContactDaysAgo)
        } else {
            val existing = repo.person(person.id)
            repo.updatePerson(person.copy(createdAt = existing?.createdAt ?: person.createdAt, skippedOn = existing?.skippedOn))
            person.id
        }
        c.afterChange()
        withContext(Dispatchers.Main) { onSaved(id) }
    }

    fun deletePerson(person: Person) = launch {
        repo.deletePerson(person)
        c.afterChange()
    }

    fun setPaused(id: Long, paused: Boolean) = launch {
        repo.setPaused(id, paused)
        c.afterChange()
    }

    fun setNote(id: Long, note: String) = launch { repo.setNote(id, note) }

    fun addOccasion(occasion: Occasion) = launch {
        repo.addOccasion(occasion)
        c.afterChange()
    }

    fun deleteOccasion(id: Long) = launch {
        repo.deleteOccasion(id)
        c.afterChange()
    }

    // ---- settings ----

    fun updateSettings(transform: (Settings) -> Settings) = launch {
        store.updateSettings(transform)
        c.reminders.reschedule()
        c.afterChange()
    }

    fun finishOnboarding() = launch { store.updateState { it.copy(onboarded = true) } }

    fun exportTo(resolver: ContentResolver, uri: Uri) = launch {
        val ok = runCatching {
            val text = c.backup.export()
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            }
        }.isSuccess
        events.send(UiEvent.Message(if (ok) "حُفظت النسخة الاحتياطية" else "تعذّر حفظ النسخة الاحتياطية"))
    }

    fun importFrom(resolver: ContentResolver, uri: Uri) = launch {
        val result = runCatching {
            val text = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            }
            c.restore(c.backup.parse(text))
        }
        events.send(UiEvent.Message(if (result.isSuccess) "استُعيدت البيانات كاملة" else "هذا الملف ليس نسخة احتياطية من صِلة"))
    }

    fun eraseAll() = launch {
        c.eraseAll()
        events.send(UiEvent.Message("مُسحت كل البيانات"))
    }
}
