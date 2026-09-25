package io.github.ahmadnayfeh.silah.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.ahmadnayfeh.silah.BuildConfig
import io.github.ahmadnayfeh.silah.domain.ArabicText
import io.github.ahmadnayfeh.silah.notify.Launchers
import io.github.ahmadnayfeh.silah.ui.components.DoneSheet
import io.github.ahmadnayfeh.silah.ui.people.PeopleScreen
import io.github.ahmadnayfeh.silah.ui.people.PersonActions
import io.github.ahmadnayfeh.silah.ui.people.PersonEditScreen
import io.github.ahmadnayfeh.silah.ui.people.PersonScreen
import io.github.ahmadnayfeh.silah.ui.settings.SettingsActions
import io.github.ahmadnayfeh.silah.ui.settings.SettingsScreen
import io.github.ahmadnayfeh.silah.ui.stats.StatsScreen
import io.github.ahmadnayfeh.silah.ui.today.TodayScreen
import kotlinx.coroutines.withTimeoutOrNull

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("today", "اليوم", Icons.Rounded.WbSunny),
    Tab("people", "الأشخاص", Icons.Rounded.People),
    Tab("stats", "الإحصائيات", Icons.Rounded.BarChart),
    Tab("settings", "الإعدادات", Icons.Rounded.Settings),
)

/** Undo stays on screen this long after a contact is recorded. */
private const val UNDO_MILLIS = 10_000L

/** How long after leaving for WhatsApp the "thread?" question is still offered. */
private const val PROMPT_WINDOW_MILLIS = 3 * 60 * 60 * 1000L

@Composable
fun SilahAppUi(vm: MainViewModel) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val appState by vm.appState.collectAsStateWithLifecycle()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    var lastResume by remember { mutableLongStateOf(0L) }
    var doneFor by remember { mutableStateOf<Pair<Long, String>?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        lastResume = System.currentTimeMillis()
        vm.onResume()
    }

    LaunchedEffect(Unit) {
        vm.uiEvents.collect { event ->
            when (event) {
                is UiEvent.Logged -> {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = withTimeoutOrNull(UNDO_MILLIS) {
                        snackbar.showSnackbar(event.message, actionLabel = "تراجع", duration = SnackbarDuration.Indefinite)
                    }
                    if (result == null) snackbar.currentSnackbarData?.dismiss()
                    if (result == SnackbarResult.ActionPerformed) vm.undo(event.contactId)
                }
                is UiEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }

    val openWhatsApp: (Long, String?) -> Unit = { id, phone ->
        val intent = Launchers.whatsapp(context, phone)
        if (intent == null) {
            Toast.makeText(context, "واتساب غير مثبّت على الجوال", Toast.LENGTH_LONG).show()
        } else {
            vm.whatsApp(id)
            context.startActivity(intent)
        }
    }
    val openDialer: (Long, String) -> Unit = { id, phone ->
        vm.call(id)
        context.startActivity(Launchers.dial(phone))
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (route in TABS.map { it.route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo("today") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding)) {
            composable("today") {
                val ui by vm.today.collectAsStateWithLifecycle()
                val today = vm.todayDate()
                TodayScreen(
                    ui = ui,
                    dateText = "${ArabicText.dayName(today.dayOfWeek)}، ${ArabicText.dayMonth(today)}",
                    onWhatsApp = { openWhatsApp(it.personId, it.phone) },
                    onCall = { card -> card.phone?.let { openDialer(card.personId, it) } },
                    onDone = { doneFor = it.personId to it.name },
                    onNotToday = { vm.notToday(it.personId) },
                    onOpenPerson = { nav.navigate("person/$it") },
                    onSuggestAnother = vm::suggestAnother,
                    onAddPerson = { nav.navigate("edit/0") },
                )
            }
            composable("people") {
                val rows by vm.people.collectAsStateWithLifecycle()
                val settings by vm.settings.collectAsStateWithLifecycle()
                PeopleScreen(rows, settings, onOpen = { nav.navigate("person/$it") }, onAdd = { nav.navigate("edit/0") })
            }
            composable("stats") {
                val ui by vm.stats.collectAsStateWithLifecycle()
                StatsScreen(ui)
            }
            composable("settings") { SettingsRoute(vm, lastResume) }
            composable("person/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                PersonRoute(vm, nav, id, openWhatsApp, openDialer) { doneFor = it }
            }
            composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val settings by vm.settings.collectAsStateWithLifecycle()
                val person = snapshot?.person(id)
                if (id == 0L || person != null) {
                    PersonEditScreen(
                        initial = person,
                        settings = settings,
                        onSave = { p, approx ->
                            vm.savePerson(p, approx) { newId ->
                                nav.popBackStack()
                                if (id == 0L) nav.navigate("person/$newId")
                            }
                        },
                        onBack = { nav.popBackStack() },
                    )
                }
            }
        }
    }

    doneFor?.let { (id, name) ->
        DoneSheet(
            name = name,
            onDismiss = { doneFor = null },
            onSave = { d, c ->
                doneFor = null
                vm.done(id, d, c)
            },
        )
    }

    // "Thread?" — one optional line after coming back from WhatsApp or a call.
    val st = appState
    val promptPerson = st?.threadPromptPerson?.let { snapshot?.person(it) }
    val promptAt = st?.threadPromptAt ?: 0L
    if (promptPerson != null && promptAt < lastResume && System.currentTimeMillis() - promptAt < PROMPT_WINDOW_MILLIS) {
        ThreadPrompt(
            name = promptPerson.name,
            onSave = { vm.answerThreadPrompt(promptPerson.id, it) },
            onDismiss = vm::dismissThreadPrompt,
        )
    }
}

