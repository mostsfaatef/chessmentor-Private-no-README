package com.chessmentor.ui.elo

import androidx.lifecycle.ViewModel
import com.chessmentor.curriculum.EloGame
import com.chessmentor.curriculum.eloGames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Phase 4 — "Guess the Elo" (Levy Rozman).
 * Show a position + the moves so far, guess the Elo band, reveal.
 * Score is kept in memory for the session; the dataset is the
 * durable part (see curriculum/EloGames.kt).
 */
data class EloUiState(
    val game: EloGame = eloGames[0],
    val index: Int = 0,
    val selectedBand: String? = null,
    val revealed: Boolean = false,
    val score: Int = 0,
    val played: Int = 0
)

class EloViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EloUiState())
    val uiState: StateFlow<EloUiState> = _uiState.asStateFlow()

    fun guess(band: String) {
        val s = _uiState.value
        if (s.revealed) return
        val correct = band == s.game.actualBand
        _uiState.update {
            it.copy(
                selectedBand = band,
                revealed = true,
                score = if (correct) it.score + 1 else it.score,
                played = it.played + 1
            )
        }
    }

    fun next() {
        val nextIndex = (_uiState.value.index + 1) % eloGames.size
        _uiState.value = EloUiState(game = eloGames[nextIndex], index = nextIndex,
            score = _uiState.value.score, played = _uiState.value.played)
    }
}
