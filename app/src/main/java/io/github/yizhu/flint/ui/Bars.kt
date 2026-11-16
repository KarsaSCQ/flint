package io.github.yizhu.flint.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yizhu.flint.core.AppEntry
import io.github.yizhu.flint.core.SearchEngine

/** One result row: icon left, label right. Single column, no group headers. */
@Composable
fun ResultRow(entry: AppEntry, onPick: (AppEntry) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = { onPick(entry) })
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(entry, size = 40)
        Spacer(Modifier.width(12.dp))
        Text(
            entry.label,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** Bottom bar: quick items when empty, engines when typing. */
@Composable
fun BottomBar(
    hasQuery: Boolean,
    engines: List<SearchEngine>,
    onEngineTap: (SearchEngine) -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasQuery) {
                for (engine in engines) {
                    EngineChip(engine) { onEngineTap(engine) }
                }
            }
        }
    }
}

/** Engine chip: colored monogram disc, no text on the main surface. */
@Composable
fun EngineChip(engine: SearchEngine, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            engine.name.take(1).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
        )
    }
}

/** Right-edge assist rail: translate / ai / settings. */
@Composable
fun AssistRail(
    onTranslate: () -> Unit,
    onAi: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier,
        shape = Glass.pillShape,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RailIcon(Icons.Filled.Translate, "translate", onTranslate)
            RailIcon(Icons.Filled.AutoAwesome, "ai", onAi)
            RailIcon(Icons.Filled.Settings, "settings", onSettings)
        }
    }
}

@Composable
private fun RailIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = desc,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .size(26.dp)
            .pressable(onClick = onClick),
    )
}
