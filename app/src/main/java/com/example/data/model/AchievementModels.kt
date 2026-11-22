package com.example.data.model

import com.example.data.local.ActivityEventEntity
import com.example.data.local.AttributeEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.TaskEntity
import com.example.data.local.parseStringCsv
import org.json.JSONArray
import java.util.Calendar
import kotlin.math.max

enum class AchievementCategory(val code: String, val label: String) {
  REGULARITY("REGULARITY", "Regularność"),
  PUNCTUALITY("PUNCTUALITY", "Terminowość"),
  FOCUS("FOCUS", "Skupienie"),
  CHARACTER("CHARACTER", "Postać"),
  SHOP("SHOP", "Sklep i nagrody"),
  ADVENTURE("ADVENTURE", "Tablica Przygód"),
  ORGANIZATION("ORGANIZATION", "Organizacja"),
  DATA_CARE("DATA_CARE", "Dbanie o dane"),
  SECRET("SECRET", "Tajne");

  companion object {
    fun fromCode(code: String): AchievementCategory =
      entries.find { it.code.equals(code, ignoreCase = true) } ?: REGULARITY
  }
}

enum class AchievementTier(val code: String, val label: String) {
  SINGLE("SINGLE", ""),
  BRONZE("BRONZE", "Brąz"),
  SILVER("SILVER", "Srebro"),
  GOLD("GOLD", "Złoto");

  companion object {
    fun fromCode(code: String): AchievementTier =
      entries.find { it.code.equals(code, ignoreCase = true) } ?: SINGLE
  }
}

enum class AchievementMetric(val code: String) {
  TASKS_DONE("TASKS_DONE"),
  BEST_STREAK("BEST_STREAK"),
  COMEBACK_AFTER_BREAK("COMEBACK_AFTER_BREAK"),
  BEST_ON_TIME_STREAK("BEST_ON_TIME_STREAK"),
  ON_TIME_TASKS_DONE("ON_TIME_TASKS_DONE"),
  CLEAN_WEEK("CLEAN_WEEK"),
  POMOS_DONE("POMOS_DONE"),
  TOTAL_FOCUS_MINUTES("TOTAL_FOCUS_MINUTES"),
  FULL_POMODORO_CYCLES("FULL_POMODORO_CYCLES"),
  PLAYER_LEVEL("PLAYER_LEVEL"),
  MAX_ATTRIBUTE_LEVEL("MAX_ATTRIBUTE_LEVEL"),
  BALANCED_ATTRIBUTES("BALANCED_ATTRIBUTES"),
  ITEMS_BOUGHT("ITEMS_BOUGHT"),
  COINS_EARNED("COINS_EARNED"),
  SPENT_ALL_GEMS("SPENT_ALL_GEMS"),
  WELL_EARNED_REWARD("WELL_EARNED_REWARD"),
  SIDE_QUESTS_COMPLETED("SIDE_QUESTS_COMPLETED"),
  FULL_ADVENTURE_BOARD_DAY("FULL_ADVENTURE_BOARD_DAY"),
  SUBTASKS_COMPLETED("SUBTASKS_COMPLETED"),
  ORG_ITEMS_COUNT("ORG_ITEMS_COUNT"),
  ALL_TASKS_IN_FOLDERS("ALL_TASKS_IN_FOLDERS"),
  BULK_UPDATES_DONE("BULK_UPDATES_DONE"),
  DISTINCT_BACKUP_MONTHS("DISTINCT_BACKUP_MONTHS"),
  CUSTOM_TASKS_CREATED("CUSTOM_TASKS_CREATED"),
  BOUGHT_MOST_EXPENSIVE("BOUGHT_MOST_EXPENSIVE"),
  COMPLETED_SUBTASK_CHAIN("COMPLETED_SUBTASK_CHAIN"),
  COMPLETED_LAST_MINUTE("COMPLETED_LAST_MINUTE"),
  COMPLETED_NO_LETTERS_TASK("COMPLETED_NO_LETTERS_TASK"),
  COMPLETED_SMALL_TASK("COMPLETED_SMALL_TASK");

