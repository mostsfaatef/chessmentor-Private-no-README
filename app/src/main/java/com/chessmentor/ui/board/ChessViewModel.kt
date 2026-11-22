package com.chessmentor.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessmentor.chess.engine.StockfishEngine
import com.chessmentor.chess.model.ChessGame
import com.chessmentor.chess.model.GameStatus
import com.chessmentor.chess.model.Move
import com.chessmentor.chess.model.Piece
import com.chessmentor.chess.model.PieceColor
import com.chessmentor.chess.model.PieceType
import com.chessmentor.chess.model.Square
import com.chessmentor.chess.model.opposite
import com.chessmentor.data.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVVM: the ViewModel owns the [ChessGame] and exposes immutable
 * [BoardUiState] via StateFlow. The Composable never mutates the game.
 *
 * Phase 1: tap/drag, legal-move dots, promotion, undo, flip.
 * Phase 2: play vs Stockfish (levels 1–10), Hint button.
 * Phase 3: game results are recorded via [ProgressRepository] when one
 * is supplied; without it the board still works fully (repository is
 * optional so the ViewModel stays unit-testable).
 */
data class BoardUiState(
    val board: List<List<Piece?>> = emptyList(),
    val turn: PieceColor = PieceColor.WHITE,
    val selected: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val lastMove: Move? = null,
    val status: GameStatus = GameStatus.ONGOING,
    val isCheck: Boolean = false,
    val moveList: List<String> = emptyList(),
    val flipped: Boolean = false,
    /** Set when a pawn move needs the player to choose a promotion piece. */
    val pendingPromotion: Pair<Square, Square>? = null,
    val message: String? = null,
    // --- Phase 2: engine ---
    val vsEngine: Boolean = false,
    val engineLevel: Int = 5,
    val playAs: PieceColor = PieceColor.WHITE,
    val engineThinking: Boolean = false,
    val engineAvailable: Boolean = false,
    val hintFrom: Square? = null,
    val hintTo: Square? = null,
    val hintUci: String? = null
)

