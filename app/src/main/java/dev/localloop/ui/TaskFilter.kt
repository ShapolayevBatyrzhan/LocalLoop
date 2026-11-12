package dev.localloop.ui

import dev.localloop.data.Task

enum class TaskFilter { ALL, OPEN, DONE }

fun List<Task>.filteredBy(filter: TaskFilter): List<Task> = when (filter) {
    TaskFilter.ALL -> this
    TaskFilter.OPEN -> filter { !it.isDone }
    TaskFilter.DONE -> filter { it.isDone }
}
