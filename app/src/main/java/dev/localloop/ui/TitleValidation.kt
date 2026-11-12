package dev.localloop.ui

const val MAX_TITLE_LENGTH = 100

enum class TitleError { EMPTY, TOO_LONG }

/** Returns null when the (already trimmed) title is valid. */
fun validateTitle(trimmed: String): TitleError? = when {
    trimmed.isEmpty() -> TitleError.EMPTY
    trimmed.length > MAX_TITLE_LENGTH -> TitleError.TOO_LONG
    else -> null
}
