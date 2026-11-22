package com.example.data.model

import com.example.data.local.PlayerProfileEntity
import com.example.data.local.TaskEntity
import com.example.data.local.parseStringCsv
import com.example.data.local.toStringCsv
import java.util.Calendar

enum class NotificationTimeSlot(
  val code: String,
  val label: String,
  val shortRange: String,
) {
  MORNING("morning", "Rano", "08:00–11:00"),
  AFTERNOON("afternoon", "Po południu", "13:00–17:00"),
  EVENING("evening", "Wieczorem", "19:00–22:00"),
  CUSTOM("custom", "Własne godziny", "Własny zakres");

  companion object {
    fun fromCode(code: String): NotificationTimeSlot? = entries.find { it.code == code }
  }
}

enum class NotificationTriggerMode(
  val code: String,
  val label: String,
  val description: String,
) {
  WITH_REMINDER(
    code = "reminder",
    label = "Gdy zadanie ma przypomnienie",
    description = "Powiadamia według ustawień przypomnienia wybranego zadania.",
  ),
  ONLY_FINISHING(
    code = "ending",
    label = "Tylko gdy zadanie się kończy",
    description = "Wysyła alert wyłącznie tuż przed upływem terminu (do 2 h przed końcem).",
  ),
  BOTH(
    code = "both",
    label = "Przypomnienie + koniec terminu",
    description = "Łączy przypomnienia zadań oraz alerty tuż przed końcem czasu.",
  );

  companion object {
    fun fromCode(code: String): NotificationTriggerMode =
      entries.find { it.code == code } ?: WITH_REMINDER
  }
}

enum class NotificationSummaryStyle(
  val code: String,
  val label: String,
  val description: String,
) {
  DETAILED_DIGEST(
    code = "digest",
    label = "Podsumowanie dnia (zadania + łączny czas)",
    description = "Zbiera liczbę zadań do wykonania danego dnia oraz szacowany łączny czas.",
  ),
  URGENT_TICK_TOCK(
    code = "tick_tock",
    label = "Krótkie przypomnienie („Tik tak...”)",
    description = "Krótki sygnał, że zadania czekają i zaraz będą po terminie.",
  );

  companion object {
    fun fromCode(code: String): NotificationSummaryStyle =
      entries.find { it.code == code } ?: DETAILED_DIGEST
  }
}

data class NotificationSettings(
  val useTaskSettings: Boolean = true,
  val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
  val allowAnytime: Boolean = true,
  val timeSlots: Set<NotificationTimeSlot> = emptySet(),
  val customStartHour: Int = 9,
  val customStartMinute: Int = 0,
  val customEndHour: Int = 21,
  val customEndMinute: Int = 0,
  val triggerMode: NotificationTriggerMode = NotificationTriggerMode.WITH_REMINDER,
  val maxPerDay: Int = 0,
  val summaryStyle: NotificationSummaryStyle = NotificationSummaryStyle.DETAILED_DIGEST,
)

val WEEKDAY_LABELS_PL: List<Pair<Int, String>> = listOf(
  1 to "Pon",
  2 to "Wt",
  3 to "Śr",
  4 to "Cz",
  5 to "Pt",
  6 to "Sob",
  7 to "Nie",
)

fun PlayerProfileEntity.toNotificationSettings(): NotificationSettings {
  val parsedDays = notifDaysCsv.split(',')
    .mapNotNull { it.trim().toIntOrNull() }
    .filter { it in 1..7 }
    .toSet()
    .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }

  val rawSlots = parseStringCsv(notifSlotsCsv)
  val isAny = rawSlots.isEmpty() || "any" in rawSlots
  val parsedSlots = rawSlots.mapNotNull { NotificationTimeSlot.fromCode(it) }.toSet()

  return NotificationSettings(
    useTaskSettings = notifUseTaskSettings,
    daysOfWeek = parsedDays,
    allowAnytime = isAny,
    timeSlots = if (isAny) emptySet() else parsedSlots,
    customStartHour = notifCustomStartHour.coerceIn(0, 23),
    customStartMinute = notifCustomStartMinute.coerceIn(0, 59),
    customEndHour = notifCustomEndHour.coerceIn(0, 23),
    customEndMinute = notifCustomEndMinute.coerceIn(0, 59),
    triggerMode = NotificationTriggerMode.fromCode(notifTriggerModeCode),
    maxPerDay = notifMaxPerDay.coerceAtLeast(0),
    summaryStyle = NotificationSummaryStyle.fromCode(notifSummaryStyleCode),
  )
}

fun PlayerProfileEntity.withNotificationSettings(settings: NotificationSettings): PlayerProfileEntity {
  val safeDays = settings.daysOfWeek.filter { it in 1..7 }.sorted()
  val daysCsv = (if (safeDays.isEmpty()) listOf(1, 2, 3, 4, 5, 6, 7) else safeDays).joinToString(",")
  val slotsCsv = if (settings.allowAnytime || settings.timeSlots.isEmpty()) {
    "any"
  } else {
    toStringCsv(settings.timeSlots.map { it.code })
  }
  return copy(
    notifUseTaskSettings = settings.useTaskSettings,
    notifDaysCsv = daysCsv,
    notifSlotsCsv = slotsCsv,
    notifCustomStartHour = settings.customStartHour.coerceIn(0, 23),
    notifCustomStartMinute = settings.customStartMinute.coerceIn(0, 59),
    notifCustomEndHour = settings.customEndHour.coerceIn(0, 23),
    notifCustomEndMinute = settings.customEndMinute.coerceIn(0, 59),
    notifTriggerModeCode = settings.triggerMode.code,
    notifMaxPerDay = settings.maxPerDay.coerceAtLeast(0),
    notifSummaryStyleCode = settings.summaryStyle.code,
  )
}

