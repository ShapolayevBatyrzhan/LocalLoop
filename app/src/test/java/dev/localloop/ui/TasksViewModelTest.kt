package dev.localloop.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private lateinit var repository: FakeTaskRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeTaskRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(handle: SavedStateHandle = SavedStateHandle()): TasksViewModel {
        val vm = TasksViewModel(repository, handle)
        // uiState uses WhileSubscribed, so keep a collector alive for the whole test.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private fun TasksViewModel.visibleTitles(): List<String> =
        (uiState.value.listState as ListState.Content).tasks.map { it.title }

    private fun TasksViewModel.add(title: String) {
        onTitleChange(title)
        addTask()
    }

    @Test
    fun `starts empty after loading`() = runTest {
        val vm = createViewModel()
        val list = vm.uiState.value.listState as ListState.Content
        assertTrue(list.tasks.isEmpty())
        assertEquals(0, list.totalCount)
    }

    @Test
    fun `creating a task trims title, stores it and clears input`() = runTest {
        val vm = createViewModel()

        vm.add("  Buy milk  ")

        assertEquals(listOf("Buy milk"), repository.current.map { it.title })
        assertEquals(listOf("Buy milk"), vm.visibleTitles())
        assertEquals("", vm.titleInput)
        assertNull(vm.uiState.value.titleError)
    }

    @Test
    fun `blank title is rejected and nothing is stored`() = runTest {
        val vm = createViewModel()

        vm.add("   ")

        assertEquals(TitleError.EMPTY, vm.uiState.value.titleError)
        assertTrue(repository.current.isEmpty())
        assertEquals("   ", vm.titleInput) // input is kept so the user can fix it
    }

    @Test
    fun `too long title is rejected`() = runTest {
        val vm = createViewModel()

        vm.add("a".repeat(MAX_TITLE_LENGTH + 1))

        assertEquals(TitleError.TOO_LONG, vm.uiState.value.titleError)
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun `typing clears a validation error`() = runTest {
        val vm = createViewModel()
        vm.add("")
        assertEquals(TitleError.EMPTY, vm.uiState.value.titleError)

        vm.onTitleChange("x")

        assertNull(vm.uiState.value.titleError)
    }

    @Test
    fun `completing and un-completing a task`() = runTest {
        val vm = createViewModel()
        vm.add("Write tests")
        val task = repository.current.single()

        vm.setDone(task, true)
        assertTrue(repository.current.single().isDone)

        vm.setDone(task, false)
        assertTrue(!repository.current.single().isDone)
    }

    @Test
    fun `filters show all, open and done tasks`() = runTest {
        val vm = createViewModel()
        vm.add("A")
        vm.add("B")
        vm.add("C")
        val b = repository.current.first { it.title == "B" }
        vm.setDone(b, true)

        assertEquals(listOf("C", "B", "A"), vm.visibleTitles())

        vm.setFilter(TaskFilter.OPEN)
        assertEquals(listOf("C", "A"), vm.visibleTitles())

        vm.setFilter(TaskFilter.DONE)
        assertEquals(listOf("B"), vm.visibleTitles())
        assertEquals(3, (vm.uiState.value.listState as ListState.Content).totalCount)
    }

    @Test
    fun `filtered list updates when a task is completed`() = runTest {
        val vm = createViewModel()
        vm.add("A")
        vm.setFilter(TaskFilter.OPEN)
        assertEquals(listOf("A"), vm.visibleTitles())

        vm.setDone(repository.current.single(), true)

        assertEquals(emptyList<String>(), vm.visibleTitles())
    }

    @Test
    fun `deleting removes the task`() = runTest {
        val vm = createViewModel()
        vm.add("A")
        vm.add("B")

        vm.deleteTask(repository.current.first { it.title == "A" })

        assertEquals(listOf("B"), vm.visibleTitles())
    }

    @Test
    fun `filter and draft are restored from SavedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        first.setFilter(TaskFilter.DONE)
        first.onTitleChange("half typed")

        // Simulates process death: a new ViewModel gets the same saved state.
        val second = createViewModel(handle)

        assertEquals(TaskFilter.DONE, second.uiState.value.filter)
        assertEquals("half typed", second.titleInput)
    }

    @Test
    fun `load failure shows error and retry recovers`() = runTest {
        repository.failObserve = true
        val vm = createViewModel()
        assertEquals(ListState.Error, vm.uiState.value.listState)

        repository.failObserve = false
        vm.retry()

        assertTrue(vm.uiState.value.listState is ListState.Content)
    }

    @Test
    fun `failed save shows message and restores input`() = runTest {
        val vm = createViewModel()
        repository.failWrites = true

        vm.add("Important")

        assertEquals(UserMessage.ADD_FAILED, vm.uiState.value.userMessage)
        assertEquals("Important", vm.titleInput)

        vm.userMessageShown()
        assertNull(vm.uiState.value.userMessage)
    }
}
