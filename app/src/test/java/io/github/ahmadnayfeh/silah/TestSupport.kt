package io.github.ahmadnayfeh.silah

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import io.github.ahmadnayfeh.silah.data.Person
import io.github.ahmadnayfeh.silah.data.SilahDatabase
import io.github.ahmadnayfeh.silah.domain.Tag
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/** A clock the test can move forward. */
class TestClock(var now: Instant, private val zone: ZoneId = ZoneId.of("Asia/Riyadh")) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = TestClock(now, zone)
    override fun instant(): Instant = now

    fun set(dateTime: LocalDateTime) {
        now = dateTime.atZone(zone).toInstant()
    }

    fun plusDays(days: Long) {
        now = now.plusSeconds(days * 86_400)
    }
}

/** Wires a fresh in-memory app with a controllable clock, and installs it as the app's container. */
class TestApp(start: LocalDateTime) {
    val app: Application = ApplicationProvider.getApplicationContext()
    val clock = TestClock(Instant.EPOCH).apply { set(start) }
    val container = AppContainer(
        context = app,
        clock = clock,
        database = SilahDatabase.inMemory(app),
        dataStoreName = "test-" + UUID.randomUUID(),
        updateWidgets = false,
    )
    val notifications: NotificationManager = app.getSystemService(NotificationManager::class.java)

    init {
        (app as SilahApp).replaceContainer(container)
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun addPerson(name: String, targetDays: Int, lastContactDaysAgo: Int?, phone: String? = "+966500000000"): Long =
        runBlocking {
            container.repo.addPerson(
                Person(name = name, tag = Tag.FAMILY, phone = phone, targetDays = targetDays, createdAt = 0),
                lastContactDaysAgo,
            )
        }

    /** The one reminder notification, or null. */
    fun reminder(): Notification? =
        shadowOf(notifications).allNotifications.firstOrNull()

    fun title(): String? = reminder()?.extras?.getString(Notification.EXTRA_TITLE)

    /** Drains the activities the app asked Android to start. */
    fun startedActivities(): List<android.content.Intent> =
        generateSequence { shadowOf(app).nextStartedActivity }.toList()

    /** Background work (receivers use a coroutine scope); wait until [condition] or fail. */
    fun waitFor(message: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            if (System.currentTimeMillis() > deadline) throw AssertionError("Timed out waiting for: $message")
            Thread.sleep(20)
        }
    }
}
