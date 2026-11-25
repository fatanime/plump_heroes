package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalUiScale = staticCompositionLocalOf { 1f }

private val DarkColorScheme = darkColorScheme(
    primary = GameGold,
    onPrimary = Color(0xFF261600),
    primaryContainer = Color(0xFF4A3900),
    onPrimaryContainer = GameGoldLight,
    secondary = StatAtkBlue,
    onSecondary = Color(0xFF001E30),
    secondaryContainer = Color(0xFF004975),
    onSecondaryContainer = Color(0xFFC7E7FF),
    tertiary = RarityEpic,
    onTertiary = Color.White,
    background = GameBgDark,
    onBackground = Color(0xFFF0EBF8),
    surface = GameSurfaceDark,
    onSurface = Color(0xFFF0EBF8),
    surfaceVariant = GameSurfaceVariant,
    onSurfaceVariant = Color(0xFFD3CCE3),
    outline = Color(0xFF6B5C82)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val baseDensity = LocalDensity.current
        val widthDp = if (constraints.hasBoundedWidth) maxWidth.value else 400f
        val heightDp = if (constraints.hasBoundedHeight) maxHeight.value else 820f

        // Smooth scale factor centered on 400x820dp reference phone
        val widthScale = (widthDp / 400f).coerceIn(0.72f, 1.25f)
        val heightScale = (heightDp / 820f).coerceIn(0.75f, 1.20f)
        val screenScale = (widthScale * 0.72f + heightScale * 0.28f).coerceIn(0.74f, 1.22f)

        // Dampen oversized system fontScale so compact layouts never overflow on smaller displays
        val normalizedUserFontScale = if (baseDensity.fontScale > 1f) {
            1f + (baseDensity.fontScale - 1f) * 0.30f
        } else {
            baseDensity.fontScale.coerceAtLeast(0.85f)
        }
        val adaptiveFontScale = (normalizedUserFontScale * screenScale).coerceIn(0.70f, 1.25f)
        val adaptiveDensity = Density(density = baseDensity.density, fontScale = adaptiveFontScale)

        CompositionLocalProvider(
            LocalDensity provides adaptiveDensity,
            LocalUiScale provides screenScale
        ) {
            MaterialTheme(
                colorScheme = DarkColorScheme,
                typography = Typography,
                content = content
            )
        }
    }
}
