package io.github.ahmadnayfeh.silah.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.ahmadnayfeh.silah.data.Settings
import io.github.ahmadnayfeh.silah.data.ThemeMode
import io.github.ahmadnayfeh.silah.domain.ArabicText
import io.github.ahmadnayfeh.silah.domain.Rhythm
import io.github.ahmadnayfeh.silah.ui.components.SectionTitle
import io.github.ahmadnayfeh.silah.ui.components.SoftCard

class SettingsActions(
    val onChange: ((Settings) -> Settings) -> Unit,
    val onNotificationSettings: () -> Unit,
    val onBatterySettings: () -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onErase: () -> Unit,
)

private enum class TimeField { NOTIFY, QUIET_START, QUIET_END }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    autoPerWeek: Int,
    notificationsAllowed: Boolean,
    versionName: String,
    a: SettingsActions,
) {
    var editingTime by remember { mutableStateOf<TimeField?>(null) }
    var confirmErase by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("الإعدادات", style = MaterialTheme.typography.headlineLarge)

        SectionTitle("التذكير اليومي")
        SoftCard {
            ValueRow("وقت التذكير", ArabicText.time(settings.notifyMinute)) { editingTime = TimeField.NOTIFY }
            Spacer(Modifier.height(10.dp))
            Text("الأيام المسموح فيها", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Rhythm.WEEK_ORDER.forEach { day ->
                    val on = day.value in settings.notifyDays
                    FilterChip(
                        selected = on,
                        onClick = {
                            a.onChange { s ->
                                val days = if (on) s.notifyDays - day.value else s.notifyDays + day.value
                                s.copy(notifyDays = days.ifEmpty { setOf(day.value) })
                            }
                        },
                        label = { Text(ArabicText.dayName(day)) },
                    )
                }
            }
        }

        SectionTitle("الهدوء الليلي")
        SoftCard {
            Text(
                "لا تذكير في هذه الفترة. التذكير المعلّق يختفي عند بدايتها ويعود صباحاً.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            ValueRow("من", ArabicText.time(settings.quietStartMinute)) { editingTime = TimeField.QUIET_START }
            ValueRow("إلى", ArabicText.time(settings.quietEndMinute)) { editingTime = TimeField.QUIET_END }
        }

        SectionTitle("الإيقاع")
        SoftCard {
            val options = listOf(true to "تلقائي", false to "يدوي")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, (auto, label) ->
                    SegmentedButton(
                        selected = settings.autoRhythm == auto,
                        onClick = { a.onChange { it.copy(autoRhythm = auto) } },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (settings.autoRhythm) {
                Text(
                    "حسب الأشخاص وفتراتهم: ${ArabicText.perWeek(autoPerWeek)}. في بقية الأيام صمت تام.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(ArabicText.perWeek(settings.manualPerWeek), style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = settings.manualPerWeek.toFloat(),
                    onValueChange = { v -> a.onChange { it.copy(manualPerWeek = v.toInt()) } },
                    valueRange = 1f..7f,
                    steps = 5,
                )
            }
        }

        SectionTitle("أسماء الوسوم")
        SoftCard {
            TagField(settings.tagFamily) { v -> a.onChange { it.copy(tagFamily = v) } }
            TagField(settings.tagFriends) { v -> a.onChange { it.copy(tagFriends = v) } }
            TagField(settings.tagOther) { v -> a.onChange { it.copy(tagOther = v) } }
        }

        SectionTitle("المظهر")
        val themes = listOf(ThemeMode.DARK to "داكن", ThemeMode.LIGHT to "فاتح", ThemeMode.SYSTEM to "حسب الجوال")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            themes.forEachIndexed { i, (mode, label) ->
                SegmentedButton(
                    selected = settings.theme == mode,
                    onClick = { a.onChange { it.copy(theme = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(i, themes.size),
                ) { Text(label) }
            }
        }

        SectionTitle("ليصلك التذكير في وقته")
        SoftCard {
            LinkRow(
                Icons.Rounded.NotificationsActive,
                "الإشعارات",
                if (notificationsAllowed) "مسموحة" else "غير مسموحة — اضغط للسماح",
                a.onNotificationSettings,
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            LinkRow(
                Icons.Rounded.BatteryChargingFull,
                "تحسين البطارية",
                "افتح «البطارية» واختر «غير مقيّد» حتى لا يؤخر الجوال التذكير",
                a.onBatterySettings,
            )
        }

        SectionTitle("النسخ الاحتياطي")
        SoftCard {
            LinkRow(Icons.Rounded.Upload, "تصدير نسخة احتياطية", "ملف واحد فيه كل بياناتك وإعداداتك", a.onExport)
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            LinkRow(Icons.Rounded.Download, "استيراد نسخة احتياطية", "يستبدل البيانات الحالية بمحتوى الملف") { confirmImport = true }
        }

        SoftCard {
            LinkRow(Icons.Rounded.DeleteForever, "مسح كل البيانات", "يحذف الأشخاص والسجل والإعدادات من هذا الجوال") { confirmErase = true }
        }

        Text(
            "صِلة $versionName · يعمل بلا إنترنت، وبياناتك على جوالك فقط.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }

    editingTime?.let { field ->
        val minute = when (field) {
            TimeField.NOTIFY -> settings.notifyMinute
            TimeField.QUIET_START -> settings.quietStartMinute
            TimeField.QUIET_END -> settings.quietEndMinute
        }
        val state = rememberTimePickerState(initialHour = minute / 60, initialMinute = minute % 60, is24Hour = false)
        AlertDialog(
            onDismissRequest = { editingTime = null },
            confirmButton = {
                TextButton(onClick = {
                    val m = state.hour * 60 + state.minute
                    a.onChange {
                        when (field) {
                            TimeField.NOTIFY -> it.copy(notifyMinute = m)
                            TimeField.QUIET_START -> it.copy(quietStartMinute = m)
                            TimeField.QUIET_END -> it.copy(quietEndMinute = m)
                        }
                    }
                    editingTime = null
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editingTime = null }) { Text("إلغاء") } },
            text = { TimePicker(state = state) },
        )
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("مسح كل البيانات؟") },
            text = { Text("لا يمكن التراجع. إن أردت الاحتفاظ بها، صدّر نسخة احتياطية أولاً.") },
            confirmButton = { TextButton(onClick = { confirmErase = false; a.onErase() }) { Text("مسح") } },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("إلغاء") } },
        )
    }
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("استيراد نسخة احتياطية؟") },
            text = { Text("ستُستبدل كل البيانات الحالية بمحتوى الملف الذي تختاره.") },
            confirmButton = { TextButton(onClick = { confirmImport = false; a.onImport() }) { Text("اختيار الملف") } },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TagField(value: String, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.take(20)
            if (text.isNotBlank()) onChange(text.trim())
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
    )
}
