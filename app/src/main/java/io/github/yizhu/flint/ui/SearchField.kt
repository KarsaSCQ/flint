package io.github.yizhu.flint.ui
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yizhu.flint.core.AppEntry

/**
 * The search field: grows with content up to 3 lines, then scrolls internally.
 * No placeholder. Ever.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    mode: Mode,
    onExitMode: () -> Unit,
    focusRequester: FocusRequester,
    onEnter: () -> Unit = {},
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .heightIn(max = 96.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onKeyEvent { e ->
                        if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                            onEnter(); true
                        } else false
                    },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onEnter() }),
                maxLines = 3,
            )
            if (mode != Mode.Search) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "exit mode",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .pressable(onClick = onExitMode)
                        .padding(2.dp),
                )
            }
        }
    }
}

@Composable
fun ResultsList(results: List<AppEntry>, onPick: (AppEntry) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(results, key = { it.key }) { entry ->
            ResultRow(entry, onPick)
        }
    }
}
