package dev.localloop.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun addTask(title: String)
    suspend fun setDone(id: Long, isDone: Boolean)
    suspend fun deleteTask(id: Long)
}

class RoomTaskRepository(
    private val dao: TaskDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : TaskRepository {
    override fun observeTasks(): Flow<List<Task>> =
        dao.observeAll().map { list -> list.map { it.toTask() } }

    override suspend fun addTask(title: String) {
        dao.insert(TaskEntity(title = title, createdAt = clock()))
    }

    override suspend fun setDone(id: Long, isDone: Boolean) {
        dao.setDone(id, isDone)
    }

    override suspend fun deleteTask(id: Long) {
        dao.delete(id)
    }
}