  fun readValue(stats: PlayerStatsSnapshot): Int = when (this) {
    TASKS_DONE -> stats.tasksDone
    BEST_STREAK -> maxOf(stats.currentStreak, stats.bestStreak)
    COMEBACK_AFTER_BREAK -> if (stats.comebackAfterBreak) 1 else 0
    BEST_ON_TIME_STREAK -> maxOf(stats.currentOnTimeStreak, stats.bestOnTimeStreak)
    ON_TIME_TASKS_DONE -> stats.onTimeTasksDone
    CLEAN_WEEK -> if (stats.hasCleanWeek) 1 else 0
    POMOS_DONE -> stats.pomosDone
    TOTAL_FOCUS_MINUTES -> stats.totalFocusMinutes
    FULL_POMODORO_CYCLES -> stats.fullPomodoroCycles
    PLAYER_LEVEL -> calculateLevel(stats.exp).level
    MAX_ATTRIBUTE_LEVEL -> stats.maxAttributeLevel
    BALANCED_ATTRIBUTES -> if (stats.hasBalancedAttributes) 1 else 0
    ITEMS_BOUGHT -> stats.itemsBought
    COINS_EARNED -> stats.coinsEarned
    SPENT_ALL_GEMS -> if (stats.spentAllGems) 1 else 0
    WELL_EARNED_REWARD -> if (stats.wellEarnedRewardBought) 1 else 0
    SIDE_QUESTS_COMPLETED -> stats.sideQuestsCompleted
    FULL_ADVENTURE_BOARD_DAY -> if (stats.fullAdventureBoardDay) 1 else 0
    SUBTASKS_COMPLETED -> stats.subtasksCompletedCount
    ORG_ITEMS_COUNT -> stats.orgItemsCount
    ALL_TASKS_IN_FOLDERS -> if (stats.allActiveTasksInFolders) 1 else 0
    BULK_UPDATES_DONE -> stats.bulkUpdatesDone
    DISTINCT_BACKUP_MONTHS -> stats.distinctBackupMonths
    CUSTOM_TASKS_CREATED -> stats.customTasksCreated
    BOUGHT_MOST_EXPENSIVE -> if (stats.boughtMostExpensive) 1 else 0
    COMPLETED_SUBTASK_CHAIN -> if (stats.completedSubtaskChain) 1 else 0
    COMPLETED_LAST_MINUTE -> if (stats.completedLastMinute) 1 else 0
    COMPLETED_NO_LETTERS_TASK -> if (stats.completedNoLettersTask) 1 else 0
    COMPLETED_SMALL_TASK -> if (stats.completedSmallTask) 1 else 0
  }

  companion object {
    fun fromCode(code: String): AchievementMetric =
      entries.find { it.code.equals(code, ignoreCase = true) } ?: TASKS_DONE
  }
}

data class PlayerStatsSnapshot(
  val exp: Int = 0,
  val tasksDone: Int = 0,
  val pomosDone: Int = 0,
  val coinsEarned: Int = 0,
  val itemsBought: Int = 0,
  val currentStreak: Int = 0,
  val bestStreak: Int = 0,
  val customTasksCreated: Int = 0,
  val boughtMostExpensive: Boolean = false,
  val completedSubtaskChain: Boolean = false,
  val comebackAfterBreak: Boolean = false,
  val currentOnTimeStreak: Int = 0,
  val bestOnTimeStreak: Int = 0,
  val onTimeTasksDone: Int = 0,
  val hasCleanWeek: Boolean = false,
  val totalFocusMinutes: Int = 0,
  val fullPomodoroCycles: Int = 0,
  val maxAttributeLevel: Int = 1,
  val hasBalancedAttributes: Boolean = false,
  val spentAllGems: Boolean = false,
  val wellEarnedRewardBought: Boolean = false,
  val sideQuestsCompleted: Int = 0,
  val fullAdventureBoardDay: Boolean = false,
  val subtasksCompletedCount: Int = 0,
  val orgItemsCount: Int = 0,
  val allActiveTasksInFolders: Boolean = false,
  val bulkUpdatesDone: Int = 0,
  val distinctBackupMonths: Int = 0,
  val completedLastMinute: Boolean = false,
  val completedNoLettersTask: Boolean = false,
  val completedSmallTask: Boolean = false,
)

