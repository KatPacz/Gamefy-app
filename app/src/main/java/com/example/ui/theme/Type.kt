package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppFontFamilyOption
import com.example.data.model.AppWordSpacing

val DefaultPixelFontFamily = FontFamily(
  Font(R.font.jersey_10, FontWeight.Normal),
  Font(R.font.jersey_10, FontWeight.Medium),
  Font(R.font.jersey_10, FontWeight.SemiBold),
  Font(R.font.jersey_10, FontWeight.Bold),
)

val DefaultBodyFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

// Lexend: open-source Google Font designed specifically for reading fluency & dyslexia accessibility
val LexendFontFamily = FontFamily(
  Font(R.font.lexend, FontWeight.Normal),
  Font(R.font.lexend, FontWeight.Medium),
  Font(R.font.lexend, FontWeight.SemiBold),
  Font(R.font.lexend, FontWeight.Bold),
)

// Arimo: open-source metric-compatible neo-grotesque equivalent of Helvetica
val ArimoFontFamily = FontFamily(
  Font(R.font.arimo, FontWeight.Normal),
  Font(R.font.arimo, FontWeight.Medium),
  Font(R.font.arimo, FontWeight.SemiBold),
  Font(R.font.arimo, FontWeight.Bold),
)

// Atkinson Hyperlegible: open-source Braille Institute font for maximum character legibility
val AtkinsonFontFamily = FontFamily(
  Font(R.font.atkinson_hyperlegible, FontWeight.Normal),
  Font(R.font.atkinson_hyperlegible, FontWeight.Medium),
  Font(R.font.atkinson_hyperlegible, FontWeight.SemiBold),
  Font(R.font.atkinson_hyperlegible, FontWeight.Bold),
)

fun displayFontFamilyFor(option: AppFontFamilyOption): FontFamily = when (option) {
  AppFontFamilyOption.DEFAULT -> DefaultPixelFontFamily
  AppFontFamilyOption.OPEN_DYSLEXIC -> LexendFontFamily
  AppFontFamilyOption.HELVETICA -> ArimoFontFamily
  AppFontFamilyOption.ATKINSON -> AtkinsonFontFamily
}

fun bodyFontFamilyFor(option: AppFontFamilyOption): FontFamily = when (option) {
  AppFontFamilyOption.DEFAULT -> DefaultBodyFontFamily
  AppFontFamilyOption.OPEN_DYSLEXIC -> LexendFontFamily
  AppFontFamilyOption.HELVETICA -> ArimoFontFamily
  AppFontFamilyOption.ATKINSON -> AtkinsonFontFamily
}

val LocalDisplayFontFamily = staticCompositionLocalOf { DefaultPixelFontFamily }
val LocalBodyFontFamily = staticCompositionLocalOf { DefaultBodyFontFamily }

val PixelFontFamily: FontFamily
  @Composable
  get() = LocalDisplayFontFamily.current

val BodyFontFamily: FontFamily
  @Composable
  get() = LocalBodyFontFamily.current

fun buildGamefyTypography(
  fontOption: AppFontFamilyOption = AppFontFamilyOption.DEFAULT,
  wordSpacing: AppWordSpacing = AppWordSpacing.STANDARD,
): Typography {
  val display = displayFontFamilyFor(fontOption)
  val body = bodyFontFamilyFor(fontOption)
  val extraLs = wordSpacing.extraLetterSpacingSp
  val lhMult = wordSpacing.lineHeightMultiplier
  val geo = if (wordSpacing == AppWordSpacing.WIDER) {
    TextGeometricTransform(scaleX = wordSpacing.horizontalScaleX)
  } else {
    null
  }

  fun style(
    family: FontFamily,
    weight: FontWeight,
    sizeSp: Float,
    lineHeightSp: Float,
    baseLetterSpacingSp: Float = 0f,
    isDisplay: Boolean = false,
  ): TextStyle {
    val appliedLs = baseLetterSpacingSp + if (isDisplay) extraLs * 0.7f else extraLs
    return TextStyle(
      fontFamily = family,
      fontWeight = weight,
      fontSize = sizeSp.sp,
      lineHeight = (lineHeightSp * lhMult).sp,
      letterSpacing = appliedLs.sp,
      textGeometricTransform = geo,
    )
  }

  return Typography(
    displayLarge = style(display, FontWeight.Bold, 42f, 48f, isDisplay = true),
    headlineMedium = style(display, FontWeight.Bold, 24f, 30f, isDisplay = true),
    titleLarge = style(display, FontWeight.Bold, 20f, 26f, isDisplay = true),
    titleMedium = style(display, FontWeight.Bold, 16f, 22f, isDisplay = true),
    titleSmall = style(display, FontWeight.Bold, 14f, 18f, baseLetterSpacingSp = 0.5f, isDisplay = true),
    bodyLarge = style(body, FontWeight.Normal, 15f, 22f),
    bodyMedium = style(body, FontWeight.Normal, 14f, 20f),
    bodySmall = style(body, FontWeight.Normal, 12f, 17f),
    labelLarge = style(display, FontWeight.Bold, 14f, 18f, baseLetterSpacingSp = 0.4f, isDisplay = true),
    labelMedium = style(display, FontWeight.Bold, 12.5f, 16f, baseLetterSpacingSp = 0.35f, isDisplay = true),
    labelSmall = style(body, FontWeight.Medium, 12f, 16f),
  )
}

val GamefyTypography = buildGamefyTypography()
