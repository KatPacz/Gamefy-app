package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskRewardsSectionCard(
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

  PixelCard {
    Text(
      text = "NAGRODY ZA WYKONANIE",
      style = MaterialTheme.typography.titleSmall,
      color = palette.textMuted,
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Prominent button right underneath "NAGRODY ZA WYKONANIE"
    PixelButton(
      text = if (autoRewardEnabled) {
        "Automatyczna kalkulacja nagrody ✓"
      } else {
        "Automatyczna kalkulacja nagrody"
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

    // Short Survey shown below the button when auto calculation is active
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
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "ANKIETA KALKULACJI NAGRODY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.accent,
          )

          // 1. Trudność zadania
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "1. Trudność zadania",
              style = MaterialTheme.typography.labelLarge,
              color = palette.textPrimary,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              TaskDifficulty.entries.forEach { diff ->
                SurveyChoiceChip(
                  label = diff.label,
                  selected = difficulty == diff,
                  onClick = { onSelectDifficulty(diff) },
                  testTag = "survey_diff_${diff.name.lowercase()}",
                )
              }
            }
          }

          // 2. Czas wykonania
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "2. Czas wykonania",
              style = MaterialTheme.typography.labelLarge,
              color = palette.textPrimary,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              TaskEffortBucket.entries.forEach { bucket ->
                SurveyChoiceChip(
                  label = bucket.label,
                  selected = effort == bucket,
                  onClick = { onSelectEffort(bucket) },
                  testTag = "survey_effort_${bucket.name.lowercase()}",
                )
              }
            }
          }

          // 3. Na ile statystyk (atrybutów) wpływa
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "3. Na ile statystyk (atrybutów) wpływa?",
              style = MaterialTheme.typography.labelLarge,
              color = palette.textPrimary,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              listOf(
                0 to "Brak (0)",
                1 to "1 statystyka",
                2 to "2 statystyki",
                3 to "3+ statystyk",
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
          }

          // 4. Czy zadanie będzie karane za pominięcie / brak terminu
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "4. Czy będzie karane za pominięcie / brak terminu?",
              style = MaterialTheme.typography.labelLarge,
              color = palette.textPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              SurveyChoiceChip(
                label = "Nie",
                selected = !isPenalized,
                onClick = { onSelectIsPenalized(false) },
                testTag = "survey_penalty_no",
              )
              SurveyChoiceChip(
                label = "Tak, z karą (+25% nagrody)",
                selected = isPenalized,
                onClick = { onSelectIsPenalized(true) },
                testTag = "survey_penalty_yes",
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
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TaskDifficulty.entries.forEach { diff ->
            SurveyChoiceChip(
              label = diff.label,
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
        label = { Text(if (autoRewardEnabled) "💎 (auto)" else "Brylanty") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
          .weight(0.9f)
          .testTag("task_gems_input"),
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "Rozwijane atrybuty (awans atrybutu = +1 💎)",
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
      val chosenItem = shopItems.find { it.id == rewardItemId }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskReminderAndPenaltyCard(
  reminderMode: ReminderMode,
  onSelectReminderMode: (ReminderMode) -> Unit,
  preferredReminderHour: Int,
  preferredReminderMinute: Int,
  onSelectPreferredTime: (Int, Int) -> Unit,
  globalForcedModeActive: Boolean = false,
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

  PixelCard {
    Text(
      text = "PRZYPOMINAJKA I KARY",
      style = MaterialTheme.typography.titleSmall,
      color = palette.textMuted,
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (globalForcedModeActive) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = PixelColors.Orange.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, PixelColors.Orange.copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
      ) {
        Text(
          text = "Uwaga: w Profil → Ustawienia wyłączono „Zależne od zadania” — obecnie wymuszone są powiadomienia globalne.",
          style = MaterialTheme.typography.bodySmall,
          color = PixelColors.Orange,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
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
        label = { Text("Przypominajka") },
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
                  contentDescription = "Wyczyść preferowaną godzinę",
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
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Tab) { onToggleAutoPenalty(isAuto) }
                .testTag(info.second),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = info.first.uppercase(),
                  fontFamily = PixelFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = if (selected) Color.White else palette.textMuted,
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
              text = "Automatyczna kara: od $penaltyMinText do $penaltyMaxText 🪙",
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
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) Color.White else palette.textPrimary,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
    )
  }
}
