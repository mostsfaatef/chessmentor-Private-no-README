package com.chessmentor.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessmentor.curriculum.allLessons
import kotlinx.coroutines.launch
import com.chessmentor.data.Achievement
import com.chessmentor.data.GameRecord
import com.chessmentor.data.LessonProgress
import com.chessmentor.data.ProgressRepository
import com.chessmentor.data.PuzzleAttempt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Phase 3 — progress screen state, all from Room via the repository. */
class ProgressViewModel(private val repository: ProgressRepository) : ViewModel() {

    /** Mark a lesson complete (also unlocks the "First lesson" achievement). */
    fun completeLesson(lessonId: String) {
        viewModelScope.launch { repository.completeLesson(lessonId) }
    }

    val lessonProgress: StateFlow<List<LessonProgress>> =
        repository.observeLessonProgress()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val puzzleAttempts: StateFlow<List<PuzzleAttempt>> =
        repository.observePuzzleAttempts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val games: StateFlow<List<GameRecord>> =
        repository.observeGames()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val achievements: StateFlow<List<Achievement>> =
        repository.observeAchievements()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalLessons: Int = allLessons().size
}
