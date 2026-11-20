package com.example

import com.example.data.local.TaskEntity
import com.example.data.model.MILLIS_PER_DAY
import com.example.data.model.PenaltyMode
import com.example.data.model.TaskDifficulty
import com.example.data.model.TaskEffortBucket
import com.example.data.model.TaskSortDirection
import com.example.data.model.buildMonthCalendarGrid
import com.example.ui.screens.buildHorizonDayBuckets
import com.example.data.model.calculateAutoPenaltyRange
import com.example.data.model.calculateAutoRewardAndPenalty
import com.example.data.model.groupTasksByCategory
import com.example.data.model.groupTasksByDifficulty
import com.example.data.model.sortTasksByDifficulty
import com.example.data.model.sortTasksByReward
import com.example.data.model.sortTasksByTimeLeft
import com.example.data.model.startOfLocalDayMillis
import com.example.data.model.startOfLocalMonthMillis
import com.example.data.model.taskRewardSortValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun autoRewardScalesWithDifficultyEffortAttributesAndPenalty() {
    val easy = calculateAutoRewardAndPenalty(
      difficulty = TaskDifficulty.EASY,
      effort = TaskEffortBucket.SHORT,
      attributeCount = 1,
      hasPenalty = false,
      penaltyMode = PenaltyMode.NONE,
    )
    val hardPenalized = calculateAutoRewardAndPenalty(
      difficulty = TaskDifficulty.HARD,
      effort = TaskEffortBucket.LONG,
      attributeCount = 3,
      hasPenalty = true,
      penaltyMode = PenaltyMode.ON_DUE_ONCE,
    )

    assertTrue(hardPenalized.exp > easy.exp)
    assertTrue(hardPenalized.coins > easy.coins)
    assertEquals(0, easy.penaltyMinCoins)
    assertEquals(0, easy.penaltyMaxCoins)
    assertTrue(hardPenalized.penaltyMinCoins >= 3)
    assertTrue(hardPenalized.penaltyMaxCoins > hardPenalized.penaltyMinCoins)
  }

  @Test
  fun autoPenaltyScalesByModeAndRewardCoins() {
    val (everyMin, everyMax) = calculateAutoPenaltyRange(
      baseCoins = 40,
      difficulty = TaskDifficulty.MEDIUM,
      penaltyMode = PenaltyMode.EVERY_MISSED_PERIOD,
    )
    val (skipMin, skipMax) = calculateAutoPenaltyRange(
      baseCoins = 40,
      difficulty = TaskDifficulty.MEDIUM,
      penaltyMode = PenaltyMode.ON_SKIP_ONLY,
    )

    assertTrue(everyMin >= 3 && everyMax > everyMin)
    assertTrue(skipMin >= everyMin && skipMax > skipMin)
  }

  @Test
  fun timeLeftSortingOrdersAscendingAndDescendingWithNoDueLast() {
    val now = 1_000_000_000L
    val soon = TaskEntity(id = 1, name = "Soon", dueTimestamp = now + 1_000L)
    val later = TaskEntity(id = 2, name = "Later", dueTimestamp = now + 10_000L)
    val noDue = TaskEntity(id = 3, name = "NoDue", dueTimestamp = null)

    val asc = sortTasksByTimeLeft(listOf(noDue, later, soon), TaskSortDirection.ASCENDING)
    assertEquals(listOf("Soon", "Later", "NoDue"), asc.map { it.name })

    val desc = sortTasksByTimeLeft(listOf(noDue, soon, later), TaskSortDirection.DESCENDING)
    assertEquals(listOf("Later", "Soon", "NoDue"), desc.map { it.name })
  }

  @Test
  fun difficultySortingOrdersVeryEasyToExtremeAndReversed() {
    val ve = TaskEntity(id = 1, name = "VE", difficultyCode = TaskDifficulty.VERY_EASY.code)
    val med = TaskEntity(id = 2, name = "M", difficultyCode = TaskDifficulty.MEDIUM.code)
    val ext = TaskEntity(id = 3, name = "X", difficultyCode = TaskDifficulty.EXTREME.code)

    val asc = sortTasksByDifficulty(listOf(ext, ve, med), TaskSortDirection.ASCENDING)
    assertEquals(listOf("VE", "M", "X"), asc.map { it.name })

    val desc = sortTasksByDifficulty(listOf(ve, med, ext), TaskSortDirection.DESCENDING)
    assertEquals(listOf("X", "M", "VE"), desc.map { it.name })

    val groupedDesc = groupTasksByDifficulty(listOf(ve, med, ext), TaskSortDirection.DESCENDING)
    assertEquals(
      listOf(TaskDifficulty.EXTREME, TaskDifficulty.MEDIUM, TaskDifficulty.VERY_EASY),
      groupedDesc.map { it.first },
    )
  }

  @Test
  fun rewardSortingTreatsOneGemAs100Coins() {
    val coinsOnly = TaskEntity(id = 1, name = "90 coins", coinReward = 90, gemReward = 0)
    val oneGemTenCoins = TaskEntity(id = 2, name = "1 gem 10 coins", coinReward = 10, gemReward = 1)
    val twoGems = TaskEntity(id = 3, name = "2 gems", coinReward = 0, gemReward = 2)

    assertEquals(90, taskRewardSortValue(coinsOnly))
    assertEquals(110, taskRewardSortValue(oneGemTenCoins))
    assertEquals(200, taskRewardSortValue(twoGems))

    val desc = sortTasksByReward(listOf(coinsOnly, twoGems, oneGemTenCoins), TaskSortDirection.DESCENDING)
    assertEquals(listOf("2 gems", "1 gem 10 coins", "90 coins"), desc.map { it.name })

    val asc = sortTasksByReward(listOf(twoGems, coinsOnly, oneGemTenCoins), TaskSortDirection.ASCENDING)
    assertEquals(listOf("90 coins", "1 gem 10 coins", "2 gems"), asc.map { it.name })
  }

  @Test
  fun calendarGridAndCategoryGroupingWorkCleanly() {
    val grid = buildMonthCalendarGrid(startOfLocalMonthMillis())
    assertTrue(grid.isNotEmpty() && grid.size % 7 == 0)

    val today = startOfLocalDayMillis()
    val tomorrow = today + MILLIS_PER_DAY
    val t1 = TaskEntity(id = 1, name = "D", categoryCode = "d", dueTimestamp = today)
    val t2 = TaskEntity(id = 2, name = "O", categoryCode = "o", dueTimestamp = tomorrow)

    val grouped = groupTasksByCategory(listOf(t2, t1), TaskSortDirection.ASCENDING)
    assertEquals(2, grouped.size)
    assertEquals("d", grouped.first().first.code)

    val threeDays = buildHorizonDayBuckets(listOf(t1, t2), today, 3, TaskSortDirection.ASCENDING)
    assertEquals(3, threeDays.size)
    assertEquals(1, threeDays[0].second.size)
    assertEquals(1, threeDays[1].second.size)
    assertEquals(0, threeDays[2].second.size)
    assertEquals(14, buildHorizonDayBuckets(listOf(t1, t2), today, 14, TaskSortDirection.ASCENDING).size)
  }
}