data class AchievementTierDefinition(
  val id: String,
  val tier: AchievementTier,
  val threshold: Int,
  val rewardExp: Int = 0,
  val rewardCoins: Int = 0,
  val rewardGems: Int = 0,
  val tierDescription: String = "",
) {
  fun rewardSummary(): String {
    val parts = mutableListOf<String>()
    if (rewardExp > 0) parts.add("+$rewardExp EXP")
    if (rewardCoins > 0) parts.add("+$rewardCoins monet")
    if (rewardGems > 0) parts.add("+${formatGemsLabel(rewardGems)}")
    return parts.joinToString(" · ")
  }
}

data class AchievementGroupDefinition(
  val id: String,
  val title: String,
  val description: String,
  val secretHint: String = "",
  val icon: String,
  val category: AchievementCategory,
  val metric: AchievementMetric,
  val unit: String = "",
  val displayDivisor: Int = 1,
  val displayUnit: String = "",
  val isSecret: Boolean = false,
  val tiers: List<AchievementTierDefinition>,
) {
  val isTiered: Boolean
    get() = tiers.size > 1

  fun formatProgressValue(rawValue: Int): String {
    return if (displayDivisor > 1) {
      val divided = rawValue / displayDivisor
      if (displayUnit.isNotBlank()) "$divided $displayUnit" else "$divided"
    } else {
      if (unit.isNotBlank()) "$rawValue $unit" else "$rawValue"
    }
  }

  fun formatProgressPair(currentRaw: Int, targetRaw: Int): String {
    val clamped = currentRaw.coerceIn(0, targetRaw)
    return if (displayDivisor > 1) {
      val curDiv = clamped / displayDivisor
      val tgtDiv = (targetRaw / displayDivisor).coerceAtLeast(1)
      if (displayUnit.isNotBlank()) "$curDiv / $tgtDiv $displayUnit" else "$curDiv / $tgtDiv"
    } else {
      if (unit.isNotBlank()) "$clamped / $targetRaw $unit" else "$clamped / $targetRaw"
    }
  }
}

/**
 * Flattened single unlocked tier/badge used for evaluation, notifications, and banners.
 */
data class AchievementDefinition(
  val id: String,
  val groupId: String,
  val icon: String,
  val title: String,
  val description: String,
  val category: AchievementCategory,
  val tier: AchievementTier,
  val metric: AchievementMetric,
  val threshold: Int,
  val rewardExp: Int = 0,
  val rewardCoins: Int = 0,
  val rewardGems: Int = 0,
  val isSecret: Boolean = false,
) {
  fun isUnlocked(stats: PlayerStatsSnapshot): Boolean = metric.readValue(stats) >= threshold

  fun rewardSummary(): String {
    val parts = mutableListOf<String>()
    if (rewardExp > 0) parts.add("+$rewardExp EXP")
    if (rewardCoins > 0) parts.add("+$rewardCoins monet")
    if (rewardGems > 0) parts.add("+${formatGemsLabel(rewardGems)}")
    return parts.joinToString(" · ")
  }
}

