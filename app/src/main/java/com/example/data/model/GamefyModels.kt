package com.example.data.model

import java.util.Calendar
import kotlin.math.roundToInt

enum class TaskCategory(val code: String, val label: String, val shortPlural: String) {
  DAILY("d", "Dzienne", "dni"),
  WEEKLY("w", "Tygodniowe", "tyg."),
  MONTHLY("m", "Miesięczne", "mies."),
  YEARLY("y", "Roczne", "lat"),
  SIDEQUEST("o", "Zadanie poboczne", "");

  companion object {
    fun fromCode(code: String): TaskCategory = entries.find { it.code == code } ?: WEEKLY
  }
}

enum class ReminderMode(val code: String, val label: String) {
  NONE("n", "Bez przypomnienia"),
  AT_DUE("at", "W chwili upływu terminu"),
  DAY_BEFORE("d1", "Dzień przed terminem"),
  THREE_DAYS_BEFORE("d3", "Codziennie przez 3 dni przed terminem"),
  DAILY_OVERDUE("od", "Codziennie, gdy zadanie jest po terminie"),
  THREE_DAYS_AND_OVERDUE("all", "3 dni przed + codziennie po terminie");

  companion object {
    fun fromCode(code: String): ReminderMode = entries.find { it.code == code } ?: DAY_BEFORE
  }
}

enum class PenaltyMode(val code: String, val label: String) {
  NONE("0", "Bez kary"),
  ON_DUE_ONCE("o", "Gdy minie termin (raz)"),
  EVERY_MISSED_PERIOD("e", "Za każdy pominięty okres"),
  ON_SKIP_ONLY("s", "Tylko przy pominięciu (Pomiń)");

  companion object {
    fun fromCode(code: String): PenaltyMode = entries.find { it.code == code } ?: NONE
  }
}

enum class PurchaseLimitPeriod(val code: String, val label: String) {
  UNLIMITED("0", "bez limitu"),
  DAILY("d", "na dzień"),
  WEEKLY("w", "na tydzień (7 dni)"),
  MONTHLY("m", "na miesiąc"),
  TOTAL("t", "łącznie");

  companion object {
    fun fromCode(code: String): PurchaseLimitPeriod = entries.find { it.code == code } ?: UNLIMITED
  }
}

enum class AppThemeId(
  val code: String,
  val label: String,
  val isDark: Boolean,
  val isHighContrast: Boolean = false,
) {
  VIOLET("violet", "Fiolet", isDark = false),
  FOREST("forest", "Las", isDark = false),
  ROSE("rose", "Róża", isDark = false),
  NIGHT("night", "Noc", isDark = true),
  AMOLED("amoled", "AMOLED", isDark = true),
  SAPPHIRE("sapphire", "Szafir", isDark = true),
  HIGH_CONTRAST_DAY("hc_day", "Jasny", isDark = false, isHighContrast = true),
  HIGH_CONTRAST_NIGHT("hc_night", "Ciemny", isDark = true, isHighContrast = true);

  companion object {
    fun fromCode(code: String): AppThemeId = entries.find { it.code == code } ?: VIOLET
  }
}

enum class AppFontSize(val code: String, val label: String, val scale: Float) {
  SMALL("small", "Mała", 0.88f),
  DEFAULT("default", "Domyślna", 1.0f),
  LARGE("large", "Duża", 1.15f),
  SUPER_LARGE("super_large", "Super duża", 1.32f);

  companion object {
    fun fromCode(code: String): AppFontSize = entries.find { it.code == code } ?: DEFAULT
  }
}

enum class AppFontFamilyOption(val code: String, val label: String, val subtitle: String) {
  DEFAULT("default", "Domyślna", "Pixelowe nagłówki i czytelny tekst"),
  OPEN_DYSLEXIC("opendyslexic", "Lexend", "Szeroki rozstaw liter ułatwiający czytanie"),
  HELVETICA("helvetica", "Arimo", "Klasyczna, neutralna czcionka"),
  ATKINSON("atkinson", "Atkinson Hyperlegible", "Bardzo czytelna — wyraźne kształty liter");

  companion object {
    fun fromCode(code: String): AppFontFamilyOption = entries.find { it.code == code } ?: DEFAULT
  }
}

enum class AppWordSpacing(
  val code: String,
  val label: String,
  val extraLetterSpacingSp: Float,
  val lineHeightMultiplier: Float,
  val horizontalScaleX: Float,
) {
  STANDARD("standard", "Standardowe", 0f, 1.0f, 1.0f),
  WIDER("wider", "Większe", 1.35f, 1.18f, 1.05f);

  companion object {
    fun fromCode(code: String): AppWordSpacing = entries.find { it.code == code } ?: STANDARD
  }
}

data class LevelProgress(
  val level: Int,
  val currentExp: Int,
  val neededExp: Int,
) {
  val fraction: Float
    get() = if (neededExp <= 0) 0f else (currentExp.toFloat() / neededExp.toFloat()).coerceIn(0f, 1f)
}

