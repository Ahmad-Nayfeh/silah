package io.github.ahmadnayfeh.silah

import android.content.pm.PackageInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ahmadnayfeh.silah.data.BackupFile
import io.github.ahmadnayfeh.silah.data.Occasion
import io.github.ahmadnayfeh.silah.data.Snapshot
import io.github.ahmadnayfeh.silah.data.ThemeMode
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.Source
import io.github.ahmadnayfeh.silah.notify.Launchers
import io.github.ahmadnayfeh.silah.ui.MainViewModel
import io.github.ahmadnayfeh.silah.ui.SilahAppUi
import io.github.ahmadnayfeh.silah.ui.UiBuilder
import io.github.ahmadnayfeh.silah.ui.theme.SilahTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class AppFlowsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val start = LocalDateTime.of(2026, 9, 26, 12, 0)

    private fun snapshot(t: TestApp): Snapshot = runBlocking { t.container.repo.currentSnapshot() }

    @Test
    fun `whatsapp from today's card logs a contact at once, and undo deletes it`() {
        val t = TestApp(start)
        runBlocking { t.container.store.updateState { it.copy(onboarded = true) } }
        val sara = t.addPerson("سارة", targetDays = 7, lastContactDaysAgo = null)
        val vm = MainViewModel(t.container)
        compose.setContent { SilahTheme { SilahAppUi(vm) } }

        compose.waitUntil(5_000) { compose.onAllNodes(hasText("واتساب")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("واتساب").performClick()

        compose.waitUntil(5_000) { snapshot(t).contactsOf(sara).isNotEmpty() }
        val c = snapshot(t).contactsOf(sara).single()
        assertEquals(Channel.WHATSAPP, c.channel)
        assertEquals(Direction.ME, c.direction)
        assertEquals(Source.AUTO, c.source)
        assertTrue(t.startedActivities().any { it.dataString == "https://wa.me/966500000000" })

        compose.waitUntil(5_000) { compose.onAllNodes(hasText("تراجع")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("تراجع").performClick()
        compose.waitUntil(5_000) { snapshot(t).contactsOf(sara).isEmpty() }
    }

    @Test
    fun `whatsapp link goes straight to the installed WhatsApp`() {
        val t = TestApp(start)
        shadowOf(t.app.packageManager).installPackage(PackageInfo().apply { packageName = "com.whatsapp" })
        val intent = Launchers.whatsapp(t.app, "+966501234567")!!
        assertEquals("https://wa.me/966501234567", intent.dataString)
        assertEquals("com.whatsapp", intent.`package`)
    }

    @Test
    fun `backup export then import restores everything`() = runBlocking {
        val t = TestApp(start)
        val c = t.container
        val a = t.addPerson("سارة", 7, 10)
        val b = t.addPerson("خالد", 30, null, phone = null)
        c.repo.logContact(a, Direction.THEM, Channel.CALL, Source.MANUAL)
        c.repo.addThread(a, "اسألها عن السفر")
        c.repo.addOccasion(Occasion(personId = b, title = "ميلاد", date = LocalDate.of(1990, 3, 1).toEpochDay(), repeatsYearly = true))
        c.repo.setNote(b, "يحب القهوة")
        c.repo.setPaused(b, true)
        c.store.updateSettings { it.copy(notifyMinute = 7 * 60, tagFriends = "الرفاق", theme = ThemeMode.LIGHT, autoRhythm = false) }

        val before = c.repo.currentSnapshot()
        val settingsBefore = c.store.currentSettings()
        val json = c.backup.export()

        c.eraseAll()
        assertTrue(c.repo.currentSnapshot().people.isEmpty())

        c.restore(c.backup.parse(json))
        val after = c.repo.currentSnapshot()
        assertEquals(before.people.sortedBy { it.id }, after.people.sortedBy { it.id })
        assertEquals(before.contacts.sortedBy { it.id }, after.contacts.sortedBy { it.id })
        assertEquals(before.threads.sortedBy { it.id }, after.threads.sortedBy { it.id })
        assertEquals(before.occasions.sortedBy { it.id }, after.occasions.sortedBy { it.id })
        assertEquals(settingsBefore, c.store.currentSettings())
        assertEquals(2, after.contacts.size) // the approximate "10 days ago" + the logged call
    }

    @Test(expected = Exception::class)
    fun `a file that is not a backup is rejected before touching data`() {
        val t = TestApp(start)
        t.container.backup.parse("""{"format":"something-else","exportedAt":0}""")
    }

    @Test
    fun `backup format marker is present`() = runBlocking {
        val t = TestApp(start)
        assertTrue(t.container.backup.export().contains(BackupFile.FORMAT))
    }

    @Test
    fun `statistics match the contact log`() = runBlocking {
        val t = TestApp(LocalDateTime.of(2026, 3, 1, 12, 0))
        val c = t.container
        val ids = listOf(t.addPerson("أ", 7, null), t.addPerson("ب", 14, null), t.addPerson("ج", 30, null))
        // Six months of contacts: person i is contacted every (i+1)*5 days, alternating who reached out.
        for (day in 0 until 200) {
            ids.forEachIndexed { i, id ->
                if (day % ((i + 1) * 5) == 0) {
                    c.repo.logContact(id, if (day % 2 == 0) Direction.ME else Direction.THEM, Channel.WHATSAPP, Source.MANUAL)
                }
            }
            t.clock.plusDays(1)
        }
        val s = c.repo.currentSnapshot()
        val zone = ZoneId.of("Asia/Riyadh")
        val today = LocalDate.now(t.clock)
        val stats = UiBuilder(today, zone).stats(s)

        // Recount straight from the raw log.
        fun date(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        stats.months.forEach { m ->
            val contacted = s.contacts.filter { YearMonth.from(date(it.dateTime)) == m.month }.map { it.personId }.toSet()
            assertEquals("month ${m.month}", contacted.size, m.contacted)
            assertEquals(3, m.total)
        }
        stats.rows.forEach { row ->
            val log = s.contacts.filter { it.personId == row.personId }
            assertEquals(log.count { it.direction == Direction.ME }, row.byMe)
            assertEquals(log.count { it.direction == Direction.THEM }, row.byThem)
        }
        val rowB = stats.rows.single { it.name == "ب" }
        assertEquals("كل 10 أيام / الهدف 14", rowB.intervalText)
    }
}
