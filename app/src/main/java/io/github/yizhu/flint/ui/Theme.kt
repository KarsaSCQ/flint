package io.github.yizhu.flint.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * Glass material system.
 *
 * Surfaces are translucent so the blurred wallpaper (FLAG_BLUR_BEHIND) shows
 * through; hairline stroke = top-lit / bottom-dim dual border for the edge
 * highlight; Monet palette comes straight from the system — hue untouched.
 */
object Glass {
    /** Card fill. Light: darker translucent tint; Dark: elevated translucent. */
    @Composable
    fun fill(): Color {
        val scheme = MaterialTheme.colorScheme
        val dark = isSystemInDarkTheme()
        return if (dark) scheme.surfaceContainerHighest.copy(alpha = 0.55f)
        else scheme.surfaceContainerHigh.copy(alpha = 0.42f)
    }

    /** Hairline stroke: brighter on top edge, dimmer at bottom. */
    @Composable
    fun stroke(): Color {
        val dark = isSystemInDarkTheme()
        return if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.55f)
    }

    fun strokeDim(dark: Boolean): Color =
        if (dark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.08f)

    val cardShape = RoundedCornerShape(28.dp)
    val pillShape = RoundedCornerShape(percent = 50)
    val barShape = RoundedCornerShape(24.dp)
}

val FlintShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun FlintTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme = remember(dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (dark) darkColorScheme() else lightColorScheme()
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        shapes = FlintShapes,
        content = content,
    )
}
