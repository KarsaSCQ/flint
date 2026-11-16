package io.github.yizhu.flint.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SearchEngine(
    val id: String,          // stable id: "google", "baidu", custom uuid
    val name: String,        // display name (used in settings only)
    val urlTemplate: String, // https://www.google.com/search?q=%s
    val enabled: Boolean = true,
)

@Serializable
data class RailModule(
    val id: String,          // "translate" | "ai" | custom url module
    val enabled: Boolean = true,
)

@Serializable
data class TranslateEngineCfg(
    val id: String,          // "mlkit" | "microsoft" | "google" | "deepl" | "caiyun" | "youdao"
    val enabled: Boolean = false,
    val apiKey: String = "",
)

@Serializable
data class AiCfg(
    val baseUrl: String = "",      // OpenAI-compatible, e.g. https://api.deepseek.com/v1
    val apiKey: String = "",       // stored encrypted at rest, plaintext in memory model
    val model: String = "",
)

@Serializable
data class QuickItem(          // bottom-bar empty-state shortcuts / quick apps
    val key: String,           // app key or engine id or custom url
    val label: String = "",
    val url: String = "",      // for custom shortcuts
)

@Serializable
data class AppConfig(
    // interaction
    val enterSends: Boolean = true,   // A: enter executes; B: enter newline, icon sends
    // search
    val engines: List<SearchEngine> = defaultEngines(),
    val defaultEngineId: String = "google",
    val systemAppsWhitelist: Set<String> = emptySet(),  // package names explicitly visible
    val appsBlacklist: Set<String> = emptySet(),         // user apps explicitly hidden
    val shortcutsWhitelist: Set<String> = emptySet(),    // shortcut keys visible
    // bottom bar (empty state): ordered quick items
    val bottomBar: List<QuickItem> = emptyList(),
    // rail
    val rail: List<RailModule> = listOf(RailModule("translate"), RailModule("ai")),
    // translate
    val translateEngines: List<TranslateEngineCfg> = listOf(
        TranslateEngineCfg("mlkit", enabled = true),
        TranslateEngineCfg("mymemory", enabled = true),
        TranslateEngineCfg("google"),
    ),
    val translateShowCount: Int = 2,
    // ai
    val ai: AiCfg = AiCfg(),
    // webdav
    val webdavUrl: String = "",     // full https URL to a directory
    val syncTimestampMs: Long = 0,  // last remote pull time, for 24h policy
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        fun parse(s: String): AppConfig =
            if (s.isBlank()) AppConfig() else json.decodeFromString(serializer(), s)

        fun defaultEngines() = listOf(
            SearchEngine("google", "Google", "https://www.google.com/search?q=%s"),
            SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
            SearchEngine("baidu", "百度", "https://www.baidu.com/s?wd=%s"),
            SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
        )
    }

    fun serialize(): String = json.encodeToString(serializer(), this)
}
