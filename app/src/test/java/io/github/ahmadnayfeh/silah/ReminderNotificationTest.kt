package io.github.ahmadnayfeh.silah

import android.app.AlarmManager
import android.app.Notification
import android.content.Intent
import io.github.ahmadnayfeh.silah.data.Occasion
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.Source
import io.github.ahmadnayfeh.silah.notify.ActionActivity
import io.github.ahmadnayfeh.silah.notify.AlarmReceiver
import io.github.ahmadnayfeh.silah.notify.SystemReceiver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The reminder: appears on time, buttons work without opening the app,
 * survives swipe-away and reboot, respects the silent days and the night.
 *
 * With two weekly people the rhythm needs 2 nudges a week → Saturday and Tuesday.
 * 26 Sep 2026 is a Saturday; the default reminder time is 7:00 pm.
 */
@RunWith(RobolectricTestRunner::class)
class ReminderNotificationTest {
    private lateinit var t: TestApp
    private var sara = 0L
    private var khalid = 0L
    private val saturday7pm = LocalDateTime.of(2026, 9, 26, 19, 0)

    @Before
    fun setUp() {
        t = TestApp(saturday7pm)
        sara = t.addPerson("سارة", targetDays = 7, lastContactDaysAgo = 30)
        khalid = t.addPerson("خالد", targetDays = 7, lastContactDaysAgo = 14)
    }

    private fun contactsOf(id: Long) = runBlocking { t.container.repo.currentSnapshot().contactsOf(id) }

    @Test
    fun `reminder appears on a suggestion day with three buttons and is ongoing`() = runBlocking {
        t.container.reminders.onDailyAlarm()
        val n = t.reminder()
        assertNotNull(n)
        assertEquals("اطمئن على سارة", t.title())
        assertEquals(listOf("واتساب", "تم", "ليس اليوم"), n!!.actions.map { it.title.toString() })
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }

    @Test
    fun `a silent day posts nothing`() = runBlocking {
        t.clock.set(LocalDateTime.of(2026, 9, 27, 19, 0)) // Sunday
        t.container.reminders.onDailyAlarm()
        assertNull(t.reminder())
    }

    @Test
    fun `the alarm is set for the reminder time and firing it posts the reminder`() {
        t.clock.set(LocalDateTime.of(2026, 9, 26, 9, 0))
        runBlocking { t.container.reminders.reschedule() }
        val alarms = shadowOf(t.app.getSystemService(AlarmManager::class.java)).scheduledAlarms
        val expected = saturday7pm.atZone(ZoneId.of("Asia/Riyadh")).toInstant().toEpochMilli()
        val daily = alarms.single { shadowOf(it.operation).savedIntent.action == AlarmReceiver.ACTION_DAILY }
        assertEquals(expected, daily.triggerAtMs)
        assertEquals(3, alarms.size) // reminder, quiet start, quiet end

        t.clock.set(saturday7pm)
        daily.operation!!.send()
        t.waitFor("reminder posted by the alarm") { t.reminder() != null }
        assertEquals("اطمئن على سارة", t.title())
    }

    @Test
    fun `not today switches the person in the same notification, without opening the app`() {
        runBlocking { t.container.reminders.onDailyAlarm() }
        t.reminder()!!.actions[2].actionIntent.send()
        t.waitFor("switched to Khalid") { t.title() == "اطمئن على خالد" }
        assertEquals(1, shadowOf(t.notifications).allNotifications.size)
        assertTrue(t.startedActivities().isEmpty())
    }

    @Test
    fun `done logs a contact and removes the reminder, without opening the app`() {
        runBlocking { t.container.reminders.onDailyAlarm() }
        val before = contactsOf(sara).size
        t.reminder()!!.actions[1].actionIntent.send()
        t.waitFor("reminder removed") { t.reminder() == null }
        val logged = contactsOf(sara)
        assertEquals(before + 1, logged.size)
        assertEquals(Source.MANUAL, logged.first().source)
        assertTrue(t.startedActivities().isEmpty())
    }