class ChessViewModel(
    private val engine: StockfishEngine = StockfishEngine(),
    private val repository: ProgressRepository? = null
) : ViewModel() {

    private val game = ChessGame()
    private val history = ArrayDeque<ChessGame>() // snapshots for Undo
    private var gameRecorded = false

    private val _uiState = MutableStateFlow(BoardUiState(engineAvailable = engine.available))
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    init { publish() }

    private fun publish(message: String? = null) {
        _uiState.update {
            it.copy(
                board = game.boardSnapshot(),
                turn = game.turn,
                selected = it.selected,
                legalTargets = it.legalTargets,
                lastMove = game.lastMove(),
                status = game.status,
                isCheck = game.isInCheck(game.turn),
                moveList = game.moveHistorySan.toList(),
                flipped = it.flipped,
                pendingPromotion = it.pendingPromotion,
                message = message,
                engineAvailable = engine.available
            )
        }
    }

    /** Colour the engine plays. Only meaningful when [BoardUiState.vsEngine] is true. */
    private val engineColor: PieceColor get() = _uiState.value.playAs.opposite()

    /** Tap a square: select a piece, move to a highlighted square, or re-select. */
    fun onSquareTapped(square: Square) {
        val state = _uiState.value
        if (state.pendingPromotion != null) return
        // Phase 2: ignore taps while the engine is thinking or it is the engine's turn.
        if (state.vsEngine && (state.engineThinking || game.turn != state.playAs)) return

        val selected = state.selected
        if (selected != null && square in state.legalTargets) {
            if (game.isPromotionMove(selected, square)) {
                _uiState.update { it.copy(pendingPromotion = selected to square) }
                return
            }
            doMove(selected, square)
            return
        }

        val piece = game.pieceAt(square)
        if (piece != null && piece.color == game.turn) {
            val targets = game.legalMoves(square).map { it.to }.toSet()
            _uiState.update { it.copy(selected = square, legalTargets = targets, message = null) }
        } else {
            _uiState.update { it.copy(selected = null, legalTargets = emptySet()) }
        }
    }

    /** Drag path: try a move directly, showing the promotion dialog if needed. */
    fun tryMove(from: Square, to: Square) {
        val state = _uiState.value
        if (state.pendingPromotion != null) return
        if (state.vsEngine && (state.engineThinking || game.turn != state.playAs)) return
        if (game.isPromotionMove(from, to)) {
            _uiState.update { it.copy(pendingPromotion = from to to) }
            return
        }
        if (game.legalMoves(from).any { it.to == to }) {
            doMove(from, to)
        } else {
            onSquareTapped(from)
        }
    }

    private fun doMove(from: Square, to: Square, promotion: PieceType? = null) {
        history.addLast(game.copy())
        val ok = game.makeMove(from, to, promotion)
        if (!ok) {
            history.removeLast()
            publish("Illegal move")
            return
        }
        val msg = when (game.status) {
            GameStatus.CHECKMATE -> "Checkmate — ${game.turn.opposite()} wins"
            GameStatus.STALEMATE -> "Stalemate — draw"
            GameStatus.CHECK -> "Check!"
            GameStatus.ONGOING -> null
        }
        _uiState.update {
            it.copy(selected = null, legalTargets = emptySet(), pendingPromotion = null,
                hintFrom = null, hintTo = null, hintUci = null)
        }
        publish(msg)
        maybeRecordGame()
        maybeEngineMove()
    }

    fun choosePromotion(type: PieceType) {
        val pending = _uiState.value.pendingPromotion ?: return
        doMove(pending.first, pending.second, promotion = type)
    }

    fun cancelPromotion() {
        _uiState.update { it.copy(pendingPromotion = null) }
    }

    fun newGame() {
        history.clear()
        game.reset()
        gameRecorded = false
        _uiState.update { it.copy(selected = null, legalTargets = emptySet(), pendingPromotion = null,
            hintFrom = null, hintTo = null, hintUci = null) }
        publish()
        maybeEngineMove()
    }

    /** Load a FEN (lesson diagram / puzzle / review). Clears move history. */
    fun loadFen(fen: String): Boolean {
        if (!game.loadFen(fen)) return false
        history.clear()
        gameRecorded = false
        _uiState.update { it.copy(selected = null, legalTargets = emptySet(), pendingPromotion = null,
            hintFrom = null, hintTo = null, hintUci = null) }
        publish()
        return true
    }

    fun currentFen(): String = game.toFen()

    /** FENs, UCIs and SANs for game review (Phase 4). */
    fun reviewData(): Triple<List<String>, List<String>, List<String>> =
        Triple(game.fenHistory.toList(), game.uciHistory.toList(), game.moveHistorySan.toList())

    private fun undoOnePly() {
        val prev = history.removeLastOrNull() ?: return
        val moves = prev.history()
        game.reset()
        history.clear()
        for (m in moves) {
            history.addLast(game.copy())
            game.makeMove(m.from, m.to, m.promotion)
        }
        _uiState.update { it.copy(selected = null, legalTargets = emptySet(), pendingPromotion = null,
            hintFrom = null, hintTo = null, hintUci = null) }
        publish()
    }

    /**
     * Undo one ply in two-player mode, two plies (yours + the engine's
     * reply) when playing vs the engine, so you don't immediately get
     * the same engine move back.
     */
    fun undo() {
        undoOnePly()
        if (_uiState.value.vsEngine) undoOnePly()
    }

    fun flipBoard() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    // ------------------------------------------------------------------
    // Phase 2 — Stockfish
    // ------------------------------------------------------------------

    fun setVsEngine(enabled: Boolean) {
        _uiState.update { it.copy(vsEngine = enabled) }
        if (enabled && !engine.available) {
            publish("Stockfish not built yet — see README §5")
            return
        }
        if (enabled) maybeEngineMove()
    }

    fun setLevel(level: Int) {
        _uiState.update { it.copy(engineLevel = level.coerceIn(1, 10)) }
    }

    /** Choose a colour and start a fresh game against the engine. */
    fun setPlayAs(color: PieceColor) {
        _uiState.update { it.copy(playAs = color, flipped = color == PieceColor.BLACK) }
        newGame()
    }

    /** Ask Stockfish for the best move and highlight its from/to squares. */
    fun requestHint() {
        val state = _uiState.value
        if (state.engineThinking) return
        if (!engine.available) {
            publish("Stockfish not built yet — see README §5")
            return
        }
        if (game.status != GameStatus.ONGOING) return
        viewModelScope.launch {
            _uiState.update { it.copy(engineThinking = true, message = "Thinking…") }
            val uci = engine.hint(game.toFen())
            _uiState.update { it.copy(engineThinking = false) }
            if (uci == null) {
                publish("Stockfish didn't answer — try again")
                return@launch
            }
            val from = runCatching { Square.fromAlgebraic(uci.substring(0, 2)) }.getOrNull()
            val to = runCatching { Square.fromAlgebraic(uci.substring(2, 4)) }.getOrNull()
            if (from == null || to == null) {
                publish("Stockfish gave an odd move: $uci")
                return@launch
            }
            _uiState.update { it.copy(hintFrom = from, hintTo = to, hintUci = uci, message = "Hint: $uci") }
        }
    }

    fun clearHint() {
        _uiState.update { it.copy(hintFrom = null, hintTo = null, hintUci = null) }
    }

    private fun maybeEngineMove() {
        val state = _uiState.value
        if (!state.vsEngine) return
        if (game.status != GameStatus.ONGOING) return
        if (game.turn != engineColor) return
        if (state.engineThinking) return
        if (!engine.available) {
            publish("Stockfish not built yet — see README §5")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(engineThinking = true) }
            val uci = engine.bestMove(game.toFen(), state.engineLevel)
            _uiState.update { it.copy(engineThinking = false) }
            if (uci == null) {
                publish("Stockfish didn't answer — your move")
                return@launch
            }
            val from = runCatching { Square.fromAlgebraic(uci.substring(0, 2)) }.getOrNull()
            val to = runCatching { Square.fromAlgebraic(uci.substring(2, 4)) }.getOrNull()
            val promotion = when (uci.getOrNull(4)?.lowercaseChar()) {
                'q' -> PieceType.QUEEN; 'r' -> PieceType.ROOK
                'b' -> PieceType.BISHOP; 'n' -> PieceType.KNIGHT
                else -> null
            }
            if (from == null || to == null) return@launch
            // Engine moves never need a promotion dialog — UCI already says which piece.
            doMove(from, to, promotion)
        }
    }

    private fun maybeRecordGame() {
        val repo = repository ?: return
        if (gameRecorded) return
        if (game.status != GameStatus.CHECKMATE && game.status != GameStatus.STALEMATE) return
        gameRecorded = true
        val result = when {
            game.status == GameStatus.STALEMATE -> "1/2-1/2"
            game.turn == PieceColor.BLACK -> "1-0" // Black to move = White just mated Black
            else -> "0-1"
        }
        val pgn = buildPgn(result)
        viewModelScope.launch {
            repo.recordGame(pgn = pgn, result = result, stockfishLevel = _uiState.value.engineLevel)
        }
    }

    private fun buildPgn(result: String): String {
        val header = "[White \"You\"]\n[Black \"Stockfish L${_uiState.value.engineLevel}\"]\n[Result \"$result\"]\n"
        val moves = game.moveHistorySan.chunked(2).mapIndexed { i, pair ->
            "${i + 1}. ${pair.joinToString(" ")}"
        }.joinToString(" ")
        return "$header\n$moves $result"
    }

    override fun onCleared() {
        engine.stop()
        super.onCleared()
    }
}
