package io.github.ahmadnayfeh.silah.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ahmadnayfeh.silah.AppContainer
import io.github.ahmadnayfeh.silah.container
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import kotlinx.coroutines.launch

/** Runs suspend work from a receiver without the system killing it midway. */
private fun BroadcastReceiver.runAsync(context: Context, block: suspend (AppContainer) -> Unit) {
    val pending = goAsync()
    val container = context.container
    container.appScope.launch {
        try {
            block(container)
        } finally {
            pending?.finish()
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        runAsync(context) { handle(it, action) }
    }

    companion object {
        const val ACTION_DAILY = "io.github.ahmadnayfeh.silah.DAILY"
        const val ACTION_QUIET_START = "io.github.ahmadnayfeh.silah.QUIET_START"
        const val ACTION_QUIET_END = "io.github.ahmadnayfeh.silah.QUIET_END"

        suspend fun handle(container: AppContainer, action: String) {
            when (action) {
                ACTION_DAILY -> container.reminders.onDailyAlarm()
                ACTION_QUIET_START -> container.reminders.onQuietStart()
                ACTION_QUIET_END -> container.reminders.onQuietEnd()
            }
            // "X days ago" changes with the date, so the widget refreshes too.
            container.updateWidget()
        }
    }
}

/** The notification's "done" and "not today" buttons, and its swipe-away. Work without opening the app. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val personId = intent.getLongExtra(Reminders.EXTRA_PERSON, -1)
        runAsync(context) { handle(it, action, personId) }
    }

    companion object {
        const val ACTION_DONE = "io.github.ahmadnayfeh.silah.DONE"
        const val ACTION_NOT_TODAY = "io.github.ahmadnayfeh.silah.NOT_TODAY"
        const val ACTION_SWIPED = "io.github.ahmadnayfeh.silah.SWIPED"

        suspend fun handle(container: AppContainer, action: String, personId: Long) {
            when (action) {
                ACTION_DONE -> if (personId > 0) container.markDone(personId, Direction.ME, Channel.OTHER)
                ACTION_NOT_TODAY -> if (personId > 0) container.notToday(personId)
                ACTION_SWIPED -> container.reminders.onSwiped()
            }
        }
    }
}

/** Boot, app update and clock changes: alarms are re-armed and the reminder restored. */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync(context) {
            it.reminders.onSystemEvent()
            it.updateWidget()
        }
    }
}
