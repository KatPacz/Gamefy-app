package com.example.widgets

import com.example.data.repository.TaskActionUndoSnapshot

data class PendingWidgetUndo(
  val taskId: Long,
  val taskName: String,
  val snapshot: TaskActionUndoSnapshot,
  val expiresAtMillis: Long,
)

object WidgetUndoStore {
  const val UNDO_WINDOW_MILLIS = 6_000L

  private val lock = Any()
  private val entries = linkedMapOf<Long, PendingWidgetUndo>()

  fun put(entry: PendingWidgetUndo) {
    synchronized(lock) {
      entries[entry.taskId] = entry
    }
  }

  fun take(taskId: Long): PendingWidgetUndo? = synchronized(lock) {
    entries.remove(taskId)
  }

  fun activeEntries(nowMillis: Long = System.currentTimeMillis()): List<PendingWidgetUndo> =
    synchronized(lock) {
      val expiredKeys = entries.values.filter { it.expiresAtMillis <= nowMillis }.map { it.taskId }
      expiredKeys.forEach { entries.remove(it) }
      entries.values.toList()
    }

  fun purgeExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
    synchronized(lock) {
      val expiredKeys = entries.values.filter { it.expiresAtMillis <= nowMillis }.map { it.taskId }
      expiredKeys.forEach { entries.remove(it) }
      expiredKeys.isNotEmpty()
    }

  fun clearAll() {
    synchronized(lock) {
      entries.clear()
    }
  }
}
