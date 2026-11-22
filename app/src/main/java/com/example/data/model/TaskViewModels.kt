package com.example.data.model

import com.example.data.local.TaskEntity
import java.util.Calendar

const val GEM_COIN_EQUIVALENT = 100

enum class TaskViewMode(val code: String, val label: String) {
  TIME_LEFT("time_left", "Czas do końca"),
  CATEGORY("category", "Kategoria"),
  CALENDAR("calendar", "Kalendarz"),
  THREE_DAYS("three_days", "3 dni"),
  WEEK("week", "Tydzień"),
  TWO_WEEKS("two_weeks", "2 tygodnie"),
  DIFFICULTY("difficulty", "Trudność zadania"),
  REWARD("reward", "Zdobycz"),
}

enum class TaskSortDirection {
  ASCENDING,
  DESCENDING,
}

fun defaultSortDirectionFor(viewMode: TaskViewMode): TaskSortDirection = when (viewMode) {
  TaskViewMode.REWARD -> TaskSortDirection.DESCENDING
  else -> TaskSortDirection.ASCENDING
}

fun sortLabelFor(viewMode: TaskViewMode, direction: TaskSortDirection): String = when (viewMode) {
  TaskViewMode.TIME_LEFT -> if (direction == TaskSortDirection.ASCENDING) {
    "Czas do końca: rosnąco"
  } else {
    "Czas do końca: malejąco"
  }
  TaskViewMode.DIFFICULTY -> if (direction == TaskSortDirection.ASCENDING) {
    "Od bardzo łatwego"
  } else {
    "Od ekstremalnie trudnego"
  }
  TaskViewMode.REWARD -> if (direction == TaskSortDirection.DESCENDING) {
    "Zdobycz: malejąco"
  } else {
    "Zdobycz: rosnąco"
  }
  TaskViewMode.CATEGORY -> if (direction == TaskSortDirection.ASCENDING) {
    "Kategorie + czas rosnąco"
  } else {
    "Kategorie odwrotnie + czas malejąco"
  }
  TaskViewMode.CALENDAR,
  TaskViewMode.THREE_DAYS,
  TaskViewMode.WEEK,
  TaskViewMode.TWO_WEEKS -> if (direction == TaskSortDirection.ASCENDING) {
    "Termin: najwcześniej"
  } else {
    "Termin: najpóźniej"
  }
}

fun taskRewardSortValue(task: TaskEntity): Int =
  task.coinReward + task.gemReward * GEM_COIN_EQUIVALENT

fun sortTasksByTimeLeft(
  tasks: List<TaskEntity>,
  direction: TaskSortDirection,
): List<TaskEntity> {
  val withDue = tasks.filter { it.dueTimestamp != null }
  val withoutDue = tasks.filter { it.dueTimestamp == null }.sortedBy { it.name.lowercase() }
  val sortedWithDue = if (direction == TaskSortDirection.ASCENDING) {
    withDue.sortedWith(compareBy<TaskEntity> { it.dueTimestamp!! }.thenBy { it.name.lowercase() })
  } else {
    withDue.sortedWith(compareByDescending<TaskEntity> { it.dueTimestamp!! }.thenBy { it.name.lowercase() })
  }
  return sortedWithDue + withoutDue
}

fun sortTasksByDifficulty(
  tasks: List<TaskEntity>,
  direction: TaskSortDirection,
): List<TaskEntity> {
  val comparator = if (direction == TaskSortDirection.ASCENDING) {
    compareBy<TaskEntity> { TaskDifficulty.fromCode(it.difficultyCode).rank }
      .thenBy { it.dueTimestamp ?: Long.MAX_VALUE }
      .thenBy { it.name.lowercase() }
  } else {
    compareByDescending<TaskEntity> { TaskDifficulty.fromCode(it.difficultyCode).rank }
      .thenBy { it.dueTimestamp ?: Long.MAX_VALUE }
      .thenBy { it.name.lowercase() }
  }
  return tasks.sortedWith(comparator)
}

fun sortTasksByReward(
  tasks: List<TaskEntity>,
  direction: TaskSortDirection,
): List<TaskEntity> {
  val comparator = if (direction == TaskSortDirection.DESCENDING) {
    compareByDescending<TaskEntity> { taskRewardSortValue(it) }
      .thenByDescending { it.expReward }
      .thenBy { it.name.lowercase() }
  } else {
    compareBy<TaskEntity> { taskRewardSortValue(it) }
      .thenBy { it.expReward }
      .thenBy { it.name.lowercase() }
  }
  return tasks.sortedWith(comparator)
}

