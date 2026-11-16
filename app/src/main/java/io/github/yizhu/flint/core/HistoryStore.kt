package io.github.yizhu.flint.core

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class HistoryItem(val text: String, val at: Long)

/**
 * Input history: recalled by pressing space on empty input; entries live 30 days.
 * Plain JSON file — exportable and trivially pruned.
 */
class HistoryStore(context: Context) {
    private val file = File(context.filesDir, "flint_history.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val items: MutableList<HistoryItem> = load()

    fun all(): List<HistoryItem> = items.sortedByDescending { it.at }

    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        items.removeAll { it.text == t }
        items.add(HistoryItem(t, System.currentTimeMillis()))
        prune()
        save()
    }

    private fun prune() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        items.removeAll { it.at < cutoff }
    }

    private fun save() {
        file.writeText(json.encodeToString(ListSerializer(HistoryItem.serializer()), items))
    }

    private fun load(): MutableList<HistoryItem> = runCatching {
        json.decodeFromString(ListSerializer(HistoryItem.serializer()), file.readText()).toMutableList()
    }.getOrDefault(mutableListOf())
}
