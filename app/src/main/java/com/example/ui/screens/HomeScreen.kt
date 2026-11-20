package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TaskEntity
import com.example.data.model.OnboardingDetailPref
import com.example.ui.GamefyUiState
import com.example.ui.MainTimerMode
import com.example.ui.MainTimerState
import com.example.ui.TaskInlineTimerState
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.TaskCardItem
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily

@Composable
fun HomeScreen(
  uiState: GamefyUiState,
  timerState: MainTimerState,
  inlineTimer: TaskInlineTimerState,
  onSelectTimerMode: (MainTimerMode) -> Unit,
  onSelectTimerPreset: (Int) -> Unit,
  onUpdateCustomDuration: (Int, Int, Int) -> Unit,
  onToggleMainTimer: () -> Unit,
  onSkipMainTimer: () -> Unit,
  onResetMainTimer: () -> Unit,
  onCompleteTask: (TaskEntity) -> Unit,
  onToggleSubtask: (TaskEntity, Long) -> Unit,
  onSkipTask: (TaskEntity) -> Unit,
  onGiveUpTask: (TaskEntity) -> Unit = {},
  onEditTask: (Long) -> Unit,
  onToggleTaskTimer: (Long, Int) -> Unit,
  onResetTaskTimer: (Long, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val now = System.currentTimeMillis()
  var expandedTaskId by remember { mutableStateOf<Long?>(null) }

  val urgentTasks = remember(uiState.tasks, now) {
    uiState.tasks
      .filter { it.isActive && it.startTimestamp <= now }
      .sortedBy { it.dueTimestamp ?: Long.MAX_VALUE }
      .take(8)
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        MainTimerCard(
          timerState = timerState,
          onSelectMode = onSelectTimerMode,
          onSelectPreset = onSelectTimerPreset,
          onUpdateCustomDuration = onUpdateCustomDuration,
          onTogglePlayPause = onToggleMainTimer,
          onSkip = onSkipMainTimer,
          onReset = onResetMainTimer,
        )
      }

      item {
        Text(
          text = "NAJBLIŻSZE TERMINY",
          style = MaterialTheme.typography.titleSmall,
          color = palette.textMuted,
          modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )
      }

      if (urgentTasks.isEmpty()) {
        item {
          PixelCard {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              PixelIcon(icon = "trophy", size = 44.dp)
              Text(
                text = "Brak aktywnych zadań!",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
              )
              Text(
                text = "Wszystko zrobione albo zaplanowane na kolejne dni.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      } else {
        item {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            urgentTasks.forEach { task ->
              TaskCardItem(
                task = task,
                folders = uiState.folders,
                attributes = uiState.attributes,
                tags = uiState.tags,
                isActive = true,
                isExpanded = expandedTaskId == task.id,
                isBulkMode = false,
                isSelected = false,
                inlineTimer = inlineTimer,
                onToggleExpand = {
                  expandedTaskId = if (expandedTaskId == task.id) null else task.id
                },
                onComplete = { onCompleteTask(task) },
                onToggleSubtask = { subId -> onToggleSubtask(task, subId) },
                onSkip = { onSkipTask(task) },
                onGiveUp = { onGiveUpTask(task) },
                onEdit = { onEditTask(task.id) },
                onToggleSelect = {},
                onToggleTaskTimer = { onToggleTaskTimer(task.id, task.durationMinutes) },
                onResetTaskTimer = { onResetTaskTimer(task.id, task.durationMinutes) },
                detailPref = OnboardingDetailPref.fromId(uiState.profile.onboardingDetailPref),
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun MainTimerCard(
  timerState: MainTimerState,
  onSelectMode: (MainTimerMode) -> Unit,
  onSelectPreset: (Int) -> Unit,
  onUpdateCustomDuration: (Int, Int, Int) -> Unit,
  onTogglePlayPause: () -> Unit,
  onSkip: () -> Unit,
  onReset: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val shape = RoundedCornerShape(22.dp)

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(Brush.linearGradient(listOf(palette.accent, palette.accentSecondary)))
      .border(2.5.dp, palette.outline, shape)
      .padding(18.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Mode Switcher: Tryb Pomodoro vs Timer
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color.Black.copy(alpha = 0.18f))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        MainTimerMode.entries.forEach { mode ->
          val selected = timerState.mode == mode
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(9.dp))
              .background(if (selected) Color.White else Color.Transparent)
              .clickable { onSelectMode(mode) }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = mode.label.uppercase(),
              fontFamily = PixelFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (selected) palette.accent else Color.White,
            )
          }
        }
      }

      if (timerState.mode == MainTimerMode.POMODORO) {
        // Standard classic Pomodoro cycle: Praca (25m) -> Krótka przerwa (5m) -> Długa przerwa (15m)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color.White.copy(alpha = 0.22f),
        ) {
          Text(
            text = "${timerState.pomodoroPhase.label.uppercase()} · SESJA #${timerState.completedFocusCount + 1}",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.6.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }
      } else {
        // Timer mode: preset chips (15 / 25 / 45 / 60 min) + HH:MM:SS custom steppers
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(15, 25, 45, 60).forEach { minutes ->
            val selected = timerState.customHours == minutes / 60 &&
              timerState.customMinutes == minutes % 60 &&
              timerState.customSeconds == 0
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
              border = BorderStroke(
                1.5.dp,
                if (selected) palette.outline else Color.White.copy(alpha = 0.4f),
              ),
              modifier = Modifier
                .clickable(enabled = !timerState.isRunning) { onSelectPreset(minutes) }
                .testTag("timer_preset_$minutes"),
            ) {
              Text(
                text = "$minutes min",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (selected) palette.accent else Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TimeUnitStepper(
            label = "HH",
            value = timerState.customHours,
            enabled = !timerState.isRunning,
            onDelta = { delta ->
              onUpdateCustomDuration(
                (timerState.customHours + delta).coerceIn(0, 23),
                timerState.customMinutes,
                timerState.customSeconds,
              )
            },
          )
          Text(
            text = ":",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White,
          )
          TimeUnitStepper(
            label = "MM",
            value = timerState.customMinutes,
            enabled = !timerState.isRunning,
            onDelta = { delta ->
              onUpdateCustomDuration(
                timerState.customHours,
                (timerState.customMinutes + delta).coerceIn(0, 59),
                timerState.customSeconds,
              )
            },
          )
          Text(
            text = ":",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White,
          )
          TimeUnitStepper(
            label = "SS",
            value = timerState.customSeconds,
            enabled = !timerState.isRunning,
            onDelta = { delta ->
              onUpdateCustomDuration(
                timerState.customHours,
                timerState.customMinutes,
                (timerState.customSeconds + delta).coerceIn(0, 59),
              )
            },
          )
        }
      }

      // Big Circular Timer Ring with crisp readable tabular digits
      Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center,
      ) {
        val progress = timerState.progressFraction
        Canvas(modifier = Modifier.fillMaxSize()) {
          val strokeWidth = 12.dp.toPx()
          drawArc(
            color = Color.White.copy(alpha = 0.24f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
          drawArc(
            color = Color.White,
            startAngle = -90f,
            sweepAngle = (1f - progress) * 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
        }

        val hrs = timerState.remainingSeconds / 3600
        val mins = (timerState.remainingSeconds % 3600) / 60
        val secs = timerState.remainingSeconds % 60
        val timeText = if (hrs > 0 || timerState.mode == MainTimerMode.TIMER) {
          String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
          String.format("%02d:%02d", mins, secs)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(
              fontFamily = BodyFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = if (timeText.length > 5) 34.sp else 46.sp,
              fontFeatureSettings = "tnum",
            ),
            color = Color.White,
          )
          Text(
            text = if (timerState.isRunning) "TRWA ODLICZANIE" else "GOTOWY",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = Color.White.copy(alpha = 0.88f),
          )
        }
      }

      // Controls: Play/Pause, Skip, Square Restart
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PixelButton(
          text = if (timerState.isRunning) "Pauza" else "Start",
          containerColor = Color.White,
          contentColor = palette.accent,
          modifier = Modifier
            .weight(1f)
            .testTag("main_timer_play_pause"),
          leadingIcon = {
            Icon(
              imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = null,
              tint = palette.accent,
              modifier = Modifier.size(20.dp),
            )
          },
          onClick = onTogglePlayPause,
        )

        PixelButton(
          text = "Pomiń",
          containerColor = PixelColors.Gold,
          contentColor = palette.outline,
          modifier = Modifier.testTag("main_timer_skip"),
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = "Pomiń fazę",
              tint = palette.outline,
              modifier = Modifier.size(20.dp),
            )
          },
          onClick = onSkip,
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color.White.copy(alpha = 0.24f),
          border = BorderStroke(2.5.dp, palette.outline),
          modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onReset)
            .testTag("main_timer_reset"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Stop,
              contentDescription = "Restartuj timer",
              tint = Color.White,
              modifier = Modifier.size(26.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TimeUnitStepper(
  label: String,
  value: Int,
  enabled: Boolean,
  onDelta: (Int) -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(Color.White.copy(alpha = 0.16f))
      .padding(horizontal = 6.dp, vertical = 4.dp),
  ) {
    Text(
      text = label,
      fontFamily = BodyFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 11.sp,
      color = Color.White.copy(alpha = 0.85f),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = { onDelta(-1) },
        enabled = enabled,
        modifier = Modifier.size(32.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Remove,
          contentDescription = "Zmniejsz $label",
          tint = Color.White,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = String.format("%02d", value),
        style = MaterialTheme.typography.titleMedium.copy(
          fontFamily = BodyFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          fontFeatureSettings = "tnum",
        ),
        color = Color.White,
        modifier = Modifier.width(30.dp),
        textAlign = TextAlign.Center,
      )
      IconButton(
        onClick = { onDelta(1) },
        enabled = enabled,
        modifier = Modifier.size(32.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Zwiększ $label",
          tint = Color.White,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}
