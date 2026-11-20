package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.parseStringCsv
import com.example.data.model.RANDOM_SIDE_QUEST_BOARD_SIZE
import com.example.data.model.RANDOM_SIDE_QUEST_DAILY_LIMIT
import com.example.data.model.RANDOM_SIDE_QUEST_TEMPLATES
import com.example.data.model.RandomSideQuestTemplate
import com.example.data.model.formatRandomQuestExpSummary
import com.example.data.model.localCalendarDayKey
import com.example.data.model.resolveBoardTemplates
import com.example.data.model.resolveDailyBoardIds
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelIcon
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily

@Composable
fun RandomSideQuestScreen(
  profile: PlayerProfileEntity,
  pendingSelectedIds: Set<String>,
  isAccepting: Boolean,
  onEnsureDailyBoard: () -> Unit,
  onToggleSelect: (String) -> Unit,
  onAcceptSelected: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val todayKey = remember { localCalendarDayKey() }

  LaunchedEffect(todayKey) {
    onEnsureDailyBoard()
  }

  val isSameDay = profile.rsqBoardDayKey == todayKey
  val acceptedIdsToday = remember(isSameDay, profile.rsqAcceptedIdsCsv) {
    if (isSameDay) parseStringCsv(profile.rsqAcceptedIdsCsv) else emptySet()
  }
  val acceptedCountToday = remember(isSameDay, profile.rsqAcceptedCountToday, acceptedIdsToday) {
    if (isSameDay) {
      maxOf(profile.rsqAcceptedCountToday, acceptedIdsToday.size)
        .coerceIn(0, RANDOM_SIDE_QUEST_DAILY_LIMIT)
    } else {
      0
    }
  }
  val remainingSlots = (RANDOM_SIDE_QUEST_DAILY_LIMIT - acceptedCountToday).coerceAtLeast(0)
  val isDailyLimitReached = remainingSlots <= 0

  val boardQuests = remember(isSameDay, profile.rsqBoardIdsCsv) {
    if (isSameDay && profile.rsqBoardIdsCsv.isNotBlank()) {
      val ids = resolveDailyBoardIds(
        catalog = RANDOM_SIDE_QUEST_TEMPLATES,
        savedIdsCsv = profile.rsqBoardIdsCsv,
        count = RANDOM_SIDE_QUEST_BOARD_SIZE,
      )
      resolveBoardTemplates(ids, RANDOM_SIDE_QUEST_TEMPLATES)
    } else {
      RANDOM_SIDE_QUEST_TEMPLATES.take(RANDOM_SIDE_QUEST_BOARD_SIZE)
    }
  }

  val effectivePendingIds = remember(pendingSelectedIds, acceptedIdsToday, remainingSlots) {
    pendingSelectedIds
      .filter { it !in acceptedIdsToday }
      .take(remainingSlots)
      .toSet()
  }
  val totalChosenCount = (acceptedCountToday + effectivePendingIds.size)
    .coerceIn(0, RANDOM_SIDE_QUEST_DAILY_LIMIT)

  var expandedIds by remember { mutableStateOf(setOf<String>()) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = palette.background,
    topBar = {
      Surface(
        color = palette.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
              .size(48.dp)
              .testTag("adventure_board_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Wróć",
              tint = palette.textPrimary,
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "⚔️ Tablica Przygód",
              fontFamily = PixelFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = palette.textPrimary,
            )
            Text(
              text = "Dzienny limit: $acceptedCountToday/$RANDOM_SIDE_QUEST_DAILY_LIMIT",
              style = MaterialTheme.typography.labelSmall,
              color = if (isDailyLimitReached) PixelColors.Red else palette.textMuted,
            )
          }
        }
      }
    },
    bottomBar = {
      Surface(
        color = palette.surface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.5.dp, palette.divider),
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
      ) {
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 640.dp)
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Wybrano: $totalChosenCount/$RANDOM_SIDE_QUEST_DAILY_LIMIT",
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = palette.textPrimary,
                modifier = Modifier.testTag("adventure_board_counter"),
              )
              val statusHint = when {
                isDailyLimitReached -> "Limit na dziś wykorzystany"
                acceptedCountToday > 0 && effectivePendingIds.isNotEmpty() ->
                  "Nowe: ${effectivePendingIds.size} · Przyjęte dziś: $acceptedCountToday"
                acceptedCountToday > 0 ->
                  "Pozostało miejsc dziś: $remainingSlots"
                else -> "Możesz wybrać od 1 do 3 misji"
              }
              Text(
                text = statusHint,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDailyLimitReached) PixelColors.Red else palette.textMuted,
              )
            }

            PixelButton(
              text = if (isAccepting) "Dodawanie misji..." else "Przyjmij wybrane misje",
              enabled = effectivePendingIds.isNotEmpty() && !isDailyLimitReached && !isAccepting,
              containerColor = PixelColors.Gold,
              contentColor = palette.outline,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("accept_random_quests_button"),
              onClick = onAcceptSelected,
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item(key = "adventure_board_header") {
          AdventureBoardHeroBanner(
            acceptedCountToday = acceptedCountToday,
            isDailyLimitReached = isDailyLimitReached,
          )
        }

        items(boardQuests, key = { it.id }) { quest ->
          val isAlreadyAccepted = quest.id in acceptedIdsToday
          val isPendingSelected = quest.id in effectivePendingIds
          val canSelectMore = effectivePendingIds.size < remainingSlots
          val isExpanded = quest.id in expandedIds

          RandomSideQuestCard(
            quest = quest,
            isAlreadyAccepted = isAlreadyAccepted,
            isPendingSelected = isPendingSelected,
            canSelect = !isAlreadyAccepted && (isPendingSelected || canSelectMore) && !isDailyLimitReached && !isAccepting,
            isExpanded = isExpanded,
            onToggleExpand = {
              expandedIds = if (isExpanded) expandedIds - quest.id else expandedIds + quest.id
            },
            onToggleSelect = { onToggleSelect(quest.id) },
          )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
      }
    }
  }
}

