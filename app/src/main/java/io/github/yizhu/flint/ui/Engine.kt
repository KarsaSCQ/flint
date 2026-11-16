package io.github.yizhu.flint.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.yizhu.flint.core.SearchEngine

fun openEngine(context: Context, engine: SearchEngine, query: String) {
    val url = engine.urlTemplate.replace("%s", Uri.encode(query))
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
