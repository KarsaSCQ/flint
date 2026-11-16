package io.github.yizhu.flint.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates the index + settings whitelists into the final search result list.
 * Priority per design doc: abbr pinyin > full pinyin > label text.
 */
class SearchService(
    private val repo: AppRepo,
    private val configStore: ConfigStore,
) {
    private var index: List<AppEntry> = emptyList()

    suspend fun rebuild() = withContext(Dispatchers.IO) {
        index = repo.scan()
    }
    fun visibleEntries(cfg: AppConfig): List<AppEntry> = index.filter { e ->
        when {
            e.shortcutId != null -> e.key in cfg.shortcutsWhitelist
            e.isSystem -> e.key in cfg.systemAppsWhitelist
            else -> e.key !in cfg.appsBlacklist
        }
    }

    fun search(query: String, cfg: AppConfig): List<AppEntry> {
        if (query.isBlank()) return emptyList()
        val visible = visibleEntries(cfg)
        val results = ArrayList<Pair<Int, AppEntry>>(64)
        for (entry in visible) {
            val score = PinyinMatcher.score(query, entry.label, entry.tokens) ?: continue
            results.add(score to entry)
        }
        android.util.Log.i("FlintScan", "search '$query' visible=${visible.size} results=${results.size}")
        results.sortBy { it.first }
        return results.map { it.second }
    }
}
