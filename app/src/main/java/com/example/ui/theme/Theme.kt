package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeId

val LocalGamefyPalette = staticCompositionLocalOf { paletteForTheme(AppThemeId.VIOLET) }

@Composable
fun GamefyTheme(
  themeId: AppThemeId,
  content: @Composable () -> Unit,
) {
  val palette = paletteForTheme(themeId)
  val colorScheme = if (palette.isDark) {
    darkColorScheme(
      primary = palette.accent,
      onPrimary = Color.White,
      secondary = palette.accentSecondary,
      onSecondary = Color.White,
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
      onPrimary = Color.White,
      secondary = palette.accentSecondary,
      onSecondary = Color.White,
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

  CompositionLocalProvider(LocalGamefyPalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = GamefyTypography,
      content = content,
    )
  }
}
