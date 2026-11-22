package com.chessmentor.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessmentor.chess.engine.StockfishEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Phase 4 — game review.
 *
 * For each move, the engine evaluates the position before and after
 * it (depth 8, White's point of view). A drop of more than 200cp
 * from the mover's point of view is a blunder; 50–200cp is an
 * inaccuracy. The engine's best move is shown as "Engine's choice".
 *
 * If Stockfish isn't built, review still works: it shows the move
 * list and Naroditsky's 3 questions, and says the engine part is
 * unavailable. No eval is ever invented.
 */
data class ReviewMove(
    val number: Int,
    val san: String,
    val uci: String,
    val fenBefore: String,
    val evalBeforeCpWhite: Int?,
    val evalAfterCpWhite: Int?,
    val bestMoveUci: String?,
    val isBlunder: Boolean,
    val isInaccuracy: Boolean
)

data class ReviewUiState(
    val moves: List<ReviewMove> = emptyList(),
    val analyzing: Boolean = false,
    val engineAvailable: Boolean = false,
    val analyzed: Boolean = false,
    val question1: String = "",
    val question2: String = "",
    val question3: String = "",
    val notesSaved: Boolean = false,
    val selectedFen: String? = null
)

class ReviewViewModel(
    private val engine: StockfishEngine = StockfishEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState(engineAvailable = engine.available))
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    /** Evaluate a FEN and return the score in White's POV (mate = ±10000). */
    private suspend fun evalWhitePov(fen: String, depth: Int = 8): Pair<Int?, String?> {
        val result = engine.eval(fen, depth) ?: return null to null
        val whiteToMove = " w " in " $fen " || fen.split(" ").getOrNull(1) == "w"
        val cp = result.cp?.let { if (whiteToMove) it else -it }
            ?: result.mate?.let { mate ->
                val signed = if (whiteToMove) mate else -mate
                if (signed > 0) 10_000 else -10_000
            }
        return cp to result.bestMoveUci
    }

    /**
     * Analyse a finished game. [fenHistory] has one more entry than
     * [uciHistory]/[sanHistory] (position before move 1, then after each move).
     */
    fun analyze(fenHistory: List<String>, uciHistory: List<String>, sanHistory: List<String>) {
        if (_uiState.value.analyzing) return
        viewModelScope.launch {
            _uiState.update { it.copy(analyzing = true, moves = emptyList(), analyzed = false) }
            val moves = mutableListOf<ReviewMove>()
            if (engine.available) {
                for (i in uciHistory.indices) {
                    val fenBefore = fenHistory.getOrNull(i) ?: break
                    val fenAfter = fenHistory.getOrNull(i + 1) ?: break
                    val (cpBefore, best) = evalWhitePov(fenBefore)
                    val (cpAfter, _) = evalWhitePov(fenAfter)
                    if (cpBefore == null || cpAfter == null) {
                        moves += ReviewMove(i + 1, sanHistory.getOrNull(i) ?: "?", uciHistory[i],
                            fenBefore, null, null, best, false, false)
                        continue
                    }
                    // Mover's POV: White's score if White moved, negated if Black moved.
                    val whiteMoved = fenBefore.split(" ").getOrNull(1) == "w"
                    val beforeMover = if (whiteMoved) cpBefore else -cpBefore
                    val afterMover = if (whiteMoved) cpAfter else -cpAfter
                    val drop = beforeMover - afterMover
                    moves += ReviewMove(
                        number = i + 1,
                        san = sanHistory.getOrNull(i) ?: "?",
                        uci = uciHistory[i],
                        fenBefore = fenBefore,
                        evalBeforeCpWhite = cpBefore,
                        evalAfterCpWhite = cpAfter,
                        bestMoveUci = best,
                        isBlunder = drop > 200,
                        isInaccuracy = drop in 51..200
                    )
                }
            } else {
                // No engine: still list every move so the 3 questions have context.
                for (i in uciHistory.indices) {
                    moves += ReviewMove(i + 1, sanHistory.getOrNull(i) ?: "?", uciHistory[i],
                        fenHistory.getOrNull(i) ?: "", null, null, null, false, false)
                }
            }
            _uiState.update { it.copy(moves = moves, analyzing = false, analyzed = true) }
        }
    }

    fun selectMove(fenBefore: String) {
        _uiState.update { it.copy(selectedFen = fenBefore) }
    }

    fun setQuestion(index: Int, text: String) {
        _uiState.update {
            when (index) {
                1 -> it.copy(question1 = text)
                2 -> it.copy(question2 = text)
                else -> it.copy(question3 = text)
            }
        }
    }

    /** Notes are kept in memory for this review session (see README §9). */
    fun saveNotes() {
        _uiState.update { it.copy(notesSaved = true) }
    }
}
