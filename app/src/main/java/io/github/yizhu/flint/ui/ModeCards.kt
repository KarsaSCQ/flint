package io.github.yizhu.flint.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Translate card: sticky source card + scrollable results area.
 * Engine aggregation lands in the module milestone; this is the layout shell
 * with fixed heights so it can never push the input field.
 */
@Composable
fun TranslateCard(query: String, onQueryChange: (String) -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
    ) {
        Text(
            "translate",
            modifier = Modifier.padding(16.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** AI card: fixed-height streaming answer surface. */
@Composable
fun AiCard(query: String, onQueryChange: (String) -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
    ) {
        Text(
            "ai",
            modifier = Modifier.padding(16.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