object AchievementCatalogLoader {
  fun parseJson(rawJson: String): List<AchievementGroupDefinition> {
    val array = JSONArray(rawJson)
    val result = mutableListOf<AchievementGroupDefinition>()
    for (i in 0 until array.length()) {
      val obj = array.getJSONObject(i)
      val tiersArr = obj.getJSONArray("tiers")
      val tiers = mutableListOf<AchievementTierDefinition>()
      for (t in 0 until tiersArr.length()) {
        val tObj = tiersArr.getJSONObject(t)
        tiers.add(
          AchievementTierDefinition(
            id = tObj.getString("id"),
            tier = AchievementTier.fromCode(tObj.optString("tier", "SINGLE")),
            threshold = tObj.getInt("threshold"),
            rewardExp = tObj.optInt("rewardExp", 0),
            rewardCoins = tObj.optInt("rewardCoins", 0),
            rewardGems = tObj.optInt("rewardGems", 0),
            tierDescription = tObj.optString("tierDescription", ""),
          )
        )
      }
      result.add(
        AchievementGroupDefinition(
          id = obj.getString("id"),
          title = obj.getString("title"),
          description = obj.getString("description"),
          secretHint = obj.optString("secretHint", ""),
          icon = obj.optString("icon", "trophy"),
          category = AchievementCategory.fromCode(obj.optString("category", "REGULARITY")),
          metric = AchievementMetric.fromCode(obj.getString("metric")),
          unit = obj.optString("unit", ""),
          displayDivisor = obj.optInt("displayDivisor", 1).coerceAtLeast(1),
          displayUnit = obj.optString("displayUnit", ""),
          isSecret = obj.optBoolean("isSecret", false),
          tiers = tiers,
        )
      )
    }
    return result
  }

  fun loadDefaultGroups(): List<AchievementGroupDefinition> {
    val stream = AchievementCatalogLoader::class.java.classLoader
      ?.getResourceAsStream("achievements.json")
      ?: return emptyList()
    return stream.bufferedReader(Charsets.UTF_8).use { parseJson(it.readText()) }
  }
}

val GAMEFY_ACHIEVEMENT_GROUPS: List<AchievementGroupDefinition> by lazy {
  AchievementCatalogLoader.loadDefaultGroups()
}

val GAMEFY_ACHIEVEMENTS: List<AchievementDefinition> by lazy {
  GAMEFY_ACHIEVEMENT_GROUPS.flatMap { group ->
    group.tiers.map { tier ->
      val displayTitle = if (group.isTiered && tier.tier != AchievementTier.SINGLE) {
        "${group.title} (${tier.tier.label})"
      } else {
        group.title
      }
      AchievementDefinition(
        id = tier.id,
        groupId = group.id,
        icon = group.icon,
        title = displayTitle,
        description = tier.tierDescription.ifBlank { group.description },
        category = group.category,
        tier = tier.tier,
        metric = group.metric,
        threshold = tier.threshold,
        rewardExp = tier.rewardExp,
        rewardCoins = tier.rewardCoins,
        rewardGems = tier.rewardGems,
        isSecret = group.isSecret,
      )
    }
  }
}

// --- Healthy Streak Freeze helpers (1 free rest day per calendar week) ---

fun isoWeekKeyForEpochDay(epochDay: Long): Int {
  val approxMillis = epochDay * MILLIS_PER_DAY + MILLIS_PER_DAY / 2
  val cal = Calendar.getInstance().apply {
    firstDayOfWeek = Calendar.MONDAY
    minimalDaysInFirstWeek = 4
    timeInMillis = approxMillis
  }
  val weekYear = cal.weekYear
  val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
  return weekYear * 100 + weekOfYear
}

fun isoWeekKeyForTimestamp(nowMillis: Long = System.currentTimeMillis()): Int =
  isoWeekKeyForEpochDay(localEpochDay(nowMillis))

data class StreakUpdateResult(
  val newStreak: Int,
  val newBestStreak: Int,
  val freezeUsedWeekKey: Int,
  val usedFreezeNow: Boolean,
  val isComebackAfterBreak: Boolean,
)

