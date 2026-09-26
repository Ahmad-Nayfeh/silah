package io.github.ahmadnayfeh.silah.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ahmadnayfeh.silah.domain.ArabicText
import io.github.ahmadnayfeh.silah.domain.MonthCoverage
import io.github.ahmadnayfeh.silah.ui.StatRow
import io.github.ahmadnayfeh.silah.ui.StatsUi
import io.github.ahmadnayfeh.silah.ui.components.Avatar
import io.github.ahmadnayfeh.silah.ui.components.EmptyState
import io.github.ahmadnayfeh.silah.ui.components.SectionTitle
import io.github.ahmadnayfeh.silah.ui.components.SoftCard
import kotlin.math.roundToInt

/** Deliberately small: coverage, a six-month trend, and one honest table. */
@Composable
fun StatsScreen(ui: StatsUi?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("الإحصائيات", style = MaterialTheme.typography.headlineLarge)
        if (ui == null) return@Column
        if (ui.current.total == 0 && ui.rows.isEmpty()) {
            EmptyState(icon = Icons.Rounded.Insights, title = "لا شيء بعد", body = "ستظهر الأرقام هنا بعد أول تواصل.")
            return@Column
        }

        SoftCard {
            Text(ArabicText.coverage(ui.current.contacted, ui.current.total), style = MaterialTheme.typography.titleLarge)
        }

        SoftCard {
            Text("التغطية في آخر 6 أشهر", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            MonthBars(ui.months)
        }

        SectionTitle("مع كل شخص")
        Text(
            "متوسط الفترة الفعلية بين التواصلات مقابل الهدف، ومن بادر.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SoftCard {
            ui.rows.forEachIndexed { i, row ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                PersonStatRow(row)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonthBars(months: List<MonthCoverage>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        months.forEachIndexed { i, m ->
            val isCurrent = i == months.lastIndex
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    if (m.total == 0) "—" else "${(m.fraction * 100).roundToInt()}٪",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .fillMaxHeight(m.fraction.coerceAtLeast(0.02f))
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    ArabicText.monthShort(m.month),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PersonStatRow(row: StatRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(row.name, row.tag, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(row.intervalText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val total = row.byMe + row.byThem
        Text(
            if (total == 0) "—" else "أنا ${row.byMe} · هو ${row.byThem}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
