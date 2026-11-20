package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeId

object PixelColors {
  val Outline = Color(0xFF2B2140)
  val Green = Color(0xFF34C759)
  val GreenDark = Color(0xFF1F8A4C)
  val Gold = Color(0xFFFFC933)
  val Orange = Color(0xFFF59A23)
  val Red = Color(0xFFE5484D)
  val Warning = Color(0xFFE08A00)
  val GemBlue = Color(0xFF3B82F6)
  val White = Color(0xFFFFFFFF)
}

data class GamefyPalette(
  val themeId: AppThemeId,
  val background: Color,
  val surface: Color,
  val surfaceSubtle: Color,
  val textPrimary: Color,
  val textMuted: Color,
  val accent: Color,
  val accentSecondary: Color,
  val divider: Color,
  val outline: Color,
  val isDark: Boolean,
)

fun paletteForTheme(themeId: AppThemeId): GamefyPalette = when (themeId) {
  AppThemeId.VIOLET -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFFF3F1FB),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFECE8F9),
    textPrimary = Color(0xFF1D1B2E),
    textMuted = Color(0xFF6B6882),
    accent = Color(0xFF6C4CF1),
    accentSecondary = Color(0xFFA06BFF),
    divider = Color(0xFFE2DDF3),
    outline = Color(0xFF2B2140),
    isDark = false,
  )

  AppThemeId.OCEAN -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFFEEF6FB),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFE2F0F8),
    textPrimary = Color(0xFF10222E),
    textMuted = Color(0xFF5D7686),
    accent = Color(0xFF0B7FC7),
    accentSecondary = Color(0xFF2FC4D6),
    divider = Color(0xFFD5E6F0),
    outline = Color(0xFF1B2B3A),
    isDark = false,
  )

  AppThemeId.FOREST -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFFF0F6F1),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFE4EFE6),
    textPrimary = Color(0xFF15261B),
    textMuted = Color(0xFF5F7766),
    accent = Color(0xFF2A8A56),
    accentSecondary = Color(0xFF6FBF5F),
    divider = Color(0xFFD7E7DA),
    outline = Color(0xFF1E3224),
    isDark = false,
  )

  AppThemeId.ROSE -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFFFDF1F4),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFFAE4EA),
    textPrimary = Color(0xFF2B1520),
    textMuted = Color(0xFF86606F),
    accent = Color(0xFFD6336C),
    accentSecondary = Color(0xFFFF7A9C),
    divider = Color(0xFFF2D6E0),
    outline = Color(0xFF3A1D2B),
    isDark = false,
  )

  AppThemeId.NIGHT -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFF141222),
    surface = Color(0xFF1F1C33),
    surfaceSubtle = Color(0xFF282442),
    textPrimary = Color(0xFFEEECFA),
    textMuted = Color(0xFFA7A2C4),
    accent = Color(0xFF8B6CFF),
    accentSecondary = Color(0xFFC08BFF),
    divider = Color(0xFF322E4F),
    outline = Color(0xFF0D0B17),
    isDark = true,
  )

  AppThemeId.AMOLED -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFF000000),
    surface = Color(0xFF111114),
    surfaceSubtle = Color(0xFF1B1C22),
    textPrimary = Color(0xFFF4F4F6),
    textMuted = Color(0xFF9797A0),
    accent = Color(0xFF0D9488),
    accentSecondary = Color(0xFF3B82F6),
    divider = Color(0xFF26262C),
    outline = Color(0xFF3A3A44),
    isDark = true,
  )
}