@Composable
private fun ThreadPrompt(name: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("خيط؟") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "شيء تريد أن تسأل $name عنه في المرة القادمة؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.replace("\n", " ") },
                    singleLine = true,
                    placeholder = { Text("اختياري") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("تجاهل") } },
    )
}

@Composable
private fun PersonRoute(
    vm: MainViewModel,
    nav: NavHostController,
    id: Long,
    openWhatsApp: (Long, String?) -> Unit,
    openDialer: (Long, String) -> Unit,
    onDone: (Pair<Long, String>) -> Unit,
) {
    val flow = remember(id) { vm.personDetail(id) }
    val ui by flow.collectAsState(initial = null)
    val detail = ui ?: return
    val p = detail.person
    PersonScreen(
        ui = detail,
        a = PersonActions(
            onBack = { nav.popBackStack() },
            onEdit = { nav.navigate("edit/${p.id}") },
            onWhatsApp = { openWhatsApp(p.id, p.phone) },
            onCall = { p.phone?.let { openDialer(p.id, it) } },
            onDone = { onDone(p.id to p.name) },
            onTheyReachedOut = { vm.theyReachedOut(p.id) },
            onAddThread = { vm.addThread(p.id, it) },
            onThreadDone = vm::setThreadDone,
            onDeleteThread = vm::deleteThread,
            onAddOccasion = vm::addOccasion,
            onDeleteOccasion = vm::deleteOccasion,
            onNote = { vm.setNote(p.id, it) },
            onPause = { vm.setPaused(p.id, it) },
            onDeleteContact = vm::deleteContact,
            onDelete = {
                vm.deletePerson(p)
                nav.popBackStack()
            },
        ),
    )
}

@Composable
private fun SettingsRoute(vm: MainViewModel, lastResume: Long) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val autoPerWeek by vm.perWeekAuto.collectAsStateWithLifecycle()
    val allowed = remember(lastResume) { NotificationManagerCompat.from(context).areNotificationsEnabled() }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportTo(context.contentResolver, it) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importFrom(context.contentResolver, it) }
    }

    SettingsScreen(
        settings = settings,
        autoPerWeek = autoPerWeek,
        notificationsAllowed = allowed,
        versionName = BuildConfig.VERSION_NAME,
        a = SettingsActions(
            onChange = vm::updateSettings,
            onNotificationSettings = { context.openSafely(notificationSettingsIntent(context)) },
            onBatterySettings = { context.openSafely(appDetailsIntent(context)) },
            onExport = { exporter.launch("silah-backup-${vm.todayDate()}.json") },
            onImport = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            onErase = vm::eraseAll,
        ),
    )
}

private fun notificationSettingsIntent(context: Context) =
    Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)

/** On Samsung the app's page has «البطارية» → «غير مقيّد». */
private fun appDetailsIntent(context: Context) =
    Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

private fun Context.openSafely(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
