package com.example.data.model

import java.util.Calendar
import kotlin.math.roundToInt

enum class TaskCategory(val code: String, val label: String, val shortPlural: String) {
  DAILY("d", "Dzienne", "dni"),
  WEEKLY("w", "Tygodniowe", "tyg."),
  MONTHLY("m", "Miesięczne", "mies."),
  YEARLY("y", "Roczne", "lat"),
  SIDEQUEST("o", "Sidequest (jednorazowe)", "");

  companion object {
    fun fromCode(code: String): TaskCategory = entries.find { it.code == code } ?: WEEKLY
  }
}

enum class ReminderMode(val code: String, val label: String) {
  NONE("n", "Bez przypomnienia"),
  AT_DUE("at", "W chwili upływu terminu"),
  DAY_BEFORE("d1", "Dzień przed terminem"),
  THREE_DAYS_BEFORE("d3", "Codziennie przez 3 dni przed terminem"),
  DAILY_OVERDUE("od", "Codziennie, gdy zadanie jest spóźnione"),
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
  HIGH_CONTRAST_DAY("hc_day", "Kontrast Dzień", isDark = false, isHighContrast = true),
  HIGH_CONTRAST_NIGHT("hc_night", "Kontrast Noc", isDark = true, isHighContrast = true);

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
  DEFAULT("default", "Domyślna", "Pixel + Jakarta"),
  OPEN_DYSLEXIC("opendyslexic", "OpenDyslexic", "odp. Lexend"),
  HELVETICA("helvetica", "Helvetica", "odp. Arimo"),
  ATKINSON("atkinson", "Atkinson Hyperlegible", "Braille Institute");

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

data class AchievementDefinition(
  val id: String,
  val icon: String,
  val title: String,
  val description: String,
  val rewardExp: Int = 0,
  val rewardCoins: Int = 0,
  val rewardGems: Int = 0,
  val isSecret: Boolean = false,
  val isUnlocked: (PlayerStatsSnapshot) -> Boolean,
) {
  fun rewardSummary(): String {
    val parts = mutableListOf<String>()
    if (rewardExp > 0) parts.add("+$rewardExp EXP")
    if (rewardCoins > 0) parts.add("+$rewardCoins 🪙")
    if (rewardGems > 0) parts.add("+$rewardGems 💎")
    return parts.joinToString(" · ")
  }
}

data class PlayerStatsSnapshot(
  val exp: Int,
  val tasksDone: Int,
  val pomosDone: Int,
  val coinsEarned: Int,
  val itemsBought: Int,
  val currentStreak: Int = 0,
  val bestStreak: Int = 0,
  val customTasksCreated: Int = 0,
  val boughtMostExpensive: Boolean = false,
  val completedNightAction: Boolean = false,
  val completedSubtaskChain: Boolean = false,
)

val GAMEFY_ACHIEVEMENTS = listOf(
  AchievementDefinition(
    id = "first_task",
    icon = "🌱",
    title = "Pierwsze zadanie!",
    description = "Wykonaj swoje pierwsze zadanie.",
    rewardExp = 30,
    rewardCoins = 15,
  ) { it.tasksDone >= 1 },
  AchievementDefinition(
    id = "ten_tasks",
    icon = "🔟",
    title = "Dziesiątka",
    description = "Wykonaj łącznie 10 zadań.",
    rewardExp = 80,
    rewardCoins = 40,
  ) { it.tasksDone >= 10 },
  AchievementDefinition(
    id = "hundred_tasks",
    icon = "💯",
    title = "Setka mistrzów",
    description = "Wykonaj łącznie 100 zadań.",
    rewardExp = 350,
    rewardCoins = 150,
    rewardGems = 2,
  ) { it.tasksDone >= 100 },
  AchievementDefinition(
    id = "streak_7",
    icon = "🎯",
    title = "Tydzień w rytmie",
    description = "Streak 7 dni! Wykonaj przynajmniej 1 zadanie dziennie przez 7 dni.",
    rewardExp = 120,
    rewardCoins = 60,
    rewardGems = 1,
  ) { maxOf(it.currentStreak, it.bestStreak) >= 7 },
  AchievementDefinition(
    id = "streak_30",
    icon = "💪",
    title = "Miesiąc konsekwencji",
    description = "Streak 30 dni! Każdego dnia przez 30 dni wykonałeś przynajmniej jedno zadanie.",
    rewardExp = 400,
    rewardCoins = 200,
    rewardGems = 3,
  ) { maxOf(it.currentStreak, it.bestStreak) >= 30 },
  AchievementDefinition(
    id = "streak_100",
    icon = "trophy",
    title = "Streak 100 dni!",
    description = "Każdego dnia przez 100 dni wykonałeś przynajmniej jedno zadanie, oto twoja nagroda!",
    rewardExp = 1500,
    rewardCoins = 500,
    rewardGems = 10,
  ) { maxOf(it.currentStreak, it.bestStreak) >= 100 },
  AchievementDefinition(
    id = "level_five",
    icon = "⭐",
    title = "Poziom 5",
    description = "Osiągnij 5. poziom ogólny.",
    rewardExp = 100,
    rewardCoins = 100,
    rewardGems = 2,
  ) { calculateLevel(it.exp).level >= 5 },
  AchievementDefinition(
    id = "focused_five",
    icon = "🍅",
    title = "Skupiony umysł",
    description = "Ukończ 5 sesji Pomodoro lub Timera.",
    rewardExp = 100,
    rewardCoins = 50,
    rewardGems = 1,
  ) { it.pomosDone >= 5 },
  AchievementDefinition(
    id = "first_buy",
    icon = "🛍️",
    title = "Pierwsze zakupy",
    description = "Kup pierwszą nagrodę w sklepie.",
    rewardExp = 50,
    rewardCoins = 25,
  ) { it.itemsBought >= 1 },
  AchievementDefinition(
    id = "treasurer",
    icon = "💰",
    title = "Skarbnik",
    description = "Zdobądź łącznie 500 monet z zadań i sesji.",
    rewardExp = 150,
    rewardCoins = 75,
    rewardGems = 2,
  ) { it.coinsEarned >= 500 },
  // Secret achievements (hidden until unlocked!)
  AchievementDefinition(
    id = "secret_create_task",
    icon = "📚",
    title = "Architekt własnego planu",
    description = "Dodaj swoje własne zadanie.",
    rewardExp = 50,
    rewardCoins = 30,
    isSecret = true,
  ) { it.customTasksCreated >= 1 },
  AchievementDefinition(
    id = "secret_most_expensive",
    icon = "gem",
    title = "Król Sklepu",
    description = "Kup najdroższy przedmiot w sklepie.",
    rewardExp = 300,
    rewardCoins = 100,
    rewardGems = 3,
    isSecret = true,
  ) { it.boughtMostExpensive },
  AchievementDefinition(
    id = "secret_subtasks",
    icon = "check",
    title = "Krok po kroku",
    description = "Ukończ zadanie wieloczęściowe poprzez odhaczenie wszystkich jego podzadań.",
    rewardExp = 100,
    rewardCoins = 50,
    rewardGems = 1,
    isSecret = true,
  ) { it.completedSubtaskChain },
  AchievementDefinition(
    id = "secret_night_owl",
    icon = "🎨",
    title = "Nocny Marek",
    description = "Wykonaj zadanie lub sesję skupienia późną nocą (23:00–04:00).",
    rewardExp = 80,
    rewardCoins = 40,
    rewardGems = 1,
    isSecret = true,
  ) { it.completedNightAction },
)

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
  if (category == TaskCategory.SIDEQUEST) return "Sidequest"
  if (interval <= 1) {
    return when (category) {
      TaskCategory.DAILY -> "Codziennie"
      TaskCategory.WEEKLY -> "Co tydzień"
      TaskCategory.MONTHLY -> "Co miesiąc"
      TaskCategory.YEARLY -> "Co rok"
      TaskCategory.SIDEQUEST -> "Sidequest"
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
