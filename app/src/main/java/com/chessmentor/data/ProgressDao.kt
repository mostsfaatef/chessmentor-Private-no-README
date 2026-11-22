package com.chessmentor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** All progress reads/writes go through here — the UI never touches Room directly. */
@Dao
interface ProgressDao {

    // --- Lessons ---
    @Query("SELECT * FROM lesson_progress")
    fun observeLessonProgress(): Flow<List<LessonProgress>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun getLessonProgress(lessonId: String): LessonProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(progress: LessonProgress)

    @Update
    suspend fun updateLessonProgress(progress: LessonProgress)

    // --- Puzzles ---
    @Insert
    suspend fun insertPuzzleAttempt(attempt: PuzzleAttempt)

    @Query("SELECT COUNT(*) FROM puzzle_attempts WHERE solved = 1")
    suspend fun countSolvedPuzzles(): Int

    @Query("SELECT COUNT(DISTINCT puzzleId) FROM puzzle_attempts WHERE solved = 1")
    suspend fun countDistinctSolvedPuzzles(): Int

    @Query("SELECT * FROM puzzle_attempts ORDER BY attemptedAtMs DESC LIMIT 50")
    fun observePuzzleAttempts(): Flow<List<PuzzleAttempt>>

    // --- Games ---
    @Insert
    suspend fun insertGame(game: GameRecord)

    @Query("SELECT * FROM games ORDER BY playedAtMs DESC LIMIT 20")
    fun observeGames(): Flow<List<GameRecord>>

    @Query("SELECT COUNT(*) FROM games WHERE result = '1-0'")
    suspend fun countWinsAsWhite(): Int

    // --- Achievements ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: Achievement): Long

    @Query("SELECT * FROM achievements ORDER BY unlockedAtMs ASC")
    fun observeAchievements(): Flow<List<Achievement>>

    @Query("SELECT COUNT(*) FROM achievements WHERE id = :id")
    suspend fun hasAchievement(id: String): Int
}
