package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SubtaskItem
import com.example.data.local.TaskEntity
import com.example.data.local.toSubtasksJson
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Gamefy", appName)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `task timer activates focus mode and auto resets when all subtasks or task complete`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>() as GamefyApp
    val mainDispatcher = StandardTestDispatcher()
    Dispatchers.setMain(mainDispatcher)
    val store = ViewModelStore()
    val vm = ViewModelProvider(
      store,
      ViewModelProvider.AndroidViewModelFactory.getInstance(app),
    )[MainViewModel::class.java]

    suspend fun drainUntil(timeoutMs: Long = 5_000L, condition: suspend () -> Boolean) {
      val deadline = System.currentTimeMillis() + timeoutMs
      while (System.currentTimeMillis() < deadline) {
        mainDispatcher.scheduler.runCurrent()
        if (condition()) return
        Thread.sleep(20L)
      }
      mainDispatcher.scheduler.runCurrent()
    }

    try {
      drainUntil { vm.uiState.value.isLoaded }
      val taskId = app.database.gamefyDao().insertTask(
        TaskEntity(
          name = "Zadanie z timerem",
          categoryCode = "o",
          durationMinutes = 10,
          expReward = 20,
          coinReward = 10,
          subtasksJson = toSubtasksJson(
            listOf(
              SubtaskItem(1L, "Krok 1", false),
              SubtaskItem(2L, "Krok 2", false),
            )
          ),
        )
      )
      val task = app.database.gamefyDao().getTasksOnce().first { it.id == taskId }

      // Start task timer -> enters focus mode
      vm.toggleTaskTimer(task.id, task.durationMinutes)
      assertTrue(vm.taskTimer.value.isRunning)
      assertTrue(vm.taskTimer.value.isFocusActive)
      assertEquals(task.id, vm.taskTimer.value.taskId)
      assertEquals(600, vm.taskTimer.value.totalSeconds)

      // Partial subtask keeps timer running
      vm.toggleSubtask(task, 1L)
      var refreshedTask: TaskEntity = task
      drainUntil {
        refreshedTask = app.database.gamefyDao().getTasksOnce().first { it.id == taskId }
        refreshedTask.subtasksJson.contains("\"isDone\":true") || refreshedTask.subtasksJson.contains("true")
      }
      assertTrue(vm.taskTimer.value.isRunning)
      assertTrue(vm.taskTimer.value.isFocusActive)

      // Final subtask completes task -> timer stops and resets
      vm.toggleSubtask(refreshedTask, 2L)
      drainUntil { !vm.taskTimer.value.isRunning && !vm.taskTimer.value.isFocusActive }
      assertFalse(vm.taskTimer.value.isRunning)
      assertFalse(vm.taskTimer.value.isFocusActive)
      assertEquals(0L, vm.taskTimer.value.taskId)
      assertEquals(600, vm.taskTimer.value.remainingSeconds)

      // Test stopAndResetAllTimers clears both main and task timers
      vm.toggleMainTimer()
      vm.toggleTaskTimer(taskId, 15)
      assertTrue(vm.mainTimer.value.isRunning)
      assertTrue(vm.taskTimer.value.isRunning)

      vm.stopAndResetAllTimers()
      assertFalse(vm.mainTimer.value.isRunning)
      assertEquals(vm.mainTimer.value.totalSeconds, vm.mainTimer.value.remainingSeconds)
      assertFalse(vm.taskTimer.value.isRunning)
      assertFalse(vm.taskTimer.value.isFocusActive)
    } finally {
      store.clear()
      Dispatchers.resetMain()
    }
  }
}
