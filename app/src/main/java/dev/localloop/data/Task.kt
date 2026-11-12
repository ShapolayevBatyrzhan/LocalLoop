package dev.localloop.data

/** UI/domain model, kept separate from the Room entity. */
data class Task(
    val id: Long,
    val title: String,
    val isDone: Boolean,
    val createdAt: Long,
)

fun TaskEntity.toTask() = Task(id = id, title = title, isDone = isDone, createdAt = createdAt)
