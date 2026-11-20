package com.example.data.model

import com.example.data.local.ActivityEventEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.ShopItemEntity
import com.example.data.local.TaskEntity
import com.example.data.local.parseIdCsv
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object ActivityEventType {
  const val TASK_DONE = "task_done"
  const val TASK_SKIP = "task_skip"
  const val FOCUS_DONE = "focus_done"
  const val SHOP_BUY = "shop_buy"
  const val PENALTY = "penalty"
  const val ACH_REWARD = "ach_reward"
}

enum class StatsCompletionPeriod(val label: String) {
  DAILY("Dziennie"),
  WEEKLY("Tygodniowo"),
  YEAR_OVER_YEAR("Rok do roku"),
}

enum class StatsCoinRange(val label: String, val days: Int?) {
  DAYS_7("7 dni", 7),
  DAYS_30("30 dni", 30),
  YEAR_1("Rok", 365),
  ALL("Wszystko", null),
}

data class StatsBarPoint(
  val label: String,
  val count: Int,
  val secondaryNote: String = "",
)

data class TaskReliabilitySummary(
  val name: String,
  val doneCount: Int,
  val onTimeCount: Int,
  val lateOrSkipCount: Int,
  val onTimePercent: Int,
  val isCurrentlyOverdue: Boolean = false,
)

data class CategoryPunctualityStat(
  val category: TaskCategory,
  val onTimeCount: Int,
  val totalDoneCount: Int,
  val onTimePercent: Int,
)

data class ShopSpendRank(
  val icon: String,
  val name: String,
  val purchaseCount: Int,
  val totalCoins: Int,
  val totalGems: Int,
)

data class CoinEconomySummary(
  val coinsEarned: Int,
  val coinsSpentShop: Int,
  val coinsLostPenalty: Int,
  val netCoins: Int,
  val topPurchases: List<ShopSpendRank>,
)

data class PlayerAnalyticsSummary(
  val avgTasksPerDay: Double,
  val activeDaysSpan: Int,
  val todayDoneCount: Int,
  val weekDoneCount: Int,
  val yearDoneCount: Int,
  val totalTasksDone: Int,
  val overallOnTimePercent: Int?,
  val totalFocusMinutes: Int,
  val totalFocusSessions: Int,
  val reliableTasks: List<TaskReliabilitySummary>,
  val delayedTasks: List<TaskReliabilitySummary>,
  val categoryPunctuality: List<CategoryPunctualityStat>,
)

