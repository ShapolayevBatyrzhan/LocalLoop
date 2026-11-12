package dev.localloop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.localloop.LocalLoopApp
import dev.localloop.data.Task
import dev.localloop.data.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of the task list itself. */
sealed interface ListState {
    data object Loading : ListState
    data object Error : ListState

    /** [tasks] is already filtered; [totalCount] is the unfiltered size (used for empty-state text). */
    data class Content(val tasks: List<Task>, val totalCount: Int) : ListState
}

/** One-off errors from write operations, shown in a snackbar. */
enum class UserMessage { ADD_FAILED, UPDATE_FAILED, DELETE_FAILED }

data class TasksUiState(
    val listState: ListState = ListState.Loading,
    val filter: TaskFilter = TaskFilter.ALL,
    val titleError: TitleError? = null,
    val userMessage: UserMessage? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
    private val repository: TaskRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * The text field value lives in Compose state (not a StateFlow) so typing is applied
     * synchronously and the cursor never jumps. It is mirrored into SavedStateHandle, so a draft
     * survives rotation (ViewModel) and process death (saved state).
     */
    var titleInput by mutableStateOf(savedStateHandle.get<String>(KEY_TITLE) ?: "")
        private set

    private val filter: StateFlow<TaskFilter> = savedStateHandle.getStateFlow(KEY_FILTER, TaskFilter.ALL)
    private val titleError = MutableStateFlow<TitleError?>(null)
    private val userMessage = MutableStateFlow<UserMessage?>(null)

    /** Bumped by [retry] to resubscribe to the repository after a load error. */
    private val retryTrigger = MutableStateFlow(0)

    private val tasks: Flow<LoadResult> = retryTrigger.flatMapLatest {
        repository.observeTasks()
            .map<List<Task>, LoadResult> { LoadResult.Success(it) }
            .onStart { emit(LoadResult.Loading) }
            .catch { emit(LoadResult.Failure) }
    }

    val uiState: StateFlow<TasksUiState> =
        combine(tasks, filter, titleError, userMessage) { result, filter, titleError, message ->
            TasksUiState(
                listState = when (result) {
                    LoadResult.Loading -> ListState.Loading
                    LoadResult.Failure -> ListState.Error
                    is LoadResult.Success -> ListState.Content(
                        tasks = result.tasks.filteredBy(filter),
                        totalCount = result.tasks.size,
                    )
                },
                filter = filter,
                titleError = titleError,
                userMessage = message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TasksUiState(filter = filter.value),
        )

    fun onTitleChange(value: String) {
        titleInput = value
        savedStateHandle[KEY_TITLE] = value
        titleError.value = null
    }

    fun addTask() {
        val trimmed = titleInput.trim()
        val error = validateTitle(trimmed)
        if (error != null) {
            titleError.value = error
            return
        }
        // Clear right away so a quick double tap does not add the same task twice.
        onTitleChange("")
        safeLaunch(onError = {
            onTitleChange(trimmed) // give the user their text back
            userMessage.value = UserMessage.ADD_FAILED
        }) {
            repository.addTask(trimmed)
        }
    }

    fun setDone(task: Task, isDone: Boolean) = safeLaunch(onError = {
        userMessage.value = UserMessage.UPDATE_FAILED
    }) {
        repository.setDone(task.id, isDone)
    }

    fun deleteTask(task: Task) = safeLaunch(onError = {
        userMessage.value = UserMessage.DELETE_FAILED
    }) {
        repository.deleteTask(task.id)
    }

    fun setFilter(value: TaskFilter) {
        savedStateHandle[KEY_FILTER] = value
    }

    fun retry() = retryTrigger.update { it + 1 }

    fun userMessageShown() {
        userMessage.value = null
    }

    private fun safeLaunch(onError: (Throwable) -> Unit, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private sealed interface LoadResult {
        data object Loading : LoadResult
        data object Failure : LoadResult
        data class Success(val tasks: List<Task>) : LoadResult
    }

    companion object {
        private const val KEY_TITLE = "title_input"
        private const val KEY_FILTER = "filter"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LocalLoopApp
                TasksViewModel(app.taskRepository, createSavedStateHandle())
            }
        }
    }
}
