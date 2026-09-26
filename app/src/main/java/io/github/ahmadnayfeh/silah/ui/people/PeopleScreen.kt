package io.github.ahmadnayfeh.silah.ui.people

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.ahmadnayfeh.silah.data.Settings
import io.github.ahmadnayfeh.silah.domain.Tag
import io.github.ahmadnayfeh.silah.ui.PersonRow
import io.github.ahmadnayfeh.silah.ui.components.Avatar
import io.github.ahmadnayfeh.silah.ui.components.EmptyState

@Composable
fun PeopleScreen(
    rows: List<PersonRow>?,
    settings: Settings,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<Tag?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                Text("الأشخاص", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث بالاسم") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "مسح البحث") }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
            val filters = listOf<Pair<Tag?, String>>(null to "الكل") + Tag.entries.map { it to settings.tagName(it) }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filters) { (tag, label) ->
                    FilterChip(selected = filter == tag, onClick = { filter = tag }, label = { Text(label) })
                }
            }
            val visible = rows.orEmpty().filter { r ->
                (filter == null || r.tag == filter) && (query.isBlank() || r.name.contains(query.trim(), ignoreCase = true))
            }
            when {
                rows == null -> Unit
                rows.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.Diversity3,
                    title = "لا أحد هنا بعد",
                    body = "اضغط «إضافة» لتضيف أول شخص. ثلاثة حقول تكفي للبدء.",
                )
                visible.isEmpty() -> EmptyState(icon = Icons.Rounded.Search, title = "لا نتائج")
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                ) {
                    items(visible, key = { it.id }) { row -> PersonListItem(row, onOpen) }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Rounded.PersonAdd, null) },
            text = { Text("إضافة") },
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun PersonListItem(row: PersonRow, onOpen: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onOpen(row.id) }
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .alpha(if (row.isPaused) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(row.name, row.tag)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            val details = buildList {
                add(row.lastText)
                add(row.everyText)
                if (row.isPaused) add("موقوف مؤقتاً")
            }.joinToString("  ·  ")
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (row.occasionText != null) {
            Text(
                row.occasionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
            )
        }
    }
}
