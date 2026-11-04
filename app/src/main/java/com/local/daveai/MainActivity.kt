package com.local.daveai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.local.daveai.ui.theme.DaveTheme
import kotlinx.serialization.Serializable

@Serializable
data object ChatRoute

@Serializable
data object LiveCanvasRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val backStack = remember { mutableStateListOf<Any>(ChatRoute) }

                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = { key ->
                            when (key) {
                                is ChatRoute -> NavEntry(key) {
                                    ChatScreen(
                                        onNavigateToLiveCanvas = {
                                            backStack.add(LiveCanvasRoute)
                                        }
                                    )
                                }
                                is LiveCanvasRoute -> NavEntry(key) {
                                    LiveCanvasScreen(
                                        onBack = {
                                            backStack.removeLastOrNull()
                                        }
                                    )
                                }
                                else -> error("Unknown route: $key")
                            }
                        }
                    )
                }
            }
        }
    }
}
