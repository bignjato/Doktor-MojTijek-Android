package com.mojtijek.app.ui.theme

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

private val Cyan500 = Color(0xFF0891b2)
private val Cyan700 = Color(0xFF0e4d64)
private val Violet500 = Color(0xFF7c3aed)
private val Violet700 = Color(0xFF4c1d95)

private val LightColorScheme = lightColorScheme(
    primary = Cyan500,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFc8f4ff),
    onPrimaryContainer = Color(0xFF001f2a),
    secondary = Violet500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFe8d5ff),
    onSecondaryContainer = Color(0xFF21005d),
    background = Color(0xFFfafcff),
    onBackground = Color(0xFF1a1c1e),
    surface = Color(0xFFfafcff),
    onSurface = Color(0xFF1a1c1e),
    error = Color(0xFFba1a1a),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4dd8f0),
    onPrimary = Color(0xFF003545),
    primaryContainer = Cyan700,
    onPrimaryContainer = Color(0xFFb4e9ff),
    secondary = Color(0xFFc4b5fd),
    onSecondary = Color(0xFF2e1065),
    secondaryContainer = Violet700,
    onSecondaryContainer = Color(0xFFe8d5ff),
    background = Color(0xFF1a1c1e),
    onBackground = Color(0xFFe3e2e6),
    surface = Color(0xFF1a1c1e),
    onSurface = Color(0xFFe3e2e6),
    error = Color(0xFFffB4ab),
    onError = Color(0xFF690005)
)

@Composable
fun MojTijekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