fun computePlayerAnalytics(
  profile: PlayerProfileEntity,
  tasks: List<TaskEntity>,
  events: List<ActivityEventEntity>,
  nowMillis: Long = System.currentTimeMillis(),
): PlayerAnalyticsSummary {
  val doneEvents = events.filter { it.eventType == ActivityEventType.TASK_DONE }
  val skipEvents = events.filter { it.eventType == ActivityEventType.TASK_SKIP }
  val focusEvents = events.filter { it.eventType == ActivityEventType.FOCUS_DONE }

  val todayStart = startOfLocalDayMillis(nowMillis)
  val tomorrowStart = addLocalDaysMillis(todayStart, 1)
  val weekStart = addLocalDaysMillis(todayStart, -6)

  val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
  val currentYear = nowCal.get(Calendar.YEAR)
  val yearStart = Calendar.getInstance().apply {
    set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis

  val todayDoneCount = doneEvents.count { it.timestamp in todayStart until tomorrowStart }
  val weekDoneCount = doneEvents.count { it.timestamp in weekStart until tomorrowStart }
  val yearDoneCount = doneEvents.count { it.timestamp in yearStart until tomorrowStart }

  val earliestTs = events.minOfOrNull { it.timestamp } ?: nowMillis
  val todayEpoch = localEpochDay(nowMillis)
  val earliestEpoch = localEpochDay(earliestTs)
  val activeDaysSpan = (todayEpoch - earliestEpoch + 1L).toInt().coerceAtLeast(1)

  val totalTasksDone = maxOf(doneEvents.size, profile.tasksDone)
  val avgTasksPerDay = if (doneEvents.isNotEmpty()) {
    doneEvents.size.toDouble() / activeDaysSpan.toDouble()
  } else {
    profile.tasksDone.toDouble()
  }

  val overallOnTimePercent = if (doneEvents.isEmpty()) {
    null
  } else {
    ((doneEvents.count { it.onTime }.toDouble() / doneEvents.size.toDouble()) * 100.0).roundToInt()
  }

  val totalFocusMinutes = focusEvents.sumOf { it.durationMinutes.coerceAtLeast(0) }
  val totalFocusSessions = maxOf(focusEvents.size, profile.pomosDone)

  // Per-task reliability aggregation (using taskId if > 0, else lowercase task name)
  data class MutableTaskAcc(
    var name: String,
    var doneCount: Int = 0,
    var onTimeCount: Int = 0,
    var lateCount: Int = 0,
    var skipCount: Int = 0,
  )

  val byTask = linkedMapOf<String, MutableTaskAcc>()
  for (ev in doneEvents) {
    val key = if (ev.taskId > 0L) "id_${ev.taskId}" else "name_${ev.taskName.lowercase()}"
    val acc = byTask.getOrPut(key) { MutableTaskAcc(name = ev.taskName.ifBlank { "Zadanie" }) }
    if (ev.taskName.isNotBlank()) acc.name = ev.taskName
    acc.doneCount++
    if (ev.onTime) acc.onTimeCount++ else acc.lateCount++
  }
  for (ev in skipEvents) {
    val key = if (ev.taskId > 0L) "id_${ev.taskId}" else "name_${ev.taskName.lowercase()}"
    val acc = byTask.getOrPut(key) { MutableTaskAcc(name = ev.taskName.ifBlank { "Zadanie" }) }
    if (ev.taskName.isNotBlank()) acc.name = ev.taskName
    acc.skipCount++
  }

  val taskSummaries = byTask.values.map { acc ->
    val totalAttempts = (acc.doneCount + acc.skipCount).coerceAtLeast(1)
    val onTimePct = ((acc.onTimeCount.toDouble() / totalAttempts.toDouble()) * 100.0).roundToInt()
    TaskReliabilitySummary(
      name = acc.name,
      doneCount = acc.doneCount,
      onTimeCount = acc.onTimeCount,
      lateOrSkipCount = acc.lateCount + acc.skipCount,
      onTimePercent = onTimePct,
    )
  }

  val reliableTasks = taskSummaries
    .filter { it.doneCount >= 1 && it.onTimePercent >= 75 && it.onTimeCount > it.lateOrSkipCount }
    .sortedWith(
      compareByDescending<TaskReliabilitySummary> { it.onTimePercent }
        .thenByDescending { it.doneCount }
    )
    .take(3)

  var delayedTasks = taskSummaries
    .filter { it.lateOrSkipCount >= 1 }
    .sortedWith(
      compareByDescending<TaskReliabilitySummary> { it.lateOrSkipCount }
        .thenBy { it.onTimePercent }
    )
    .take(3)

  if (delayedTasks.isEmpty()) {
    delayedTasks = tasks
      .filter { it.dueTimestamp != null && it.dueTimestamp < nowMillis }
      .sortedBy { it.dueTimestamp }
      .take(3)
      .map { task ->
        TaskReliabilitySummary(
          name = task.name,
          doneCount = task.completedCount,
          onTimeCount = task.completedCount,
          lateOrSkipCount = 1,
          onTimePercent = 0,
          isCurrentlyOverdue = true,
        )
      }
  }

  // Category punctuality
  val categoryPunctuality = doneEvents
    .groupBy { TaskCategory.fromCode(it.categoryCode) }
    .map { (cat, list) ->
      val onTimeCount = list.count { it.onTime }
      val pct = ((onTimeCount.toDouble() / list.size.toDouble()) * 100.0).roundToInt()
      CategoryPunctualityStat(
        category = cat,
        onTimeCount = onTimeCount,
        totalDoneCount = list.size,
        onTimePercent = pct,
      )
    }
    .sortedWith(
      compareByDescending<CategoryPunctualityStat> { it.onTimePercent }
        .thenByDescending { it.totalDoneCount }
    )

  return PlayerAnalyticsSummary(
    avgTasksPerDay = avgTasksPerDay,
    activeDaysSpan = activeDaysSpan,
    todayDoneCount = todayDoneCount,
    weekDoneCount = weekDoneCount,
    yearDoneCount = yearDoneCount,
    totalTasksDone = totalTasksDone,
    overallOnTimePercent = overallOnTimePercent,
    totalFocusMinutes = totalFocusMinutes,
    totalFocusSessions = totalFocusSessions,
    reliableTasks = reliableTasks,
    delayedTasks = delayedTasks,
    categoryPunctuality = categoryPunctuality,
  )
}

fun buildCompletionChartSeries(
  events: List<ActivityEventEntity>,
  period: StatsCompletionPeriod,
  nowMillis: Long = System.currentTimeMillis(),
): List<StatsBarPoint> {
  val doneEvents = events.filter { it.eventType == ActivityEventType.TASK_DONE }
  val todayStart = startOfLocalDayMillis(nowMillis)

  return when (period) {
    StatsCompletionPeriod.DAILY -> {
      val dayFmt = SimpleDateFormat("dd.MM", Locale("pl", "PL"))
      (13 downTo 0).map { offset ->
        val dayStart = addLocalDaysMillis(todayStart, -offset)
        val dayEnd = addLocalDaysMillis(dayStart, 1)
        val count = doneEvents.count { it.timestamp in dayStart until dayEnd }
        val label = if (offset == 0) "Dziś" else dayFmt.format(Date(dayStart))
        StatsBarPoint(label = label, count = count)
      }
    }

    StatsCompletionPeriod.WEEKLY -> {
      val cal = Calendar.getInstance().apply {
        timeInMillis = todayStart
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
      }
      val currentWeekStart = cal.timeInMillis
      val weekFmt = SimpleDateFormat("dd.MM", Locale("pl", "PL"))
      (7 downTo 0).map { offset ->
        val wStart = addLocalDaysMillis(currentWeekStart, -offset * 7)
        val wEnd = addLocalDaysMillis(wStart, 7)
        val count = doneEvents.count { it.timestamp in wStart until wEnd }
        val label = if (offset == 0) "Ten tydz." else weekFmt.format(Date(wStart))
        StatsBarPoint(label = label, count = count)
      }
    }

    StatsCompletionPeriod.YEAR_OVER_YEAR -> {
      val currentYear = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.YEAR)
      val yearsFromEvents = doneEvents.map { ev ->
        Calendar.getInstance().apply { timeInMillis = ev.timestamp }.get(Calendar.YEAR)
      }
      val minYear = (yearsFromEvents.minOrNull() ?: currentYear).coerceAtMost(currentYear - 1)
      val years = (minYear..currentYear).toList().takeLast(5)
      val countsByYear = years.associateWith { year ->
        doneEvents.count { ev ->
          Calendar.getInstance().apply { timeInMillis = ev.timestamp }.get(Calendar.YEAR) == year
        }
      }
      years.mapIndexed { idx, year ->
        val count = countsByYear[year] ?: 0
        val note = if (idx > 0) {
          val prev = countsByYear[years[idx - 1]] ?: 0
          if (prev > 0) {
            val deltaPct = (((count - prev).toDouble() / prev.toDouble()) * 100.0).roundToInt()
            if (deltaPct >= 0) "+$deltaPct% r/r" else "$deltaPct% r/r"
          } else ""
        } else ""
        StatsBarPoint(label = year.toString(), count = count, secondaryNote = note)
      }
    }
  }
}

