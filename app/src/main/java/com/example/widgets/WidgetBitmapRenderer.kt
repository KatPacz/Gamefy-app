package com.example.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ui.components.PIXEL_PALETTE_ARGB
import com.example.ui.components.PIXEL_SPRITES
import com.example.ui.components.resolveSpriteKey
import kotlin.math.ceil
import kotlin.math.max

object WidgetBitmapRenderer {
  private fun loadPixelTypeface(context: Context): Typeface {
    return runCatching {
      ResourcesCompat.getFont(context, R.font.jersey_10)
    }.getOrNull() ?: Typeface.DEFAULT_BOLD
  }

  fun renderPixelHeaderBitmap(
    context: Context,
    text: String,
    textSizeSp: Float,
    colorArgb: Int,
    maxWidthDp: Int = 240,
  ): Bitmap {
    val safeText = text.ifBlank { " " }
    val metrics = context.resources.displayMetrics
    val textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSizeSp, metrics)
    val maxWidthPx = TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      maxWidthDp.toFloat(),
      metrics,
    ).toInt().coerceAtLeast(32)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = loadPixelTypeface(context)
      color = colorArgb
      textSize = textSizePx
      isSubpixelText = true
    }

    var measuredWidth = paint.measureText(safeText)
    if (measuredWidth > maxWidthPx && measuredWidth > 1f) {
      val scale = (maxWidthPx / measuredWidth).coerceIn(0.65f, 1f)
      paint.textSize = textSizePx * scale
      measuredWidth = paint.measureText(safeText)
    }

    val fm = paint.fontMetrics
    val bmpWidth = ceil(measuredWidth + 4f).toInt().coerceIn(8, maxWidthPx + 8)
    val bmpHeight = ceil((fm.descent - fm.ascent) + 4f).toInt().coerceAtLeast(16)

    val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawText(safeText, 2f, -fm.ascent + 2f, paint)
    return bitmap
  }

  fun renderPixelSpriteBitmap(
    context: Context,
    icon: String,
    sizeDp: Int = 24,
    monochromeTintArgb: Int? = null,
    grayscale: Boolean = false,
  ): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = max(24, (sizeDp * density).toInt())
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val spriteKey = resolveSpriteKey(icon) ?: "star"
    val sprite = PIXEL_SPRITES[spriteKey] ?: PIXEL_SPRITES["star"]!!
    val rows = sprite.split('|')
    val offsetY = (12 - rows.size) / 2
    val cellW = sizePx / 12f
    val cellH = sizePx / 12f
    val paint = Paint().apply {
      isAntiAlias = false
      isFilterBitmap = false
      style = Paint.Style.FILL
    }

    rows.forEachIndexed { y, row ->
      row.forEachIndexed { x, ch ->
        val baseColor = PIXEL_PALETTE_ARGB[ch] ?: return@forEachIndexed
        val resolved = when {
          monochromeTintArgb != null -> when (ch) {
            'k' -> monochromeTintArgb
            'w' -> Color.TRANSPARENT
            else -> Color.argb(
              72,
              Color.red(monochromeTintArgb),
              Color.green(monochromeTintArgb),
              Color.blue(monochromeTintArgb),
            )
          }
          grayscale -> {
            val luma = (0.299f * Color.red(baseColor) +
              0.587f * Color.green(baseColor) +
              0.114f * Color.blue(baseColor)).toInt().coerceIn(0, 255)
            Color.argb(Color.alpha(baseColor), luma, luma, luma)
          }
          else -> baseColor
        }
        if (Color.alpha(resolved) > 0) {
          paint.color = resolved
          val left = x * cellW
          val top = (y + offsetY) * cellH
          canvas.drawRect(left, top, left + cellW + 0.5f, top + cellH + 0.5f, paint)
        }
      }
    }
    return bitmap
  }

  fun renderRoundedPanelBitmap(
    context: Context,
    widthDp: Int,
    heightDp: Int,
    fillArgb: Int,
    borderArgb: Int,
    borderWidthDp: Float = 2f,
    cornerRadiusDp: Float = 14f,
  ): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = max(32, (widthDp * density).toInt())
    val h = max(32, (heightDp * density).toInt())
    val strokePx = (borderWidthDp * density).coerceAtLeast(1f)
    val radiusPx = (cornerRadiusDp * density).coerceAtLeast(2f)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val halfStroke = strokePx / 2f
    val rect = RectF(halfStroke, halfStroke, w - halfStroke, h - halfStroke)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = fillArgb
    }
    canvas.drawRoundRect(rect, radiusPx, radiusPx, fillPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = strokePx
      color = borderArgb
    }
    canvas.drawRoundRect(rect, radiusPx, radiusPx, borderPaint)
    return bitmap
  }
}
