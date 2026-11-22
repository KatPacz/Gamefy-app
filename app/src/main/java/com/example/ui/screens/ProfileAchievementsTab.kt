package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AchievementCategory
import com.example.data.model.AchievementGroupProgress
import com.example.data.model.AchievementTier
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelEmptyStateCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.PixelProgressBar
import com.example.ui.components.PixelSectionHeader
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily

enum class AchievementSortMode(val label: String, val tag: String) {
  CLOSEST("Najbliższe odblokowania", "ach_sort_closest"),
  CATALOG("Kategorie", "ach_sort_catalog"),
  UNLOCKED("Zdobyte", "ach_sort_unlocked"),
  LOCKED("Do zdobycia", "ach_sort_locked"),
}

fun filterAndSortAchievementGroups(
  allProgress: List<AchievementGroupProgress>,
  selectedCategory: AchievementCategory?,
  sortMode: AchievementSortMode,
): List<AchievementGroupProgress> {
  val byCategory = if (selectedCategory == null) {
    allProgress
  } else {
    allProgress.filter { it.group.category == selectedCategory }
  }

  val byStatus = when (sortMode) {
    AchievementSortMode.UNLOCKED -> byCategory.filter { it.hasAnyUnlocked }
    AchievementSortMode.LOCKED -> byCategory.filter { !it.isFullyUnlocked }
    AchievementSortMode.CLOSEST, AchievementSortMode.CATALOG -> byCategory
  }

  return when (sortMode) {
    AchievementSortMode.CLOSEST, AchievementSortMode.LOCKED -> {
      byStatus.sortedWith(
        compareBy<AchievementGroupProgress> {
          when {
            it.isFullyUnlocked -> 2
            it.group.isSecret && !it.hasAnyUnlocked -> 1
            else -> 0
          }
        }.thenByDescending {
          if (it.group.isSecret && !it.hasAnyUnlocked) -1f else it.progressFraction
        }.thenBy { it.group.category.ordinal }
      )
    }
    AchievementSortMode.UNLOCKED -> {
      byStatus.sortedWith(
        compareByDescending<AchievementGroupProgress> { it.isFullyUnlocked }
          .thenByDescending { it.unlockedTierIds.size }
          .thenBy { it.group.category.ordinal }
      )
    }
    AchievementSortMode.CATALOG -> byStatus
  }
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.profileAchievementsItems(
  unlockedCount: Int,
  totalCount: Int,
  currentStreak: Int,
  bestStreak: Int,
  streakFreezeAvailable: Boolean,
  selectedCategory: AchievementCategory?,
  onSelectCategory: (AchievementCategory?) -> Unit,
  sortMode: AchievementSortMode,
  onSelectSortMode: (AchievementSortMode) -> Unit,
  visibleGroups: List<AchievementGroupProgress>,
) {
  item(key = "achievements_summary") {
    AchievementsSummaryCard(
      unlockedCount = unlockedCount,
      totalCount = totalCount,
      currentStreak = currentStreak,
      bestStreak = bestStreak,
      streakFreezeAvailable = streakFreezeAvailable,
    )
  }

  item(key = "achievements_category_chips") {
    AchievementCategoryChipsRow(
      selectedCategory = selectedCategory,
      onSelectCategory = onSelectCategory,
    )
  }

  item(key = "achievements_sort_chips") {
    AchievementSortRow(
      sortMode = sortMode,
      onSelectSortMode = onSelectSortMode,
    )
  }

  if (visibleGroups.isEmpty()) {
    item(key = "achievements_empty") {
      PixelEmptyStateCard(
        illustrationRes = R.drawable.img_empty_chest,
        illustrationContentDescription = "Ilustracja pustej listy osiągnięć",
        title = "Brak pasujących osiągnięć",
        subtitle = "W wybranym filtrze nie ma jeszcze żadnych osiągnięć.",
        primaryActionLabel = if (selectedCategory != null || sortMode != AchievementSortMode.CLOSEST) {
          "Pokaż wszystkie"
        } else {
          null
        },
        onPrimaryAction = if (selectedCategory != null || sortMode != AchievementSortMode.CLOSEST) {
          {
            onSelectCategory(null)
            onSelectSortMode(AchievementSortMode.CLOSEST)
          }
        } else {
          null
        },
        primaryActionTestTag = "empty_achievements_reset_filter_button",
      )
    }
  } else {
    items(visibleGroups, key = { "ach_group_${it.group.id}" }) { progress ->
      TieredAchievementCard(progress = progress)
    }
  }
}

@Composable
private fun AchievementsSummaryCard(
  unlockedCount: Int,
  totalCount: Int,
  currentStreak: Int,
  bestStreak: Int,
  streakFreezeAvailable: Boolean,
) {
  val palette = LocalGamefyPalette.current
  val fraction = if (totalCount <= 0) 0f else (unlockedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

  PixelCard {
    PixelSectionHeader(
      title = "Osiągnięcia",
      badgeText = "$unlockedCount z $totalCount",
      modifier = Modifier.testTag("achievements_section_header"),
    )
    Spacer(modifier = Modifier.height(6.dp))
    PixelProgressBar(
      progress = fraction,
      leadingIcon = "trophy",
      barHeight = 16.dp,
      fillStartColor = PixelColors.Gold,
      fillEndColor = PixelColors.Orange,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = palette.surfaceSubtle,
      border = BorderStroke(1.dp, palette.divider),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("achievements_streak_freeze_card"),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PixelIcon(icon = "flame", size = 16.dp)
          Text(
            text = "Seria: $currentStreak dni ·",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          PixelIcon(icon = "trophy", size = 16.dp)
          Text(
            text = "Rekord: $bestStreak dni",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PixelIcon(
            icon = "freeze",
            size = 16.dp,
            grayscale = !streakFreezeAvailable,
          )
          Text(
            text = if (streakFreezeAvailable) {
              "Zamrożenie serii: gotowe (1 dzień wolny w tyg.)"
            } else {
              "Zamrożenie serii: wykorzystane w tym tygodniu"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (streakFreezeAvailable) palette.accent else palette.textMuted,
          )
        }
        Text(
          text = "Jeden dzień odpoczynku w tygodniu nie zeruje serii. Każde trofeum daje bonus EXP, monety lub kryształy.",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textMuted,
        )
      }
    }
  }
}

@Composable
private fun AchievementCategoryChipsRow(
  selectedCategory: AchievementCategory?,
  onSelectCategory: (AchievementCategory?) -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val categories: List<AchievementCategory?> = listOf(null) + AchievementCategory.entries

  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(vertical = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("achievements_category_row"),
  ) {
    items(categories, key = { it?.code ?: "ALL" }) { category ->
      val selected = selectedCategory == category
      val label = category?.label ?: "Wszystkie"
      Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) palette.accent else palette.surface,
        border = BorderStroke(1.5.dp, if (selected) palette.outline else palette.divider),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(50))
          .clickable(role = Role.Tab) { onSelectCategory(category) }
          .testTag("ach_category_${category?.code ?: "ALL"}"),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) palette.onAccent else palette.textPrimary,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AchievementSortRow(
  sortMode: AchievementSortMode,
  onSelectSortMode: (AchievementSortMode) -> Unit,
) {
  val palette = LocalGamefyPalette.current
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "Sortowanie i widok",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = palette.textMuted,
      modifier = Modifier.padding(start = 2.dp),
    )
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AchievementSortMode.entries.forEach { mode ->
        val selected = sortMode == mode
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.surfaceSubtle,
          border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) palette.accent else palette.divider,
          ),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Tab) { onSelectSortMode(mode) }
            .testTag(mode.tag),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          ) {
            Text(
              text = mode.label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
              color = if (selected) palette.accent else palette.textPrimary,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TieredAchievementCard(
  progress: AchievementGroupProgress,
) {
  val palette = LocalGamefyPalette.current
  val group = progress.group
  val isHiddenSecret = group.isSecret && !progress.hasAnyUnlocked
  val isCompleted = progress.isFullyUnlocked

  val accentBorder = when {
    isCompleted -> PixelColors.Gold
    progress.hasAnyUnlocked -> palette.accent
    else -> palette.divider
  }
  val cardBg = when {
    isCompleted -> PixelColors.Gold.copy(alpha = if (palette.isDark) 0.16f else 0.12f)
    progress.hasAnyUnlocked -> palette.accent.copy(alpha = if (palette.isDark) 0.10f else 0.06f)
    else -> palette.surface
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = cardBg,
    border = BorderStroke(1.5.dp, accentBorder),
    modifier = Modifier
      .fillMaxWidth()
      .alpha(if (progress.hasAnyUnlocked || !isHiddenSecret) 1f else 0.85f)
      .testTag("achievement_group_${group.id}"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (isHiddenSecret) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(palette.surfaceSubtle)
              .border(1.5.dp, palette.divider, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "?",
              fontFamily = PixelFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp,
              color = palette.textMuted,
            )
          }
        } else {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(palette.surfaceSubtle)
              .border(1.dp, palette.divider, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
          ) {
            PixelIcon(
              icon = group.icon,
              size = 30.dp,
              grayscale = !progress.hasAnyUnlocked,
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = if (isHiddenSecret) "Tajne osiągnięcie" else group.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )

            if (group.isSecret) {
              SecretStatusChip(unlocked = progress.hasAnyUnlocked)
            } else {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = palette.surfaceSubtle,
                border = BorderStroke(1.dp, palette.divider),
              ) {
                Text(
                  text = group.category.label,
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = palette.textMuted,
                  maxLines = 1,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }

          val subtitleText = when {
            isHiddenSecret -> {
              val hint = group.secretHint.ifBlank { "Odkryj podczas korzystania z aplikacji." }
              "Podpowiedź: $hint"
            }
            progress.nextLockedTier != null -> {
              progress.nextLockedTier.tierDescription.ifBlank { group.description }
            }
            else -> group.description
          }

          Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
          )
        }

        if (isCompleted) {
          PixelIcon(icon = "trophy", size = 24.dp)
        }
      }

      // Tier badges row (Brąz / Srebro / Złoto) for multi-tier achievements
      if (group.isTiered && !isHiddenSecret) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          group.tiers.forEach { tierDef ->
            val earned = tierDef.id in progress.unlockedTierIds
            val isNextTarget = progress.nextLockedTier?.id == tierDef.id
            TierPill(
              tier = tierDef.tier,
              thresholdText = group.formatProgressValue(tierDef.threshold),
              earned = earned,
              isNextTarget = isNextTarget,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Progress bar (hidden for locked secret badges so we don't spoil them)
      if (!isHiddenSecret) {
        val targetTier = progress.nextLockedTier ?: group.tiers.lastOrNull()
        if (targetTier != null) {
          val progressLabel = if (isCompleted) {
            if (group.isTiered) "Ukończono wszystkie stopnie" else "Zdobyte"
          } else {
            val pairText = group.formatProgressPair(progress.currentValue, targetTier.threshold)
            if (group.isTiered && targetTier.tier != AchievementTier.SINGLE) {
              "Cel (${targetTier.tier.label}): $pairText"
            } else {
              "Postęp: $pairText"
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = progressLabel,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (isCompleted) {
                if (palette.isDark) PixelColors.Gold else PixelColors.Warning
              } else {
                palette.textMuted
              },
            )
            Text(
              text = "${(progress.progressFraction * 100f).toInt()}%",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (isCompleted) {
                if (palette.isDark) PixelColors.Gold else PixelColors.Warning
              } else {
                palette.accent
              },
            )
          }

          PixelProgressBar(
            progress = progress.progressFraction,
            barHeight = 10.dp,
            fillStartColor = if (isCompleted) PixelColors.Gold else palette.accent,
            fillEndColor = if (isCompleted) PixelColors.Orange else palette.accentSecondary,
          )
        }
      }

      // Reward footer
      val rewardText = when {
        isHiddenSecret -> "Nagroda: ???"
        progress.nextLockedTier != null -> {
          val prefix = if (group.isTiered && progress.nextLockedTier.tier != AchievementTier.SINGLE) {
            "Nagroda (${progress.nextLockedTier.tier.label}): "
          } else {
            "Nagroda: "
          }
          prefix + progress.nextLockedTier.rewardSummary()
        }
        else -> {
          val highest = progress.highestUnlockedTier
          if (highest != null) "Zdobyta nagroda: ${highest.rewardSummary()}" else "Zdobyte!"
        }
      }
      val rewardColor = if (isCompleted) {
        if (palette.isDark) PixelColors.Gold else PixelColors.Warning
      } else if (isHiddenSecret) {
        palette.textMuted
      } else {
        palette.accent
      }
      Text(
        text = rewardText,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = rewardColor,
      )
    }
  }
}

@Composable
private fun TierPill(
  tier: AchievementTier,
  thresholdText: String,
  earned: Boolean,
  isNextTarget: Boolean,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val tierAccent = when (tier) {
    AchievementTier.BRONZE -> if (palette.isDark) Color(0xFFE09F5A) else Color(0xFF9A5B1A)
    AchievementTier.SILVER -> if (palette.isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    AchievementTier.GOLD -> if (palette.isDark) PixelColors.Gold else Color(0xFFA16207)
    AchievementTier.SINGLE -> palette.accent
  }

  val bg = when {
    earned -> tierAccent.copy(alpha = if (palette.isDark) 0.22f else 0.16f)
    isNextTarget -> palette.surfaceSubtle
    else -> palette.surface
  }
  val border = when {
    earned -> tierAccent
    isNextTarget -> palette.accent.copy(alpha = 0.6f)
    else -> palette.divider
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bg,
    border = BorderStroke(1.dp, border),
    modifier = modifier,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        if (earned) {
          PixelIcon(icon = "star", size = 11.dp)
        }
        Text(
          text = tier.label,
          style = MaterialTheme.typography.labelSmall,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = if (earned) tierAccent else palette.textMuted,
          maxLines = 1,
        )
      }
      Text(
        text = thresholdText,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 12.sp,
        color = if (earned) palette.textPrimary else palette.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SecretStatusChip(unlocked: Boolean) {
  val palette = LocalGamefyPalette.current
  val unlockedGoldTint = if (palette.isDark) PixelColors.Gold else PixelColors.Warning
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = if (unlocked) PixelColors.Gold.copy(alpha = 0.25f) else palette.surfaceSubtle,
    border = BorderStroke(1.dp, if (unlocked) PixelColors.Gold else palette.divider),
  ) {
    if (unlocked) {
      Row(
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        PixelIcon(icon = "sparkle", size = 12.dp)
        Text(
          text = "Odkryte",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = unlockedGoldTint,
        )
      }
    } else {
      Text(
        text = "TAJNE",
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.7.sp,
        color = palette.textMuted,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      )
    }
  }
}
