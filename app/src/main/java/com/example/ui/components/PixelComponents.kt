package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AchievementDefinition
import com.example.data.model.LevelProgress
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily

@Composable
fun PixelCard(
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  containerColor: Color = LocalGamefyPalette.current.surface,
  borderColor: Color = LocalGamefyPalette.current.divider,
  content: @Composable ColumnScope.() -> Unit,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = containerColor,
    border = BorderStroke(if (palette.isHighContrast) 2.5.dp else 1.5.dp, borderColor),
    tonalElevation = if (palette.isHighContrast) 0.dp else 2.dp,
    shadowElevation = if (palette.isHighContrast) 0.dp else 2.dp,
  ) {
    Column(modifier = Modifier.padding(contentPadding), content = content)
  }
}

@Composable
fun PixelButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  containerColor: Color = LocalGamefyPalette.current.accent,
  contentColor: Color = if (containerColor == LocalGamefyPalette.current.accent) {
    LocalGamefyPalette.current.onAccent
  } else {
    Color.White
  },
  compact: Boolean = false,
  horizontalPadding: Dp = if (compact) 12.dp else 18.dp,
  fontSize: androidx.compose.ui.unit.TextUnit = if (compact) 12.sp else 14.sp,
  leadingIcon: (@Composable () -> Unit)? = null,
) {
  val palette = LocalGamefyPalette.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val pressOffset by animateDpAsState(
    targetValue = if (pressed && enabled) 2.dp else 0.dp,
    animationSpec = spring(),
    label = "pixelBtnOffset",
  )

  val shape = RoundedCornerShape(10.dp)
  val bg = if (enabled) containerColor else palette.surfaceSubtle
  val fg = if (enabled) contentColor else palette.textMuted
  val heightMin = if (compact) 40.dp else 48.dp

  Box(
    modifier = modifier
      .offset(y = pressOffset)
      .clip(shape)
      .background(bg)
      .border(2.5.dp, palette.outline, shape)
      .drawBehind {
        // Top inner specular highlight & bottom 3D ledge matching pixel-art button style
        drawRect(
          color = Color.White.copy(alpha = 0.25f),
          topLeft = Offset(0f, 0f),
          size = Size(size.width, 4.dp.toPx()),
        )
        if (!pressed) {
          drawRect(
            color = Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(0f, size.height - 4.dp.toPx()),
            size = Size(size.width, 4.dp.toPx()),
          )
        }
      }
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(),
        enabled = enabled,
        role = Role.Button,
        onClick = onClick,
      )
      .padding(
        horizontal = horizontalPadding,
        vertical = if (compact) 8.dp else 11.dp,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier = Modifier.heightIn(min = heightMin - if (compact) 16.dp else 22.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      if (leadingIcon != null) {
        leadingIcon()
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = text,
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun PixelLockedButton(
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val shape = RoundedCornerShape(12.dp)
  val bg = if (palette.isDark) Color(0xFF393B4A) else Color(0xFFD5D7E0)
  val borderColor = if (palette.isDark) Color(0xFF5D6076) else palette.outline.copy(alpha = 0.65f)

  Box(
    modifier = modifier
      .heightIn(min = 48.dp)
      .clip(shape)
      .background(bg)
      .border(2.5.dp, borderColor, shape)
      .drawBehind {
        // Retro pixel button top highlight & bottom 3D stone/iron ledge
        drawRect(
          color = Color.White.copy(alpha = if (palette.isDark) 0.12f else 0.42f),
          topLeft = Offset(0f, 0f),
          size = Size(size.width, 4.dp.toPx()),
        )
        drawRect(
          color = Color.Black.copy(alpha = if (palette.isDark) 0.35f else 0.18f),
          topLeft = Offset(0f, size.height - 4.dp.toPx()),
          size = Size(size.width, 4.dp.toPx()),
        )
      }
      .semantics(mergeDescendants = true) {
        this.contentDescription = contentDescription
      }
      .padding(horizontal = 12.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    PixelIcon(
      icon = "lock",
      size = 24.dp,
    )
  }
}

@Composable
fun PixelCheckbox(
  checked: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  boxSize: Dp = 26.dp,
  touchTargetSize: Dp = 48.dp,
) {
  val palette = LocalGamefyPalette.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val bgColor by androidx.compose.animation.animateColorAsState(
    targetValue = if (checked) PixelColors.Green else palette.surface,
    animationSpec = androidx.compose.animation.core.tween(durationMillis = 220),
    label = "checkboxBg",
  )
  val borderColor by androidx.compose.animation.animateColorAsState(
    targetValue = if (checked) PixelColors.GreenDark else palette.outline,
    animationSpec = androidx.compose.animation.core.tween(durationMillis = 220),
    label = "checkboxBorder",
  )
  val checkScale by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (checked) 1f else 0f,
    animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
    label = "checkboxScale",
  )
  val shape = RoundedCornerShape(7.dp)

  Box(
    modifier = modifier
      .size(touchTargetSize)
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = false, radius = touchTargetSize / 2),
        role = Role.Checkbox,
        onClick = onClick,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(boxSize)
        .offset(y = if (pressed) 1.dp else 0.dp)
        .clip(shape)
        .background(bgColor)
        .border(2.5.dp, borderColor, shape),
      contentAlignment = Alignment.Center,
    ) {
      if (checkScale > 0.05f) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Wykonane",
          tint = Color.White,
          modifier = Modifier
            .size(boxSize * 0.72f)
            .graphicsLayer {
              scaleX = checkScale
              scaleY = checkScale
            },
        )
      }
    }
  }
}

