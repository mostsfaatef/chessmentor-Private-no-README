package com.chessmentor.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.chessmentor.chess.model.ChessGame

/**
 * Read-only board for lesson diagrams, Elo positions and review.
 * Builds a [BoardUiState] from a FEN and renders the Phase 1
 * [ChessBoard] with no-op handlers — taps do nothing here.
 * For a playable board, use [ChessViewModel.loadFen] + the Play screen.
 */
@Composable
fun FenBoard(fen: String, modifier: Modifier = Modifier) {
    val state = remember(fen) {
        val game = ChessGame()
        game.loadFen(fen)
        BoardUiState(
            board = game.boardSnapshot(),
            turn = game.turn,
            status = game.status,
            isCheck = game.isInCheck(game.turn)
        )
    }
    ChessBoard(
        state = state,
        onSquareTapped = {},
        onDragMove = { _, _ -> },
        modifier = modifier
    )
}
