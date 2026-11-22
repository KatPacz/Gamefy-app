package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TaskEntity
import com.example.data.model.GEM_COIN_EQUIVALENT
import com.example.data.model.OnboardingDetailPref
import com.example.data.model.TaskDifficulty
import com.example.data.model.TaskSortDirection
import com.example.data.model.TaskViewMode
import com.example.data.model.addLocalDaysMillis
import com.example.data.model.buildMonthCalendarGrid
import com.example.data.model.sortLabelFor
import com.example.data.model.startOfLocalDayMillis
import com.example.data.model.taskRewardSortValue
import com.example.data.model.tasksDueOnDay
import com.example.ui.GamefyUiState
import com.example.ui.TaskInlineTimerState
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.TaskCardItem
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun viewModeIcon(mode: TaskViewMode): ImageVector = when (mode) {
  TaskViewMode.TIME_LEFT -> Icons.Default.Schedule
  TaskViewMode.CATEGORY -> Icons.Default.Category
  TaskViewMode.CALENDAR -> Icons.Default.CalendarMonth
  TaskViewMode.THREE_DAYS -> Icons.Default.DateRange
  TaskViewMode.WEEK -> Icons.Default.ViewWeek
  TaskViewMode.TWO_WEEKS -> Icons.Default.DateRange
  TaskViewMode.DIFFICULTY -> Icons.Default.Shield
  TaskViewMode.REWARD -> Icons.Default.EmojiEvents
}

