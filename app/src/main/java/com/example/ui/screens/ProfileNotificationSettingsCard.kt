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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TaskEntity
import com.example.data.model.NotificationSettings
import com.example.data.model.NotificationSummaryStyle
import com.example.data.model.NotificationTimeSlot
import com.example.data.model.NotificationTriggerMode
import com.example.data.model.ReminderMode
import com.example.data.model.WEEKDAY_LABELS_PL
import com.example.data.model.buildSummaryNotificationText
import com.example.data.model.formatTimeHm
import com.example.data.model.tasksPlannedForDay
import com.example.ui.components.PixelSectionHeader
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notifications.NotificationHelper
import com.example.ui.components.PixelIcon

@Composable
fun HighContrastPixelSwitch(
  checked: Boolean,
  onCheckedChange: ((Boolean) -> Unit)?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val palette = LocalGamefyPalette.current
  val uncheckedBorder = if (palette.isDark) {
    palette.textMuted
  } else {
    palette.outline.copy(alpha = 0.72f)
  }
  val uncheckedTrack = if (palette.isDark) {
    palette.surfaceSubtle
  } else {
    palette.divider.copy(alpha = 0.6f)
  }
  val uncheckedThumb = if (palette.isDark) {
    palette.textPrimary
  } else {
    palette.outline
  }
  Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    enabled = enabled,
    modifier = modifier.heightIn(min = 48.dp),
    colors = SwitchDefaults.colors(
      checkedThumbColor = Color.White,
      checkedTrackColor = PixelColors.Green,
      checkedBorderColor = PixelColors.GreenDark,
      uncheckedThumbColor = uncheckedThumb,
      uncheckedTrackColor = uncheckedTrack,
      uncheckedBorderColor = uncheckedBorder,
      disabledCheckedThumbColor = Color.White.copy(alpha = 0.7f),
      disabledCheckedTrackColor = palette.divider,
      disabledCheckedBorderColor = palette.textMuted.copy(alpha = 0.5f),
      disabledUncheckedThumbColor = palette.textMuted.copy(alpha = 0.7f),
      disabledUncheckedTrackColor = palette.surfaceSubtle,
      disabledUncheckedBorderColor = palette.textMuted.copy(alpha = 0.5f),
    ),
  )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesBlock(
  notificationsEnabled: Boolean,
  permissionDeniedByUser: Boolean = false,
  settings: NotificationSettings,
  tasks: List<TaskEntity>,
  onToggleNotifications: (Boolean) -> Unit,
  onUpdateSettings: (NotificationSettings) -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var editingStartTime by remember { mutableStateOf(false) }
  var editingEndTime by remember { mutableStateOf(false) }

  var systemAllowed by remember {
    mutableStateOf(NotificationHelper.areSystemNotificationsEnabled(context))
  }
  var hasPostPermission by remember {
    mutableStateOf(NotificationHelper.hasNotificationPermission(context))
  }

  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        systemAllowed = NotificationHelper.areSystemNotificationsEnabled(context)
        hasPostPermission = NotificationHelper.hasNotificationPermission(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val isBlockedInSystem = !systemAllowed || (permissionDeniedByUser && !hasPostPermission)
  val effectiveEnabled = notificationsEnabled && !isBlockedInSystem

  val statusLabel = when {
    isBlockedInSystem -> "zablokowane"
    effectiveEnabled -> "włączone"
    else -> "wyłączone"
  }
  val subtitleText = when {
    isBlockedInSystem -> "Powiadomienia są wyłączone w ustawieniach systemu Android."
    effectiveEnabled -> "Przypomnienia o zadaniach działają w tle. Wybierz tryb poniżej."
    else -> "Włącz, aby przypomnienia o zadaniach przychodziły w tle."
  }

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(
          role = Role.Switch,
          onClickLabel = if (isBlockedInSystem) "Otwórz ustawienia powiadomień" else "Przełącz powiadomienia",
        ) {
          if (isBlockedInSystem) {
            NotificationHelper.openSystemNotificationSettings(context)
          } else {
            onToggleNotifications(!notificationsEnabled)
          }
        }
        .padding(vertical = 4.dp)
        .testTag("notifications_master_row"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = "Powiadomienia · $statusLabel",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
          modifier = Modifier.testTag("notifications_status_title"),
        )
        Text(
          text = subtitleText,
          style = MaterialTheme.typography.bodySmall,
          fontSize = 13.sp,
          color = palette.textMuted,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      HighContrastPixelSwitch(
        checked = effectiveEnabled,
        enabled = !isBlockedInSystem,
        onCheckedChange = { next ->
          if (isBlockedInSystem) {
            NotificationHelper.openSystemNotificationSettings(context)
          } else {
            onToggleNotifications(next)
          }
        },
        modifier = Modifier.testTag("notifications_master_switch"),
      )
    }

    AnimatedVisibility(visible = isBlockedInSystem) {
      val amberBg = if (palette.isHighContrast) {
        palette.surfaceSubtle
      } else if (palette.isDark) {
        PixelColors.AmberDarkBg
      } else {
        PixelColors.AmberLightBg
      }
      val amberInk = if (palette.isHighContrast || palette.isDark) {
        palette.textPrimary
      } else {
        PixelColors.AmberInkLight
      }
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = amberBg,
        border = BorderStroke(1.5.dp, PixelColors.Amber),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp)
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable(role = Role.Button) {
            NotificationHelper.openSystemNotificationSettings(context)
          }
          .testTag("notifications_system_blocked_banner"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          PixelIcon(icon = "warning", size = 20.dp)
          Text(
            text = "Zablokowane w systemie · Otwórz ustawienia",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = amberInk,
            modifier = Modifier.weight(1f),
          )
          Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = "Otwórz ustawienia systemowe",
            tint = amberInk,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    AnimatedVisibility(visible = effectiveEnabled) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp)
          .testTag("notification_schedule_options"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        HorizontalDivider(color = palette.divider)

        // Highlighted live preview right at the top of the notifications section
        val plannedToday = remember(tasks) {
          tasksPlannedForDay(tasks.filter { it.isActive }, System.currentTimeMillis())
        }
        val previewText = remember(plannedToday, settings.summaryStyle) {
          buildSummaryNotificationText(plannedToday, settings.summaryStyle)
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = palette.background,
          border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.55f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("notif_digest_preview"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = palette.accent,
              modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text = "Podgląd na dziś",
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
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

        // Segmented switch: Per-task vs Shared global settings
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Tryb powiadomień",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.surfaceSubtle,
            border = BorderStroke(1.5.dp, palette.divider),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("notif_mode_segmented_control"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              NotificationModeSegment(
                label = "Ustawiaj w każdym zadaniu",
                selected = settings.useTaskSettings,
                testTag = "notif_mode_per_task",
                modifier = Modifier.weight(1f),
                onClick = {
                  if (!settings.useTaskSettings) {
                    onUpdateSettings(settings.copy(useTaskSettings = true))
                  }
                },
              )
              NotificationModeSegment(
                label = "Jedne ustawienia dla wszystkich",
                selected = !settings.useTaskSettings,
                testTag = "notif_mode_global",
                modifier = Modifier.weight(1f),
                onClick = {
                  if (settings.useTaskSettings) {
                    onUpdateSettings(settings.copy(useTaskSettings = false))
                  }
                },
              )
            }
          }
        }

        // Show ONLY the active mode's settings (never render the inactive section greyed out)
        if (settings.useTaskSettings) {
          val activeWithReminder = remember(tasks) {
            tasks.count {
              it.isActive && ReminderMode.fromCode(it.reminderModeCode) != ReminderMode.NONE
            }
          }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.surfaceSubtle,
            border = BorderStroke(1.dp, palette.divider),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("notif_per_task_info_card"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.Top,
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier
                  .padding(top = 2.dp)
                  .size(18.dp),
              )
              Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = "Każde zadanie korzysta z własnego przypomnienia i godziny wybranej w edytorze zadania.",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textPrimary,
                )
                Text(
                  text = "Aktywne zadania z włączonym przypomnieniem: $activeWithReminder",
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.textMuted,
                )
              }
            }
          }
        } else {
          // Shared schedule settings (visible ONLY when "Jedne ustawienia dla wszystkich" is selected)
          Column(
            modifier = Modifier.testTag("notif_global_settings_section"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            PixelSectionHeader(
              title = "Ustawienia wspólne",
              titleColor = palette.textPrimary,
            )
            // 1. Dni tygodnia
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Dni powiadomień",
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
        }

        HorizontalDivider(color = palette.divider)

        // Daily cap (applies to both modes)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Maksymalna liczba powiadomień dziennie",
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
              1 to "Do 1",
              2 to "Do 2",
              3 to "Do 3",
              5 to "Do 5",
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

        // Consolidated notification style when daily limit is enabled
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
private fun NotificationModeSegment(
  label: String,
  selected: Boolean,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(9.dp),
    color = if (selected) palette.accent else Color.Transparent,
    border = if (selected && palette.isHighContrast) BorderStroke(1.5.dp, palette.outline) else null,
    modifier = modifier
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(9.dp))
      .clickable(role = Role.Tab, onClick = onClick)
      .testTag(testTag),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) palette.onAccent else palette.textPrimary,
        textAlign = TextAlign.Center,
        maxLines = 2,
      )
    }
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
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) palette.onAccent else palette.textPrimary,
      )
    }
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