fun computeNextStreakWithFreeze(
  currentStreak: Int,
  bestStreak: Int,
  lastTaskEpochDay: Long,
  freezeUsedWeekKey: Int,
  todayEpochDay: Long,
): StreakUpdateResult {
  if (lastTaskEpochDay < 0L) {
    val next = max(1, currentStreak)
    return StreakUpdateResult(
      newStreak = next,
      newBestStreak = max(bestStreak, next),
      freezeUsedWeekKey = freezeUsedWeekKey,
      usedFreezeNow = false,
      isComebackAfterBreak = false,
    )
  }

  val gapDays = todayEpochDay - lastTaskEpochDay
  return when {
    gapDays <= 0L -> {
      val next = max(1, currentStreak)
      StreakUpdateResult(
        newStreak = next,
        newBestStreak = max(bestStreak, next),
        freezeUsedWeekKey = freezeUsedWeekKey,
        usedFreezeNow = false,
        isComebackAfterBreak = false,
      )
    }
    gapDays == 1L -> {
      val next = currentStreak + 1
      StreakUpdateResult(
        newStreak = next,
        newBestStreak = max(bestStreak, next),
        freezeUsedWeekKey = freezeUsedWeekKey,
        usedFreezeNow = false,
        isComebackAfterBreak = false,
      )
    }
    gapDays == 2L && currentStreak > 0 -> {
      val missedDay = todayEpochDay - 1L
      val missedWeekKey = isoWeekKeyForEpochDay(missedDay)
      if (freezeUsedWeekKey != missedWeekKey) {
        val next = currentStreak + 1
        StreakUpdateResult(
          newStreak = next,
          newBestStreak = max(bestStreak, next),
          freezeUsedWeekKey = missedWeekKey,
          usedFreezeNow = true,
          isComebackAfterBreak = false,
        )
      } else {
        StreakUpdateResult(
          newStreak = 1,
          newBestStreak = max(bestStreak, 1),
          freezeUsedWeekKey = freezeUsedWeekKey,
          usedFreezeNow = false,
          isComebackAfterBreak = false,
        )
      }
    }
    else -> {
      StreakUpdateResult(
        newStreak = 1,
        newBestStreak = max(bestStreak, 1),
        freezeUsedWeekKey = freezeUsedWeekKey,
        usedFreezeNow = false,
        isComebackAfterBreak = gapDays >= 7L,
      )
    }
  }
}

fun isWeeklyStreakFreezeAvailable(
  freezeUsedWeekKey: Int,
  nowMillis: Long = System.currentTimeMillis(),
): Boolean = freezeUsedWeekKey != isoWeekKeyForTimestamp(nowMillis)

fun yearMonthKey(timestampMillis: Long): String {
  val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
  val y = cal.get(Calendar.YEAR)
  val m = cal.get(Calendar.MONTH) + 1
  return "%04d-%02d".format(y, m)
}

