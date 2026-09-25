package io.github.ahmadnayfeh.silah.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.ahmadnayfeh.silah.ui.TodayCard
import io.github.ahmadnayfeh.silah.ui.TodayUi
import io.github.ahmadnayfeh.silah.ui.components.Avatar
import io.github.ahmadnayfeh.silah.ui.components.EmptyState
import io.github.ahmadnayfeh.silah.ui.components.TagLabel

@Composable
fun TodayScreen(
    ui: TodayUi,
    dateText: String,
    onWhatsApp: (TodayCard) -> Unit,
    onCall: (TodayCard) -> Unit,
    onDone: (TodayCard) -> Unit,
    onNotToday: (TodayCard) -> Unit,
    onOpenPerson: (Long) -> Unit,
    onSuggestAnother: () -> Unit,
    onAddPerson: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("اليوم", style = MaterialTheme.typography.headlineLarge)
        Text(dateText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        when (ui) {
            TodayUi.Loading -> Unit
            TodayUi.NoPeople -> EmptyState(
                icon = Icons.Rounded.Diversity3,
                title = "أهلاً بك في صِلة",
                body = "أضف من تحب أن تبقى على تواصل معهم، وسيقترح عليك التطبيق شخصاً واحداً في الوقت المناسب.",
            ) {
                Button(onClick = onAddPerson, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Rounded.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("أضف أول شخص")
                }
            }
            is TodayUi.Suggest -> SuggestionCard(ui.card, onWhatsApp, onCall, onDone, onNotToday, onOpenPerson)
            is TodayUi.Done -> EmptyState(
                icon = Icons.Rounded.CheckCircle,
                title = "تواصلت اليوم مع ${ui.name}",
            ) {
                if (ui.othersAvailable) {
                    TextButton(onClick = onSuggestAnother) { Text("اقترح شخصاً آخر") }
                }
            }
            TodayUi.NoOne -> EmptyState(icon = Icons.Rounded.NightsStay, title = "لا أحد اليوم.")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SuggestionCard(
    card: TodayCard,
    onWhatsApp: (TodayCard) -> Unit,
    onCall: (TodayCard) -> Unit,
    onDone: (TodayCard) -> Unit,
    onNotToday: (TodayCard) -> Unit,
    onOpenPerson: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { onOpenPerson(card.personId) },
            ) {
                Avatar(card.name, card.tag, size = 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(card.name, style = MaterialTheme.typography.headlineSmall)
                    TagLabel(card.tagName, card.tag)
                }
            }
            Spacer(Modifier.height(18.dp))
            InfoLine(Icons.Rounded.Schedule, card.lastContactText)
            if (card.occasionText != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.Cake, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(card.occasionText, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (card.threads.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("خيوط مفتوحة", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                card.threads.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(10.dp))
                        Text(t.text, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { onWhatsApp(card) },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Chat, null)
                Spacer(Modifier.width(10.dp))
                Text("واتساب", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (card.phone != null) {
                    OutlinedButton(
                        onClick = { onCall(card) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Rounded.Call, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("اتصال")
                    }
                }
                OutlinedButton(
                    onClick = { onDone(card) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تم")
                }
            }
            TextButton(
                onClick = { onNotToday(card) },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) { Text("ليس اليوم") }
        }
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
