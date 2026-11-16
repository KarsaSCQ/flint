package io.github.yizhu.flint.ui
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Glass surface: translucent fill + hairline stroke (top-lit). */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = Glass.cardShape,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Glass.fill())
            .border(1.dp, Glass.stroke(), shape)
    ) {
        content()
    }
}

/**
 * Pressable: scale-to-0.96 feedback driven by press state; no ripple.
 * Use for every tappable surface.
 */
@Composable
fun Modifier.pressable(
    shape: RoundedCornerShape = RoundedCornerShape(percent = 50),
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
