package io.github.ahmadnayfeh.silah

import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ahmadnayfeh.silah.data.Occasion
import io.github.ahmadnayfeh.silah.data.Person
import io.github.ahmadnayfeh.silah.data.ThemeMode
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.Source
import io.github.ahmadnayfeh.silah.domain.Tag
import io.github.ahmadnayfeh.silah.ui.MainViewModel
import io.github.ahmadnayfeh.silah.ui.OnboardingScreen
import io.github.ahmadnayfeh.silah.ui.SilahAppUi
import io.github.ahmadnayfeh.silah.ui.theme.SilahTheme
import io.github.ahmadnayfeh.silah.widget.SilahWidget
import io.github.ahmadnayfeh.silah.widget.WidgetData
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Renders the real app with made-up people for the README.
 * Only writes images when run with: ./gradlew recordRoborazziDebug
 * All names and numbers here are fictional.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val dir = "../docs/screenshots"
    private val now = LocalDateTime.of(2026, 9, 26, 19, 5) // Saturday evening

    private fun populate(t: TestApp) = runBlocking {
        val repo = t.container.repo
        val clock = t.clock
        data class Seed(val name: String, val tag: Tag, val target: Int, val every: Int, val phone: String?)
        val seeds = listOf(
            Seed("نورة", Tag.FAMILY, 14, 16, "+966500000001"),
            Seed("أبو خالد", Tag.FAMILY, 7, 6, "+966500000002"),
            Seed("سارة", Tag.FAMILY, 7, 9, "+966500000003"),
            Seed("عمر", Tag.FRIENDS, 14, 19, "+966500000004"),
            Seed("ليلى", Tag.FAMILY, 30, 24, "+966500000005"),
            Seed("يوسف", Tag.FRIENDS, 30, 41, null),
            Seed("ريم", Tag.FRIENDS, 14, 12, "+966500000006"),
            Seed("الأستاذ فهد", Tag.OTHER, 30, 35, "+966500000007"),
        )
        // Build ~6 months of history by walking the clock forward.
        clock.set(now.minusDays(185))
        val ids = seeds.map { s ->
            repo.addPerson(Person(name = s.name, tag = s.tag, phone = s.phone, targetDays = s.target, createdAt = 0))
        }
        for (day in 0 until 185) {
            seeds.forEachIndexed { i, s ->
                // Noura: nothing in the last 17 days, so she is today's person.
                val quiet = i == 0 && day > 185 - 17
                if (!quiet && (day + i * 3) % s.every == 0) {
                    val dir = if ((day / s.every + i) % 3 == 0) Direction.THEM else Direction.ME
                    val ch = listOf(Channel.WHATSAPP, Channel.WHATSAPP, Channel.CALL, Channel.VISIT)[(day + i) % 4]
                    repo.logContact(ids[i], dir, ch, if (ch == Channel.WHATSAPP) Source.AUTO else Source.MANUAL)
                }
            }
            clock.plusDays(1)
        }
        clock.set(now)
        val nora = ids[0]
        // Noura: a birthday tomorrow and two open threads.
        repo.addOccasion(Occasion(personId = nora, title = "ميلاد", date = LocalDate.of(1994, 9, 27).toEpochDay(), repeatsYearly = true))
        repo.addOccasion(Occasion(personId = ids[3], title = "سفر", date = LocalDate.of(2026, 10, 14).toEpochDay(), repeatsYearly = false))
        repo.addThread(nora, "اسألها عن نتيجة المقابلة")
        repo.addThread(nora, "موعد زيارة الوالدة الأسبوع القادم")
        repo.addThread(ids[2], "كتاب الطبخ الذي وعدتها به")
        repo.setNote(nora, "تحب القهوة العربية. تعمل ممرضة في المناوبات المسائية.")
        t.container.store.updateState { it.copy(onboarded = true) }
        ids
    }

    private fun app(theme: ThemeMode = ThemeMode.DARK): Pair<TestApp, MainViewModel> {
        val t = TestApp(now)
        populate(t)
        val vm = MainViewModel(t.container)
        compose.setContent {
            SilahTheme(theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { SilahAppUi(vm) }
            }
        }
        return t to vm
    }

    private fun waitForText(text: String) =
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    private fun shoot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$dir/$name.png")
    }

    @Test
    fun screens() {
        app()
        waitForText("واتساب")
        shoot("01-today")

        compose.onNodeWithText("الأشخاص").performClick()
        waitForText("بحث بالاسم")
        shoot("02-people")

        compose.onNodeWithText("سارة").performClick()
        waitForText("سجل التواصل")
        shoot("03-person")

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("الإحصائيات").performClick()
        waitForText("التغطية")
        shoot("04-stats")

        compose.onNodeWithText("الإعدادات").performClick()
        waitForText("التذكير اليومي")
        shoot("05-settings")
    }

    @Test
    fun doneSheet() {
        app()
        waitForText("واتساب")
        compose.onNodeWithText("تم").performClick()
        waitForText("من بادر؟")
        shoot("06-done")
    }

    @Test
    fun lightTheme() {
        app(ThemeMode.LIGHT)
        waitForText("واتساب")
        shoot("07-today-light")
    }

    @Test
    fun onboarding() {
        compose.setContent {
            SilahTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { OnboardingScreen(onStart = {}) }
            }
        }
        waitForText("ابدأ")
        shoot("00-welcome")
    }

    @Test
    fun widget() {
        val context = compose.activity
        fun render(data: WidgetData, name: String) {
            val views = SilahWidget.render(context, data)
            val frame = FrameLayout(context)
            val view: View = views.apply(context, frame)
            val w = (300 * context.resources.displayMetrics.density).toInt()
            val h = (130 * context.resources.displayMetrics.density).toInt()
            frame.addView(view, FrameLayout.LayoutParams(w, h))
            frame.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            frame.layout(0, 0, w, h)
            frame.captureRoboImage("$dir/$name.png")
        }
        render(WidgetData.Suggest(1, "نورة", "آخر تواصل قبل 17 يوماً"), "08-widget")
    }
}