@Composable
fun TaskViewAndSortBar(
  selectedView: TaskViewMode,
  sortDirection: TaskSortDirection,
  onSelectView: (TaskViewMode) -> Unit,
  onToggleSortDirection: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  var menuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 640.dp)
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(modifier = Modifier.weight(1f)) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.6f)),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(10.dp))
          .clickable { menuExpanded = true }
          .testTag("task_view_selector"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = viewModeIcon(selectedView),
            contentDescription = "Widok zadań",
            tint = palette.accent,
            modifier = Modifier.size(18.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Widok",
              style = MaterialTheme.typography.labelSmall,
              color = palette.textMuted,
            )
            Text(
              text = selectedView.label,
              fontFamily = PixelFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = palette.textPrimary,
          )
        }
      }

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
      ) {
        TaskViewMode.entries.forEach { mode ->
          DropdownMenuItem(
            leadingIcon = {
              Icon(
                imageVector = viewModeIcon(mode),
                contentDescription = null,
                tint = if (mode == selectedView) palette.accent else palette.textMuted,
              )
            },
            text = {
              Text(
                text = mode.label,
                fontWeight = if (mode == selectedView) FontWeight.Bold else FontWeight.Normal,
                color = if (mode == selectedView) palette.accent else palette.textPrimary,
              )
            },
            onClick = {
              onSelectView(mode)
              menuExpanded = false
            },
            modifier = Modifier.testTag("view_option_${mode.code}"),
          )
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(10.dp),
      color = palette.surfaceSubtle,
      border = BorderStroke(1.5.dp, palette.divider),
      modifier = Modifier
        .weight(1f)
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onToggleSortDirection)
        .testTag("task_sort_toggle"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.SwapVert,
          contentDescription = "Zmień kierunek sortowania",
          tint = palette.accent,
          modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Sortowanie",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted,
          )
          Text(
            text = sortLabelFor(selectedView, sortDirection),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun CollapsibleTaskSectionHeader(
  title: String,
  count: Int,
  expanded: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  accentColor: Color? = null,
  testTag: String? = null,
) {
  val palette = LocalGamefyPalette.current
  val headerColor = accentColor ?: palette.textPrimary
  val badgeTint = accentColor ?: palette.textMuted

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (expanded) Color.Transparent else palette.surface,
    border = if (expanded) null else BorderStroke(1.5.dp, palette.divider),
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(
        role = Role.Button,
        onClickLabel = if (expanded) "Zwiń sekcję $title" else "Rozwiń sekcję $title",
        onClick = onToggle,
      )
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = if (expanded) 4.dp else 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = title,
          fontFamily = PixelFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = headerColor,
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
      }
      Spacer(modifier = Modifier.width(8.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = badgeTint.copy(alpha = 0.14f),
        ) {
          Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = badgeTint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (expanded) "Zwiń sekcję $title" else "Rozwiń sekcję $title",
          tint = badgeTint,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}

fun LazyListScope.taskGroupSectionItems(
  sectionKey: String,
  header: String,
  headerAccent: Color? = null,
  expanded: Boolean = true,
  onToggleSection: () -> Unit = {},
  tasks: List<TaskEntity>,
  now: Long,
  uiState: GamefyUiState,
  expandedTaskId: Long?,
  isBulkMode: Boolean,
  selectedTaskIds: Set<Long>,
  inlineTimer: TaskInlineTimerState,
  showRewardWorthBadge: Boolean = false,
  onToggleExpand: (Long) -> Unit,
  onCompleteTask: (TaskEntity) -> Unit,
  onToggleSubtask: (TaskEntity, Long) -> Unit,
  onSkipTask: (TaskEntity) -> Unit,
  onGiveUpTask: (TaskEntity) -> Unit = {},
  onEditTask: (Long) -> Unit,
  onToggleSelect: (Long) -> Unit,
  onToggleTaskTimer: (Long, Int) -> Unit,
  onResetTaskTimer: (Long, Int) -> Unit,
) {
  item(key = "section_header_$sectionKey") {
    CollapsibleTaskSectionHeader(
      title = header,
      count = tasks.size,
      expanded = expanded,
      onToggle = onToggleSection,
      accentColor = headerAccent,
      testTag = "task_section_header_$sectionKey",
    )
  }
  if (expanded) {
    items(
      items = tasks,
      key = { task -> "section_${sectionKey}_task_${task.id}" },
    ) { task ->
      val isActive = task.startTimestamp <= now
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (showRewardWorthBadge) {
          val worth = taskRewardSortValue(task)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.End,
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = PixelColors.Gold.copy(alpha = 0.16f),
            ) {
              Text(
                text = "Zdobycz: $worth monet",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PixelColors.Warning,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        TaskCardItem(
          task = task,
          folders = uiState.folders,
          attributes = uiState.attributes,
          tags = uiState.tags,
          isActive = isActive,
          isExpanded = expandedTaskId == task.id,
          isBulkMode = isBulkMode,
          isSelected = task.id in selectedTaskIds,
          inlineTimer = inlineTimer,
          onToggleExpand = { onToggleExpand(task.id) },
          onComplete = { if (isActive) onCompleteTask(task) },
          onToggleSubtask = { subId -> if (isActive) onToggleSubtask(task, subId) },
          onSkip = { if (isActive) onSkipTask(task) },
          onGiveUp = { if (isActive) onGiveUpTask(task) },
          onEdit = { onEditTask(task.id) },
          onToggleSelect = { onToggleSelect(task.id) },
          onToggleTaskTimer = { onToggleTaskTimer(task.id, task.durationMinutes) },
          onResetTaskTimer = { onResetTaskTimer(task.id, task.durationMinutes) },
          detailPref = OnboardingDetailPref.fromId(uiState.profile.onboardingDetailPref),
        )
      }
    }
  }
}

@Composable
fun CalendarMonthCard(
  monthStartMillis: Long,
  selectedDayStartMillis: Long,
  tasks: List<TaskEntity>,
  onPrevMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onSelectToday: () -> Unit,
  onSelectDay: (Long) -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val todayStart = remember { startOfLocalDayMillis() }
  val monthFormatter = remember { SimpleDateFormat("LLLL yyyy", Locale("pl", "PL")) }
  val monthCells = remember(monthStartMillis) { buildMonthCalendarGrid(monthStartMillis) }
  val countsByDay = remember(tasks, monthCells) {
    monthCells.associate { cell ->
      val next = addLocalDaysMillis(cell.dayStartMillis, 1)
      val count = tasks.count { t ->
        val due = t.dueTimestamp
        due != null && due >= cell.dayStartMillis && due < next
      }
      cell.dayStartMillis to count
    }
  }

  PixelCard(
    contentPadding = PaddingValues(12.dp),
    modifier = Modifier.testTag("calendar_month_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(onClick = onPrevMonth, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
          contentDescription = "Poprzedni miesiąc",
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = monthFormatter.format(Date(monthStartMillis)).replaceFirstChar { it.uppercase() },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
        Text(
          text = "Dziś",
          style = MaterialTheme.typography.labelSmall,
          color = palette.accent,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onSelectToday)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
      IconButton(onClick = onNextMonth, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "Następny miesiąc",
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
      listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd").forEach { dow ->
        Text(
          text = dow,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = palette.textMuted,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
    monthCells.chunked(7).forEach { week ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        week.forEach { cell ->
          val count = countsByDay[cell.dayStartMillis] ?: 0
          val isSelected = cell.dayStartMillis == selectedDayStartMillis
          val isToday = cell.dayStartMillis == todayStart
          val bgColor = when {
            isSelected -> palette.accent
            isToday -> palette.accent.copy(alpha = 0.14f)
            else -> Color.Transparent
          }
          val borderStroke = when {
            isSelected -> BorderStroke(1.5.dp, palette.outline)
            isToday -> BorderStroke(1.dp, palette.accent)
            else -> null
          }
          Box(
            modifier = Modifier
              .weight(1f)
              .aspectRatio(1f)
              .padding(vertical = 2.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(bgColor)
              .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(10.dp)) else Modifier)
              .clickable { onSelectDay(cell.dayStartMillis) }
              .testTag("calendar_day_${cell.dayOfMonth}_${cell.isCurrentMonth}"),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = cell.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                  isSelected -> Color.White
                  !cell.isCurrentMonth -> palette.textMuted.copy(alpha = 0.45f)
                  else -> palette.textPrimary
                },
              )
              if (count > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else palette.accent),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun HorizonNavigatorCard(
  windowStartMillis: Long,
  dayCount: Int,
  onPrevWindow: () -> Unit,
  onNextWindow: () -> Unit,
  onResetToday: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val rangeFormatter = remember { SimpleDateFormat("d MMM", Locale("pl", "PL")) }
  val windowEndMillis = remember(windowStartMillis, dayCount) {
    addLocalDaysMillis(windowStartMillis, (dayCount - 1).coerceAtLeast(0))
  }

  PixelCard(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(onClick = onPrevWindow, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
          contentDescription = "Poprzedni okres",
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "${rangeFormatter.format(Date(windowStartMillis))} – ${rangeFormatter.format(Date(windowEndMillis))}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
        Text(
          text = "Dziś",
          style = MaterialTheme.typography.labelSmall,
          color = palette.accent,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onResetToday)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
      IconButton(onClick = onNextWindow, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "Następny okres",
        )
      }
    }
  }
}

fun formatScheduleDayTitle(dayStartMillis: Long, todayStartMillis: Long): String {
  val dayFormatter = SimpleDateFormat("EEEE, dd.MM", Locale("pl", "PL"))
  val base = dayFormatter.format(Date(dayStartMillis)).replaceFirstChar { it.uppercase() }
  val tomorrow = addLocalDaysMillis(todayStartMillis, 1)
  val dayAfter = addLocalDaysMillis(todayStartMillis, 2)
  return when (dayStartMillis) {
    todayStartMillis -> "Dziś · $base"
    tomorrow -> "Jutro · $base"
    dayAfter -> "Pojutrze · $base"
    else -> base
  }
}

fun difficultyColor(difficulty: TaskDifficulty, fallback: Color): Color = when (difficulty) {
  TaskDifficulty.VERY_EASY -> PixelColors.Green
  TaskDifficulty.EASY -> PixelColors.GemBlue
  TaskDifficulty.MEDIUM -> fallback
  TaskDifficulty.HARD -> PixelColors.Warning
  TaskDifficulty.EXTREME -> PixelColors.Red
}

@Composable
fun RewardSortLegendBanner() {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.dp, palette.divider),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PixelIcon(icon = "gem", size = 18.dp)
      Text(
        text = "Sortowanie wg zdobyczy: 1 brylant = $GEM_COIN_EQUIVALENT monet",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = palette.textPrimary,
      )
    }
  }
}

@Composable
fun EmptyDayCard(dayLabel: String) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.dp, palette.divider),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = "$dayLabel — brak zadań z tym terminem",
      style = MaterialTheme.typography.bodySmall,
      color = palette.textMuted,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
  }
}

fun buildHorizonDayBuckets(
  tasks: List<TaskEntity>,
  windowStartMillis: Long,
  dayCount: Int,
  sortDirection: TaskSortDirection,
): List<Pair<Long, List<TaskEntity>>> {
  val dayStarts = (0 until dayCount).map { offset -> addLocalDaysMillis(windowStartMillis, offset) }
  val orderedDays = if (sortDirection == TaskSortDirection.ASCENDING) dayStarts else dayStarts.reversed()
  return orderedDays.map { dayStart ->
    dayStart to tasksDueOnDay(tasks, dayStart, sortDirection)
  }
}