    @Test
    fun `whatsapp button opens the chat, logs the contact and removes the reminder`() {
        runBlocking { t.container.reminders.onDailyAlarm() }
        val pi = t.reminder()!!.actions[0].actionIntent
        val target = shadowOf(pi).savedIntent
        assertEquals(ActionActivity::class.java.name, target.component?.className)

        // Android starts the invisible screen; it records and hands over to WhatsApp.
        Robolectric.buildActivity(ActionActivity::class.java, target).create()
        t.waitFor("contact logged") { contactsOf(sara).any { it.channel == Channel.WHATSAPP } }
        val c = contactsOf(sara).first { it.channel == Channel.WHATSAPP }
        assertEquals(Direction.ME, c.direction)
        assertEquals(Source.AUTO, c.source)
        t.waitFor("reminder removed") { t.reminder() == null }
        val opened = mutableListOf<Intent>()
        t.waitFor("WhatsApp opened") { opened += t.startedActivities(); opened.any { it.dataString != null } }
        assertEquals("https://wa.me/966500000000", opened.first { it.dataString != null }.dataString)
    }

    @Test
    fun `a swiped-away reminder comes straight back`() {
        runBlocking { t.container.reminders.onDailyAlarm() }
        val deleteIntent = t.reminder()!!.deleteIntent
        t.notifications.cancelAll() // what the system does when the user swipes it (Android 14+)
        assertNull(t.reminder())
        deleteIntent.send()
        t.waitFor("reminder re-posted") { t.reminder() != null }
        assertEquals("اطمئن على سارة", t.title())
    }

    @Test
    fun `night quiet period puts the reminder away and the morning brings it back`() = runBlocking {
        t.container.reminders.onDailyAlarm()
        assertNotNull(t.reminder())

        t.clock.set(LocalDateTime.of(2026, 9, 26, 22, 30))
        t.container.reminders.onQuietStart()
        assertNull(t.reminder())

        // Sunday is a silent day, yet the unanswered reminder returns in the morning.
        t.clock.set(LocalDateTime.of(2026, 9, 27, 8, 0))
        t.container.reminders.onQuietEnd()
        assertEquals("اطمئن على سارة", t.title())
    }

    @Test
    fun `after a reboot the alarms are re-armed and the reminder is restored`() {
        runBlocking { t.container.reminders.onDailyAlarm() }
        // A reboot clears notifications and alarms.
        t.notifications.cancelAll()
        val alarmManager = t.app.getSystemService(AlarmManager::class.java)
        shadowOf(alarmManager).scheduledAlarms.forEach { alarmManager.cancel(it.operation!!) }
        assertEquals(0, shadowOf(alarmManager).scheduledAlarms.size)

        t.clock.set(LocalDateTime.of(2026, 9, 26, 20, 0))
        SystemReceiver().onReceive(t.app, Intent(Intent.ACTION_BOOT_COMPLETED))
        t.waitFor("reminder restored") { t.reminder() != null }
        assertEquals("اطمئن على سارة", t.title())
        t.waitFor("alarms re-armed") { shadowOf(alarmManager).scheduledAlarms.size == 3 }
    }

    @Test
    fun `a reminder missed while the phone was off is caught up on boot`() {
        t.clock.set(LocalDateTime.of(2026, 9, 26, 21, 0)) // after 7 pm, alarm never fired
        SystemReceiver().onReceive(t.app, Intent(Intent.ACTION_BOOT_COMPLETED))
        t.waitFor("caught-up reminder") { t.reminder() != null }
    }

    @Test
    fun `an occasion speaks even on a silent day`() = runBlocking {
        t.clock.set(LocalDateTime.of(2026, 9, 27, 19, 0)) // silent Sunday
        val nora = t.addPerson("نورة", targetDays = 30, lastContactDaysAgo = 3)
        t.container.repo.addOccasion(
            Occasion(personId = nora, title = "ميلاد", date = LocalDate.of(1995, 9, 28).toEpochDay(), repeatsYearly = true),
        )
        t.container.reminders.onDailyAlarm()
        assertEquals("غداً ميلاد نورة", t.title())
    }

    @Test
    fun `contacting today's person from the app removes the reminder`() = runBlocking {
        t.container.reminders.onDailyAlarm()
        assertNotNull(t.reminder())
        t.container.markDone(sara, Direction.THEM, Channel.CALL)
        assertNull(t.reminder())
    }
}
