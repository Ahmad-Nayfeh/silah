package io.github.ahmadnayfeh.silah

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.ahmadnayfeh.silah.data.BackupFile
import io.github.ahmadnayfeh.silah.data.BackupManager
import io.github.ahmadnayfeh.silah.data.SettingsStore
import io.github.ahmadnayfeh.silah.data.SilahDatabase
import io.github.ahmadnayfeh.silah.data.SilahRepository
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.Source
import io.github.ahmadnayfeh.silah.notify.AlarmScheduler
import io.github.ahmadnayfeh.silah.notify.Reminders
import io.github.ahmadnayfeh.silah.widget.SilahWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/**
 * Hand-wired dependencies, plus the user actions shared by the app, the notification
 * and the widget. Every action ends with [afterChange] so all three stay in agreement.
 */
class AppContainer(
    private val context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
    val database: SilahDatabase = SilahDatabase.create(context),
    dataStoreName: String = "silah",
    /** Tests turn this off: widget hosts do not exist there. */
    private val updateWidgets: Boolean = true,
) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val store = SettingsStore(
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile(dataStoreName) }),
    )
    val repo = SilahRepository(database, store, clock)
    val scheduler = AlarmScheduler(context, clock)
    val reminders = Reminders(context, repo, scheduler)
    val backup = BackupManager(repo)

    /** Keeps the notification and the home-screen widget in step with the data. */
    suspend fun afterChange() {
        reminders.refresh()
        updateWidget()
    }

    suspend fun updateWidget() {
        if (updateWidgets) runCatching { SilahWidget.updateAll(context) }
    }

    /** WhatsApp was opened for this person: record it right away (undo is offered in the app). */
    suspend fun contactedViaWhatsApp(personId: Long): Long = autoContact(personId, Channel.WHATSAPP)

    /** The dialer was opened for this person. */
    suspend fun contactedViaCall(personId: Long): Long = autoContact(personId, Channel.CALL)

    private suspend fun autoContact(personId: Long, channel: Channel): Long {
        val id = repo.logContact(personId, Direction.ME, channel, Source.AUTO)
        store.updateState { it.copy(threadPromptPerson = personId, threadPromptAt = repo.now()) }
        afterChange()
        return id
    }

    /** "Done": a contact that happened some other way. */
    suspend fun markDone(personId: Long, direction: Direction, channel: Channel): Long {
        val id = repo.logContact(personId, direction, channel, Source.MANUAL)
        afterChange()
        return id
    }

    suspend fun undoContact(contactId: Long) {
        repo.deleteContact(contactId)
        store.updateState { it.copy(threadPromptPerson = null, threadPromptAt = null) }
        afterChange()
    }

    suspend fun notToday(personId: Long) {
        repo.skipToday(personId)
        afterChange()
    }

    /** After today's person is done: offer one more, if anyone is due. */
    suspend fun suggestAnother() {
        repo.clearPick()
        afterChange()
    }

    suspend fun dismissThreadPrompt() = store.updateState { it.copy(threadPromptPerson = null, threadPromptAt = null) }

    suspend fun restore(file: BackupFile) {
        backup.restore(file)
        reminders.reschedule()
        afterChange()
    }

    suspend fun eraseAll() {
        repo.eraseAll()
        reminders.cancel()
        reminders.reschedule()
        afterChange()
    }
}
