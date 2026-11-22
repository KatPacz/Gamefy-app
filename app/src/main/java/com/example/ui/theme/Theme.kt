package com.example.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.data.model.AppFontFamilyOption
import com.example.data.model.AppFontSize
import com.example.data.model.AppThemeId
import com.example.data.model.AppWordSpacing

val LocalGamefyPalette = staticCompositionLocalOf { paletteForTheme(AppThemeId.VIOLET) }

const val BASE_FONT_SCALE_MULTIPLIER = 1.15f

@Composable
fun GamefyTheme(
  themeId: AppThemeId,
  fontSize: AppFontSize = AppFontSize.DEFAULT,
  fontFamilyOption: AppFontFamilyOption = AppFontFamilyOption.DEFAULT,
  wordSpacing: AppWordSpacing = AppWordSpacing.STANDARD,
  content: @Composable () -> Unit,
) {
  val palette = remember(themeId) { paletteForTheme(themeId) }
  val typography = remember(fontFamilyOption, wordSpacing) {
    buildGamefyTypography(fontOption = fontFamilyOption, wordSpacing = wordSpacing)
  }
  val displayFamily = remember(fontFamilyOption) { displayFontFamilyFor(fontFamilyOption) }
  val bodyFamily = remember(fontFamilyOption) { bodyFontFamilyFor(fontFamilyOption) }
  val baseDensity = LocalDensity.current
  val scaledDensity = remember(baseDensity, fontSize) {
    Density(
      density = baseDensity.density,
      fontScale = (baseDensity.fontScale * BASE_FONT_SCALE_MULTIPLIER * fontSize.scale)
        .coerceIn(0.85f, 2.5f),
    )
  }

  val colorScheme = if (palette.isDark) {
    darkColorScheme(
      primary = palette.accent,
      onPrimary = palette.onAccent,
      secondary = palette.accentSecondary,
      onSecondary = palette.onAccent,
      background = palette.background,
      onBackground = palette.textPrimary,
      surface = palette.surface,
      onSurface = palette.textPrimary,
      surfaceVariant = palette.surfaceSubtle,
      onSurfaceVariant = palette.textMuted,
      outline = palette.divider,
      error = PixelColors.Red,
      onError = Color.White,
    )
  } else {
    lightColorScheme(
      primary = palette.accent,
      onPrimary = palette.onAccent,
      secondary = palette.accentSecondary,
      onSecondary = palette.onAccent,
      background = palette.background,
      onBackground = palette.textPrimary,
      surface = palette.surface,
      onSurface = palette.textPrimary,
      surfaceVariant = palette.surfaceSubtle,
      onSurfaceVariant = palette.textMuted,
      outline = palette.divider,
      error = PixelColors.Red,
      onError = Color.White,
    )
  }

  CompositionLocalProvider(
    LocalDensity provides scaledDensity,
    LocalGamefyPalette provides palette,
    LocalDisplayFontFamily provides displayFamily,
    LocalBodyFontFamily provides bodyFamily,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
    ) {
      CompositionLocalProvider(
        LocalTextStyle provides typography.bodyMedium,
        content = content,
      )
    }
  }
}
