package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DEFAULT_FOLDER_COLOR_PALETTE
import com.example.data.local.FolderEntity
import com.example.data.local.normalizeColorHex
import com.example.data.local.resolvedColorHex
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelFontFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

fun parseHexColor(hex: String, fallback: Color = Color(0xFF3B82F6)): Color {
  val normalized = normalizeColorHex(hex)
  if (normalized.isEmpty()) return fallback
  val rgb = normalized.substring(1).toLongOrNull(16) ?: return fallback
  return Color(0xFF000000L or rgb)
}

fun resolveFolderColor(folder: FolderEntity): Color =
  parseHexColor(folder.resolvedColorHex())

fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
  val h = ((hue % 360f) + 360f) % 360f
  val s = saturation.coerceIn(0f, 1f)
  val v = value.coerceIn(0f, 1f)
  val c = v * s
  val x = c * (1f - abs((h / 60f) % 2f - 1f))
  val m = v - c
  val (r1, g1, b1) = when {
    h < 60f -> Triple(c, x, 0f)
    h < 120f -> Triple(x, c, 0f)
    h < 180f -> Triple(0f, c, x)
    h < 240f -> Triple(0f, x, c)
    h < 300f -> Triple(x, 0f, c)
    else -> Triple(c, 0f, x)
  }
  return Color(
    red = (r1 + m).coerceIn(0f, 1f),
    green = (g1 + m).coerceIn(0f, 1f),
    blue = (b1 + m).coerceIn(0f, 1f),
    alpha = 1f,
  )
}

fun colorToHsv(color: Color): Triple<Float, Float, Float> {
  val r = color.red
  val g = color.green
  val b = color.blue
  val maxVal = max(r, max(g, b))
  val minVal = min(r, min(g, b))
  val delta = maxVal - minVal

  val hue = when {
    delta <= 0.0001f -> 0f
    maxVal == r -> 60f * (((g - b) / delta) % 6f)
    maxVal == g -> 60f * (((b - r) / delta) + 2f)
    else -> 60f * (((r - g) / delta) + 4f)
  }.let { if (it < 0f) it + 360f else it }

  val sat = if (maxVal <= 0.0001f) 0f else delta / maxVal
  return Triple(hue, sat.coerceIn(0f, 1f), maxVal.coerceIn(0f, 1f))
}