@Composable
fun PixelProgressBar(
  progress: Float,
  modifier: Modifier = Modifier,
  leadingIcon: String? = null,
  barHeight: Dp = 16.dp,
  fillStartColor: Color = LocalGamefyPalette.current.accentSecondary,
  fillEndColor: Color = LocalGamefyPalette.current.accent,
) {
  val palette = LocalGamefyPalette.current
  val clamped = progress.coerceIn(0f, 1f)

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (leadingIcon != null) {
      PixelIcon(icon = leadingIcon, size = 24.dp)
      Spacer(modifier = Modifier.width(6.dp))
    }
    Box(
      modifier = Modifier
        .weight(1f)
        .height(barHeight)
        .clip(RoundedCornerShape(5.dp))
        .background(Color.White)
        .border(2.5.dp, palette.outline, RoundedCornerShape(5.dp)),
    ) {
      if (clamped > 0f) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(clamped)
            .background(
              Brush.verticalGradient(listOf(fillStartColor, fillEndColor))
            )
            .drawBehind {
              // Subtle pixel segment vertical ticks
              val step = 10.dp.toPx()
              var x = step
              while (x < size.width) {
                drawLine(
                  color = Color.Black.copy(alpha = 0.18f),
                  start = Offset(x, 0f),
                  end = Offset(x, size.height),
                  strokeWidth = 1.5.dp.toPx(),
                )
                x += step
              }
            },
        )
      }
    }
  }
}

@Composable
fun TopHudHeader(
  levelProgress: LevelProgress,
  coins: Int,
  gems: Int,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Level square badge
    Box(
      modifier = Modifier
        .size(46.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(Brush.linearGradient(listOf(palette.accent, palette.accentSecondary)))
        .border(2.5.dp, palette.outline, RoundedCornerShape(10.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "${levelProgress.level}",
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Color.White,
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Poziom ${levelProgress.level} · ${levelProgress.currentExp}/${levelProgress.neededExp} EXP",
        style = MaterialTheme.typography.labelMedium,
        color = palette.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      PixelProgressBar(
        progress = levelProgress.fraction,
        leadingIcon = "star",
        barHeight = 16.dp,
      )
    }

    // Currency pill
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = palette.surface,
      border = BorderStroke(2.dp, palette.outline),
      tonalElevation = 2.dp,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          PixelIcon(icon = "coin", size = 18.dp)
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "$coins",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = palette.textPrimary,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          PixelIcon(icon = "gem", size = 18.dp)
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "$gems",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = palette.textPrimary,
          )
        }
      }
    }
  }
}

private val URL_REGEX = Regex("https?://[^\\s]+")

@Composable
fun LinkifiedText(
  text: String,
  modifier: Modifier = Modifier,
  maxLines: Int = Int.MAX_VALUE,
) {
  val palette = LocalGamefyPalette.current
  val uriHandler = LocalUriHandler.current
  val annotated = remember(text, palette.accent) {
    buildAnnotatedString {
      append(text)
      URL_REGEX.findAll(text).forEach { match ->
        addLink(
          url = LinkAnnotation.Url(
            url = match.value,
            styles = TextLinkStyles(
              style = SpanStyle(
                color = palette.accent,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.SemiBold,
              )
            ),
            linkInteractionListener = {
              runCatching { uriHandler.openUri(match.value) }
            },
          ),
          start = match.range.first,
          end = match.range.last + 1,
        )
      }
    }
  }

  Text(
    text = annotated,
    style = MaterialTheme.typography.bodyMedium,
    color = palette.textPrimary,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier,
  )
}

