package io.github.yizhu.flint.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yizhu.flint.LocalConfig
import io.github.yizhu.flint.ai.AiRepo
import io.github.yizhu.flint.ai.ChatMessage
import io.github.yizhu.flint.core.AppConfig
import kotlinx.coroutines.launch

/**
 * AI mode: fixed-height card, streaming answer, follow-up questions.
 * Session lives in memory only; leaving the mode destroys it.
 */
@Composable
fun AiCard(
    pendingQuestion: String,
    onQuestionConsumed: () -> Unit,
) {
    val cfgStore = LocalConfig.current
    val cfg = cfgStore.config.value
    val scope = rememberCoroutineScope()

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var streaming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    fun ask(question: String) {
        if (question.isBlank() || cfg.ai.baseUrl.isBlank()) {
            error = if (cfg.ai.baseUrl.isBlank()) "configure AI endpoint in settings" else null
            return
        }
        streaming = true
        error = null
        messages.add(ChatMessage("user", question))
        messages.add(ChatMessage("assistant", ""))
        val repo = AiRepo(cfg.ai.baseUrl, cfg.ai.apiKey, cfg.ai.model)
        scope.launch {
            try {
                repo.chatStream(messages.dropLast(1)).collect { partial ->
                    messages[messages.size - 1] = ChatMessage("assistant", partial)
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                streaming = false
            }
        }
    }

    // A question typed in the search field and "sent" arrives here.
    LaunchedEffect(pendingQuestion) {
        if (pendingQuestion.isNotBlank()) {
            ask(pendingQuestion)
            onQuestionConsumed()
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages) { m ->
                if (m.role == "user") {
                    Text(
                        m.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            m.content + if (streaming && m === messages.lastOrNull()) " ▍" else "",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        if (m.content.isNotEmpty() && m === messages.lastOrNull() && !streaming) {
                            CopyButton(m.content)
                        }
                    }
                }
            }
            if (error != null) {
                item {
                    Text(
                        error!!,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (messages.isEmpty() && error == null) {
                item {
                    Text(
                        "…",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
