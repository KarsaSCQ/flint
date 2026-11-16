package io.github.yizhu.flint.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Single-file JSON store: trivially exportable, importable, and WebDAV-syncable.
 * Every mutation persists immediately and notifies listeners (silent upload hook).
 */
class ConfigStore(context: Context) {

    private val file = File(context.filesDir, "flint_config.json")
    private val _config = MutableStateFlow(load())
    val config: StateFlow<AppConfig> = _config

    var onSaved: (() -> Unit)? = null   // WebDAV silent upload hook

    private fun load(): AppConfig = runCatching { AppConfig.parse(file.readText()) }
        .getOrDefault(AppConfig())

    fun update(transform: (AppConfig) -> AppConfig) {
        val next = transform(_config.value)
        _config.value = next
        file.writeText(next.serialize())
        onSaved?.invoke()
    }

    fun replaceFrom(raw: String): Boolean = runCatching {
        val next = AppConfig.parse(raw)
        _config.value = next
        file.writeText(next.serialize())
        onSaved?.invoke()
        true
    }.getOrDefault(false)
}
