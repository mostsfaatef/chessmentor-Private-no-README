package com.chessmentor.data

import kotlinx.coroutines.flow.Flow

/**
 * Phase 3 — repository: the single place ViewModels get progress data.
 * It also owns achievement unlocking, so the rule "unlock once" lives
 * in exactly one place.
 */
class ProgressRepository(private val dao: ProgressDao) {

    fun observeLessonProgress(): Flow<List<LessonProgress>> = dao.observeLessonProgress()
    fun observePuzzleAttempts(): Flow<List<PuzzleAttempt>> = dao.observePuzzleAttempts()
    fun observeGames(): Flow<List<GameRecord>> = dao.observeGames()
    fun observeAchievements(): Flow<List<Achievement>> = dao.observeAchievements()

    suspend fun completeLesson(lessonId: String, score: Int = 100) {
        val existing = dao.getLessonProgress(lessonId)
        dao.upsertLessonProgress(
            LessonProgress(
                lessonId = lessonId,
                completed = true,
                completedAtMs = System.currentTimeMillis(),
                bestScore = maxOf(score, existing?.bestScore ?: 0)
            )
        )
        unlock(Achievements.FIRST_LESSON)
    }

    suspend fun recordPuzzleAttempt(
        puzzleId: String,
        solved: Boolean,
        attempts: Int,
        timeMs: Long,
        isMate: Boolean = false
    ) {
        dao.insertPuzzleAttempt(PuzzleAttempt(puzzleId, solved, attempts, timeMs))
        if (solved) {
            if (isMate) unlock(Achievements.FIRST_MATE)
            if (dao.countDistinctSolvedPuzzles() >= 5) unlock(Achievements.FIVE_PUZZLES)
        }
    }

    /** Record a finished game; returns nothing — achievements are checked inside. */
    suspend fun recordGame(pgn: String, result: String, stockfishLevel: Int) {
        dao.insertGame(GameRecord(pgn = pgn, result = result, stockfishLevel = stockfishLevel))
        // result is from White's point of view; the user usually plays White.
        if (result == "1-0") unlock(Achievements.FIRST_WIN)
    }

    suspend fun countSolvedPuzzles(): Int = dao.countSolvedPuzzles()

    /** Unlock once; returns true only the first time. */
    suspend fun unlock(achievement: Achievement): Boolean {
        if (dao.hasAchievement(achievement.id) > 0) return false
        dao.insertAchievement(achievement)
        return true
    }
}
