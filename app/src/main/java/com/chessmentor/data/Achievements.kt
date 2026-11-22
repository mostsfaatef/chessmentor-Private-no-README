package com.chessmentor.data

/**
 * Phase 3 — achievements. IDs are stable strings: never rename one,
 * or users would unlock it twice after an update.
 */
object Achievements {
    val FIRST_LESSON = Achievement(
        id = "first_lesson",
        title = "First lesson done",
        description = "You finished your first lesson."
    )
    val FIRST_MATE = Achievement(
        id = "first_mate",
        title = "First mate!",
        description = "You solved your first checkmate puzzle."
    )
    val FIVE_PUZZLES = Achievement(
        id = "five_puzzles",
        title = "Puzzle solver",
        description = "You solved 5 different puzzles."
    )
    val FIRST_WIN = Achievement(
        id = "first_win",
        title = "First win",
        description = "You won your first game (as White)."
    )

    val ALL = listOf(FIRST_LESSON, FIRST_MATE, FIVE_PUZZLES, FIRST_WIN)
}
