package dev.localloop.ui

import dev.localloop.data.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskFilterTest {
    private val open = Task(1, "open", isDone = false, createdAt = 0)
    private val done = Task(2, "done", isDone = true, createdAt = 1)
    private val all = listOf(open, done)

    @Test
    fun all() = assertEquals(all, all.filteredBy(TaskFilter.ALL))

    @Test
    fun open() = assertEquals(listOf(open), all.filteredBy(TaskFilter.OPEN))

    @Test
    fun done() = assertEquals(listOf(done), all.filteredBy(TaskFilter.DONE))

    @Test
    fun `title validation checks empty and max length`() {
        assertEquals(TitleError.EMPTY, validateTitle(""))
        assertEquals(null, validateTitle("a"))
        assertEquals(null, validateTitle("a".repeat(MAX_TITLE_LENGTH)))
        assertEquals(TitleError.TOO_LONG, validateTitle("a".repeat(MAX_TITLE_LENGTH + 1)))
    }
}
