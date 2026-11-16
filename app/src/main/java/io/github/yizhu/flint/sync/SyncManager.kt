package io.github.yizhu.flint.sync

import android.content.Context
import io.github.yizhu.flint.core.AppConfig
import io.github.yizhu.flint.core.ConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Sync orchestration:
 *  - every config save -> silent push (debounced)
 *  - app cold start -> pull if last pull > 24h ago (or first run on a new device)
 *  - settings "import" button -> immediate pull & apply
 * Credentials live in EncryptedSharedPreferences; never in the repo.
 */
class SyncManager(
    private val context: Context,
    private val store: ConfigStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy {
        androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            "flint_secure",
            androidx.security.crypto.MasterKey.Builder(context)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build(),
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun repo(): WebDavRepo? {
        val url = prefs.getString("webdav_url", null) ?: return null
        val user = prefs.getString("webdav_user", null) ?: return null
        val pass = prefs.getString("webdav_pass", null) ?: return null
        return WebDavRepo("$url/flint_config.json", user, pass)
    }

    fun hookSilentPush() {
        store.onSaved = { scope.launch { store.config.value.let { repo()?.push(it.serialize()) } } }
    }

    fun saveCredentials(url: String, user: String, pass: String) {
        prefs.edit()
            .putString("webdav_url", url.trimEnd('/'))
            .putString("webdav_user", user)
            .putString("webdav_pass", pass)
            .apply()
        // push immediately after credentials change
        scope.launch { repo()?.push(store.config.value.serialize()) }
    }

    /** Cold-start pull: only if remote is newer than 24h since last pull. */
    fun maybePullOnStart() {
        scope.launch {
            val last = store.config.value.syncTimestampMs
            val now = System.currentTimeMillis()
            if (now - last > 24L * 3600 * 1000) {
                pullAndApply()
            }
        }
    }

    /** Manual import: pull remote config and apply it wholesale. */
    suspend fun pullAndApply(): Boolean {
        val remote = repo()?.pull() ?: return false
        val ok = store.replaceFrom(remote)
        if (ok) store.update { it.copy(syncTimestampMs = System.currentTimeMillis()) }
        return ok
    }

    /** Local export/import for backup without WebDAV. */
    fun exportTo(file: File) {
        file.writeText(store.config.value.serialize())
    }

    fun importFrom(file: File): Boolean =
        runCatching { store.replaceFrom(file.readText()) }.getOrDefault(false)
}
