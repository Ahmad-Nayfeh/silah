package io.github.ahmadnayfeh.silah.ui.people

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.ahmadnayfeh.silah.data.Person
import io.github.ahmadnayfeh.silah.data.Settings
import io.github.ahmadnayfeh.silah.domain.PhoneNumbers
import io.github.ahmadnayfeh.silah.domain.Tag
import io.github.ahmadnayfeh.silah.ui.components.ChoiceChips

private val PRESETS = listOf(1 to "يومياً", 3 to "كل 3 أيام", 7 to "أسبوعياً", 14 to "كل أسبوعين", 30 to "شهرياً")
private const val CUSTOM = -1

private val APPROX = listOf<Pair<Int?, String>>(
    null to "لا أذكر",
    3 to "هذا الأسبوع",
    14 to "قبل أسبوعين تقريباً",
    30 to "قبل شهر تقريباً",
    60 to "أكثر من ذلك",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditScreen(
    initial: Person?,
    settings: Settings,
    onSave: (Person, approxLastContactDaysAgo: Int?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val isNew = initial == null
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var tag by rememberSaveable { mutableStateOf(initial?.tag ?: Tag.FAMILY) }
    val initialTarget = initial?.targetDays ?: 7
    var preset by rememberSaveable { mutableStateOf(if (PRESETS.any { it.first == initialTarget }) initialTarget else CUSTOM) }
    var custom by rememberSaveable { mutableStateOf(if (preset == CUSTOM) initialTarget.toString() else "") }
    var phone by rememberSaveable { mutableStateOf(initial?.phone.orEmpty()) }
    var approx by rememberSaveable { mutableStateOf<Int?>(null) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    // The system contact picker hands back only the chosen number — no contacts permission needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    phone = c.getString(0).orEmpty()
                    if (name.isBlank()) name = c.getString(1).orEmpty()
                }
            }
        }.onFailure {
            Toast.makeText(context, "تعذّر قراءة الرقم، اكتبه يدوياً", Toast.LENGTH_LONG).show()
        }
    }

    val targetDays = if (preset == CUSTOM) custom.toIntOrNull() else preset
    val normalizedPhone = PhoneNumbers.normalize(phone)
    val nameError = name.isBlank()
    val targetError = targetDays == null || targetDays !in 1..365
    val phoneError = phone.isNotBlank() && normalizedPhone == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "شخص جديد" else "تعديل") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم") },
                singleLine = true,
                isError = showErrors && nameError,
                supportingText = if (showErrors && nameError) ({ Text("اكتب الاسم") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )

            Label("الوسم")
            ChoiceChips(options = Tag.entries.map { it to settings.tagName(it) }, selected = tag, onSelect = { tag = it })

            Label("كم مرة تودّ أن تتواصلا؟")
            ChoiceChips(
                options = PRESETS + (CUSTOM to "مخصص"),
                selected = preset,
                onSelect = { preset = it },
            )
            if (preset == CUSTOM) {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { v -> custom = v.filter { it.isDigit() }.take(3) },
                    label = { Text("كل كم يوماً؟") },
                    singleLine = true,
                    isError = showErrors && targetError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
            }

            Label("رقم الجوال (اختياري)")
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = { Text("05xxxxxxxx") },
                singleLine = true,
                isError = phoneError,
                supportingText = {
                    Text(
                        when {
                            phoneError -> "الرقم غير واضح. اكتبه كاملاً، مثل 0501234567 أو ‎+9665…"
                            normalizedPhone != null -> "سيُحفظ هكذا: ‎$normalizedPhone"
                            else -> "بدون رقم، يفتح زر واتساب التطبيق فقط."
                        },
                    )
                },
                trailingIcon = {
                    IconButton(onClick = {
                        runCatching {
                            picker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                        }
                    }) { Icon(Icons.Rounded.Contacts, "اختر من جهات الاتصال") }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )

            if (isNew) {
                Label("متى آخر مرة تواصلتما؟ (اختياري)")
                ChoiceChips(options = APPROX, selected = approx, onSelect = { approx = it })
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    showErrors = true
                    if (!nameError && !targetError && !phoneError) {
                        val base = initial ?: Person(name = "", tag = tag, targetDays = 7, createdAt = 0)
                        onSave(
                            base.copy(name = name.trim(), tag = tag, targetDays = targetDays!!, phone = normalizedPhone),
                            if (isNew) approx else null,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.medium,
            ) { Text("حفظ", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}