/**
 * Converts Calendar.DAY_OF_WEEK (Sunday=1 .. Saturday=7) to ISO weekday (Monday=1 .. Sunday=7).
 */
fun isoWeekdayOf(millis: Long): Int {
  val cal = Calendar.getInstance().apply { timeInMillis = millis }
  return when (cal.get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY -> 1
    Calendar.TUESDAY -> 2
    Calendar.WEDNESDAY -> 3
    Calendar.THURSDAY -> 4
    Calendar.FRIDAY -> 5
    Calendar.SATURDAY -> 6
    Calendar.SUNDAY -> 7
    else -> 1
  }
}

fun isWithinMinuteRange(minuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean {
  return if (startMinute <= endMinute) {
    minuteOfDay in startMinute..endMinute
  } else {
    // Handles overnight custom ranges such as 22:00 - 06:00
    minuteOfDay >= startMinute || minuteOfDay <= endMinute
  }
}

fun isWithinNotificationSchedule(
  settings: NotificationSettings,
  nowMillis: Long,
): Boolean {
  val weekday = isoWeekdayOf(nowMillis)
  if (weekday !in settings.daysOfWeek) return false

  if (settings.allowAnytime || settings.timeSlots.isEmpty()) return true

  val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
  val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

  return settings.timeSlots.any { slot ->
    when (slot) {
      NotificationTimeSlot.MORNING -> minuteOfDay in (8 * 60)..(11 * 60)
      NotificationTimeSlot.AFTERNOON -> minuteOfDay in (13 * 60)..(17 * 60)
      NotificationTimeSlot.EVENING -> minuteOfDay in (19 * 60)..(22 * 60)
      NotificationTimeSlot.CUSTOM -> {
        val start = settings.customStartHour * 60 + settings.customStartMinute
        val end = settings.customEndHour * 60 + settings.customEndMinute
        isWithinMinuteRange(minuteOfDay, start, end)
      }
    }
  }
}

fun polishTasksWord(count: Int): String = when {
  count == 1 -> "zadanie"
  count % 10 in 2..4 && count % 100 !in 12..14 -> "zadania"
  else -> "zadań"
}

fun formatEstimatedDurationMinutes(totalMinutes: Int): String {
  if (totalMinutes <= 0) return "nie określono"
  val hours = totalMinutes / 60
  val mins = totalMinutes % 60
  return when {
    hours > 0 && mins > 0 -> "$hours h $mins min"
    hours > 0 -> "$hours h"
    else -> "$mins min"
  }
}

fun tasksPlannedForDay(
  tasks: List<TaskEntity>,
  nowMillis: Long,
): List<TaskEntity> {
  val todayStart = startOfLocalDayMillis(nowMillis)
  val tomorrowStart = addLocalDaysMillis(todayStart, 1)
  return tasks.filter { task ->
    val due = task.dueTimestamp
    val startsToday = task.startTimestamp in todayStart until tomorrowStart
    val activeNow = task.startTimestamp <= nowMillis
    when {
      due != null && due < tomorrowStart -> activeNow || startsToday
      due == null -> activeNow
      else -> startsToday
    }
  }
}

fun buildSummaryNotificationText(
  tasksForToday: List<TaskEntity>,
  style: NotificationSummaryStyle,
): String {
  val count = tasksForToday.size
  val noun = polishTasksWord(count)
  return when (style) {
    NotificationSummaryStyle.DETAILED_DIGEST -> {
      val totalMinutes = tasksForToday.sumOf { it.durationMinutes.coerceAtLeast(0) }
      val timePart = if (totalMinutes > 0) {
        "Łączny szac. czas: ${formatEstimatedDurationMinutes(totalMinutes)}."
      } else {
        "Brak określonego czasu w zadaniach."
      }
      "Plan na dziś: $count $noun do wykonania. $timePart Kliknij, aby otworzyć listę."
    }
    NotificationSummaryStyle.URGENT_TICK_TOCK -> {
      if (count <= 0) {
        "⏰ Tik tak! Sprawdź swoje zadania na dziś."
      } else {
        "⏰ Tik tak! Twoje zadania ($count) czekają i zaraz będą po terminie!"
      }
    }
  }
}

fun isTaskPreferredTimeReached(
  task: TaskEntity,
  nowMillis: Long,
): Boolean {
  if (task.preferredReminderHour < 0) return true
  val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
  val nowMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  val prefMinuteOfDay =
    task.preferredReminderHour.coerceIn(0, 23) * 60 + task.preferredReminderMinute.coerceIn(0, 59)
  return nowMinuteOfDay >= prefMinuteOfDay
}

fun formatTimeHm(hour: Int, minute: Int): String =
  String.format("%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
