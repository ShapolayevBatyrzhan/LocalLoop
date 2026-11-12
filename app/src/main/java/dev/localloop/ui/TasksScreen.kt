package dev.localloop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.localloop.data.Task

@Composable
fun TasksScreen(viewModel: TasksViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TasksContent(
        state = state,
        titleInput = viewModel.titleInput,
        onTitleChange = viewModel::onTitleChange,
        onAdd = viewModel::addTask,
        onFilterChange = viewModel::setFilter,
        onToggle = viewModel::setDone,
        onDelete = viewModel::deleteTask,
        onRetry = viewModel::retry,
        onMessageShown = viewModel::userMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksContent(
    state: TasksUiState,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    onAdd: () -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onToggle: (Task, Boolean) -> Unit,
    onDelete: (Task) -> Unit,
    onRetry: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.userMessage) {
        val message = state.userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message.text())
        onMessageShown()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("LocalLoop") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            AddTaskRow(titleInput, state.titleError, onTitleChange, onAdd)
            Spacer(Modifier.height(12.dp))
            FilterRow(state.filter, onFilterChange)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val list = state.listState) {
                    ListState.Loading -> CircularProgressIndicator(
                        Modifier
                            .align(Alignment.Center)
                            .testTag("loading"),
                    )

                    ListState.Error -> ErrorState(onRetry, Modifier.align(Alignment.Center))

                    is ListState.Content ->
                        if (list.tasks.isEmpty()) {
                            Text(
                                text = emptyText(state.filter, list.totalCount),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .testTag("empty"),
                            )
                        } else {
                            TaskList(list.tasks, onToggle, onDelete)
                        }
                }
            }
        }
    }
}

@Composable
private fun AddTaskRow(
    titleInput: String,
    titleError: TitleError?,
    onTitleChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = titleInput,
            onValueChange = onTitleChange,
            label = { Text("New task") },
            singleLine = true,
            isError = titleError != null,
            supportingText = titleError?.let { error -> { Text(error.text()) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            modifier = Modifier
                .weight(1f)
                .testTag("titleInput"),
        )
        Button(
            onClick = onAdd,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("addButton"),
        ) {
            Text("Add")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(selected: TaskFilter, onFilterChange: (TaskFilter) -> Unit) {
    val filters = TaskFilter.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        filters.forEachIndexed { index, filter ->
            SegmentedButton(
                selected = filter == selected,
                onClick = { onFilterChange(filter) },
                shape = SegmentedButtonDefaults.itemShape(index, filters.size),
                modifier = Modifier.testTag("filter_${filter.name}"),
            ) {
                Text(filter.label())
            }
        }
    }
}

@Composable
private fun TaskList(tasks: List<Task>, onToggle: (Task, Boolean) -> Unit, onDelete: (Task) -> Unit) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .testTag("taskList")
    ) {
        items(tasks, key = { it.id }) { task ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_${task.title}"),
            ) {
                Checkbox(
                    checked = task.isDone,
                    onCheckedChange = { onToggle(task, it) },
                    modifier = Modifier.testTag("check_${task.title}"),
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onDelete(task) },
                    modifier = Modifier.testTag("delete_${task.title}"),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${task.title}")
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.testTag("error"), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Couldn't load tasks.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

private fun emptyText(filter: TaskFilter, totalCount: Int): String = when {
    totalCount == 0 -> "No tasks yet. Add your first one above."
    filter == TaskFilter.OPEN -> "No open tasks. Nice work!"
    filter == TaskFilter.DONE -> "No completed tasks yet."
    else -> "No tasks."
}

private fun TaskFilter.label() = when (this) {
    TaskFilter.ALL -> "All"
    TaskFilter.OPEN -> "Open"
    TaskFilter.DONE -> "Done"
}

private fun TitleError.text() = when (this) {
    TitleError.EMPTY -> "Title can't be empty"
    TitleError.TOO_LONG -> "Title is too long (max $MAX_TITLE_LENGTH)"
}

private fun UserMessage.text() = when (this) {
    UserMessage.ADD_FAILED -> "Couldn't save the task. Try again."
    UserMessage.UPDATE_FAILED -> "Couldn't update the task."
    UserMessage.DELETE_FAILED -> "Couldn't delete the task."
}
