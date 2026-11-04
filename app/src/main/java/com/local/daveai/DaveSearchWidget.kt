package com.local.daveai

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import java.util.UUID

class DaveSearchWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val query = prefs[QUERY_KEY] ?: ""
            val answer = prefs[ANSWER_KEY] ?: ""

            val backgroundColor = Color(0xFF0F172A)
            val searchBarBackground = Color(0xFF1E293B)
            val neonCyan = Color(0xFF00F0FF)
            val textColor = Color(0xFFF8FAFC)
            val secondaryTextColor = Color(0xFF94A3B8)
            val accentGreen = Color(0xFF10B981)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(12.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "⚡ DAVE SEARCH",
                        style = TextStyle(
                            color = ColorProvider(neonCyan, neonCyan),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = if (answer.isNotEmpty()) "ACTIVE" else "READY",
                        style = TextStyle(
                            color = ColorProvider(accentGreen, accentGreen),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                if (answer.isNotEmpty()) {
                    // Expanded Response Box directly inside the Glance widget layout
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(searchBarBackground)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "Q: $query",
                                style = TextStyle(
                                    color = ColorProvider(secondaryTextColor, secondaryTextColor),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Row(
                                modifier = GlanceModifier
                                    .background(Color(0xFF334155))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clickable(actionRunCallback<DaveClearAction>()),
                                verticalAlignment = Alignment.Vertical.CenterVertically
                            ) {
                                Text(
                                    text = "✕ Clear",
                                    style = TextStyle(
                                        color = ColorProvider(textColor, textColor),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        Text(
                            text = answer,
                            style = TextStyle(
                                color = ColorProvider(textColor, textColor),
                                fontSize = 12.sp
                            ),
                            maxLines = 4
                        )
                    }
                } else {
                    // Query Input / Action Chips View
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(searchBarBackground)
                            .padding(10.dp)
                            .clickable(actionRunCallback<DaveQueryAction>(actionParametersOf(DaveQueryAction.QUERY_PARAM_KEY to query.ifEmpty { "What is Dave AI status and capabilities?" }))),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = if (query.isNotEmpty()) "Query: $query (Tap to Submit)" else "Tap to ask Dave AI or run search...",
                            style = TextStyle(
                                color = ColorProvider(textColor, textColor),
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "🔍",
                            style = TextStyle(fontSize = 14.sp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier
                                .background(Color(0xFF334155))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable(actionRunCallback<DaveUpdateQueryAction>(actionParametersOf(DaveUpdateQueryAction.QUERY_PARAM_KEY to "Summarize recent project notes"))),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "📝 Set Query",
                                style = TextStyle(
                                    color = ColorProvider(textColor, textColor),
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Row(
                            modifier = GlanceModifier
                                .background(Color(0xFF334155))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable(actionRunCallback<DaveQueryAction>(actionParametersOf(DaveQueryAction.QUERY_PARAM_KEY to "Summarize recent project notes"))),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "✨ Summarize",
                                style = TextStyle(
                                    color = ColorProvider(textColor, textColor),
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Row(
                            modifier = GlanceModifier
                                .background(Color(0xFF334155))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable(actionRunCallback<DaveQueryAction>(actionParametersOf(DaveQueryAction.QUERY_PARAM_KEY to "Brainstorm AI assistant ideas"))),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "💡 Brainstorm",
                                style = TextStyle(
                                    color = ColorProvider(textColor, textColor),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        val QUERY_KEY = stringPreferencesKey("dave_search_query")
        val ANSWER_KEY = stringPreferencesKey("dave_search_answer")
    }
}

class DaveUpdateQueryAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val query = parameters[QUERY_PARAM_KEY] ?: ""
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[DaveSearchWidget.QUERY_KEY] = query
        }
        DaveSearchWidget().update(context, glanceId)
    }

    companion object {
        val QUERY_PARAM_KEY = ActionParameters.Key<String>("dave_update_query_param")
    }
}

class DaveQueryAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val query = parameters[QUERY_PARAM_KEY] ?: "Hello Dave"
        val db = ChatDatabase.getDatabase(context)
        val dao = db.messageDao()

        val response = try {
            val config = generationConfig { maxOutputTokens = 8192 }
            val cloudModel = GenerativeModel(
                modelName = "gemini-1.5-pro",
                apiKey = BuildConfig.GEMINI_API_KEY,
                systemInstruction = content { text(DaveSystemInstruction.daveSystemInstruction) },
                generationConfig = config
            )
            val result = cloudModel.generateContent(query)
            result.text ?: "Dave AI: I processed your query: '$query'."
        } catch (e: Throwable) {
            "Dave AI: Received query '$query'."
        }

        val currentTime = System.currentTimeMillis()
        val userMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            text = query,
            isUser = true,
            timestamp = currentTime,
            persona = "DAVE"
        )
        val aiMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            text = response,
            isUser = false,
            timestamp = currentTime + 1,
            persona = "DAVE"
        )
        try {
            dao.insertMessage(userMsg)
            dao.insertMessage(aiMsg)
        } catch (_: Throwable) {}

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[DaveSearchWidget.QUERY_KEY] = query
            prefs[DaveSearchWidget.ANSWER_KEY] = response
        }
        DaveSearchWidget().update(context, glanceId)
    }

    companion object {
        val QUERY_PARAM_KEY = ActionParameters.Key<String>("dave_query_param")
    }
}

class DaveClearAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[DaveSearchWidget.QUERY_KEY] = ""
            prefs[DaveSearchWidget.ANSWER_KEY] = ""
        }
        DaveSearchWidget().update(context, glanceId)
    }
}