fun buildPlayerStatsSnapshot(
  profile: PlayerProfileEntity,
  tasks: List<TaskEntity>,
  attributes: List<AttributeEntity>,
  events: List<ActivityEventEntity>,
  nowMillis: Long = System.currentTimeMillis(),
): PlayerStatsSnapshot {
  val attrLevels = attributes.map { calculateLevel(it.exp).level }
  val maxAttrLevel = attrLevels.maxOrNull() ?: 1
  val hasBalancedAttrs = attrLevels.size >= 2 &&
    attrLevels.min() >= 2 &&
    attrLevels.distinct().size == 1

  val activeTasks = tasks.filter { it.isActive }
  val allActiveInFolders = activeTasks.size >= 3 && activeTasks.all { it.folderId > 0L }

  val sevenDaysAgo = nowMillis - 7L * MILLIS_PER_DAY
  val recentEvents = events.filter { it.timestamp >= sevenDaysAgo }
  val doneLast7d = recentEvents.filter { it.eventType == ActivityEventType.TASK_DONE }
  val hasOverdueActive = activeTasks.any {
    it.startTimestamp <= nowMillis && it.dueTimestamp != null && it.dueTimestamp < nowMillis
  }
  val hasCleanWeek = !hasOverdueActive &&
    doneLast7d.size >= 5 &&
    doneLast7d.all { it.onTime } &&
    recentEvents.none {
      it.eventType == ActivityEventType.PENALTY || it.eventType == ActivityEventType.TASK_SKIP
    }

  val historyFocusMinutes = events
    .filter { it.eventType == ActivityEventType.FOCUS_DONE }
    .sumOf { it.durationMinutes }
  val historyOnTimeCount = events.count {
    it.eventType == ActivityEventType.TASK_DONE && it.onTime
  }

  val parsedBackupMonths = parseStringCsv(profile.backupMonthsCsv)
  val backupMonthsCount = maxOf(
    parsedBackupMonths.size,
    if (profile.lastBackupExportTimestamp > 0L) 1 else 0,
  )

  return PlayerStatsSnapshot(
    exp = profile.exp,
    tasksDone = profile.tasksDone,
    pomosDone = profile.pomosDone,
    coinsEarned = profile.coinsEarned,
    itemsBought = profile.itemsBought,
    currentStreak = profile.currentStreak,
    bestStreak = profile.bestStreak,
    customTasksCreated = profile.customTasksCreated,
    boughtMostExpensive = profile.boughtMostExpensive,
    completedSubtaskChain = profile.completedSubtaskChain,
    comebackAfterBreak = profile.comebackAfterBreak,
    currentOnTimeStreak = profile.currentOnTimeStreak,
    bestOnTimeStreak = profile.bestOnTimeStreak,
    onTimeTasksDone = maxOf(profile.onTimeTasksDone, historyOnTimeCount),
    hasCleanWeek = hasCleanWeek,
    totalFocusMinutes = maxOf(profile.totalFocusMinutes, historyFocusMinutes),
    fullPomodoroCycles = profile.fullPomodoroCycles,
    maxAttributeLevel = maxAttrLevel,
    hasBalancedAttributes = hasBalancedAttrs,
    spentAllGems = profile.spentAllGems,
    wellEarnedRewardBought = profile.wellEarnedRewardBought,
    sideQuestsCompleted = profile.sideQuestsCompleted,
    fullAdventureBoardDay = profile.fullAdventureBoardDay,
    subtasksCompletedCount = profile.subtasksCompletedCount,
    orgItemsCount = profile.orgItemsCreated,
    allActiveTasksInFolders = allActiveInFolders,
    bulkUpdatesDone = profile.bulkUpdatesDone,
    distinctBackupMonths = backupMonthsCount,
    completedLastMinute = profile.completedLastMinute,
    completedNoLettersTask = profile.completedNoLettersTask,
    completedSmallTask = profile.completedSmallTask,
  )
}

data class AchievementGroupProgress(
  val group: AchievementGroupDefinition,
  val currentValue: Int,
  val unlockedTierIds: Set<String>,
  val nextLockedTier: AchievementTierDefinition?,
  val highestUnlockedTier: AchievementTierDefinition?,
  val progressFraction: Float,
) {
  val isFullyUnlocked: Boolean
    get() = nextLockedTier == null
  val hasAnyUnlocked: Boolean
    get() = unlockedTierIds.isNotEmpty()
}

fun computeGroupProgress(
  group: AchievementGroupDefinition,
  stats: PlayerStatsSnapshot,
  unlockedIds: Set<String>,
): AchievementGroupProgress {
  val currentValue = group.metric.readValue(stats)
  val unlockedInGroup = group.tiers.filter { it.id in unlockedIds }
  val unlockedSet = unlockedInGroup.map { it.id }.toSet()
  val nextLocked = group.tiers.firstOrNull { it.id !in unlockedSet }
  val highestUnlocked = unlockedInGroup.lastOrNull()
  val fraction = if (nextLocked == null) {
    1f
  } else if (nextLocked.threshold <= 0) {
    0f
  } else {
    (currentValue.toFloat() / nextLocked.threshold.toFloat()).coerceIn(0f, 1f)
  }
  return AchievementGroupProgress(
    group = group,
    currentValue = currentValue,
    unlockedTierIds = unlockedSet,
    nextLockedTier = nextLocked,
    highestUnlockedTier = highestUnlocked,
    progressFraction = fraction,
  )
}

