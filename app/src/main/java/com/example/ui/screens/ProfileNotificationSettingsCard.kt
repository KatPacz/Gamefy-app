package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.TaskEntity
import com.example.data.model.NotificationSettings
import com.example.data.model.NotificationSummaryStyle
import com.example.data.model.NotificationTimeSlot
import com.example.data.model.NotificationTriggerMode
import com.example.data.model.WEEKDAY_LABELS_PL
import com.example.data.model.buildSummaryNotificationText
import com.example.data.model.formatTimeHm
import com.example.data.model.tasksPlannedForDay
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesBlock(
  notificationsEnabled: Boolean,
  settings: NotificationSettings,
  tasks: List<TaskEntity>,
  onToggleNotifications: (Boolean) -> Unit,
  onUpdateSettings: (NotificationSettings) -> Unit,
) {
  val palette = LocalGamefyPalette.current
  var editingStartTime by remember { mutableStateOf(false) }
  var editingEndTime by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Powiadomienia lokalne",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
        Text(
          text = "Przypomnienia działają offline w tle według wybranego harmonogramu.",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textMuted,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Switch(
        checked = notificationsEnabled,
        onCheckedChange = onToggleNotifications,
        modifier = Modifier.testTag("notifications_master_switch"),
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor = PixelColors.Green,
        ),
      )
    }

    AnimatedVisibility(visible = notificationsEnabled) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp)
          .testTag("notification_schedule_options"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        HorizontalDivider(color = palette.divider)

        // 0. Zależne od zadania vs wymuszone ustawienia globalne
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (settings.useTaskSettings) {
            palette.accent.copy(alpha = 0.12f)
          } else {
            PixelColors.Orange.copy(alpha = 0.12f)
          },
          border = BorderStroke(
            width = 1.5.dp,
            color = if (settings.useTaskSettings) palette.accent else PixelColors.Orange,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              onUpdateSettings(settings.copy(useTaskSettings = !settings.useTaskSettings))
            }
            .testTag("notif_use_task_settings_row"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Checkbox(
              checked = settings.useTaskSettings,
              onCheckedChange = { checked ->
                onUpdateSettings(settings.copy(useTaskSettings = checked))
              },
              modifier = Modifier.testTag("notif_use_task_settings_checkbox"),
              colors = CheckboxDefaults.colors(
                checkedColor = palette.accent,
                uncheckedColor = PixelColors.Orange,
                checkmarkColor = Color.White,
              ),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Zależne od zadania (wg ustawień w zadaniu)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
              )
              Text(
                text = if (settings.useTaskSettings) {
                  "Powiadomienia działają wg przypominajki i preferowanej godziny wybranej w każdym zadaniu. Odznacz, aby wymusić ustawienia globalne poniżej."
                } else {
                  "Tryb globalny WYMUSZONY — przypominajki i godziny bezpośrednio w zadaniach są ignorowane."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (settings.useTaskSettings) palette.textMuted else PixelColors.Orange,
              )
            }
          }
        }

        // 1. Dni tygodnia (globalne)
        Column(
          modifier = Modifier.alpha(if (settings.useTaskSettings) 0.72f else 1f),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = if (settings.useTaskSettings) {
              "USTAWIENIA GLOBALNE (nieaktywne dopóki „Zależne od zadania” jest zaznaczone)"
            } else {
              "USTAWIENIA GLOBALNE (AKTYWNE I WYMUSZONE)"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (settings.useTaskSettings) palette.textMuted else PixelColors.Orange,
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
            Text(
              text = "Kiedy chcesz otrzymywać powiadomienia?",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
              modifier = Modifier.weight(1f),
            )
            val isDaily = settings.daysOfWeek.size == 7
            NotifChip(
              label = "Codziennie",
              selected = isDaily,
              testTag = "notif_days_daily",
              onClick = {
                onUpdateSettings(settings.copy(daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7)))
              },
            )
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            WEEKDAY_LABELS_PL.forEach { (dayNum, label) ->
              val selected = dayNum in settings.daysOfWeek
              NotifChip(
                label = label,
                selected = selected,
                testTag = "notif_day_$dayNum",
                onClick = {
                  val nextDays = if (selected) {
                    if (settings.daysOfWeek.size > 1) settings.daysOfWeek - dayNum else settings.daysOfWeek
                  } else {
                    settings.daysOfWeek + dayNum
                  }
                  onUpdateSettings(settings.copy(daysOfWeek = nextDays))
                },
              )
            }
          }
        }

        // 2. Pory dnia i własne godziny
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Pora dnia",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            NotifChip(
              label = "Cały dzień",
              selected = settings.allowAnytime || settings.timeSlots.isEmpty(),
              testTag = "notif_slot_anytime",
              onClick = {
                onUpdateSettings(settings.copy(allowAnytime = true, timeSlots = emptySet()))
              },
            )
            NotificationTimeSlot.entries.forEach { slot ->
              val selected = !settings.allowAnytime && slot in settings.timeSlots
              val chipLabel = if (slot == NotificationTimeSlot.CUSTOM) {
                slot.label
              } else {
                "${slot.label} (${slot.shortRange})"
              }
              NotifChip(
                label = chipLabel,
                selected = selected,
                testTag = "notif_slot_${slot.code}",
                onClick = {
                  val nextSlots = if (selected) {
                    settings.timeSlots - slot
                  } else {
                    settings.timeSlots + slot
                  }
                  onUpdateSettings(
                    settings.copy(
                      allowAnytime = nextSlots.isEmpty(),
                      timeSlots = nextSlots,
                    )
                  )
                },
              )
            }
          }

          AnimatedVisibility(visible = !settings.allowAnytime && NotificationTimeSlot.CUSTOM in settings.timeSlots) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              TimePickPill(
                label = "Od",
                timeText = formatTimeHm(settings.customStartHour, settings.customStartMinute),
                testTag = "notif_custom_start_button",
                modifier = Modifier.weight(1f),
                onClick = { editingStartTime = true },
              )
              TimePickPill(
                label = "Do",
                timeText = formatTimeHm(settings.customEndHour, settings.customEndMinute),
                testTag = "notif_custom_end_button",
                modifier = Modifier.weight(1f),
                onClick = { editingEndTime = true },
              )
            }
          }
        }

        // 3. Warunek wyzwalania
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Kiedy wysyłać alert o zadaniu?",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NotificationTriggerMode.entries.forEach { mode ->
              val selected = settings.triggerMode == mode
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) palette.accent.copy(alpha = 0.14f) else palette.surfaceSubtle,
                border = BorderStroke(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) palette.accent else palette.divider,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onUpdateSettings(settings.copy(triggerMode = mode)) }
                  .testTag("notif_trigger_${mode.code}"),
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                  Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) palette.accent else palette.textPrimary,
                  )
                  Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textMuted,
                  )
                }
              }
            }
          }
        }
        }

        HorizontalDivider(color = palette.divider)

        // 4. Ograniczenie liczby powiadomień dziennie
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Ogranicz liczbę powiadomień (maks. dziennie)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf(
              0 to "Bez limitu",
              1 to "Maks. 1",
              2 to "Maks. 2",
              3 to "Maks. 3",
              5 to "Maks. 5",
            ).forEach { (limitVal, label) ->
              NotifChip(
                label = label,
                selected = settings.maxPerDay == limitVal,
                testTag = "notif_limit_$limitVal",
                onClick = { onUpdateSettings(settings.copy(maxPerDay = limitVal)) },
              )
            }
          }

          var customLimitText by remember(settings.maxPerDay) {
            mutableStateOf(if (settings.maxPerDay > 0) settings.maxPerDay.toString() else "")
          }
          OutlinedTextField(
            value = customLimitText,
            onValueChange = { raw ->
              val clean = raw.filter(Char::isDigit).take(2)
              customLimitText = clean
              val parsed = clean.toIntOrNull()?.coerceIn(0, 50) ?: 0
              onUpdateSettings(settings.copy(maxPerDay = parsed))
            },
            label = { Text("Własny limit dzienny (0 = bez limitu)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("notif_custom_limit_input"),
          )
        }

        // 5. Tryb powiadomienia zbiorczego przy limicie
        AnimatedVisibility(visible = settings.maxPerDay > 0) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "Co wysyłać przy ograniczeniu powiadomień?",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
            NotificationSummaryStyle.entries.forEach { style ->
              val selected = settings.summaryStyle == style
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) palette.accent.copy(alpha = 0.14f) else palette.surfaceSubtle,
                border = BorderStroke(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) palette.accent else palette.divider,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onUpdateSettings(settings.copy(summaryStyle = style)) }
                  .testTag("notif_style_${style.code}"),
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                  Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) palette.accent else palette.textPrimary,
                  )
                  Text(
                    text = style.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textMuted,
                  )
                }
              }
            }

            // Live preview of the consolidated notification for today
            val plannedToday = remember(tasks) {
              tasksPlannedForDay(tasks, System.currentTimeMillis())
            }
            val previewText = remember(plannedToday, settings.summaryStyle) {
              buildSummaryNotificationText(plannedToday, settings.summaryStyle)
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = palette.background,
              border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("notif_digest_preview"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Icon(
                  imageVector = Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = palette.accent,
                  modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "PODGLĄD POWIADOMIENIA NA DZIŚ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                  )
                  Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (editingStartTime) {
    TimePickerAlertDialog(
      title = "Godzina rozpoczęcia",
      initialHour = settings.customStartHour,
      initialMinute = settings.customStartMinute,
      onConfirm = { h, m ->
        onUpdateSettings(settings.copy(customStartHour = h, customStartMinute = m))
        editingStartTime = false
      },
      onDismiss = { editingStartTime = false },
    )
  }

  if (editingEndTime) {
    TimePickerAlertDialog(
      title = "Godzina zakończenia",
      initialHour = settings.customEndHour,
      initialMinute = settings.customEndMinute,
      onConfirm = { h, m ->
        onUpdateSettings(settings.copy(customEndHour = h, customEndMinute = m))
        editingEndTime = false
      },
      onDismiss = { editingEndTime = false },
    )
  }
}

@Composable
private fun NotifChip(
  label: String,
  selected: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (selected) palette.accent else palette.surfaceSubtle,
    border = BorderStroke(1.dp, if (selected) palette.outline else palette.divider),
    modifier = Modifier
      .heightIn(min = 38.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) Color.White else palette.textPrimary,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
    )
  }
}

@Composable
private fun TimePickPill(
  label: String,
  timeText: String,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.5f)),
    modifier = modifier
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Default.AccessTime,
        contentDescription = null,
        tint = palette.accent,
        modifier = Modifier.size(18.dp),
      )
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = palette.textMuted,
        )
        Text(
          text = timeText,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerAlertDialog(
  title: String,
  initialHour: Int,
  initialMinute: Int,
  onConfirm: (Int, Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val pickerState = rememberTimePickerState(
    initialHour = initialHour.coerceIn(0, 23),
    initialMinute = initialMinute.coerceIn(0, 59),
    is24Hour = true,
  )
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { TimePicker(state = pickerState) },
    confirmButton = {
      TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
        Text("Ustaw")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Anuluj")
      }
    },
  )
}
