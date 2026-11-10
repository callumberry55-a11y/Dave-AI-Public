package com.local.daveai.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun DaveTheme(
    palette: DaveThemePalette = DaveThemePalette.NEON_OBSIDIAN,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkScheme = darkColorScheme(
        primary = palette.primary,
        secondary = palette.secondary,
        tertiary = Purple80,
        background = palette.background,
        surface = palette.surface,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightColorScheme(primary = palette.primary, secondary = palette.secondary)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
