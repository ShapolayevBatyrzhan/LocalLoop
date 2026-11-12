package dev.localloop.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY created_at DESC, id DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query("UPDATE tasks SET is_done = :isDone WHERE id = :id")
    suspend fun setDone(id: Long, isDone: Boolean): Int

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long): Int
}
