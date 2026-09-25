package io.github.ahmadnayfeh.silah.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.ui.channelName

/** "Done": two small choices — who reached out, and how. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoneSheet(name: String, onDismiss: () -> Unit, onSave: (Direction, Channel) -> Unit) {
    var direction by remember { mutableStateOf(Direction.ME) }
    var channel by remember { mutableStateOf(Channel.WHATSAPP) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("تواصلت مع $name", style = MaterialTheme.typography.titleLarge)
            Text("من بادر؟", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val directions = listOf(Direction.ME to "أنا بادرت", Direction.THEM to "هو بادر")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                directions.forEachIndexed { i, (value, label) ->
                    SegmentedButton(
                        selected = direction == value,
                        onClick = { direction = value },
                        shape = SegmentedButtonDefaults.itemShape(i, directions.size),
                    ) { Text(label) }
                }
            }
            Text("كيف؟", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(
                options = Channel.entries.map { it to channelName(it) },
                selected = channel,
                onSelect = { channel = it },
            )
            Button(
                onClick = { onSave(direction, channel) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("حفظ") }
        }
    }
}
