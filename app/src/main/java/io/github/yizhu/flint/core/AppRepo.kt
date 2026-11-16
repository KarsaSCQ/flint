package io.github.yizhu.flint.core

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process

data class AppEntry(
    val key: String,            // packageName or packageName/shortcutId
    val label: String,
    val packageName: String,
    val shortcutId: String?,    // non-null => app shortcut ("微信扫一扫")
    val isSystem: Boolean,
    val tokens: List<String>,   // pinyin tokens for matching
)

/**
 * Scans launchable activities plus app shortcuts into a flat, searchable index.
 * System apps and shortcuts are reported here; visibility is filtered later by
 * settings whitelists (defaults: user apps visible, system apps and shortcuts hidden).
 */
class AppRepo(private val context: Context) {

    fun scan(): List<AppEntry> {
        val pm = context.packageManager
        val out = ArrayList<AppEntry>(256)

        for (info in pm.getInstalledPackages(0)) {
            val ai = info.applicationInfo ?: continue
            if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue
            val label = runCatching { ai.loadLabel(pm).toString() }.getOrNull() ?: ai.packageName
            out.add(
                AppEntry(
                    key = ai.packageName,
                    label = label,
                    packageName = ai.packageName,
                    shortcutId = null,
                    isSystem = ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0,
                    tokens = labelToTokens(label),
                )
            )
        }

        // App shortcuts need the launcher role; without it getShortcuts throws
        // SecurityException. Shortcuts are optional — scan defensively.
        runCatching {
            val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return@runCatching
            val query = LauncherApps.ShortcutQuery().apply {
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                )
            }
            val user = Process.myUserHandle()
            for (shortcut in launcherApps.getShortcuts(query, user) ?: emptyList()) {
                val label = runCatching { shortcut.shortLabel.toString() }.getOrNull() ?: continue
                out.add(
                    AppEntry(
                        key = "${shortcut.`package`}/${shortcut.id}",
                        label = label,
                        packageName = shortcut.`package`,
                        shortcutId = shortcut.id,
                        isSystem = true, // conservative: shortcuts hidden unless whitelisted
                        tokens = labelToTokens(label),
                    )
                )
            }
        }
        return out
    }

    fun start(entry: AppEntry) {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return
        if (entry.shortcutId != null) {
            launcherApps.startShortcut(entry.packageName, entry.shortcutId, null, null, Process.myUserHandle())
        } else {
            val intent = context.packageManager.getLaunchIntentForPackage(entry.packageName)
            if (intent != null) context.startActivity(intent)
        }
    }
}