fun sortTasksForView(
  tasks: List<TaskEntity>,
  viewMode: TaskViewMode,
  direction: TaskSortDirection,
): List<TaskEntity> = when (viewMode) {
  TaskViewMode.TIME_LEFT,
  TaskViewMode.CALENDAR,
  TaskViewMode.THREE_DAYS,
  TaskViewMode.WEEK,
  TaskViewMode.TWO_WEEKS -> sortTasksByTimeLeft(tasks, direction)
  TaskViewMode.DIFFICULTY -> sortTasksByDifficulty(tasks, direction)
  TaskViewMode.REWARD -> sortTasksByReward(tasks, direction)
  TaskViewMode.CATEGORY -> {
    val cats = if (direction == TaskSortDirection.ASCENDING) {
      TaskCategory.entries
    } else {
      TaskCategory.entries.reversed()
    }
    cats.flatMap { cat ->
      sortTasksByTimeLeft(tasks.filter { it.categoryCode == cat.code }, direction)
    }
  }
}

fun groupTasksByCategory(
  tasks: List<TaskEntity>,
  direction: TaskSortDirection,
): List<Pair<TaskCategory, List<TaskEntity>>> {
  val cats = if (direction == TaskSortDirection.ASCENDING) {
    TaskCategory.entries
  } else {
    TaskCategory.entries.reversed()
  }
  return cats.mapNotNull { cat ->
    val bucket = sortTasksByTimeLeft(tasks.filter { it.categoryCode == cat.code }, direction)
    if (bucket.isEmpty()) null else cat to bucket
  }
}

fun groupTasksByDifficulty(
  tasks: List<TaskEntity>,
  direction: TaskSortDirection,
): List<Pair<TaskDifficulty, List<TaskEntity>>> {
  val diffs = if (direction == TaskSortDirection.ASCENDING) {
    TaskDifficulty.entries.sortedBy { it.rank }
  } else {
    TaskDifficulty.entries.sortedByDescending { it.rank }
  }
  return diffs.mapNotNull { diff ->
    val bucket = sortTasksByTimeLeft(
      tasks.filter { TaskDifficulty.fromCode(it.difficultyCode) == diff },
      TaskSortDirection.ASCENDING,
    )
    if (bucket.isEmpty()) null else diff to bucket
  }
}

fun startOfLocalDayMillis(millis: Long = System.currentTimeMillis()): Long {
  val cal = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  return cal.timeInMillis
}

fun addLocalDaysMillis(baseDayStartMillis: Long, deltaDays: Int): Long {
  val cal = Calendar.getInstance().apply {
    timeInMillis = baseDayStartMillis
    add(Calendar.DAY_OF_YEAR, deltaDays)
  }
  return startOfLocalDayMillis(cal.timeInMillis)
}

fun addLocalMonthsMillis(monthStartMillis: Long, deltaMonths: Int): Long {
  val cal = Calendar.getInstance().apply {
    timeInMillis = monthStartMillis
    set(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MONTH, deltaMonths)
  }
  return startOfLocalDayMillis(cal.timeInMillis)
}

fun startOfLocalMonthMillis(millis: Long = System.currentTimeMillis()): Long {
  val cal = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  return cal.timeInMillis
}

data class CalendarDayCell(
  val dayStartMillis: Long,
  val dayOfMonth: Int,
  val isCurrentMonth: Boolean,
)

fun buildMonthCalendarGrid(monthStartMillis: Long): List<CalendarDayCell> {
  val cal = Calendar.getInstance().apply {
    timeInMillis = startOfLocalMonthMillis(monthStartMillis)
  }
  val targetMonth = cal.get(Calendar.MONTH)
  // Convert Calendar.DAY_OF_WEEK (Sun=1..Sat=7) into Monday=0..Sunday=6
  val firstDowOffset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
  val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val totalCells = ((firstDowOffset + daysInMonth + 6) / 7) * 7

  val gridStart = addLocalDaysMillis(cal.timeInMillis, -firstDowOffset)
  return (0 until totalCells).map { idx ->
    val cellMillis = addLocalDaysMillis(gridStart, idx)
    val cellCal = Calendar.getInstance().apply { timeInMillis = cellMillis }
    CalendarDayCell(
      dayStartMillis = cellMillis,
      dayOfMonth = cellCal.get(Calendar.DAY_OF_MONTH),
      isCurrentMonth = cellCal.get(Calendar.MONTH) == targetMonth,
    )
  }
}

fun tasksDueOnDay(
  tasks: List<TaskEntity>,
  dayStartMillis: Long,
  sortDirection: TaskSortDirection,
): List<TaskEntity> {
  val nextDay = addLocalDaysMillis(dayStartMillis, 1)
  val matched = tasks.filter { task ->
    val due = task.dueTimestamp ?: return@filter false
    due >= dayStartMillis && due < nextDay
  }
  return sortTasksByTimeLeft(matched, sortDirection)
}
