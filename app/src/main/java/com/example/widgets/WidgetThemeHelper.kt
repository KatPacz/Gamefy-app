package com.example.widgets

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.AppThemeId
import com.example.data.model.resolveEffectiveThemeId
import com.example.ui.theme.PixelColors
import com.example.ui.theme.paletteForTheme

data class WidgetColors(
  val themeId: AppThemeId,
  val background: Int,
  val surface: Int,
  val surfaceSubtle: Int,
  val textPrimary: Int,
  val textMuted: Int,
  val accent: Int,
  val onAccent: Int,
  val divider: Int,
  val outline: Int,
  val success: Int,
  val danger: Int,
  val isDark: Boolean,
  val isHighContrast: Boolean,
)

object WidgetThemeHelper {
  fun isSystemDarkMode(context: Context): Boolean {
    val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightMask == Configuration.UI_MODE_NIGHT_YES
  }

  fun resolveColors(themeCode: String, isSystemDark: Boolean): WidgetColors {
    val themeId = resolveEffectiveThemeId(themeCode, isSystemDark)
    val palette = paletteForTheme(themeId)
    return WidgetColors(
      themeId = themeId,
      background = palette.background.toArgb(),
      surface = palette.surface.toArgb(),
      surfaceSubtle = palette.surfaceSubtle.toArgb(),
      textPrimary = palette.textPrimary.toArgb(),
      textMuted = palette.textMuted.toArgb(),
      accent = palette.accent.toArgb(),
      onAccent = palette.onAccent.toArgb(),
      divider = palette.divider.toArgb(),
      outline = palette.outline.toArgb(),
      success = if (palette.isHighContrast) palette.accent.toArgb() else PixelColors.GreenDark.toArgb(),
      danger = PixelColors.Red.toArgb(),
      isDark = palette.isDark,
      isHighContrast = palette.isHighContrast,
    )
  }

  fun resolveColors(context: Context, themeCode: String): WidgetColors =
    resolveColors(themeCode = themeCode, isSystemDark = isSystemDarkMode(context))
}