fun colorToHexString(color: Color): String {
  val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
  val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
  val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)
  return String.format("#%02X%02X%02X", r, g, b)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorWheelPickerDialog(
  initialColorHex: String,
  previewName: String,
  previewIcon: String,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val initialHsv = remember(initialColorHex) {
    colorToHsv(parseHexColor(initialColorHex))
  }
  var hue by remember(initialColorHex) { mutableFloatStateOf(initialHsv.first) }
  var saturation by remember(initialColorHex) { mutableFloatStateOf(initialHsv.second) }
  var brightness by remember(initialColorHex) { mutableFloatStateOf(initialHsv.third.coerceIn(0.30f, 1f)) }

  val selectedColor = hsvToColor(hue, saturation, brightness)
  val selectedHex = colorToHexString(selectedColor)
  val fullBrightColor = hsvToColor(hue, saturation, 1f)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Kolor kategorii",
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Live task-frame preview in chosen category color
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = palette.surface,
          border = BorderStroke(2.5.dp, selectedColor),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(selectedColor)
                .border(1.5.dp, palette.outline, CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                PixelIcon(icon = previewIcon.ifBlank { "📁" }, size = 15.dp)
                Text(
                  text = previewName.ifBlank { "Nowa kategoria" },
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = "Podgląd ramki zadania · $selectedHex",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
              )
            }
          }
        }

        // Interactive HSV Color Wheel
        Box(
          modifier = Modifier
            .size(204.dp)
            .semantics { contentDescription = "Koło kolorów kategorii" }
            .testTag("category_color_wheel"),
          contentAlignment = Alignment.Center,
        ) {
          val sweepColors = remember {
            listOf(
              Color.Red,
              Color.Yellow,
              Color.Green,
              Color.Cyan,
              Color.Blue,
              Color.Magenta,
              Color.Red,
            )
          }

          fun updateFromOffset(offset: Offset, width: Float, height: Float) {
            val radius = (min(width, height) / 2f) - 10f
            if (radius <= 1f) return
            val cx = width / 2f
            val cy = height / 2f
            val dx = offset.x - cx
            val dy = offset.y - cy
            val dist = hypot(dx, dy)
            val newSat = (dist / radius).coerceIn(0f, 1f)
            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val newHue = if (angleDeg < 0f) angleDeg + 360f else angleDeg
            hue = newHue
            saturation = newSat
          }

          Canvas(
            modifier = Modifier
              .size(204.dp)
              .pointerInput(Unit) {
                detectTapGestures { offset ->
                  updateFromOffset(offset, size.width.toFloat(), size.height.toFloat())
                }
              }
              .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                  change.consume()
                  updateFromOffset(change.position, size.width.toFloat(), size.height.toFloat())
                }
              },
          ) {
            val radius = (min(size.width, size.height) / 2f) - 10.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
              brush = Brush.sweepGradient(colors = sweepColors, center = center),
              radius = radius,
              center = center,
            )
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius,
              ),
              radius = radius,
              center = center,
            )
            if (brightness < 0.995f) {
              drawCircle(
                color = Color.Black.copy(alpha = (1f - brightness).coerceIn(0f, 0.72f)),
                radius = radius,
                center = center,
              )
            }
            drawCircle(
              color = palette.outline,
              radius = radius,
              center = center,
              style = Stroke(width = 2.dp.toPx()),
            )

            // Thumb marker at current (hue, saturation)
            val rad = hue * (PI / 180.0)
            val thumbOffset = Offset(
              x = center.x + (cos(rad) * saturation * radius).toFloat(),
              y = center.y + (sin(rad) * saturation * radius).toFloat(),
            )
            drawCircle(
              color = Color.White,
              radius = 11.dp.toPx(),
              center = thumbOffset,
            )
            drawCircle(
              color = selectedColor,
              radius = 8.dp.toPx(),
              center = thumbOffset,
            )
            drawCircle(
              color = palette.outline,
              radius = 11.dp.toPx(),
              center = thumbOffset,
              style = Stroke(width = 2.dp.toPx()),
            )
          }
        }

        // Brightness / Value slider
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Jasność koloru",
              style = MaterialTheme.typography.labelMedium,
              color = palette.textMuted,
            )
            Text(
              text = "${(brightness * 100f).roundToInt()}%",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
          }
          Slider(
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = 0.30f..1f,
            colors = SliderDefaults.colors(
              thumbColor = selectedColor,
              activeTrackColor = fullBrightColor,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("color_wheel_brightness_slider"),
          )
        }

        // Quick preset swatches (48dp touch targets)
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "Szybkie kolory",
            style = MaterialTheme.typography.labelMedium,
            color = palette.textMuted,
          )
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            DEFAULT_FOLDER_COLOR_PALETTE.forEach { presetHex ->
              val presetColor = parseHexColor(presetHex)
              val isSelected = selectedHex.equals(presetHex, ignoreCase = true)
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .clickable {
                    val (h, s, v) = colorToHsv(presetColor)
                    hue = h
                    saturation = s
                    brightness = v.coerceIn(0.30f, 1f)
                  }
                  .testTag("color_preset_${presetHex.removePrefix("#")}"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(presetColor)
                    .border(
                      width = if (isSelected) 2.5.dp else 1.5.dp,
                      color = if (isSelected) palette.outline else palette.divider,
                      shape = CircleShape,
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Wybrany kolor $presetHex",
                      tint = if (presetColor.luminance() > 0.55f) Color.Black else Color.White,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      PixelButton(
        text = "Zastosuj",
        compact = true,
        containerColor = selectedColor,
        contentColor = if (selectedColor.luminance() > 0.55f) Color.Black else Color.White,
        modifier = Modifier.testTag("color_wheel_confirm_button"),
        onClick = { onConfirm(selectedHex) },
      )
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("color_wheel_cancel_button"),
      ) {
        Text("Anuluj")
      }
    },
  )
}
