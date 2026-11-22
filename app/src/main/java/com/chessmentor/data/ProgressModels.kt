package com.chessmentor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 3 — real Room entities.
 *
 * Tables:
 *  lesson_progress  — one row per lesson, completed / best score
 *  puzzle_attempts  — one row per attempt (solved or not)
 *  games            — one row per finished game vs the engine / a friend
 *  achievements     — one row per unlocked achievement
 */
@Entity(tableName = "lesson_progress")
data class LessonProgress(
    @PrimaryKey val lessonId: String,
    val completed: Boolean = false,
    val completedAtMs: Long? = null,
    val bestScore: Int = 0
)

@Entity(tableName = "puzzle_attempts")
data class PuzzleAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puzzleId: String,
    val solved: Boolean,
    val attempts: Int,
    val timeMs: Long,
    val attemptedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "games")
data class GameRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pgn: String,
    val result: String, // "1-0", "0-1", "1/2-1/2"
    val stockfishLevel: Int,
    val playedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String, // e.g. "first_mate", "five_puzzles", "first_win"
    val title: String,
    val description: String,
    val unlockedAtMs: Long = System.currentTimeMillis()
)
