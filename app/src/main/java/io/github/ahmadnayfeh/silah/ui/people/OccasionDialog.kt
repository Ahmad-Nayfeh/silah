package io.github.ahmadnayfeh.silah.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ahmadnayfeh.silah.data.Occasion
import io.github.ahmadnayfeh.silah.domain.ArabicText
import io.github.ahmadnayfeh.silah.ui.components.ChoiceChips
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val SUGGESTIONS = listOf("ميلاد" to true, "ذكرى زواج" to true, "سفر" to false, "عملية" to false, "اختبار" to false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccasionDialog(personId: Long, onDismiss: () -> Unit, onSave: (Occasion) -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var yearly by remember { mutableStateOf(true) }
    var remind by remember { mutableStateOf(2) }
    var pickDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مناسبة جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceChips(
                    options = SUGGESTIONS.map { it.first to it.first },
                    selected = title,
                    onSelect = { t ->
                        title = t
                        yearly = SUGGESTIONS.first { it.first == t }.second
                    },
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("المناسبة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp))
                    Text("  " + (date?.let { ArabicText.date(it) } ?: "اختر التاريخ"))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تتكرر كل سنة", modifier = Modifier.weight(1f))
                    Switch(checked = yearly, onCheckedChange = { yearly = it })
                }
                Text("ذكّرني قبلها", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceChips(
                    options = listOf(0 to "في يومها", 1 to "بيوم", 2 to "بيومين", 7 to "بأسبوع"),
                    selected = remind,
                    onSelect = { remind = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && date != null,
                onClick = {
                    onSave(
                        Occasion(
                            personId = personId,
                            title = title.trim(),
                            date = date!!.toEpochDay(),
                            repeatsYearly = yearly,
                            remindDaysBefore = remind,
                        ),
                    )
                },
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("تم") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("إلغاء") } },
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}