@Composable
private fun AdventureBoardHeroBanner(
  acceptedCountToday: Int,
  isDailyLimitReached: Boolean,
) {
  val palette = LocalGamefyPalette.current
  val shape = RoundedCornerShape(18.dp)

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(
        Brush.linearGradient(
          listOf(
            palette.accent.copy(alpha = 0.92f),
            palette.accentSecondary.copy(alpha = 0.92f),
          )
        )
      )
      .border(2.5.dp, palette.outline, shape)
      .padding(16.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "⚔️ Tablica Przygód",
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Color.White,
      )
      Text(
        text = "Mistrz Gry przygotował dla Ciebie sześć podejrzanych propozycji. Możesz przyjąć maksymalnie trzy. Reszta niech radzi sobie sama.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.94f),
      )

      if (isDailyLimitReached) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color.Black.copy(alpha = 0.28f),
          border = BorderStroke(1.dp, PixelColors.Gold),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("adventure_board_limit_banner"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Limit wykorzystany",
              tint = PixelColors.Gold,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "Przyjęto już 3/3 misje na dziś. Kolejne propozycje odblokują się jutro!",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
            )
          }
        }
      } else if (acceptedCountToday > 0) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color.White.copy(alpha = 0.18f),
        ) {
          Text(
            text = "Przyjęto dziś: $acceptedCountToday/$RANDOM_SIDE_QUEST_DAILY_LIMIT · Możesz dobrać jeszcze ${RANDOM_SIDE_QUEST_DAILY_LIMIT - acceptedCountToday}.",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RandomSideQuestCard(
  quest: RandomSideQuestTemplate,
  isAlreadyAccepted: Boolean,
  isPendingSelected: Boolean,
  canSelect: Boolean,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onToggleSelect: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val diffColor = difficultyColor(quest.taskDifficulty, palette.accent)
  val borderColor = when {
    isAlreadyAccepted -> PixelColors.Green
    isPendingSelected -> PixelColors.Gold
    else -> palette.divider
  }
  val borderWidth = if (isPendingSelected || isAlreadyAccepted) 2.5.dp else 1.5.dp
  val cardAlpha = if (!isAlreadyAccepted && !isPendingSelected && !canSelect) 0.72f else 1f

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = palette.surface,
    border = BorderStroke(borderWidth, borderColor),
    shadowElevation = if (isPendingSelected) 4.dp else 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .alpha(cardAlpha)
      .testTag("rsq_card_${quest.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Emoji badge
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              when {
                isAlreadyAccepted -> PixelColors.Green.copy(alpha = 0.16f)
                isPendingSelected -> PixelColors.Gold.copy(alpha = 0.24f)
                else -> palette.surfaceSubtle
              }
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = quest.emoji,
            fontSize = 24.sp,
          )
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggleExpand),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = quest.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          if (!isExpanded) {
            Text(
              text = "„${quest.story}”",
              style = MaterialTheme.typography.bodySmall,
              fontStyle = FontStyle.Italic,
              color = palette.textMuted,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // Selection toggle (48dp touch target)
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = canSelect, onClick = onToggleSelect)
            .testTag("rsq_select_${quest.id}"),
          contentAlignment = Alignment.Center,
        ) {
          val boxBg = when {
            isAlreadyAccepted -> PixelColors.Green
            isPendingSelected -> PixelColors.Gold
            else -> palette.surfaceSubtle
          }
          val boxBorder = when {
            isAlreadyAccepted -> PixelColors.Green
            isPendingSelected -> palette.outline
            else -> palette.outline.copy(alpha = 0.55f)
          }
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(boxBg)
              .border(2.dp, boxBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
          ) {
            if (isAlreadyAccepted || isPendingSelected) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isAlreadyAccepted) "Przyjęto dziś" else "Wybrano misję",
                tint = if (isAlreadyAccepted) Color.White else palette.outline,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      // Meta chips: time, difficulty, coins, EXP
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = palette.surfaceSubtle,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = palette.textMuted,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "${quest.estimatedMinutes} min",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = palette.textPrimary,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = diffColor.copy(alpha = 0.14f),
        ) {
          Text(
            text = "Trudność ${quest.difficultyLevel} · ${quest.taskDifficulty.label}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = diffColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = PixelColors.Gold.copy(alpha = 0.18f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "+${quest.coins}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (palette.isDark) PixelColors.Gold else PixelColors.Warning,
            )
            PixelIcon(icon = "coin", size = 14.dp)
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = palette.accent.copy(alpha = 0.14f),
        ) {
          Text(
            text = formatRandomQuestExpSummary(quest),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }

        if (isAlreadyAccepted) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = PixelColors.Green.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, PixelColors.Green),
          ) {
            Text(
              text = "Przyjęto dziś ✓",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = PixelColors.Green,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }

      // Expand / collapse trigger
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 36.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onToggleExpand)
          .testTag("rsq_expand_${quest.id}")
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = if (isExpanded) "Ukryj pełną historię i cel" else "Rozwiń pełną historię i cel",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = palette.accent,
        )
        Icon(
          imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (isExpanded) "Zwiń szczegóły misji" else "Rozwiń szczegóły misji",
          tint = palette.accent,
          modifier = Modifier.size(20.dp),
        )
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          HorizontalDivider(color = palette.divider)

          Text(
            text = "„${quest.story}”",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = palette.textPrimary,
          )

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.background,
            border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "Cel zadania",
                tint = palette.accent,
                modifier = Modifier.size(18.dp),
              )
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "CEL ZADANIA",
                  fontFamily = PixelFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = palette.accent,
                )
                Text(
                  text = quest.goal,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = palette.textPrimary,
                )
              }
            }
          }

          if (quest.subtasks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Kroki misji:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textMuted,
              )
              quest.subtasks.forEach { step ->
                Text(
                  text = "• $step",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textPrimary,
                )
              }
            }
          }

          if (quest.tags.isNotEmpty()) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              quest.tags.forEach { tag ->
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = palette.surfaceSubtle,
                ) {
                  Text(
                    text = "#$tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