fun calculateLevel(totalExp: Int): LevelProgress {
  var remaining = totalExp.coerceAtLeast(0)
  var level = 1
  var needed = 100
  while (remaining >= needed) {
    remaining -= needed
    level++
    needed = 100 + (level - 1) * 50
  }
  return LevelProgress(level = level, currentExp = remaining, neededExp = needed)
}

fun nextCycleTimestamp(baseMillis: Long, category: TaskCategory, interval: Int): Long {
  val safeInterval = interval.coerceAtLeast(1)
  val cal = Calendar.getInstance().apply { timeInMillis = baseMillis }
  when (category) {
    TaskCategory.DAILY -> cal.add(Calendar.DAY_OF_YEAR, safeInterval)
    TaskCategory.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7 * safeInterval)
    TaskCategory.MONTHLY -> cal.add(Calendar.MONTH, safeInterval)
    TaskCategory.YEARLY -> cal.add(Calendar.YEAR, safeInterval)
    TaskCategory.SIDEQUEST -> return baseMillis
  }
  return cal.timeInMillis
}

fun formatGemsLabel(count: Int): String = when {
  count == 1 -> "1 brylant"
  count % 10 in 2..4 && count % 100 !in 12..14 -> "$count brylanty"
  else -> "$count brylantów"
}

const val MILLIS_PER_DAY = 86_400_000L
const val MILLIS_PER_HOUR = 3_600_000L
const val MILLIS_PER_MINUTE = 60_000L

fun localEpochDay(millis: Long = System.currentTimeMillis()): Long {
  val cal = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  val tzOffset = cal.timeZone.getOffset(cal.timeInMillis)
  return (cal.timeInMillis + tzOffset) / MILLIS_PER_DAY
}

const val BACKUP_WARNING_THRESHOLD_DAYS = 14

fun backupDaysSince(
  lastBackupExportTimestamp: Long,
  nowMillis: Long = System.currentTimeMillis(),
): Int? {
  if (lastBackupExportTimestamp <= 0L) return null
  val elapsed = (nowMillis - lastBackupExportTimestamp).coerceAtLeast(0L)
  return (elapsed / MILLIS_PER_DAY).toInt()
}

fun isBackupOverdueWarning(
  lastBackupExportTimestamp: Long,
  nowMillis: Long = System.currentTimeMillis(),
): Boolean {
  val days = backupDaysSince(lastBackupExportTimestamp, nowMillis) ?: return true
  return days > BACKUP_WARNING_THRESHOLD_DAYS
}

fun formatLastBackupStatus(
  lastBackupExportTimestamp: Long,
  nowMillis: Long = System.currentTimeMillis(),
): String {
  val days = backupDaysSince(lastBackupExportTimestamp, nowMillis) ?: return "Ostatnia kopia: nigdy"
  return when (days) {
    0 -> "Ostatnia kopia: dzisiaj"
    1 -> "Ostatnia kopia: 1 dzień temu"
    else -> "Ostatnia kopia: $days dni temu"
  }
}

private val POLISH_MONTHS_GENITIVE = listOf(
  "stycznia",
  "lutego",
  "marca",
  "kwietnia",
  "maja",
  "czerwca",
  "lipca",
  "sierpnia",
  "września",
  "października",
  "listopada",
  "grudnia",
)

fun formatDetailedBackupStatus(
  lastBackupExportTimestamp: Long,
  nowMillis: Long = System.currentTimeMillis(),
): String {
  val days = backupDaysSince(lastBackupExportTimestamp, nowMillis) ?: return "Brak kopii zapasowej"
  val cal = Calendar.getInstance().apply { timeInMillis = lastBackupExportTimestamp }
  val day = cal.get(Calendar.DAY_OF_MONTH)
  val monthName = POLISH_MONTHS_GENITIVE.getOrElse(cal.get(Calendar.MONTH)) { "" }
  val relative = when (days) {
    0 -> "dzisiaj"
    1 -> "1 dzień temu"
    else -> "$days dni temu"
  }
  return "Ostatnia kopia: $day $monthName ($relative)"
}

