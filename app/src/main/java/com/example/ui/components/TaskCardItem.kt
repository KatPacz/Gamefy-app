package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AttributeEntity
import com.example.data.local.FolderEntity
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.parseIdCsv
import com.example.data.local.parseSubtasksJson
import com.example.data.model.MILLIS_PER_DAY
import com.example.data.model.PenaltyMode
import com.example.data.model.TaskCategory
import com.example.data.model.TaskDifficulty
import com.example.data.model.formatDurationShort
import com.example.data.model.formatFrequencyLabel
import com.example.ui.TaskInlineTimerState
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCardItem(
  task: TaskEntity,
  folders: List<FolderEntity>,
  attributes: List<AttributeEntity>,
  tags: List<TagEntity>,
  isActive: Boolean,
  isExpanded: Boolean,
  isBulkMode: Boolean,
  isSelected: Boolean,
  inlineTimer: TaskInlineTimerState,
  onToggleExpand: () -> Unit,
  onComplete: () -> Unit,
  onToggleSubtask: (Long) -> Unit,
  onSkip: () -> Unit,
  onEdit: () -> Unit,
  onToggleSelect: () -> Unit,
  onToggleTaskTimer: () -> Unit,
  onResetTaskTimer: () -> Unit,
  modifier: Modifier = Modifier,
  showInlineTimer: Boolean = true,
) {
  val palette = LocalGamefyPalette.current
  val uriHandler = LocalUriHandler.current
  val now = System.currentTimeMillis()
  val folder = folders.find { it.id == task.folderId }
  val taskAttrs = parseIdCsv(task.attributeIdsCsv).mapNotNull { id -> attributes.find { it.id == id } }
  val taskTags = parseIdCsv(task.tagIdsCsv).mapNotNull { id -> tags.find { it.id == id } }
  val subtasks = remember(task.subtasksJson) { parseSubtasksJson(task.subtasksJson) }
  val category = TaskCategory.fromCode(task.categoryCode)
  val difficulty = TaskDifficulty.fromCode(task.difficultyCode)
  val penaltyMode = PenaltyMode.fromCode(task.penaltyModeCode)

  var showCompleteAllDialog by remember { mutableStateOf(false) }
  var isAnimatingComplete by remember(task.id, task.completedCount, task.startTimestamp) {
    mutableStateOf(false)
  }

  LaunchedEffect(isAnimatingComplete) {
    if (isAnimatingComplete) {
      delay(380L)
      onComplete()
    }
  }

  val due = task.dueTimestamp
  val dueRemaining = due?.minus(now)
  val statusText: String
  val statusBg: Color
  val statusFg: Color

  when {
    !isActive -> {
      val untilStart = (task.startTimestamp - now).coerceAtLeast(0L)
      statusText = "za ${formatDurationShort(untilStart)}"
      statusBg = palette.surfaceSubtle
      statusFg = palette.textMuted
    }
    dueRemaining == null -> {
      statusText = "bez terminu"
      statusBg = palette.surfaceSubtle
      statusFg = palette.textMuted
    }
    dueRemaining < 0L -> {
      statusText = "spóźnione ${formatDurationShort(dueRemaining)}"
      statusBg = PixelColors.Red.copy(alpha = 0.16f)
      statusFg = PixelColors.Red
    }
    dueRemaining < MILLIS_PER_DAY -> {
      statusText = formatDurationShort(dueRemaining)
      statusBg = PixelColors.Warning.copy(alpha = 0.16f)
      statusFg = PixelColors.Warning
    }
    else -> {
      statusText = formatDurationShort(dueRemaining)
      statusBg = palette.surfaceSubtle
      statusFg = palette.textMuted
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (isBulkMode) {
        Checkbox(
          checked = isSelected,
          onCheckedChange = { onToggleSelect() },
          modifier = Modifier.testTag("task_select_${task.id}"),
        )
      } else if (isActive) {
        PixelCheckbox(
          checked = isAnimatingComplete,
          onClick = {
            if (isAnimatingComplete) return@PixelCheckbox
            val hasUncheckedSubtasks = subtasks.any { !it.isDone }
            if (hasUncheckedSubtasks) {
              showCompleteAllDialog = true
            } else {
              isAnimatingComplete = true
            }
          },
          modifier = Modifier.testTag("task_done_${task.id}"),
        )
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(10.dp))
          .clickable(onClick = onToggleExpand)
          .padding(vertical = 4.dp),
      ) {
        Text(
          text = task.name,
          style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Bold,
            textDecoration = if (isAnimatingComplete) TextDecoration.LineThrough else TextDecoration.None,
          ),
          color = if (isAnimatingComplete) palette.textMuted else palette.textPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (folder != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              PixelIcon(icon = folder.icon, size = 14.dp)
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = folder.name,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
              )
            }
          }
          Text(
            text = formatFrequencyLabel(category, task.repeatInterval),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
          )
          taskAttrs.forEach { attr ->
            PixelIcon(icon = attr.icon, size = 15.dp)
          }
          Text(
            text = "+${task.expReward} EXP · +${task.coinReward}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textMuted,
          )
          PixelIcon(icon = "coin", size = 14.dp)
          if (task.gemReward > 0) {
            Text(
              text = "+${task.gemReward}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = PixelColors.GemBlue,
            )
            PixelIcon(icon = "gem", size = 14.dp)
          }
          val diffColor = when (difficulty) {
            TaskDifficulty.VERY_EASY -> PixelColors.Green
            TaskDifficulty.EASY -> PixelColors.GemBlue
            TaskDifficulty.MEDIUM -> palette.accent
            TaskDifficulty.HARD -> PixelColors.Warning
            TaskDifficulty.EXTREME -> PixelColors.Red
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = diffColor.copy(alpha = 0.14f),
          ) {
            Text(
              text = difficulty.label,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = diffColor,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
          }
          if (task.rewardItemId > 0L) {
            PixelIcon(icon = "gift", size = 14.dp)
          }
          if (task.repeatLimit > 0) {
            Text(
              text = "${task.completedCount}/${task.repeatLimit}",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textMuted,
            )
          }
        }

        if (taskTags.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            taskTags.take(4).forEach { tag ->
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = palette.surfaceSubtle,
              ) {
                Text(
                  text = "#${tag.name}",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.accent,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
            if (taskTags.size > 4) {
              Text(
                text = "+${taskTags.size - 4}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = statusBg,
      ) {
        Text(
          text = statusText,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = statusFg,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }

    // Subtasks rendered directly below the main task as smaller indented list items
    if (subtasks.isNotEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 34.dp, top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        subtasks.forEach { sub ->
          val visuallyChecked = sub.isDone || isAnimatingComplete
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable(enabled = isActive && !isAnimatingComplete) {
                onToggleSubtask(sub.id)
              }
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            PixelCheckbox(
              checked = visuallyChecked,
              onClick = {
                if (isActive && !isAnimatingComplete) {
                  onToggleSubtask(sub.id)
                }
              },
              boxSize = 20.dp,
              touchTargetSize = 36.dp,
              modifier = Modifier.testTag("subtask_check_${task.id}_${sub.id}"),
            )
            Text(
              text = sub.title,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                textDecoration = if (visuallyChecked) TextDecoration.LineThrough else TextDecoration.None,
              ),
              color = if (visuallyChecked) palette.textMuted else palette.textPrimary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    AnimatedVisibility(visible = isExpanded) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = palette.background,
        border = BorderStroke(1.5.dp, palette.divider),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (task.description.isNotBlank()) {
            LinkifiedText(text = task.description)
          }

          if (task.link.isNotBlank()) {
            val normalizedUrl = if (
              task.link.startsWith("http://", ignoreCase = true) ||
              task.link.startsWith("https://", ignoreCase = true)
            ) {
              task.link
            } else {
              "https://${task.link}"
            }
            PixelButton(
              text = "Otwórz link",
              compact = true,
              containerColor = palette.surfaceSubtle,
              contentColor = palette.accent,
              leadingIcon = {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                  contentDescription = null,
                  tint = palette.accent,
                  modifier = Modifier.size(16.dp),
                )
              },
              onClick = { runCatching { uriHandler.openUri(normalizedUrl) } },
            )
          }

          if (showInlineTimer && isActive) {
            val isThisTask = inlineTimer.taskId == task.id
            val effectiveMinutes = if (task.durationMinutes > 0) task.durationMinutes else 25
            val displaySeconds = if (isThisTask) inlineTimer.remainingSeconds else effectiveMinutes * 60
            val mm = displaySeconds / 60
            val ss = displaySeconds % 60
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  text = if (task.durationMinutes > 0) {
                    "CZASOMIERZ ZADANIA"
                  } else {
                    "CZASOMIERZ ZADANIA (DOM. 25 MIN)"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.textMuted,
                )
                Text(
                  text = String.format("%02d:%02d", mm, ss),
                  fontFamily = BodyFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 24.sp,
                  style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                  color = palette.textPrimary,
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                PixelButton(
                  text = if (isThisTask && inlineTimer.isRunning) "Pauza" else "Start",
                  compact = true,
                  modifier = Modifier.testTag("task_timer_start_${task.id}"),
                  leadingIcon = {
                    Icon(
                      imageVector = if (isThisTask && inlineTimer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(16.dp),
                    )
                  },
                  onClick = onToggleTaskTimer,
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                  onClick = onResetTaskTimer,
                  modifier = Modifier.size(40.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Resetuj czasomierz zadania",
                    tint = palette.textMuted,
                  )
                }
              }
            }
          }

          if (penaltyMode == PenaltyMode.ON_SKIP_ONLY) {
            val range = if (task.penaltyMaxCoins > task.penaltyMinCoins) {
              "${task.penaltyMinCoins}–${task.penaltyMaxCoins}"
            } else {
              "${task.penaltyMinCoins}"
            }
            Text(
              text = "Kara za pominięcie: $range monet",
              style = MaterialTheme.typography.bodySmall,
              color = PixelColors.Red,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PixelButton(
              text = "Edytuj",
              compact = true,
              containerColor = palette.surface,
              contentColor = palette.textPrimary,
              modifier = Modifier
                .weight(1f)
                .testTag("edit_task_${task.id}"),
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = null,
                  tint = palette.textPrimary,
                  modifier = Modifier.size(16.dp),
                )
              },
              onClick = onEdit,
            )
            if (isActive) {
              PixelButton(
                text = "Pomiń",
                compact = true,
                containerColor = palette.surfaceSubtle,
                contentColor = palette.textPrimary,
                modifier = Modifier
                  .weight(1f)
                  .testTag("skip_task_${task.id}"),
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = palette.textPrimary,
                    modifier = Modifier.size(16.dp),
                  )
                },
                onClick = onSkip,
              )
            }
          }
        }
      }
    }
  }

  if (showCompleteAllDialog) {
    AlertDialog(
      onDismissRequest = { showCompleteAllDialog = false },
      title = { Text("Zakończyć całe zadanie?") },
      text = {
        Text("Chcesz zaznaczyć wszystkie podrzędne zadania jako wykonane?")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showCompleteAllDialog = false
            isAnimatingComplete = true
          },
        ) {
          Text("Tak, zaznacz wszystkie")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCompleteAllDialog = false }) {
          Text("Anuluj")
        }
      },
    )
  }
}
