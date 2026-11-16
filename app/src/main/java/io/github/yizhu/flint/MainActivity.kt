package io.github.yizhu.flint

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import io.github.yizhu.flint.sync.SyncManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.yizhu.flint.core.AppRepo
import io.github.yizhu.flint.core.ConfigStore
import io.github.yizhu.flint.core.HistoryStore
import io.github.yizhu.flint.core.PinyinInit
import io.github.yizhu.flint.core.SearchService
import io.github.yizhu.flint.ui.FlintTheme
import io.github.yizhu.flint.ui.SearchScreen

val LocalSearch = staticCompositionLocalOf<SearchService> {
    error("no SearchService")
}
val LocalConfig = staticCompositionLocalOf<ConfigStore> { error("no ConfigStore") }
val LocalHistory = staticCompositionLocalOf<HistoryStore> { error("no HistoryStore") }
val LocalAppRepo = staticCompositionLocalOf<AppRepo> { error("no AppRepo") }

class MainActivity : ComponentActivity() {

    lateinit var configStore: ConfigStore
    lateinit var historyStore: HistoryStore
    lateinit var appRepo: AppRepo
    lateinit var searchService: SearchService
    lateinit var syncManager: io.github.yizhu.flint.sync.SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PinyinInit.init(applicationContext)
        configStore = ConfigStore(applicationContext)
        historyStore = HistoryStore(applicationContext)
        appRepo = AppRepo(this)
        searchService = SearchService(appRepo, configStore)
        syncManager = SyncManager(applicationContext, configStore).also {
            it.hookSilentPush()
            it.maybePullOnStart()
        }
        // Real blur-behind: the window is translucent, the wallpaper shows through.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
            WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
        )
        window.attributes = window.attributes.apply {
            blurBehindRadius = 80
            dimAmount = 0.12f
        }

        setContent {
            FlintTheme {
                CompositionLocalProvider(
                    LocalSearch provides searchService,
                    LocalConfig provides configStore,
                    LocalHistory provides historyStore,
                    LocalAppRepo provides appRepo,
                ) {
                    SearchScreen(
                        prefill = intent?.getStringExtra("prefill") ?: "",
                        startMode = intent?.getStringExtra("mode"),
                        onExit = { finishAndKill() },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(0)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAndKill()
    }

    /** Zero-residency exit: destroy the process the moment the UI leaves. */
    fun finishAndKill() {
        finishAndRemoveTask()
        android.os.Process.killProcess(android.os.Process.myPid())
        kotlin.system.exitProcess(0)
    }
}
