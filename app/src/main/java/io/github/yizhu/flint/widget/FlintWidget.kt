package io.github.yizhu.flint.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.yizhu.flint.MainActivity

/**
 * 2x1 glass switch: tap -> Flint opens with the keyboard up.
 * No text input in the widget — it is only a beautiful launcher button.
 */
class FlintWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                GlassButton()
            }
        }
    }

    @Composable
    private fun GlassButton() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(28.dp)
                .background(Color(0x66000000))
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                "燧",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFE8C56A)),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

class FlintWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlintWidget()
}