@Composable
fun AchievementPillBanner(
  achievement: AchievementDefinition,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pillShape = RoundedCornerShape(50)
  val rewardText = remember(achievement.id) { achievement.rewardSummary() }

  Surface(
    shape = pillShape,
    color = Color(0xFF1A172B),
    border = BorderStroke(2.dp, PixelColors.Gold),
    shadowElevation = 10.dp,
    tonalElevation = 6.dp,
    modifier = modifier
      .widthIn(max = 520.dp)
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(pillShape)
      .clickable(role = Role.Button, onClickLabel = "Zobacz odblokowane osiągnięcie", onClick = onClick)
      .testTag("achievement_pill_banner"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(PixelColors.Gold.copy(alpha = 0.2f))
          .border(1.5.dp, PixelColors.Gold, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        PixelIcon(icon = achievement.icon, size = 28.dp)
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (achievement.isSecret) {
            "Tajne osiągnięcie odblokowane!"
          } else {
            "Osiągnięcie odblokowane!"
          },
          fontFamily = PixelFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = PixelColors.Gold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = achievement.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (rewardText.isNotBlank()) {
          Text(
            text = rewardText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = PixelColors.Gold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.14f),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        ) {
          Text(
            text = "Zobacz",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color.White,
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
fun PixelSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  badgeText: String? = null,
  titleColor: Color = LocalGamefyPalette.current.textPrimary,
  badgeBgColor: Color = LocalGamefyPalette.current.surfaceSubtle,
  badgeTextColor: Color = LocalGamefyPalette.current.textMuted,
  leading: (@Composable () -> Unit)? = null,
  trailing: (@Composable () -> Unit)? = null,
) {
  val palette = LocalGamefyPalette.current
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (leading != null) {
          leading()
        }
        Text(
          text = title,
          fontFamily = PixelFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
      }
      if (badgeText != null || trailing != null) {
        Spacer(modifier = Modifier.width(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (badgeText != null) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = badgeBgColor,
            ) {
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeTextColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
          if (trailing != null) {
            trailing()
          }
        }
      }
    }
    if (!subtitle.isNullOrBlank()) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = palette.textMuted,
      )
    }
  }
}

@Composable
fun PixelEmptyStateCard(
  @DrawableRes illustrationRes: Int,
  illustrationContentDescription: String,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  primaryActionLabel: String? = null,
  onPrimaryAction: (() -> Unit)? = null,
  primaryActionTestTag: String? = null,
  secondaryActionLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  secondaryActionTestTag: String? = null,
  illustrationSize: Dp = 132.dp,
) {
  val palette = LocalGamefyPalette.current
  PixelCard(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surfaceSubtle,
        border = BorderStroke(2.5.dp, palette.outline),
        shadowElevation = 2.dp,
        modifier = Modifier.size(illustrationSize),
      ) {
        Image(
          painter = painterResource(id = illustrationRes),
          contentDescription = illustrationContentDescription,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = title,
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = palette.textPrimary,
        textAlign = TextAlign.Center,
      )

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = palette.textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 420.dp),
      )

      if (primaryActionLabel != null && onPrimaryAction != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PixelButton(
            text = primaryActionLabel,
            compact = false,
            modifier = Modifier
              .fillMaxWidth()
              .let { base -> if (primaryActionTestTag != null) base.testTag(primaryActionTestTag) else base },
            onClick = onPrimaryAction,
          )
          if (secondaryActionLabel != null && onSecondaryAction != null) {
            PixelButton(
              text = secondaryActionLabel,
              compact = false,
              containerColor = palette.surfaceSubtle,
              contentColor = palette.textPrimary,
              modifier = Modifier
                .fillMaxWidth()
                .let { base -> if (secondaryActionTestTag != null) base.testTag(secondaryActionTestTag) else base },
              onClick = onSecondaryAction,
            )
          }
        }
      }
    }
  }
}

data class ActiveFilterChipItem(
  val key: String,
  val label: String,
  val onRemove: () -> Unit,
  val testTag: String,
)

@Composable
fun SearchFilterIconButton(
  activeCount: Int,
  isOpen: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  contentDescription: String = "Szukaj i filtruj",
) {
  val palette = LocalGamefyPalette.current
  val highlighted = isOpen || activeCount > 0
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (highlighted) palette.accent.copy(alpha = 0.16f) else palette.surface,
    border = BorderStroke(2.dp, if (highlighted) palette.accent else palette.outline),
    modifier = modifier
      .heightIn(min = 48.dp)
      .widthIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(
        role = Role.Button,
        onClickLabel = contentDescription,
        onClick = onClick,
      )
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = if (activeCount > 0) 10.dp else 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = contentDescription,
        tint = if (highlighted) palette.accent else palette.textPrimary,
        modifier = Modifier.size(22.dp),
      )
      if (activeCount > 0) {
        Surface(
          shape = CircleShape,
          color = palette.accent,
        ) {
          Text(
            text = "$activeCount",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = palette.onAccent,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFilterChipsRow(
  chips: List<ActiveFilterChipItem>,
  onClearAll: () -> Unit,
  modifier: Modifier = Modifier,
  clearButtonTestTag: String = "clear_all_filters_button",
) {
  if (chips.isEmpty()) return
  val palette = LocalGamefyPalette.current
  FlowRow(
    modifier = modifier
      .fillMaxWidth()
      .widthIn(max = 640.dp)
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    chips.forEach { chip ->
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = palette.accent.copy(alpha = 0.14f),
        border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.55f)),
        modifier = Modifier
          .heightIn(min = 38.dp)
          .clip(RoundedCornerShape(999.dp))
          .clickable(
            role = Role.Button,
            onClickLabel = "Usuń filtr ${chip.label}",
            onClick = chip.onRemove,
          )
          .testTag(chip.testTag),
      ) {
        Row(
          modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = chip.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Usuń filtr ${chip.label}",
            tint = palette.accent,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }

    TextButton(
      onClick = onClearAll,
      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
      modifier = Modifier
        .heightIn(min = 38.dp)
        .testTag(clearButtonTestTag),
    ) {
      Text(
        text = "Wyczyść filtry",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = PixelColors.Red,
      )
    }
  }
}


