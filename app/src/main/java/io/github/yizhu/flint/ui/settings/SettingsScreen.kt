package io.github.yizhu.flint.ui.settings

import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yizhu.flint.LocalConfig
import io.github.yizhu.flint.core.AppConfig
import io.github.yizhu.flint.ui.GlassCard
import io.github.yizhu.flint.ui.pressable

/**
 * Settings: stacked glass cards over the blurred wallpaper.
 * Groups: interaction / search & engines / translate / AI / sync.
 */
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val store = LocalConfig.current
    val cfg by store.config.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val sync = remember {
        io.github.yizhu.flint.sync.SyncManager(context, store)
    }
    var importState by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "设置",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.Close,
                contentDescription = "close settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(26.dp)
                    .pressable(onClick = onClose),
            )
        }
        Spacer(Modifier.height(20.dp))

        // --- 外观与交互 ---
        SettingsCard("交互") {
            SwitchRow(
                "回车执行",
                "关闭后回车换行，点发送图标执行",
                cfg.enterSends,
            ) { v -> store.update { it.copy(enterSends = v) } }
        }

        // --- 搜索与引擎 ---
        SettingsCard("搜索引擎") {
            for (engine in cfg.engines) {
                SwitchRow(
                    engine.name,
                    engine.urlTemplate,
                    engine.enabled,
                ) { v ->
                    store.update { c ->
                        c.copy(engines = c.engines.map {
                            if (it.id == engine.id) it.copy(enabled = v) else it
                        })
                    }
                }
            }
        }

        // --- 翻译 ---
        SettingsCard("翻译引擎") {
            for (te in cfg.translateEngines) {
                SwitchRow(
                    te.id,
                    null,
                    te.enabled,
                ) { v ->
                    store.update { c ->
                        c.copy(translateEngines = c.translateEngines.map {
                            if (it.id == te.id) it.copy(enabled = v) else it
                        })
                    }
                }
            }
        }

        // --- AI ---
        SettingsCard("AI 问答") {
            FieldRow("接口地址") { v -> store.update { it.copy(ai = it.ai.copy(baseUrl = v)) } }
            FieldRow("模型") { v -> store.update { it.copy(ai = it.ai.copy(model = v)) } }
            FieldRow("API Key") { v -> store.update { it.copy(ai = it.ai.copy(apiKey = v)) } }
        }

        // --- 同步 ---
        SettingsCard("WebDAV 同步") {
            FieldRow("服务器地址") { v -> store.update { it.copy(webdavUrl = v) } }
            FieldRow("账号") { v -> sync.saveCredentials(cfg.webdavUrl, v, "") }
            FieldRow("密码") { v -> sync.saveCredentials(cfg.webdavUrl, cfg.webdavUrl, v) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionButton("从云端导入") {
                    scope.launch {
                        val ok = sync.pullAndApply()
                        importState = if (ok) "已导入" else "导入失败：检查地址/凭据/网络"
                    }
                }
                ActionButton("导出到本地") {
                    val dir = android.os.Environment.getExternalStorageDirectory()
                    val f = java.io.File(context.filesDir, "flint_export.json")
                    sync.exportTo(f)
                    importState = "已导出: ${f.absolutePath}"
                }
            }
            if (importState.isNotEmpty()) {
                Text(importState, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        GlassSwitch(checked, onChange)
    }
}

/** Deep-customized switch: no default M3 thumb/track colors. */
@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    )
}

@Composable
private fun FieldRow(label: String, onCommit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextField(
        value = text,
        onValueChange = {
            text = it
            onCommit(it)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .pressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
