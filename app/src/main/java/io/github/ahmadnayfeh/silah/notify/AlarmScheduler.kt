package io.github.ahmadnayfeh.silah.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.ahmadnayfeh.silah.data.Settings
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Three daily alarms: the reminder time, the start of the night quiet period
 * (reminder is put away) and its end (a put-away reminder comes back).
 */
class AlarmScheduler(private val context: Context, private val clock: Clock) {

    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun scheduleAll(settings: Settings) {
        schedule(AlarmReceiver.ACTION_DAILY, REQ_DAILY, settings.notifyMinute)
        schedule(AlarmReceiver.ACTION_QUIET_START, REQ_QUIET_START, settings.quietStartMinute)
        schedule(AlarmReceiver.ACTION_QUIET_END, REQ_QUIET_END, settings.quietEndMinute)
    }

    /** Next moment (strictly in the future) that the clock shows [minuteOfDay]. */
    fun nextTrigger(minuteOfDay: Int): Long {
        val now = ZonedDateTime.now(clock)
        var at = LocalDate.now(clock).atStartOfDay(clock.zone).plusMinutes(minuteOfDay.toLong())
        if (!at.isAfter(now)) {
            at = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.zone).plusMinutes(minuteOfDay.toLong())
        }
        return at.toInstant().toEpochMilli()
    }

    private fun schedule(action: String, requestCode: Int, minuteOfDay: Int) {
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val at = nextTrigger(minuteOfDay)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exactAllowed) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    companion object {
        const val REQ_DAILY = 100
        const val REQ_QUIET_START = 101
        const val REQ_QUIET_END = 102
    }
}
