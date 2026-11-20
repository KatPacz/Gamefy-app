package com.example.data.model

import java.util.Calendar
import kotlin.random.Random

const val RANDOM_SIDE_QUEST_DAILY_LIMIT = 3
const val RANDOM_SIDE_QUEST_BOARD_SIZE = 6

data class RandomSideQuestBoardState(
  val dayKey: Long,
  val quests: List<RandomSideQuestTemplate>,
  val acceptedIdsToday: Set<String>,
  val acceptedCountToday: Int,
) {
  val remainingSlots: Int
    get() = (RANDOM_SIDE_QUEST_DAILY_LIMIT - acceptedCountToday).coerceAtLeast(0)

  val isDailyLimitReached: Boolean
    get() = remainingSlots <= 0
}

/**
 * Local calendar day identifier (YYYYMMDD) in the device's local timezone.
 * Guarantees that the daily Random Side Quest limit resets at local midnight.
 */
fun localCalendarDayKey(nowMillis: Long = System.currentTimeMillis()): Long {
  val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
  val year = cal.get(Calendar.YEAR).toLong()
  val month = (cal.get(Calendar.MONTH) + 1).toLong()
  val day = cal.get(Calendar.DAY_OF_MONTH).toLong()
  return year * 10_000L + month * 100L + day
}

/**
 * Picks up to [count] distinct Random Side Quests in randomized order from [catalog].
 * Works seamlessly both for the initial 6-quest MVP catalog and any future expansion.
 */
fun pickRandomSideQuests(
  catalog: List<RandomSideQuestTemplate> = RANDOM_SIDE_QUEST_TEMPLATES,
  count: Int = RANDOM_SIDE_QUEST_BOARD_SIZE,
  random: Random = Random.Default,
): List<RandomSideQuestTemplate> {
  if (count <= 0 || catalog.isEmpty()) return emptyList()
  val uniqueById = catalog.distinctBy { it.id }
  return uniqueById.shuffled(random).take(count)
}

/**
 * Preserves today's previously rolled quest IDs in their saved order,
 * topping up with remaining distinct random templates if the catalog changed.
 */
fun resolveDailyBoardIds(
  catalog: List<RandomSideQuestTemplate> = RANDOM_SIDE_QUEST_TEMPLATES,
  savedIdsCsv: String,
  count: Int = RANDOM_SIDE_QUEST_BOARD_SIZE,
  random: Random = Random.Default,
): List<String> {
  val uniqueCatalog = catalog.distinctBy { it.id }
  val byId = uniqueCatalog.associateBy { it.id }
  val targetSize = count.coerceAtMost(uniqueCatalog.size)
  if (targetSize <= 0) return emptyList()

  val preserved = savedIdsCsv
    .split(',')
    .map { it.trim() }
    .filter { it.isNotEmpty() && it in byId }
    .distinct()
    .take(targetSize)

  if (preserved.size >= targetSize) return preserved

  val preservedSet = preserved.toSet()
  val extras = uniqueCatalog
    .filter { it.id !in preservedSet }
    .shuffled(random)
    .take(targetSize - preserved.size)
    .map { it.id }

  return preserved + extras
}

fun resolveBoardTemplates(
  boardIds: List<String>,
  catalog: List<RandomSideQuestTemplate> = RANDOM_SIDE_QUEST_TEMPLATES,
): List<RandomSideQuestTemplate> {
  val byId = catalog.associateBy { it.id }
  return boardIds.mapNotNull { byId[it] }
}

fun formatRandomQuestExpSummary(template: RandomSideQuestTemplate): String {
  val weightsText = template.attributeWeights.entries.joinToString(", ") { (attr, pct) ->
    "$attr $pct%"
  }
  return if (weightsText.isBlank()) {
    "+${template.exp} EXP"
  } else {
    "+${template.exp} EXP ($weightsText)"
  }
}

fun formatRandomQuestTaskDescription(template: RandomSideQuestTemplate): String {
  return buildString {
    append("„${template.story}”")
    append("\n\nCel: ${template.goal}")
  }
}
