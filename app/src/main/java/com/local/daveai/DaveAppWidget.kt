package com.local.daveai

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.firstOrNull

class DaveAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaveAppWidget()
}

class DaveAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var recentPrompt = "Tap to open Dave AI assistant..."
        try {
            val db = ChatDatabase.getDatabase(context)
            val messages = db.messageDao().getAllMessages().firstOrNull()
            val lastUserMsg = messages?.lastOrNull { it.isUser } ?: messages?.lastOrNull()
            if (lastUserMsg != null) {
                recentPrompt = lastUserMsg.text
            }
        } catch (_: Exception) {
            // fallback
        }

        provideContent {
            val backgroundColor = Color(0xFF0F172A)
            val neonCyan = Color(0xFF00F0FF)
            val textColor = Color(0xFFF8FAFC)
            val secondaryTextColor = Color(0xFF94A3B8)
            val accentGreen = Color(0xFF10B981)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "⚡ DAVE AI",
                        style = TextStyle(
                            color = ColorProvider(neonCyan, neonCyan),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "ONLINE",
                        style = TextStyle(
                            color = ColorProvider(accentGreen, accentGreen),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "Recent Query:",
                    style = TextStyle(
                        color = ColorProvider(secondaryTextColor, secondaryTextColor),
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    text = recentPrompt,
                    style = TextStyle(
                        color = ColorProvider(textColor, textColor),
                        fontSize = 13.sp
                    ),
                    maxLines = 2
                )
            }
        }
    }
}
