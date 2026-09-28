package com.mojtijek.doktor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.Black,
    primaryContainer = TealDark,
    secondary = TealLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant
)

// Force dark theme to match iOS
private val LightColorScheme = DarkColorScheme

@Composable
fun DoktorMojTijekTheme(
    darkTheme: Boolean = true, // Force dark theme to match iOS
    dynamicColor: Boolean = false, // Disable dynamic color to match iOS
    content: @Composable () -> Unit
) {
    // Always use dark theme to match iOS MojTijek
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
