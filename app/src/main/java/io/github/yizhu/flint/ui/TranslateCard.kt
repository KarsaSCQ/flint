package io.github.yizhu.flint.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yizhu.flint.LocalConfig
import io.github.yizhu.flint.core.AppConfig
import io.github.yizhu.flint.translate.TrResult
import io.github.yizhu.flint.translate.TranslateRepo
import io.github.yizhu.flint.translate.Translation
import kotlinx.coroutines.launch

/**
 * Translate mode surface. Sticky architecture:
 *   [source card] pinned under the input field (max 3 lines, internal scroll)
 *   [results area] fixed height, scrolls internally; engines append vertically.
 */
@Composable
fun TranslateCard(
    source: String,
    engines: List<String>,
    showCount: Int,
    onNewTranslation: (String) -> Unit,
    onExit: () -> Unit,
) {
    val repo = remember { TranslateRepo() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val translations = remember { mutableStateListOf<Translation>() }
    var expanded by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }

    // New source text arrives -> retranslate, replacing results (per design: overwrite).
    LaunchedEffect(source) {
        if (source.isBlank()) {
            translations.clear()
            expanded = false
        } else {
            working = true
            translations.clear()
            val results = repo.translateAll(context, source, engines)
            translations.addAll(results)
            working = false
        }
    }

    val visible = if (expanded) translations else translations.take(showCount)
    val hidden = translations.size - visible.size

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Sticky source card
            Text(
                source,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))

            // Scrollable results area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visible) { tr ->
                    TranslationRow(tr)
                }
                if (hidden > 0) {
                    item {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "more engines",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .pressable(onClick = { expanded = true })
                                .align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationRow(tr: Translation) {
    Column {
        when (val r = tr.result) {
            is TrResult.Ok -> {
                Text(
                    r.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            is TrResult.Err -> {
                Text(
                    r.message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr.engineId,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            CopyButton(tr.result.let { (it as? TrResult.Ok)?.text ?: "" })
        }
    }
}

/** Copy icon that flips to a checkmark on tap. No text, ever. */
@Composable
fun CopyButton(text: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Icon(
        if (copied) Icons.Filled.Done else Icons.Filled.ContentCopy,
        contentDescription = "copy",
        tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(18.dp)
            .pressable(onClick = {
                if (text.isNotEmpty()) {
                    clipboard.setText(AnnotatedString(text))
                    copied = true
                }
            }),
    )
}
