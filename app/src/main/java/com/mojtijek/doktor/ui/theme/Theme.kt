package com.mojtijek.doktor.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta je prenesena s iOS-a: mtBackground, mtCard (kartice na pozadini), MTCard (sekundarna
// sistemska pozadina), mtTealSoft kao podloga kartice doze. Tema prati sustav, kao na iOS-u.
private val Svijetla = lightColorScheme(
    primary = Mt.Teal, onPrimary = Color.White,
    primaryContainer = Color(0xFFE9F6F4), onPrimaryContainer = Mt.TealDark,
    secondary = Mt.Teal, onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9F6F4), onSecondaryContainer = Mt.TealDark,
    tertiary = Mt.Purple, error = Mt.Red,
    background = Color(0xFFF6F8F9), onBackground = Color(0xFF111111),
    surface = Color.White, onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF2F2F7), onSurfaceVariant = Color(0xFF6B6B70),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White, surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF2F2F7), surfaceContainerHighest = Color(0xFFF2F2F7),
    outline = Color(0xFFC6C6C8), outlineVariant = Color(0xFFE5E5EA),
)
private val Tamna = darkColorScheme(
    primary = Mt.Teal, onPrimary = Color.White,
    // Na tamnoj podlozi mtTealDark nije čitljiv, pa je tekst na njoj svjetliji ton iste boje.
    primaryContainer = Color(0xFF123330), onPrimaryContainer = Color(0xFF5EEAD4),
    secondary = Mt.Teal, onSecondary = Color.White,
    secondaryContainer = Color(0xFF123330), onSecondaryContainer = Color(0xFF5EEAD4),
    tertiary = Mt.Purple, error = Mt.Red,
    background = Color(0xFF0E1214), onBackground = Color.White,
    surface = Color(0xFF1C1C1E), onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E), onSurfaceVariant = Color(0xFF98989F),
    surfaceContainerLowest = Color(0xFF1C1C1E), surfaceContainerLow = Color(0xFF1C1C1E), surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF1C1C1E), surfaceContainerHighest = Color(0xFF1C1C1E),
    outline = Color(0xFF48484A), outlineVariant = Color(0xFF38383A),
)
private val Oblici = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),          // kartice (MTCard)
    large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
)

/** Izbor teme u Profilu (iOS `appearance`): "system", "light" ili "dark"; pamti se u postavkama aplikacije. */
object Tema {
    private const val KLJUC = "appearance"
    var izbor by mutableStateOf("system")
        private set
    fun ucitaj(context: Context) { izbor = context.getSharedPreferences("mojtijek", Context.MODE_PRIVATE).getString(KLJUC, null) ?: "system" }
    fun postavi(context: Context, vrijednost: String) {
        izbor = vrijednost
        context.getSharedPreferences("mojtijek", Context.MODE_PRIVATE).edit().putString(KLJUC, vrijednost).apply()
    }
}

@Composable @ReadOnlyComposable
fun tamnaTema(): Boolean = when (Tema.izbor) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }

@Composable
fun DoktorMojTijekTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (tamnaTema()) Tamna else Svijetla, shapes = Oblici, content = content)
}
