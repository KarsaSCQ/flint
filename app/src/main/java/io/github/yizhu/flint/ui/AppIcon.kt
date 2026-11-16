package io.github.yizhu.flint.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import io.github.yizhu.flint.LocalAppRepo
import io.github.yizhu.flint.core.AppEntry

/** App / shortcut icon: loaded async, monogram fallback. */
@Composable
fun AppIcon(entry: AppEntry, size: Int = 40) {
    val context = LocalContext.current
    var icon by remember(entry.key) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    LaunchedEffect(entry.key) {
        icon = runCatching {
            context.packageManager.getApplicationIcon(entry.packageName)
        }.getOrNull()
    }
    if (icon != null) {
        val bmp = remember(entry.key) {
            (icon as Drawable).toBitmapSafe(96)
        }
        if (bmp != null) {
            Image(
                painter = BitmapPainter(bmp.asImageBitmap()),
                contentDescription = entry.label,
                modifier = Modifier.size(size.dp),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        Monogram(entry.label, size)
    }
}

private fun Drawable.toBitmapSafe(px: Int): android.graphics.Bitmap? = runCatching {
    val b = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(b)
    setBounds(0, 0, px, px)
    draw(c)
    b
}.getOrNull()

@Composable
private fun Monogram(label: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .pressable(onClick = {}),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(
            label.take(1),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
