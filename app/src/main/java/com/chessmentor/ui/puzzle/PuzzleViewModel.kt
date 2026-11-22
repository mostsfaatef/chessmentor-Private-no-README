package com.chessmentor.ui.puzzle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessmentor.chess.model.ChessGame
import com.chessmentor.chess.model.PieceType
import com.chessmentor.chess.model.Square
import com.chessmentor.curriculum.Puzzle
import com.chessmentor.curriculum.allPuzzles
import com.chessmentor.data.ProgressRepository
import com.chessmentor.ui.board.BoardUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Phase 3 — puzzle screen logic.
 *
 * The user plays the side to move from the FEN. Each user move must
 * match the next UCI move in [Puzzle.solutionUci]; a wrong move is
 * rejected (the position stays) and counts as an attempt. After a
 * correct move, the opponent's reply from the solution line is
 * played automatically. Solving records a [PuzzleAttempt] in Room.
 */
data class PuzzleUiState(
    val board: BoardUiState = BoardUiState(),
    val puzzle: Puzzle? = null,
    val puzzleIndex: Int = 0,
    val solved: Boolean = false,
    val attempts: Int = 0,
    val hintShown: Boolean = false,
    val message: String? = null
)

class PuzzleViewModel(
    private val repository: ProgressRepository? = null
) : ViewModel() {

    private val game = ChessGame()
    private var solutionIndex = 0
    private var startedAtMs = 0L
    private var recorded = false

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    val puzzles: List<Puzzle> = allPuzzles()

    init { loadPuzzle(0) }

    fun loadPuzzle(index: Int) {
        val puzzle = puzzles.getOrNull(index) ?: return
        game.loadFen(puzzle.fen)
        solutionIndex = 0
        startedAtMs = System.currentTimeMillis()
        recorded = false
        _uiState.value = PuzzleUiState(
            board = boardState(),
            puzzle = puzzle,
            puzzleIndex = index,
            message = "Find the best move — ${puzzle.title}"
        )
    }

    fun nextPuzzle() = loadPuzzle((_uiState.value.puzzleIndex + 1) % puzzles.size)

    private fun boardState(selected: Square? = null, legalTargets: Set<Square> = emptySet()): BoardUiState =
        BoardUiState(
            board = game.boardSnapshot(),
            turn = game.turn,
            selected = selected,
            legalTargets = legalTargets,
            lastMove = game.lastMove(),
            status = game.status,
            isCheck = game.isInCheck(game.turn),
            moveList = game.moveHistorySan.toList()
        )

    private fun publish(message: String? = null) {
        val s = _uiState.value
        _uiState.update {
            it.copy(board = boardState(s.board.selected, s.board.legalTargets), message = message ?: it.message)
        }
    }

    fun onSquareTapped(square: Square) {
        val state = _uiState.value
        if (state.solved || state.puzzle == null) return
        val selected = state.board.selected
        if (selected != null && square in state.board.legalTargets) {
            tryUserMove(selected, square)
            return
        }
        val piece = game.pieceAt(square)
        if (piece != null && piece.color == game.turn) {
            val targets = game.legalMoves(square).map { it.to }.toSet()
            _uiState.update { it.copy(board = boardState(selected = square, legalTargets = targets)) }
        } else {
            _uiState.update { it.copy(board = boardState()) }
        }
    }

    fun tryMove(from: Square, to: Square) {
        val state = _uiState.value
        if (state.solved || state.puzzle == null) return
        if (game.legalMoves(from).any { it.to == to }) tryUserMove(from, to)
        else onSquareTapped(from)
    }

    private fun tryUserMove(from: Square, to: Square) {
        val puzzle = _uiState.value.puzzle ?: return
        val expected = puzzle.solutionUci.getOrNull(solutionIndex) ?: return
        val promotion = if (game.isPromotionMove(from, to)) PieceType.QUEEN else null
        val userUci = from.algebraic + to.algebraic + when (promotion) {
            PieceType.QUEEN -> "q"; else -> ""
        }
        val attempts = _uiState.value.attempts + 1
        if (userUci != expected) {
            // Wrong move: position stays, attempt counts, hint stays available.
            _uiState.update { it.copy(attempts = attempts, board = boardState(), message = "Not that one — try again.") }
            return
        }
        game.makeMove(from, to, promotion)
        solutionIndex++
        _uiState.update { it.copy(attempts = attempts, board = boardState()) }
        if (solutionIndex >= puzzle.solutionUci.size) {
            solve()
            return
        }
        // Opponent's reply from the solution line, after a short beat.
        val reply = puzzle.solutionUci[solutionIndex]
        solutionIndex++
        viewModelScope.launch {
            delay(400)
            game.applyUci(reply)
            publish("Good — now find the next move.")
        }
    }

    private fun solve() {
        val puzzle = _uiState.value.puzzle ?: return
        _uiState.update { it.copy(solved = true, board = boardState(), message = "Solved!") }
        if (recorded) return
        recorded = true
        val repo = repository ?: return
        val timeMs = System.currentTimeMillis() - startedAtMs
        val attempts = _uiState.value.attempts
        viewModelScope.launch {
            repo.recordPuzzleAttempt(
                puzzleId = puzzle.id,
                solved = true,
                attempts = attempts,
                timeMs = timeMs,
                isMate = puzzle.isMate
            )
        }
    }

    fun showHint() {
        _uiState.update { it.copy(hintShown = true) }
    }
}
