package io.github.ahmadnayfeh.silah.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.ahmadnayfeh.silah.MainActivity
import io.github.ahmadnayfeh.silah.R
import io.github.ahmadnayfeh.silah.data.Person
import io.github.ahmadnayfeh.silah.data.SilahRepository
import io.github.ahmadnayfeh.silah.domain.ArabicText
import io.github.ahmadnayfeh.silah.domain.Ranked
import io.github.ahmadnayfeh.silah.domain.Rhythm
import io.github.ahmadnayfeh.silah.domain.SelectionEngine
import io.github.ahmadnayfeh.silah.domain.StoredPick
import io.github.ahmadnayfeh.silah.domain.TodayDecision
import io.github.ahmadnayfeh.silah.domain.TodayPlanner
import java.time.LocalTime

/**
 * The single reminder notification and every event that touches it.
 *
 * "Live" means: today is a day to remind, and the reminder should stay visible until
 * the person is contacted (WhatsApp / done) or the night quiet period begins.
 */
class Reminders(
    private val context: Context,
    private val repo: SilahRepository,
    private val scheduler: AlarmScheduler,
) {
    private val store = repo.store
    private val manager = NotificationManagerCompat.from(context)

    private fun minuteNow(): Int = LocalTime.now(repo.clock).let { it.hour * 60 + it.minute }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "تذكير اليوم", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "اقتراح واحد في اليوم للتواصل مع شخص تحبه"
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    suspend fun reschedule() = scheduler.scheduleAll(store.currentSettings())

    /** The reminder time has come. */
    suspend fun onDailyAlarm() {
        val settings = store.currentSettings()
        val today = repo.today()
        val state = store.currentState()
        store.updateState { it.copy(handledDay = today.toEpochDay()) }
        if (!settings.isQuiet(minuteNow()) && state.liveDay != today.toEpochDay()) {
            val candidates = repo.candidates(repo.currentSnapshot())
            val perWeek = Rhythm.perWeek(candidates, settings.autoRhythm, settings.manualPerWeek)
            if (TodayPlanner.shouldNotify(candidates, today, perWeek, settings.allowedDays)) {
                store.updateState { it.copy(liveDay = today.toEpochDay()) }
            }
        }
        refresh()
        scheduler.scheduleAll(settings)
    }

    /** Night falls: put the reminder away, and remember it for the morning. */
    suspend fun onQuietStart() {
        val state = store.currentState()
        if (state.liveDay != null) {
            val decision = repo.decideToday()
            val carry = (decision as? TodayDecision.Suggest)?.ranked?.personId
            store.updateState { it.copy(liveDay = null, carryPerson = carry) }
        }
        cancel()
        reschedule()
    }

    /** Morning: bring back yesterday's reminder if that person is still due. */
    suspend fun onQuietEnd() {
        val carry = store.currentState().carryPerson
        if (carry != null) {
            store.updateState { it.copy(carryPerson = null) }
            val today = repo.today()
            val eligible = SelectionEngine.eligible(repo.candidates(repo.currentSnapshot()), today)
            if (eligible.any { it.personId == carry }) {
                repo.rememberPick(StoredPick(today, carry))
                store.updateState { it.copy(liveDay = today.toEpochDay()) }
            }
        }
        refresh()
        reschedule()
    }

    /** After boot, app update, clock change or app start: re-arm and catch up on anything missed. */
    suspend fun onSystemEvent() {
        ensureChannel()
        reschedule()
        val settings = store.currentSettings()
        val today = repo.today().toEpochDay()
        val state = store.currentState()
        val now = minuteNow()
        if (state.handledDay != today && now >= settings.notifyMinute && !settings.isQuiet(now)) {
            onDailyAlarm()
        } else {
            refresh()
        }
    }

    /** The user swiped the reminder away (allowed on Android 14+). It is meant to stay, so it returns. */
    suspend fun onSwiped() = refresh()

    /** Shows, updates or removes the reminder to match the current data. */
    suspend fun refresh() {
        val settings = store.currentSettings()
        val today = repo.today().toEpochDay()
        val state = store.currentState()
        if (state.liveDay != today || settings.isQuiet(minuteNow())) {
            cancel()
            return
        }
        when (val decision = repo.decideToday()) {
            is TodayDecision.Suggest -> {
                val snapshot = repo.currentSnapshot()
                val person = snapshot.person(decision.ranked.personId) ?: return cancel()
                post(
                    person = person,
                    ranked = decision.ranked,
                    lastContactMillis = snapshot.lastContactMillis(person.id),
                    threads = snapshot.openThreads(person.id).map { it.text },
                    silent = state.alertedDay == today,
                )
                store.updateState { it.copy(alertedDay = today) }
            }
            else -> cancel()
        }
    }

    fun cancel() = manager.cancel(NOTIFICATION_ID)

    fun isShowing(): Boolean =
        context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == NOTIFICATION_ID }

    private fun post(person: Person, ranked: Ranked, lastContactMillis: Long?, threads: List<String>, silent: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel()
        val occasion = ranked.occasion
        val title = if (occasion != null) {
            ArabicText.occasion(occasion.occasion.title, person.name, occasion.daysUntil)
        } else {
            "اطمئن على ${person.name}"
        }
        val lines = buildList {
            add(ArabicText.lastContact(repo.daysSince(lastContactMillis)))
            threads.take(3).forEach { add("• $it") }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand))
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(silent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppIntent())
            .setDeleteIntent(broadcast(ActionReceiver.ACTION_SWIPED, person.id, REQ_SWIPED))
            .addAction(0, "واتساب", ActionActivity.pendingIntent(context, person.id, ActionActivity.SOURCE_NOTIFICATION))
            .addAction(0, "تم", broadcast(ActionReceiver.ACTION_DONE, person.id, REQ_DONE))
            .addAction(0, "ليس اليوم", broadcast(ActionReceiver.ACTION_NOT_TODAY, person.id, REQ_NOT_TODAY))
            .build()
        @Suppress("MissingPermission")
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQ_OPEN,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun broadcast(action: String, personId: Long, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, ActionReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("silah://person/$personId"))
            .putExtra(EXTRA_PERSON, personId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_ID = "today"
        const val NOTIFICATION_ID = 1
        const val EXTRA_PERSON = "person_id"
        private const val REQ_OPEN = 10
        private const val REQ_DONE = 11
        private const val REQ_NOT_TODAY = 12
        private const val REQ_SWIPED = 13
    }
}
