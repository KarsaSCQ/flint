package io.github.yizhu.flint.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import io.github.yizhu.flint.LocalAppRepo
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yizhu.flint.LocalConfig
import io.github.yizhu.flint.LocalHistory
import io.github.yizhu.flint.LocalSearch
import io.github.yizhu.flint.core.AppEntry
import kotlinx.coroutines.launch

/**
 * Single-column Spotlight-style search surface.
 * Layout: search field (top) -> breathing gap (wallpaper shows through)
 * -> results / mode cards -> bottom bar docked above keyboard.
 */
@Composable
fun SearchScreen(
    onExit: () -> Unit,
    prefill: String = "",
    startMode: String? = null,
) {
    val search = LocalSearch.current
    val configStore = LocalConfig.current
    val history = LocalHistory.current
    val appRepo = LocalAppRepo.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val cfg by configStore.config.collectAsState()

    var query by remember { mutableStateOf(prefill) }
    var mode by remember {
        mutableStateOf(
            when (startMode) {
                "translate" -> Mode.Translate
                "ai" -> Mode.Ai
                else -> Mode.Search
            }
        )
    }
    var indexVersion by remember { mutableStateOf(0) }
    val results = remember(query, cfg, indexVersion) { search.search(query, cfg) }

    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        search.rebuild()
        indexVersion++
        focus.requestFocus()
    }

    var translateSource by remember { mutableStateOf(prefill) }
    var pendingAiQuestion by remember { mutableStateOf(prefill) }
    var showSettings by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { e ->
                if (e.key == Key.Escape) { onExit(); true } else false
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            SearchField(
                value = query,
                onValueChange = { new ->
                    // Space on empty input recalls history (first item fills the field).
                    if (query.isEmpty() && new == " ") {
                        history.all().firstOrNull()?.let { query = it.text }
                    } else {
                        query = new
                    }
                },
                mode = mode,
                onExitMode = { mode = Mode.Search },
                focusRequester = focus,
                onEnter = {
                    when (mode) {
                        Mode.Ai -> {
                            pendingAiQuestion = query
                        }
                        Mode.Translate -> {
                            // Enter in translate mode: nothing to execute; keep editing.
                        }
                        Mode.Search -> if (cfg.enterSends) {
                            val first = results.firstOrNull()
                            if (first != null) {
                                history.add(query)
                                appRepo.start(first)
                                onExit()
                            } else {
                                val engine = cfg.engines.firstOrNull { it.id == cfg.defaultEngineId }
                                    ?: cfg.engines.firstOrNull()
                                if (engine != null && query.isNotBlank()) {
                                    history.add(query)
                                    openEngine(context, engine, query)
                                    onExit()
                                }
                            }
                        }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))

            when (mode) {
                Mode.Search -> {
                    if (query.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        ResultsList(results) { entry ->
                            history.add(query)
                            appRepo.start(entry)
                            onExit()
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
                Mode.Translate -> {
                    // Typing in translate mode retranslates the new content.
                    TranslateCard(
                        source = translateSource,
                        engines = cfg.translateEngines.filter { it.enabled }.map { it.id },
                        showCount = cfg.translateShowCount,
                        onNewTranslation = {},
                        onExit = { mode = Mode.Search },
                    )
                    Spacer(Modifier.weight(1f))
                }
                Mode.Ai -> {
                    AiCard(
                        pendingQuestion = pendingAiQuestion,
                        onQuestionConsumed = { pendingAiQuestion = ""; query = "" },
                    )
                    Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))
            // Intent isolation: engine bar hidden while in translate/AI mode.
            if (mode == Mode.Search) {
                BottomBar(
                    hasQuery = query.isNotEmpty(),
                    engines = cfg.engines.filter { it.enabled },
                    onEngineTap = { engine ->
                        history.add(query)
                        openEngine(context, engine, query)
                        onExit()
                    },
                )
            }
        }

        // Assist rail: right edge, independent glass capsule.
        AssistRail(
            onTranslate = {
                translateSource = query
                mode = Mode.Translate
                query = ""
            },
            onAi = {
                pendingAiQuestion = query
                mode = Mode.Ai
                query = ""
            },
            onSettings = { showSettings = true },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
        )

        if (showSettings) {
            io.github.yizhu.flint.ui.settings.SettingsScreen(onClose = { showSettings = false })
        }
    }
}

enum class Mode { Search, Translate, Ai }
