package dev.localloop

import android.app.Application
import dev.localloop.data.AppDatabase
import dev.localloop.data.RoomTaskRepository
import dev.localloop.data.TaskRepository

/**
 * Manual dependency container. Hilt would be overkill for a single screen,
 * so the repository is created once here and shared.
 */
class LocalLoopApp : Application() {
    val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(AppDatabase.getInstance(this).taskDao())
    }
}
