package com.example.widgets

import com.example.data.local.ActivityEventEntity
import com.example.data.local.TaskEntity
import com.example.data.model.ActivityEventType
import com.example.data.model.addLocalDaysMillis
import com.example.data.model.formatDurationShort
import com.example.data.model.startOfLocalDayMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TodayWidgetRow(
  val taskId: Long,
  val title: String,
  val subtitle: String,
  val isOverdue: Boolean = false,
  val isPendingUndo: Boolean = false,
)

data class TodayTasksWidgetSnapshot(
  val completedTodayCount: Int,
  val totalTodayCount: Int,
  val rows: List<TodayWidgetRow>,
) {
  val headerCounterText: String
    get() = "Dziś: $completedTodayCount/$totalTodayCount"
}

object TodayTasksWidgetLogic {
  const val MAX_WIDGET_ROWS = 5

  fun computeMaxRowsForHeightDp(heightDp: Int): Int = when {
    heightDp < 155 -> 2
    heightDp < 215 -> 3
    heightDp < 275 -> 4
    else -> MAX_WIDGET_ROWS
  }

  fun buildSnapshot(
    tasks: List<TaskEntity>,
    activityEvents: List<ActivityEventEntity>,
    pendingUndos: List<PendingWidgetUndo>,
    nowMillis: Long = System.currentTimeMillis(),
    maxRows: Int = 4,
  ): TodayTasksWidgetSnapshot {
    val safeMaxRows = maxRows.coerceIn(1, MAX_WIDGET_ROWS)
    val todayStart = startOfLocalDayMillis(nowMillis)
    val tomorrowStart = addLocalDaysMillis(todayStart, 1)

    val completedTodayCount = activityEvents.count { ev ->
      ev.eventType == ActivityEventType.TASK_DONE && ev.timestamp >= todayStart
    }

    val pendingUndoIds = pendingUndos.map { it.taskId }.toSet()
    val activeTodayTasks = tasks
      .filter { task ->
        task.isActive &&
          task.startTimestamp <= nowMillis &&
          task.id !in pendingUndoIds &&
          (task.dueTimestamp == null || task.dueTimestamp < tomorrowStart)
      }
      .sortedWith(
        compareBy<TaskEntity>(
          { task ->
            val due = task.dueTimestamp
            when {
              due != null && due < nowMillis -> 0
              due != null -> 1
              else -> 2
            }
          },
          { it.dueTimestamp ?: Long.MAX_VALUE },
          { it.id },
        )
      )

    val totalTodayCount = completedTodayCount + activeTodayTasks.size

    val rows = mutableListOf<TodayWidgetRow>()
    // Recently completed tasks remain grayed-out with "Cofnij" for a few seconds before disappearing
    pendingUndos.forEach { undo ->
      if (rows.size < safeMaxRows) {
        rows.add(
          TodayWidgetRow(
            taskId = undo.taskId,
            title = undo.taskName,
            subtitle = "Ukończono",
            isOverdue = false,
            isPendingUndo = true,
          )
        )
      }
    }

    val timeFmt = SimpleDateFormat("HH:mm", Locale("pl", "PL"))
    activeTodayTasks.forEach { task ->
      if (rows.size < safeMaxRows) {
        val due = task.dueTimestamp
        val isOverdue = due != null && due < nowMillis
        val subtitle = when {
          isOverdue -> "Po terminie · ${formatDurationShort(nowMillis - due!!)}"
          due != null -> "Dziś ${timeFmt.format(Date(due))} · +${task.expReward} EXP"
          else -> "+${task.expReward} EXP · +${task.coinReward} monet"
        }
        rows.add(
          TodayWidgetRow(
            taskId = task.id,
            title = task.name,
            subtitle = subtitle,
            isOverdue = isOverdue,
            isPendingUndo = false,
          )
        )
      }
    }

    return TodayTasksWidgetSnapshot(
      completedTodayCount = completedTodayCount,
      totalTodayCount = totalTodayCount,
      rows = rows,
    )
  }
}
