package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AttributeEntity
import com.example.data.local.ShopItemEntity
import com.example.data.model.PenaltyMode
import com.example.data.model.ReminderMode
import com.example.data.model.TaskDifficulty
import com.example.data.model.TaskEffortBucket
import com.example.data.model.formatTimeHm
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily

@Composable
fun CollapsibleEditorCard(
  title: String,
  summary: String,
  expanded: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  badgeText: String? = null,
  testTag: String? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val palette = LocalGamefyPalette.current
  PixelCard(
    modifier = modifier,
    contentPadding = PaddingValues(0.dp),
    borderColor = if (expanded) palette.accent.copy(alpha = 0.6f) else palette.divider,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .clickable(role = Role.Button, onClick = onToggle)
        .let { m -> if (testTag != null) m.testTag(testTag) else m }
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = title,
          fontFamily = PixelFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = if (expanded) palette.accent else palette.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = summary,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (badgeText != null) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = (if (expanded) palette.accent else palette.textMuted).copy(alpha = 0.14f),
        ) {
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (expanded) palette.accent else palette.textMuted,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
      }
      Icon(
        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = if (expanded) "Zwiń sekcję $title" else "Rozwiń sekcję $title",
        tint = if (expanded) palette.accent else palette.textMuted,
        modifier = Modifier.size(24.dp),
      )
    }

    AnimatedVisibility(visible = expanded) {
      Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = palette.divider)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          content = content,
        )
      }
    }
  }
}

private fun TaskDifficulty.shortChipLabel(): String = when (this) {
  TaskDifficulty.VERY_EASY -> "Bardzo łatwe"
  TaskDifficulty.EASY -> "Łatwe"
  TaskDifficulty.MEDIUM -> "Średnie"
  TaskDifficulty.HARD -> "Trudne"
  TaskDifficulty.EXTREME -> "Ekstremalne"
}