fun computeCoinEconomySummary(
  profile: PlayerProfileEntity,
  shopItems: List<ShopItemEntity>,
  events: List<ActivityEventEntity>,
  range: StatsCoinRange,
  nowMillis: Long = System.currentTimeMillis(),
): CoinEconomySummary {
  val todayStart = startOfLocalDayMillis(nowMillis)
  val minTimestamp = range.days?.let { days ->
    addLocalDaysMillis(todayStart, -(days - 1))
  } ?: Long.MIN_VALUE

  val inRange = events.filter { it.timestamp >= minTimestamp }
  val shopBuyEvents = inRange.filter { it.eventType == ActivityEventType.SHOP_BUY }

  val earned = if (events.isEmpty() && range == StatsCoinRange.ALL) {
    profile.coinsEarned
  } else {
    inRange.filter { it.coinsDelta > 0 }.sumOf { it.coinsDelta }
  }

  val penalties = inRange
    .filter { it.eventType == ActivityEventType.PENALTY || (it.eventType == ActivityEventType.TASK_SKIP && it.coinsDelta < 0) }
    .sumOf { -it.coinsDelta }

  // If no SHOP_BUY events exist yet (e.g., upgrades from earlier DB versions), fall back to ShopItemEntity.purchaseHistoryCsv
  val hasAnyShopBuyEvents = events.any { it.eventType == ActivityEventType.SHOP_BUY }
  val topPurchases: List<ShopSpendRank>
  val spentShop: Int

  if (hasAnyShopBuyEvents) {
    spentShop = shopBuyEvents.sumOf { (-it.coinsDelta).coerceAtLeast(0) }
    topPurchases = shopBuyEvents
      .groupBy { it.itemName.ifBlank { "Przedmiot" } }
      .map { (name, list) ->
        ShopSpendRank(
          icon = list.firstOrNull { it.itemIcon.isNotBlank() }?.itemIcon ?: "🎁",
          name = name,
          purchaseCount = list.size,
          totalCoins = list.sumOf { (-it.coinsDelta).coerceAtLeast(0) },
          totalGems = list.sumOf { (-it.gemsDelta).coerceAtLeast(0) },
        )
      }
      .sortedWith(
        compareByDescending<ShopSpendRank> { it.totalCoins + it.totalGems * 100 }
          .thenByDescending { it.purchaseCount }
      )
      .take(4)
  } else {
    val legacyRanks = shopItems.mapNotNull { item ->
      val timestamps = parseIdCsv(item.purchaseHistoryCsv).filter { it >= minTimestamp }
      if (timestamps.isEmpty()) null
      else ShopSpendRank(
        icon = item.icon,
        name = item.name,
        purchaseCount = timestamps.size,
        totalCoins = timestamps.size * item.coinPrice,
        totalGems = timestamps.size * item.gemPrice,
      )
    }.sortedWith(
      compareByDescending<ShopSpendRank> { it.totalCoins + it.totalGems * 100 }
        .thenByDescending { it.purchaseCount }
    )
    spentShop = legacyRanks.sumOf { it.totalCoins }
    topPurchases = legacyRanks.take(4)
  }

  return CoinEconomySummary(
    coinsEarned = earned,
    coinsSpentShop = spentShop,
    coinsLostPenalty = penalties,
    netCoins = earned - spentShop - penalties,
    topPurchases = topPurchases,
  )
}
