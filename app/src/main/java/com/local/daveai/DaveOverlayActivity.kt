package com.local.daveai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.daveai.ui.common.GlassCard
import com.local.daveai.ui.theme.DaveTheme
import com.local.daveai.ui.theme.DaveThemePalette
import com.local.daveai.ui.theme.GlowOverlayCanvas

class DaveOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsManager = DaveSettingsManager(applicationContext)

        setContent {
            val activePalette by settingsManager.activePaletteFlow.collectAsStateWithLifecycle(
                initialValue = DaveThemePalette.NEON_OBSIDIAN
            )

            DaveTheme(palette = activePalette) {
                val viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val messages by viewModel.messages.collectAsStateWithLifecycle()
                val inputText by viewModel.inputText.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val isListeningVoice by viewModel.isListeningVoice.collectAsStateWithLifecycle()
                val listState = rememberLazyListState()

                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // C++ / AGSL Gemini-Style Glowing Border Overlay
                    GlowOverlayCanvas(palette = activePalette)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.40f)) // Semi-transparent background scrim
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                finish()
                            }
                            .windowInsetsPadding(WindowInsets.statusBars),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth(0.96f)
                                .fillMaxHeight(0.62f)
                                .navigationBarsPadding()
                                .padding(bottom = 8.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {}, // Consume clicks inside card
                            cornerRadius = 28.dp,
                            tintColor = activePalette.surface.copy(alpha = 0.92f),
                            borderColor = activePalette.primary.copy(alpha = 0.50f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = activePalette.primary.copy(alpha = 0.2f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = activePalette.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Dave Quick Assist",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "🟢 Gemini-Style Voice Assist • ${activePalette.displayName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = activePalette.primary
                                            )
                                        }
                                    }

                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(messages) { message ->
                                        val isUser = message.isUser
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                        ) {
                                            GlassCard(
                                                cornerRadius = 16.dp,
                                                tintColor = if (isUser) activePalette.primary.copy(alpha = 0.22f) else activePalette.surface.copy(alpha = 0.70f),
                                                borderColor = if (isUser) activePalette.primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f),
                                                modifier = Modifier.widthIn(max = 280.dp)
                                            ) {
                                                Text(
                                                    text = message.text,
                                                    modifier = Modifier.padding(12.dp),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    if (isLoading) {
                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                GlassCard(
                                                    cornerRadius = 16.dp,
                                                    tintColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                                                    borderColor = Color.White.copy(alpha = 0.25f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp,
                                                            color = activePalette.primary
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Dave is thinking...",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isListeningVoice) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.GraphicEq,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Listening to Callum...",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                // Input bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .imePadding()
                                        .padding(bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isListeningVoice) {
                                                viewModel.stopVoiceInput()
                                            } else {
                                                viewModel.startVoiceInput()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isListeningVoice) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                            contentDescription = "Voice Input",
                                            tint = if (isListeningVoice) MaterialTheme.colorScheme.error else activePalette.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { viewModel.updateInputText(it) },
                                        placeholder = { Text("Ask Dave anything...") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(24.dp),
                                        maxLines = 3
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (inputText.isNotBlank()) {
                                                viewModel.sendMessage(inputText)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                color = activePalette.primary,
                                                shape = RoundedCornerShape(24.dp)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Send,
                                            contentDescription = "Send",
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
