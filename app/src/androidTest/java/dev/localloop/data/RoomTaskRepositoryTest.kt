package dev.localloop.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against a real (in-memory) Room database on a device/emulator. */
@RunWith(AndroidJUnit4::class)
class RoomTaskRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RoomTaskRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomTaskRepository(db.taskDao(), clock = { now++ })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addedTasksAreReturnedNewestFirst() = runTest {
        repository.addTask("first")
        repository.addTask("second")

        val tasks = repository.observeTasks().first()

        assertEquals(listOf("second", "first"), tasks.map { it.title })
        assertTrue(tasks.none { it.isDone })
    }

    @Test
    fun setDoneAndDeletePersist() = runTest {
        repository.addTask("a")
        repository.addTask("b")
        val a = repository.observeTasks().first().first { it.title == "a" }

        repository.setDone(a.id, true)
        assertTrue(repository.observeTasks().first().first { it.id == a.id }.isDone)

        repository.deleteTask(a.id)
        assertEquals(listOf("b"), repository.observeTasks().first().map { it.title })
    }
}
