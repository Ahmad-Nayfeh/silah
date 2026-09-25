package io.github.ahmadnayfeh.silah.ui.people

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MarkChatRead
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.ahmadnayfeh.silah.data.Occasion
import io.github.ahmadnayfeh.silah.data.Thread
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.Source
import io.github.ahmadnayfeh.silah.ui.ContactRow
import io.github.ahmadnayfeh.silah.ui.OccasionRow
import io.github.ahmadnayfeh.silah.ui.PersonDetailUi
import io.github.ahmadnayfeh.silah.ui.channelName
import io.github.ahmadnayfeh.silah.ui.components.Avatar
import io.github.ahmadnayfeh.silah.ui.components.SectionTitle
import io.github.ahmadnayfeh.silah.ui.components.SoftCard
import io.github.ahmadnayfeh.silah.ui.components.TagLabel
import kotlinx.coroutines.delay

class PersonActions(
    val onBack: () -> Unit,
    val onEdit: () -> Unit,
    val onWhatsApp: () -> Unit,
    val onCall: () -> Unit,
    val onDone: () -> Unit,
    val onTheyReachedOut: () -> Unit,
    val onAddThread: (String) -> Unit,
    val onThreadDone: (Thread, Boolean) -> Unit,
    val onDeleteThread: (Long) -> Unit,
    val onAddOccasion: (Occasion) -> Unit,
    val onDeleteOccasion: (Long) -> Unit,
    val onNote: (String) -> Unit,
    val onPause: (Boolean) -> Unit,
    val onDeleteContact: (Long) -> Unit,
    val onDelete: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(ui: PersonDetailUi, a: PersonActions) {
    val p = ui.person
    var confirmDelete by remember { mutableStateOf(false) }
    var addOccasion by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<ContactRow?>(null) }
    var showDoneThreads by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = a.onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع") } },
                actions = { IconButton(onClick = a.onEdit) { Icon(Icons.Rounded.Edit, "تعديل") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(p.name, p.tag, size = 84.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(p.name, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    TagLabel(ui.tagName, p.tag)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${ui.lastContactText}  ·  ${ui.everyText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (p.isPaused) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "موقوف مؤقتاً — لن يُقترح حتى تعيده",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionTile(Icons.AutoMirrored.Rounded.Chat, "واتساب", primary = true, onClick = a.onWhatsApp)
                    if (p.phone != null) ActionTile(Icons.Rounded.Call, "اتصال", onClick = a.onCall)
                    ActionTile(Icons.Rounded.Check, "تم", onClick = a.onDone)
                    ActionTile(Icons.Rounded.MarkChatRead, "هو راسلني", onClick = a.onTheyReachedOut)
                }
            }

            // Threads
            item { SectionTitle("الخيوط") }
            item { ThreadInput(a.onAddThread) }
            items(ui.openThreads, key = { "t${it.id}" }) { t -> ThreadItem(t, a) }
            if (ui.doneThreads.isNotEmpty()) {
                item {
                    TextButton(onClick = { showDoneThreads = !showDoneThreads }) {
                        Text(if (showDoneThreads) "إخفاء المنتهية" else "المنتهية (${ui.doneThreads.size})")
                    }
                }
                if (showDoneThreads) items(ui.doneThreads, key = { "d${it.id}" }) { t -> ThreadItem(t, a) }
            }

            // Occasions
            item {
                SectionTitle("المناسبات") {
                    TextButton(onClick = { addOccasion = true }) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة")
                    }
                }
            }
            if (ui.occasions.isEmpty()) {
                item { Hint("ميلاد، ذكرى، سفر، عملية، اختبار… سيذكّرك التطبيق قبلها بيومين.") }
            }
            items(ui.occasions, key = { "o${it.id}" }) { o -> OccasionItem(o) { a.onDeleteOccasion(o.id) } }

            // Note
            item { SectionTitle("ملاحظة") }
            item { NoteField(p.note, a.onNote) }

            // Timeline
            item { SectionTitle("سجل التواصل") }
            if (ui.timeline.isEmpty()) {
                item { Hint("لا يوجد تواصل مسجّل بعد.") }
            } else {
                item { Hint("اضغط مطوّلاً على أي سطر لحذفه.") }
            }
            items(ui.timeline, key = { "c${it.id}" }) { c -> TimelineItem(c) { contactToDelete = c } }

            item {
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("إيقاف مؤقت", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "لن يُقترح ولن يُحسب في الإحصائيات حتى تعيده.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = p.isPaused, onCheckedChange = a.onPause)
                    }
                }
            }
            item {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("حذف ${p.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف ${p.name}؟") },
            text = { Text("سيُحذف مع خيوطه ومناسباته وسجل التواصل معه. لا يمكن التراجع.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; a.onDelete() }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") } },
        )
    }
    contactToDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("حذف هذا التسجيل؟") },
            text = { Text(c.dateText) },
            confirmButton = { TextButton(onClick = { contactToDelete = null; a.onDeleteContact(c.id) }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { contactToDelete = null }) { Text("إلغاء") } },
        )
    }
    if (addOccasion) {
        OccasionDialog(
            personId = p.id,
            onDismiss = { addOccasion = false },
            onSave = { addOccasion = false; a.onAddOccasion(it) },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ActionTile(
    icon: ImageVector,
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(76.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ThreadInput(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = {
        if (text.isNotBlank()) {
            onAdd(text)
            text = ""
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.replace("\n", " ") },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("خيط جديد… مثلاً: اسأله عن نتيجة المقابلة") },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            if (text.isNotBlank()) IconButton(onClick = submit) { Icon(Icons.Rounded.Add, "إضافة") }
        },
    )
}

@Composable
private fun ThreadItem(t: Thread, a: PersonActions) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = t.isDone, onCheckedChange = { a.onThreadDone(t, it) })
        Text(
            t.text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (t.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (t.isDone) {
            IconButton(onClick = { a.onDeleteThread(t.id) }) {
                Icon(Icons.Rounded.DeleteOutline, "حذف", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OccasionItem(o: OccasionRow, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Icon(
            if (o.repeatsYearly) Icons.Rounded.Cake else Icons.Rounded.Event,
            null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(o.title, style = MaterialTheme.typography.titleMedium)
            val sub = listOfNotNull(o.dateText, if (o.repeatsYearly) "كل سنة" else null, o.nextText).joinToString("  ·  ")
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.DeleteOutline, "حذف المناسبة", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoteField(initial: String, onNote: (String) -> Unit) {
    var text by rememberSaveable(initial) { mutableStateOf(initial) }
    // Saves quietly a moment after typing stops.
    LaunchedEffect(text) {
        if (text != initial) {
            delay(600)
            onNote(text)
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("أي شيء تريد تذكّره عنه…") },
        minLines = 2,
        shape = MaterialTheme.shapes.small,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineItem(c: ContactRow, onLongPress: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(vertical = 6.dp),
    ) {
        val mine = c.direction == Direction.ME
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (mine) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                null,
                tint = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            val who = if (mine) "أنا بادرت" else "هو بادر"
            val auto = if (c.source == Source.AUTO) " · تلقائي" else ""
            Text("$who · ${channelName(c.channel)}$auto", style = MaterialTheme.typography.bodyMedium)
            Text(c.dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