private fun ReminderMode.shortSummaryLabel(): String = when (this) {
  ReminderMode.NONE -> "Przypomnienie: brak"
  ReminderMode.AT_DUE -> "W terminie"
  ReminderMode.DAY_BEFORE -> "Dzień przed"
  ReminderMode.THREE_DAYS_BEFORE -> "3 dni przed"
  ReminderMode.DAILY_OVERDUE -> "Gdy po terminie"
  ReminderMode.THREE_DAYS_AND_OVERDUE -> "3 dni + po terminie"
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskRewardsSectionCard(
  expanded: Boolean,
  onToggleExpand: () -> Unit,
  autoRewardEnabled: Boolean,
  onToggleAutoReward: () -> Unit,
  difficulty: TaskDifficulty,
  onSelectDifficulty: (TaskDifficulty) -> Unit,
  effort: TaskEffortBucket,
  onSelectEffort: (TaskEffortBucket) -> Unit,
  surveyAttrCount: Int,
  onSelectSurveyAttrCount: (Int) -> Unit,
  isPenalized: Boolean,
  onSelectIsPenalized: (Boolean) -> Unit,
  expText: String,
  onExpTextChange: (String) -> Unit,
  coinsText: String,
  onCoinsTextChange: (String) -> Unit,
  gemsText: String,
  onGemsTextChange: (String) -> Unit,
  attributes: List<AttributeEntity>,
  selectedAttrIds: List<Long>,
  onToggleAttribute: (Long) -> Unit,
  shopItems: List<ShopItemEntity>,
  rewardItemId: Long,
  onSelectRewardItem: (Long) -> Unit,
) {
  val palette = LocalGamefyPalette.current
  var rewardMenuExpanded by remember { mutableStateOf(false) }

  val expVal = (expText.toIntOrNull() ?: 0).coerceAtLeast(0)
  val coinsVal = (coinsText.toIntOrNull() ?: 0).coerceAtLeast(0)
  val gemsVal = (gemsText.toIntOrNull() ?: 0).coerceAtLeast(0)
  val chosenItem = shopItems.find { it.id == rewardItemId }

  val summary = buildString {
    append("Zdobycz: +$expVal EXP, $coinsVal monet")
    if (gemsVal > 0) append(", ${com.example.data.model.formatGemsLabel(gemsVal)}")
    if (chosenItem != null) append(" · ${chosenItem.name}")
  }

  CollapsibleEditorCard(
    title = "Zdobycz",
    summary = summary,
    expanded = expanded,
    onToggle = onToggleExpand,
    testTag = "editor_section_rewards",
  ) {
    // Prominent button to enable/disable compact 4-row reward calculation survey
    PixelButton(
      text = if (autoRewardEnabled) {
        "Wycena zdobyczy (aktywna)"
      } else {
        "Wycena zdobyczy"
      },
      compact = true,
      containerColor = if (autoRewardEnabled) PixelColors.Green else palette.surfaceSubtle,
      contentColor = if (autoRewardEnabled) Color.White else palette.textPrimary,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("auto_reward_toggle_button"),
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = if (autoRewardEnabled) Color.White else palette.accent,
          modifier = Modifier.size(16.dp),
        )
      },
      onClick = onToggleAutoReward,
    )

    // Compact 4-row chip survey in one card with fixed live reward footer underneath
    AnimatedVisibility(visible = autoRewardEnabled) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceSubtle,
        border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.45f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp)
          .testTag("auto_reward_survey"),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
              Text(
                text = "Wycena zdobyczy",
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = palette.accent,
              )
              Text(
                text = "4 pytania dopasowujące monety i EXP",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
              )
            }

            // Row 1: Trudność
            SurveyChipRow(label = "1. Trudność") {
              TaskDifficulty.entries.forEach { diff ->
                SurveyChoiceChip(
                  label = diff.shortChipLabel(),
                  selected = difficulty == diff,
                  onClick = { onSelectDifficulty(diff) },
                  testTag = "survey_diff_${diff.name.lowercase()}",
                )
              }
            }

            // Row 2: Czas wykonania
            SurveyChipRow(label = "2. Czas") {
              TaskEffortBucket.entries.forEach { bucket ->
                SurveyChoiceChip(
                  label = bucket.label,
                  selected = effort == bucket,
                  onClick = { onSelectEffort(bucket) },
                  testTag = "survey_effort_${bucket.name.lowercase()}",
                )
              }
            }

            // Row 3: Liczba atrybutów
            SurveyChipRow(label = "3. Na ile atrybutów wpływa?") {
              listOf(
                0 to "Brak (0)",
                1 to "1 atrybut",
                2 to "2 atrybuty",
                3 to "3+ atrybuty",
              ).forEach { (count, label) ->
                val isSelected = if (count >= 3) surveyAttrCount >= 3 else surveyAttrCount == count
                SurveyChoiceChip(
                  label = label,
                  selected = isSelected,
                  onClick = { onSelectSurveyAttrCount(count) },
                  testTag = "survey_attr_count_$count",
                )
              }
            }

            // Row 4: Kara za pominięcie / termin
            SurveyChipRow(label = "4. Kara") {
              SurveyChoiceChip(
                label = "Nie",
                selected = !isPenalized,
                onClick = { onSelectIsPenalized(false) },
                testTag = "survey_penalty_no",
              )
              SurveyChoiceChip(
                label = "Tak (+25% zdobyczy)",
                selected = isPenalized,
                onClick = { onSelectIsPenalized(true) },
                testTag = "survey_penalty_yes",
              )
            }
          }

          HorizontalDivider(color = palette.accent.copy(alpha = 0.3f))

          // Fixed live result strip right beneath the 4 chip rows
          Surface(
            color = palette.accent.copy(alpha = 0.14f),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("survey_live_reward_result"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = buildString {
                  append("Zdobycz: $coinsVal monet · $expVal EXP")
                  if (gemsVal > 0) append(" · ${com.example.data.model.formatGemsLabel(gemsVal)}")
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    if (!autoRewardEnabled) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Trudność zadania",
          style = MaterialTheme.typography.labelLarge,
          color = palette.textPrimary,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TaskDifficulty.entries.forEach { diff ->
            SurveyChoiceChip(
              label = diff.shortChipLabel(),
              selected = difficulty == diff,
              onClick = { onSelectDifficulty(diff) },
              testTag = "manual_diff_${diff.name.lowercase()}",
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = expText,
        onValueChange = { if (!autoRewardEnabled) onExpTextChange(it.filter(Char::isDigit)) },
        readOnly = autoRewardEnabled,
        label = { Text(if (autoRewardEnabled) "EXP (auto)" else "EXP") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("task_exp_input"),
      )
      OutlinedTextField(
        value = coinsText,
        onValueChange = { if (!autoRewardEnabled) onCoinsTextChange(it.filter(Char::isDigit)) },
        readOnly = autoRewardEnabled,
        label = { Text(if (autoRewardEnabled) "Monety (auto)" else "Monety") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("task_coins_input"),
      )
      OutlinedTextField(
        value = gemsText,
        onValueChange = { if (!autoRewardEnabled) onGemsTextChange(it.filter(Char::isDigit)) },
        readOnly = autoRewardEnabled,
        label = { Text(if (autoRewardEnabled) "Brylanty (auto)" else "Brylanty") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("task_gems_input"),
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "Rozwijane atrybuty (awans atrybutu = +1 brylant)",
      style = MaterialTheme.typography.bodySmall,
      color = palette.textMuted,
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      attributes.forEach { attr ->
        val selected = attr.id in selectedAttrIds
        FilterChip(
          selected = selected,
          onClick = { onToggleAttribute(attr.id) },
          leadingIcon = { PixelIcon(icon = attr.icon, size = 18.dp) },
          label = { Text(attr.name) },
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    ExposedDropdownMenuBox(
      expanded = rewardMenuExpanded,
      onExpandedChange = { rewardMenuExpanded = !rewardMenuExpanded },
    ) {
      OutlinedTextField(
        value = chosenItem?.name ?: "Brak nagrody-przedmiotu",
        onValueChange = {},
        readOnly = true,
        label = { Text("Dodatkowa nagroda ze sklepu") },
        trailingIcon = {
          ExposedDropdownMenuDefaults.TrailingIcon(expanded = rewardMenuExpanded)
        },
        modifier = Modifier
          .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          .fillMaxWidth(),
      )
      ExposedDropdownMenu(
        expanded = rewardMenuExpanded,
        onDismissRequest = { rewardMenuExpanded = false },
      ) {
        DropdownMenuItem(
          text = { Text("Brak") },
          onClick = { onSelectRewardItem(0L); rewardMenuExpanded = false },
        )
        shopItems.forEach { item ->
          DropdownMenuItem(
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                PixelIcon(icon = item.icon, size = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.name)
              }
            },
            onClick = { onSelectRewardItem(item.id); rewardMenuExpanded = false },
          )
        }
      }
    }
  }
}

@Composable
private fun SurveyChipRow(
  label: String,
  chips: @Composable () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = palette.textPrimary,
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      chips()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskReminderAndPenaltyCard(
  expanded: Boolean,
  onToggleExpand: () -> Unit,
  reminderMode: ReminderMode,
  onSelectReminderMode: (ReminderMode) -> Unit,
  preferredReminderHour: Int,
  preferredReminderMinute: Int,
  onSelectPreferredTime: (Int, Int) -> Unit,
  globalForcedModeActive: Boolean = false,
  onOpenNotificationSettings: () -> Unit = {},
  penaltyMode: PenaltyMode,
  onSelectPenaltyMode: (PenaltyMode) -> Unit,
  autoPenaltyEnabled: Boolean,
  onToggleAutoPenalty: (Boolean) -> Unit,
  penaltyMinText: String,
  onPenaltyMinChange: (String) -> Unit,
  penaltyMaxText: String,
  onPenaltyMaxChange: (String) -> Unit,
) {
  val palette = LocalGamefyPalette.current
  var reminderMenuExpanded by remember { mutableStateOf(false) }
  var penaltyMenuExpanded by remember { mutableStateOf(false) }
  var showPreferredTimePicker by remember { mutableStateOf(false) }

  val minP = (penaltyMinText.toIntOrNull() ?: 0).coerceAtLeast(0)
  val maxP = (penaltyMaxText.toIntOrNull() ?: minP).coerceAtLeast(minP)
  val penaltySummary = if (penaltyMode == PenaltyMode.NONE) {
    "Kara: brak"
  } else if (minP == maxP) {
    "Kara: −$minP monet"
  } else {
    "Kara: −$minP–$maxP monet"
  }
  val reminderSummary = if (globalForcedModeActive) {
    "Wspólne powiadomienia"
  } else {
    buildString {
      append(reminderMode.shortSummaryLabel())
      if (reminderMode != ReminderMode.NONE && preferredReminderHour >= 0) {
        append(" (${formatTimeHm(preferredReminderHour, preferredReminderMinute)})")
      }
    }
  }
  val headerSummary = "$penaltySummary · $reminderSummary"

  CollapsibleEditorCard(
    title = "Przypomnienie i kara",
    summary = headerSummary,
    expanded = expanded,
    onToggle = onToggleExpand,
    testTag = "editor_section_reminders_penalties",
  ) {
    if (globalForcedModeActive) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = PixelColors.Orange.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, PixelColors.Orange.copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
          .testTag("global_notifications_warning_banner"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 12.dp, end = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Włączono wspólne powiadomienia.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          TextButton(
            onClick = onOpenNotificationSettings,
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("change_notification_mode_link"),
          ) {
            Text(
              text = "Zmień tryb",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = palette.accent,
              maxLines = 1,
            )
          }
        }
      }
    }

    ExposedDropdownMenuBox(
      expanded = reminderMenuExpanded,
      onExpandedChange = { reminderMenuExpanded = !reminderMenuExpanded },
    ) {
      OutlinedTextField(
        value = reminderMode.label,
        onValueChange = {},
        readOnly = true,
        label = { Text("Przypomnienie") },
        trailingIcon = {
          ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderMenuExpanded)
        },
        modifier = Modifier
          .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          .fillMaxWidth(),
      )
      ExposedDropdownMenu(
        expanded = reminderMenuExpanded,
        onDismissRequest = { reminderMenuExpanded = false },
      ) {
        ReminderMode.entries.forEach { mode ->
          DropdownMenuItem(
            text = { Text(mode.label) },
            onClick = { onSelectReminderMode(mode); reminderMenuExpanded = false },
          )
        }
      }
    }

    AnimatedVisibility(visible = reminderMode != ReminderMode.NONE) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "Preferowana godzina powiadomienia",
          style = MaterialTheme.typography.labelLarge,
          color = palette.textPrimary,
        )
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = palette.surfaceSubtle,
          border = BorderStroke(
            width = 1.5.dp,
            color = if (preferredReminderHour >= 0) palette.accent else palette.divider,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showPreferredTimePicker = true }
            .testTag("task_preferred_time_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = if (preferredReminderHour >= 0) palette.accent else palette.textMuted,
                modifier = Modifier.size(18.dp),
              )
              Column {
                Text(
                  text = if (preferredReminderHour >= 0) {
                    formatTimeHm(preferredReminderHour, preferredReminderMinute)
                  } else {
                    "Dowolna (brak konkretnej godziny)"
                  },
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = palette.textPrimary,
                )
                Text(
                  text = "Kliknij, aby ustawić dokładną godzinę powiadomienia",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textMuted,
                )
              }
            }
            if (preferredReminderHour >= 0) {
              IconButton(
                onClick = { onSelectPreferredTime(-1, 0) },
                modifier = Modifier
                  .size(32.dp)
                  .testTag("task_clear_preferred_time_button"),
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Usuń godzinę",
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SurveyChoiceChip(
            label = "Dowolna",
            selected = preferredReminderHour < 0,
            onClick = { onSelectPreferredTime(-1, 0) },
            testTag = "task_pref_hour_any",
          )
          listOf(8 to 0, 12 to 0, 18 to 0, 20 to 0).forEach { (h, m) ->
            SurveyChoiceChip(
              label = formatTimeHm(h, m),
              selected = preferredReminderHour == h && preferredReminderMinute == m,
              onClick = { onSelectPreferredTime(h, m) },
              testTag = "task_pref_hour_$h",
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    ExposedDropdownMenuBox(
      expanded = penaltyMenuExpanded,
      onExpandedChange = { penaltyMenuExpanded = !penaltyMenuExpanded },
    ) {
      OutlinedTextField(
        value = penaltyMode.label,
        onValueChange = {},
        readOnly = true,
        label = { Text("Kara za spóźnienie lub pominięcie") },
        trailingIcon = {
          ExposedDropdownMenuDefaults.TrailingIcon(expanded = penaltyMenuExpanded)
        },
        modifier = Modifier
          .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          .fillMaxWidth(),
      )
      ExposedDropdownMenu(
        expanded = penaltyMenuExpanded,
        onDismissRequest = { penaltyMenuExpanded = false },
      ) {
        PenaltyMode.entries.forEach { mode ->
          DropdownMenuItem(
            text = { Text(mode.label) },
            onClick = { onSelectPenaltyMode(mode); penaltyMenuExpanded = false },
          )
        }
      }
    }

    if (penaltyMode != PenaltyMode.NONE) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Kalkulacja kary",
        style = MaterialTheme.typography.labelLarge,
        color = palette.textPrimary,
      )
      Spacer(modifier = Modifier.height(6.dp))

      // Automatic vs Manual penalty calculation switch
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceSubtle,
        border = BorderStroke(1.dp, palette.divider),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          listOf(
            true to Pair("Automatyczna", "auto_penalty_mode_button"),
            false to Pair("Manualna", "manual_penalty_mode_button"),
          ).forEach { (isAuto, info) ->
            val selected = autoPenaltyEnabled == isAuto
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (selected) palette.accent else Color.Transparent,
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Tab) { onToggleAutoPenalty(isAuto) }
                .testTag(info.second),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = info.first,
                  fontFamily = PixelFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (selected) palette.onAccent else palette.textMuted,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      if (autoPenaltyEnabled) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PixelColors.Red.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, PixelColors.Red.copy(alpha = 0.35f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auto_penalty_summary"),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "Automatyczna kara: od $penaltyMinText do $penaltyMaxText monet",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = PixelColors.Red,
            )
            Text(
              text = "Wyliczona proporcjonalnie do trudności, trybu kary i nagrody w monetach.",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textMuted,
            )
          }
        }
      } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = penaltyMinText,
            onValueChange = { onPenaltyMinChange(it.filter(Char::isDigit)) },
            label = { Text("Kara od (monet)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("penalty_min_input"),
          )
          OutlinedTextField(
            value = penaltyMaxText,
            onValueChange = { onPenaltyMaxChange(it.filter(Char::isDigit)) },
            label = { Text("do (losowo)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("penalty_max_input"),
          )
        }
      }
    }
  }

  if (showPreferredTimePicker) {
    val pickerState = rememberTimePickerState(
      initialHour = if (preferredReminderHour >= 0) preferredReminderHour.coerceIn(0, 23) else 9,
      initialMinute = if (preferredReminderHour >= 0) preferredReminderMinute.coerceIn(0, 59) else 0,
      is24Hour = true,
    )
    AlertDialog(
      onDismissRequest = { showPreferredTimePicker = false },
      title = { Text("Preferowana godzina powiadomienia") },
      text = { TimePicker(state = pickerState) },
      confirmButton = {
        TextButton(
          onClick = {
            onSelectPreferredTime(pickerState.hour, pickerState.minute)
            showPreferredTimePicker = false
          },
        ) { Text("Ustaw") }
      },
      dismissButton = {
        TextButton(onClick = { showPreferredTimePicker = false }) { Text("Anuluj") }
      },
    )
  }
}

@Composable
private fun SurveyChoiceChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (selected) palette.accent else palette.surface,
    border = BorderStroke(1.5.dp, if (selected) palette.outline else palette.divider),
    modifier = Modifier
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) palette.onAccent else palette.textPrimary,
        maxLines = 1,
      )
    }
  }
}
