package io.github.yizhu.flint

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yizhu.flint.ui.FlintTheme
import io.github.yizhu.flint.ui.GlassCard
import io.github.yizhu.flint.ui.pressable

/**
 * PROCESS_TEXT entry: user selects text anywhere -> "Flint" -> floating glass
 * dialog offering translate / ask AI / open Flint with the text.
 */
class ProcessTextActivity : ComponentActivity() {

    private val selectedText: String
        get() = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = selectedText
        setContent {
            FlintTheme {
                ProcessTextSheet(
                    text = text,
                    onTranslate = { openMain(text, "translate") },
                    onAsk = { openMain(text, "ai") },
                    onOpen = { openMain(text, null) },
                )
            }
        }
    }

    private fun openMain(text: String, mode: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("prefill", text)
            if (mode != null) putExtra("mode", mode)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
private fun ProcessTextSheet(
    text: String,
    onTranslate: () -> Unit,
    onAsk: () -> Unit,
    onOpen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                )
                Spacer(Modifier.size(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionIcon(Icons.Filled.Translate, "translate", onTranslate)
                    ActionIcon(Icons.Filled.AutoAwesome, "ai", onAsk)
                    ActionIcon(Icons.Filled.Search, "flint", onOpen)
                }
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = desc,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(28.dp)
            .pressable(onClick = onClick),
    )
}
