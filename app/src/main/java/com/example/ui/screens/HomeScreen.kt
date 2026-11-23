package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.TaskEntity
import com.example.data.local.parseStringCsv
import com.example.data.model.OnboardingDetailPref
import com.example.data.model.RANDOM_SIDE_QUEST_DAILY_LIMIT
import com.example.data.model.localCalendarDayKey
import com.example.ui.GamefyUiState
import com.example.ui.MainTimerMode
import com.example.ui.MainTimerState
import com.example.ui.PomodoroPhase
import com.example.ui.TaskInlineTimerState
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelEmptyStateCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.PixelSectionHeader
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
  onCreateNewTask: () -> Unit = {},
  onOpenReadyTasksDb: () -> Unit = {},
  onOpenRandomQuest: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val now = System.currentTimeMillis()
  var expandedTaskId by remember { mutableStateOf<Long?>(null) }
  var isTimerExpanded by rememberSaveable { mutableStateOf(true) }

  val urgentTasks = remember(uiState.tasks, now) {
    uiState.tasks
      .filter { it.isActive && it.startTimestamp <= now }
      .sortedBy { it.dueTimestamp ?: Long.MAX_VALUE }
      .take(8)
  }

  val acceptedSideQuestsToday = remember(
    uiState.profile.rsqBoardDayKey,
    uiState.profile.rsqAcceptedIdsCsv,
    uiState.profile.rsqAcceptedCountToday,
    now,
  ) {
    val todayKey = localCalendarDayKey(now)
    if (uiState.profile.rsqBoardDayKey == todayKey) {
      maxOf(
        uiState.profile.rsqAcceptedCountToday,
        parseStringCsv(uiState.profile.rsqAcceptedIdsCsv).size,
      ).coerceIn(0, RANDOM_SIDE_QUEST_DAILY_LIMIT)
    } else {
      0
    }
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
      item(key = "home_main_timer") {
        Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
          if (isTimerExpanded) {
            MainTimerCard(
              timerState = timerState,
              onSelectMode = onSelectTimerMode,
              onSelectPreset = onSelectTimerPreset,
              onUpdateCustomDuration = onUpdateCustomDuration,
              onTogglePlayPause = onToggleMainTimer,
              onSkip = onSkipMainTimer,
              onReset = onResetMainTimer,
              onCollapse = { isTimerExpanded = false },
            )
          } else {
            CollapsedMainTimerButton(
              timerState = timerState,
              onExpand = { isTimerExpanded = true },
            )
          }
        }
      }

      item {
        val bannerBg = if (palette.isHighContrast) {
          palette.surface
        } else if (palette.isDark) {
          PixelColors.AmberDarkBg
        } else {
          PixelColors.Gold.copy(alpha = 0.22f)
        }
        val bannerBorder = if (palette.isHighContrast) palette.outline else PixelColors.Amber
        val remainingToday = (RANDOM_SIDE_QUEST_DAILY_LIMIT - acceptedSideQuestsToday).coerceAtLeast(0)
        val subtitleText = if (remainingToday > 0) {
          "Nowe zadania poboczne · Przyjęte dziś: $acceptedSideQuestsToday/$RANDOM_SIDE_QUEST_DAILY_LIMIT"
        } else {
          "Przyjęte dziś: $acceptedSideQuestsToday/$RANDOM_SIDE_QUEST_DAILY_LIMIT · nowe jutro"
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = bannerBg,
          border = BorderStroke(1.5.dp, bannerBorder),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
              role = Role.Button,
              onClickLabel = "Otwórz Tablicę Przygód",
              onClick = onOpenRandomQuest,
            )
            .testTag("home_adventure_board_card"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            PixelIcon(icon = "sword", size = 26.dp)
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = "Tablica Przygód",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
              )
              Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.5.sp,
                color = palette.textMuted,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = palette.textPrimary,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }

      item {
        PixelSectionHeader(
          title = "Najbliższe terminy",
          badgeText = if (urgentTasks.isNotEmpty()) "${urgentTasks.size}" else null,
          modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )
      }

      if (urgentTasks.isEmpty()) {
        item {
          PixelEmptyStateCard(
            illustrationRes = R.drawable.img_empty_tasks,
            illustrationContentDescription = "Ilustracja pustej tablicy zadań ze zwojem",
            title = "Brak aktywnych zadań!",
            subtitle = "Wszystko zrobione albo zaplanowane na kolejne dni. Dodaj nowe zadanie lub wybierz gotowe z bazy.",
            primaryActionLabel = "Dodaj zadanie",
            onPrimaryAction = onCreateNewTask,
            primaryActionTestTag = "home_empty_add_task_button",
            secondaryActionLabel = "Przeglądaj bazę zadań",
            onSecondaryAction = onOpenReadyTasksDb,
            secondaryActionTestTag = "home_empty_browse_db_button",
          )
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

/** Kompaktowy, pojedynczy przycisk w miejscu dużego kafelka. */
@Composable
private fun CollapsedMainTimerButton(
  timerState: MainTimerState,
  onExpand: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val shape = RoundedCornerShape(12.dp)
  val remainingTime = if (timerState.mode == MainTimerMode.TIMER || timerState.remainingSeconds >= 3600) {
    String.format(
      "%02d:%02d:%02d",
      timerState.remainingSeconds / 3600,
      (timerState.remainingSeconds % 3600) / 60,
      timerState.remainingSeconds % 60,
    )
  } else {
    String.format(
      "%02d:%02d",
      timerState.remainingSeconds / 60,
      timerState.remainingSeconds % 60,
    )
  }

  Row(
    modifier = Modifier
      .clip(shape)
      .background(Brush.horizontalGradient(listOf(palette.accent, palette.accentSecondary)))
      .border(2.dp, palette.outline, shape)
      .clickable(
        role = Role.Button,
        onClickLabel = "Rozwiń ${timerState.mode.label}",
        onClick = onExpand,
      )
      .testTag("main_timer_expand")
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PixelIcon(icon = "timer", size = 20.dp, monochromeTint = Color.White)
    Text(
      text = timerState.mode.label,
      fontFamily = PixelFontFamily,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White,
    )
    // Odliczanie jest nadal widoczne, gdy timer pracuje w tle.
    if (timerState.isRunning) {
      Text(
        text = remainingTime,
        fontFamily = BodyFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )
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
  onCollapse: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val shape = RoundedCornerShape(22.dp)
  var isTimerEditorVisible by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(timerState.mode, timerState.isRunning) {
    if (timerState.mode != MainTimerMode.TIMER || timerState.isRunning) {
      isTimerEditorVisible = false
    }
  }

  val isPaused = !timerState.isRunning &&
    timerState.remainingSeconds in 1 until timerState.totalSeconds
  val isCountdownActive = timerState.isRunning || isPaused

  // Active countdown ring color with high contrast against every theme's card gradient
  val activeRingColor = if (palette.themeId == com.example.data.model.AppThemeId.HIGH_CONTRAST_NIGHT) {
    Color(0xFF1A1A1A)
  } else {
    PixelColors.Gold
  }
  val ringBaseColor by animateColorAsState(
    targetValue = if (isCountdownActive) activeRingColor else Color.White,
    animationSpec = tween(durationMillis = 320),
    label = "timer_ring_base_color",
  )
  val animatedElapsedFraction by animateFloatAsState(
    targetValue = if (isCountdownActive) timerState.progressFraction else 0f,
    animationSpec = tween(durationMillis = 280),
    label = "timer_ring_elapsed_fraction",
  )

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
      // Top row: [Pomodoro | Timer] switcher + compact collapse button right next to "Timer"
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          MainTimerMode.entries.forEach { mode ->
            val selected = timerState.mode == mode
            Box(
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (selected) Color.White else Color.Transparent)
                .clickable { onSelectMode(mode) }
                .testTag("main_timer_mode_${mode.name.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = mode.label,
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (selected) palette.accent else Color.White,
              )
            }
          }
        }

        Row(
          modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .clickable(
              role = Role.Button,
              onClickLabel = "Zwiń Pomodoro / Timer",
              onClick = onCollapse,
            )
            .testTag("main_timer_collapse")
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Zwiń",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color.White,
          )
        }
      }

      if (timerState.mode == MainTimerMode.TIMER) {
        // Timer mode: preset chips (15 / 25 / 45 / 60 min); HH:MM:SS editor appears only after tapping the dial
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

        AnimatedVisibility(
          visible = isTimerEditorVisible && !timerState.isRunning,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("timer_custom_hhmmss_editor"),
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
      }

      // Big Circular Timer Ring: white when ready -> switches to active color on start -> fills back to white as time reaches 00:00:00
      val canTapDialToEdit = timerState.mode == MainTimerMode.TIMER && !timerState.isRunning
      Box(
        modifier = Modifier
          .size(200.dp)
          .clip(CircleShape)
          .then(
            if (canTapDialToEdit) {
              Modifier.clickable(
                role = Role.Button,
                onClickLabel = if (isTimerEditorVisible) "Ukryj edycję czasu" else "Edytuj czas timera",
                onClick = { isTimerEditorVisible = !isTimerEditorVisible },
              )
            } else {
              Modifier
            }
          )
          .testTag("main_timer_dial"),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
          val strokeWidth = 12.dp.toPx()
          // Full 360° base ring: White when idle/finished, Active Color when countdown is underway
          drawArc(
            color = ringBaseColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
          )
          // Elapsed portion returns progressively to original White until 100% white at 00:00:00
          if (isCountdownActive && animatedElapsedFraction > 0.001f) {
            drawArc(
              color = Color.White,
              startAngle = -90f,
              sweepAngle = (animatedElapsedFraction * 360f).coerceIn(0f, 360f),
              useCenter = false,
              style = Stroke(
                width = strokeWidth,
                cap = if (animatedElapsedFraction >= 0.995f) StrokeCap.Butt else StrokeCap.Round,
              ),
            )
          }
        }

        val hrs = timerState.remainingSeconds / 3600
        val mins = (timerState.remainingSeconds % 3600) / 60
        val secs = timerState.remainingSeconds % 60
        val timeText = if (hrs > 0 || timerState.mode == MainTimerMode.TIMER) {
          String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
          String.format("%02d:%02d", mins, secs)
        }

        val insideBadgeText = if (timerState.mode == MainTimerMode.POMODORO) {
          val baseSessionLabel = when (timerState.pomodoroPhase) {
            PomodoroPhase.FOCUS -> "Sesja ${timerState.completedFocusCount + 1}"
            PomodoroPhase.SHORT_BREAK -> "Krótka przerwa"
            PomodoroPhase.LONG_BREAK -> "Długa przerwa"
          }
          if (isPaused) "$baseSessionLabel · Pauza" else baseSessionLabel
        } else {
          when {
            timerState.isRunning -> "Odliczanie"
            isTimerEditorVisible -> "Ukryj edycję"
            isPaused -> "Pauza · Edytuj"
            else -> "Edytuj czas"
          }
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(
              fontFamily = BodyFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = if (timeText.length > 5) 32.sp else 46.sp,
              fontFeatureSettings = "tnum",
            ),
            color = Color.White,
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.20f),
            modifier = Modifier.testTag("main_timer_session_label"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (canTapDialToEdit && !isTimerEditorVisible) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(12.dp),
                )
              }
              Text(
                text = insideBadgeText,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = Color.White,
              )
            }
          }
        }
      }

      // Controls: Play/Pause, Skip, Square Reset
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PixelButton(
          text = if (timerState.isRunning) "Zatrzymaj" else "Start",
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
          text = "Pomiń fazę",
          containerColor = PixelColors.Gold,
          contentColor = palette.outline,
          modifier = Modifier.testTag("main_timer_skip"),
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = null,
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
              contentDescription = "Zresetuj",
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