fun formatDurationShort(diffMillis: Long): String {
  val abs = kotlin.math.abs(diffMillis)
  val days = abs / MILLIS_PER_DAY
  val hours = (abs % MILLIS_PER_DAY) / MILLIS_PER_HOUR
  val minutes = (abs % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE
  return when {
    days > 0 -> "${days} d ${hours} h"
    hours > 0 -> "${hours} h ${minutes} min"
    else -> "${minutes.coerceAtLeast(1)} min"
  }
}

fun formatFrequencyLabel(category: TaskCategory, interval: Int): String {
  if (category == TaskCategory.SIDEQUEST) return "Poboczne"
  if (interval <= 1) {
    return when (category) {
      TaskCategory.DAILY -> "Codziennie"
      TaskCategory.WEEKLY -> "Co tydzień"
      TaskCategory.MONTHLY -> "Co miesiąc"
      TaskCategory.YEARLY -> "Co rok"
      TaskCategory.SIDEQUEST -> "Poboczne"
    }
  }
  return "Co $interval ${category.shortPlural}"
}

enum class TaskDifficulty(
  val code: String,
  val label: String,
  val rank: Int,
  val multiplier: Float,
) {
  VERY_EASY("ve", "Bardzo łatwe", 0, 0.8f),
  EASY("e", "Łatwe", 1, 1.0f),
  MEDIUM("m", "Średnie", 2, 1.6f),
  HARD("h", "Trudne", 3, 2.5f),
  EXTREME("x", "Ekstremalnie trudne", 4, 4.0f);

  companion object {
    fun fromCode(code: String): TaskDifficulty = entries.find { it.code == code } ?: MEDIUM
  }
}

enum class TaskEffortBucket(val label: String, val multiplier: Float) {
  SHORT("Do 15 min", 1.0f),
  MEDIUM("15–45 min", 1.6f),
  LONG("45–120 min", 2.4f),
  VERY_LONG("2h+", 3.5f);

  companion object {
    fun fromMinutes(minutes: Int): TaskEffortBucket = when {
      minutes <= 15 -> SHORT
      minutes <= 45 -> MEDIUM
      minutes <= 120 -> LONG
      else -> VERY_LONG
    }
  }
}

data class AutoRewardResult(
  val exp: Int,
  val coins: Int,
  val gems: Int = 0,
  val penaltyMinCoins: Int,
  val penaltyMaxCoins: Int,
)

fun calculateAutoPenaltyRange(
  baseCoins: Int,
  difficulty: TaskDifficulty,
  penaltyMode: PenaltyMode,
): Pair<Int, Int> {
  if (penaltyMode == PenaltyMode.NONE) return 0 to 0
  val effectiveCoins = baseCoins.coerceAtLeast(5)
  val severity = when (penaltyMode) {
    PenaltyMode.NONE -> 0f
    PenaltyMode.EVERY_MISSED_PERIOD -> 0.55f
    PenaltyMode.ON_DUE_ONCE -> 0.75f
    PenaltyMode.ON_SKIP_ONLY -> 0.85f
  }
  val diffBoost = 1f + (difficulty.rank * 0.08f)
  val minP = (effectiveCoins * severity * 0.8f * diffBoost).roundToInt().coerceAtLeast(3)
  val maxP = (effectiveCoins * severity * 1.25f * diffBoost).roundToInt().coerceAtLeast(minP + 2)
  return minP to maxP
}

fun resolveGiveUpPenaltyRange(
  coinReward: Int,
  difficultyCode: String,
  penaltyModeCode: String,
  penaltyMinCoins: Int,
  penaltyMaxCoins: Int,
): Pair<Int, Int> {
  val mode = PenaltyMode.fromCode(penaltyModeCode)
  if (mode != PenaltyMode.NONE && (penaltyMinCoins > 0 || penaltyMaxCoins > 0)) {
    val low = penaltyMinCoins.coerceAtLeast(0)
    val high = penaltyMaxCoins.coerceAtLeast(low)
    return low to high
  }
  return calculateAutoPenaltyRange(
    baseCoins = coinReward,
    difficulty = TaskDifficulty.fromCode(difficultyCode),
    penaltyMode = PenaltyMode.ON_SKIP_ONLY,
  )
}

fun calculateAutoRewardAndPenalty(
  difficulty: TaskDifficulty,
  effort: TaskEffortBucket,
  attributeCount: Int,
  hasPenalty: Boolean,
  penaltyMode: PenaltyMode = if (hasPenalty) PenaltyMode.ON_DUE_ONCE else PenaltyMode.NONE,
): AutoRewardResult {
  val attrMult = when {
    attributeCount <= 0 -> 0.9f
    attributeCount == 1 -> 1.0f
    attributeCount == 2 -> 1.2f
    else -> 1.4f
  }
  val effectivePenalty = hasPenalty && penaltyMode != PenaltyMode.NONE
  val riskMult = if (effectivePenalty) 1.25f else 1.0f

  val rawExp = 15f * difficulty.multiplier * effort.multiplier * attrMult * riskMult
  val rawCoins = 8f * difficulty.multiplier * effort.multiplier *
    (0.95f + attributeCount.coerceIn(0, 3) * 0.1f) * riskMult

  val exp = ((rawExp / 5f).roundToInt() * 5).coerceIn(10, 500)
  val coins = rawCoins.roundToInt().coerceIn(5, 250)
  val gems = when {
    difficulty == TaskDifficulty.EXTREME && (effort != TaskEffortBucket.SHORT || effectivePenalty) -> 1
    else -> 0
  }

  val (pMin, pMax) = calculateAutoPenaltyRange(
    baseCoins = coins,
    difficulty = difficulty,
    penaltyMode = if (effectivePenalty) penaltyMode else PenaltyMode.NONE,
  )
  return AutoRewardResult(
    exp = exp,
    coins = coins,
    gems = gems,
    penaltyMinCoins = pMin,
    penaltyMaxCoins = pMax,
  )
}
