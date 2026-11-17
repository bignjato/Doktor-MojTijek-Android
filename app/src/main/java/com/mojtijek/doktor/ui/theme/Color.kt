package com.mojtijek.doktor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Boje MojTijeka — iste vrijednosti kao iOS `Theme.swift` (mtTeal, mtInk…). */
object Mt {
    val Teal = Color(0xFF0E9488)
    val TealDark = Color(0xFF0F766E)
    val Green = Color(0xFF16A34A)
    val Red = Color(0xFFEF4444)
    val Purple = Color(0xFF8B5CF6)
    val Blue = Color(0xFF0EA5E9)
    val Orange = Color(0xFFF59E0B)

    /** Tinta za naslove i vrijednosti (mtInk). */
    val ink: Color
        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFFEAF0F4) else Color(0xFF17324A)
}
