package com.local.daveai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.daveai.ui.theme.DaveTheme
import com.local.daveai.ui.theme.DaveThemePalette
import kotlinx.serialization.Serializable

@Serializable
data object ChatRoute

@Serializable
data object LiveCanvasRoute

@Serializable
data object SettingsRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsManager = DaveSettingsManager(applicationContext)

        setContent {
            val activePalette by settingsManager.activePaletteFlow.collectAsStateWithLifecycle(
                initialValue = DaveThemePalette.NEON_OBSIDIAN
            )

            DaveTheme(palette = activePalette) {
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                ) { /* Permissions Granted Result */ }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val permissions = mutableListOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.READ_CALENDAR,
                        android.Manifest.permission.WRITE_CALENDAR,
                        android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.SEND_SMS,
                        android.Manifest.permission.CALL_PHONE,
                        android.Manifest.permission.CAMERA
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                        permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                        permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                        permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }

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
                                        },
                                        onNavigateToSettings = {
                                            backStack.add(SettingsRoute)
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
                                is SettingsRoute -> NavEntry(key) {
                                    SettingsScreen(
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
