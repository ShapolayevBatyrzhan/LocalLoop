package dev.localloop.ui

import dev.localloop.data.Task
import dev.localloop.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.io.IOException

/** In-memory repository with switches to simulate storage failures. */
class FakeTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L
    private var clock = 0L

    var failObserve = false
    var failWrites = false

    val current: List<Task> get() = tasks.value

    override fun observeTasks(): Flow<List<Task>> = flow {
        if (failObserve) throw IOException("disk error")
        tasks.collect { emit(it) }
    }

    override suspend fun addTask(title: String) {
        if (failWrites) throw IOException("disk error")
        // Newest first, same as the Room query.
        tasks.update { listOf(Task(nextId++, title, isDone = false, createdAt = clock++)) + it }
    }

    override suspend fun setDone(id: Long, isDone: Boolean) {
        if (failWrites) throw IOException("disk error")
        tasks.update { list -> list.map { if (it.id == id) it.copy(isDone = isDone) else it } }
    }

    override suspend fun deleteTask(id: Long) {
        if (failWrites) throw IOException("disk error")
        tasks.update { list -> list.filterNot { it.id == id } }
    }
}
