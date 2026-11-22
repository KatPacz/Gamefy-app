package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeId

object PixelColors {
  val Outline = Color(0xFF2B2140)
  val Green = Color(0xFF34C759)
  val GreenDark = Color(0xFF1F8A4C)
  val Gold = Color(0xFFFFC933)
  val Orange = Color(0xFFF59A23)
  val Amber = Color(0xFFD97706)
  val AmberLightBg = Color(0xFFFEF3C7)
  val AmberDarkBg = Color(0xFF3A280A)
  val AmberInkLight = Color(0xFF451A03)
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
  val isHighContrast: Boolean = false,
  val onAccent: Color = Color.White,
)

fun paletteForTheme(themeId: AppThemeId): GamefyPalette = when (themeId) {
  // --- 3 motywy jasne ---
  AppThemeId.VIOLET -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFFF3F1FB),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFECE8F9),
    textPrimary = Color(0xFF1D1B2E),
    textMuted = Color(0xFF4F4B66),
    accent = Color(0xFF6C4CF1),
    accentSecondary = Color(0xFFA06BFF),
    divider = Color(0xFFD6CFF0),
    outline = Color(0xFF2B2140),
    isDark = false,
  )

  AppThemeId.FOREST -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFFF0F6F1),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFE4EFE6),
    textPrimary = Color(0xFF15261B),
    textMuted = Color(0xFF3D5544),
    accent = Color(0xFF2A8A56),
    accentSecondary = Color(0xFF6FBF5F),
    divider = Color(0xFFC9DFCD),
    outline = Color(0xFF1E3224),
    isDark = false,
  )

  AppThemeId.ROSE -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFFFDF1F4),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFFAE4EA),
    textPrimary = Color(0xFF2B1520),
    textMuted = Color(0xFF61404E),
    accent = Color(0xFFD6336C),
    accentSecondary = Color(0xFFFF7A9C),
    divider = Color(0xFFECCBD7),
    outline = Color(0xFF3A1D2B),
    isDark = false,
  )

  // --- 3 motywy ciemne ---
  AppThemeId.NIGHT -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFF141222),
    surface = Color(0xFF1F1C33),
    surfaceSubtle = Color(0xFF282442),
    textPrimary = Color(0xFFEEECFA),
    textMuted = Color(0xFFB8B3D6),
    accent = Color(0xFF8B6CFF),
    accentSecondary = Color(0xFFC08BFF),
    divider = Color(0xFF3A355C),
    outline = Color(0xFF0D0B17),
    isDark = true,
  )

  AppThemeId.AMOLED -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFF000000),
    surface = Color(0xFF111114),
    surfaceSubtle = Color(0xFF1B1C22),
    textPrimary = Color(0xFFF4F4F6),
    textMuted = Color(0xFFB0B0BA),
    accent = Color(0xFF0D9488),
    accentSecondary = Color(0xFF3B82F6),
    divider = Color(0xFF31313A),
    outline = Color(0xFF474754),
    isDark = true,
  )

  AppThemeId.SAPPHIRE -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFF0A1324),
    surface = Color(0xFF122038),
    surfaceSubtle = Color(0xFF1B2E4E),
    textPrimary = Color(0xFFEEF5FF),
    textMuted = Color(0xFFB0C7E6),
    accent = Color(0xFF0284C7),
    accentSecondary = Color(0xFF38BDF8),
    divider = Color(0xFF2D4773),
    outline = Color(0xFF060C18),
    isDark = true,
  )

  // --- 2 motywy High Contrast (WCAG AAA) ---
  AppThemeId.HIGH_CONTRAST_DAY -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFEDEDED),
    textPrimary = Color(0xFF000000),
    textMuted = Color(0xFF1A1A1A),
    accent = Color(0xFF0026B3),
    accentSecondary = Color(0xFF7A0066),
    divider = Color(0xFF000000),
    outline = Color(0xFF000000),
    isDark = false,
    isHighContrast = true,
    onAccent = Color.White,
  )

  AppThemeId.HIGH_CONTRAST_NIGHT -> GamefyPalette(
    themeId = themeId,
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceSubtle = Color(0xFF1A1A1A),
    textPrimary = Color(0xFFFFFFFF),
    textMuted = Color(0xFFE6E6E6),
    accent = Color(0xFFFFD600),
    accentSecondary = Color(0xFF00E5FF),
    divider = Color(0xFFFFFFFF),
    outline = Color(0xFFFFFFFF),
    isDark = true,
    isHighContrast = true,
    onAccent = Color(0xFF000000),
  )
}
