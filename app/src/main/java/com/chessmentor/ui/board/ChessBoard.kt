package com.chessmentor.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessmentor.chess.model.PieceType
import com.chessmentor.chess.model.Square
import com.chessmentor.ui.theme.BoardColors
import kotlin.math.roundToInt

/**
 * Phase 1 board.
 *
 * Interaction (GothamChess style — principles first, no memorisation):
 *  - Tap a piece → its legal squares light up.
 *  - Tap a lit square → move there.
 *  - Drag a piece → drop it on the target square.
 * Both paths go through the ViewModel, so the rules live in one place.
 */
@Composable
fun ChessBoard(
    state: BoardUiState,
    onSquareTapped: (Square) -> Unit,
    onDragMove: (Square, Square) -> Unit,
    modifier: Modifier = Modifier
) {
    // Drag state lives in the UI only — the game state stays in the ViewModel.
    var dragFrom by remember { mutableStateOf<Square?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val squareSizePx = maxWidth.toPx() / 8f

        fun squareAt(offset: Offset): Square? {
            val file = (offset.x / squareSizePx).toInt()
            val rankFromTop = (offset.y / squareSizePx).toInt()
            if (file !in 0..7 || rankFromTop !in 0..7) return null
            // Display rank 7 at the top when not flipped, rank 0 at the top when flipped.
            val rank = if (state.flipped) rankFromTop else 7 - rankFromTop
            val f = if (state.flipped) 7 - file else file
            return Square(f, rank)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.flipped, state.board) {
                    detectTapGestures { offset ->
                        squareAt(offset)?.let(onSquareTapped)
                    }
                }
                .pointerInput(state.flipped, state.board) {
                    // Long-press drag so it doesn't fight with tap-to-move.
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            dragFrom = squareAt(offset)
                            dragStart = offset
                            dragOffset = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        },
                        onDragEnd = {
                            val from = dragFrom
                            if (from != null) {
                                val end = dragStart + dragOffset
                                val to = squareAt(end)
                                if (to != null && to != from) onDragMove(from, to)
                            }
                            dragFrom = null
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            dragFrom = null
                            dragOffset = Offset.Zero
                        }
                    )
                }
        ) {
            Column(Modifier.fillMaxSize()) {
                val displayRanks = if (state.flipped) 0..7 else 7 downTo 0
                for (rank in displayRanks) {
                    Row(Modifier.weight(1f)) {
                        val displayFiles = if (state.flipped) 7 downTo 0 else 0..7
                        for (file in displayFiles) {
                            val square = Square(file, rank)
                            val piece = state.board.getOrNull(rank)?.getOrNull(file)
                            val isLight = (file + rank) % 2 == 1
                            val isSelected = state.selected == square
                            val isLastMove = state.lastMove?.from == square || state.lastMove?.to == square
                            val isTarget = square in state.legalTargets
                            val isCheckSquare = state.isCheck && piece?.type == PieceType.KING &&
                                piece.color == state.turn
                            // Phase 2: Stockfish hint squares.
                            val isHint = state.hintFrom == square || state.hintTo == square

                            val bg = when {
                                isSelected -> BoardColors.selected
                                isHint -> BoardColors.hint
                                isCheckSquare -> BoardColors.check
                                isLastMove -> BoardColors.lastMove
                                isLight -> BoardColors.lightSquare
                                else -> BoardColors.darkSquare
                            }
                            // The dragged piece is hidden from its home square while dragging.
                            val isDragged = dragFrom == square

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != null && !isDragged) {
                                    Text(
                                        text = piece.symbol,
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (piece.color == com.chessmentor.chess.model.PieceColor.WHITE)
                                            Color.White else Color.Black
                                    )
                                }
                                if (isTarget) {
                                    val hasCapture = piece != null
                                    Box(
                                        modifier = Modifier
                                            .size(if (hasCapture) 44.dp else 16.dp)
                                            .background(
                                                color = if (hasCapture) Color.Transparent
                                                else BoardColors.legalDot,
                                                shape = CircleShape
                                            )
                                            .then(
                                                if (hasCapture) Modifier.border(
                                                    3.dp, BoardColors.captureRing, CircleShape
                                                ) else Modifier
                                            )
                                    )
                                }
                                // File / rank labels on the edge squares.
                                if (rank == 0 && !state.flipped || rank == 7 && state.flipped) {
                                    Text(
                                        text = ('a' + file).toString(),
                                        fontSize = 10.sp,
                                        color = if (isLight) BoardColors.darkSquare else BoardColors.lightSquare,
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating dragged piece, drawn above the board.
            val dragged = dragFrom
            if (dragged != null) {
                val piece = state.board.getOrNull(dragged.rank)?.getOrNull(dragged.file)
                if (piece != null) {
                    Text(
                        text = piece.symbol,
                        fontSize = 34.sp,
                        color = if (piece.color == com.chessmentor.chess.model.PieceColor.WHITE)
                            Color.White else Color.Black,
                        modifier = Modifier.offset {
                            // Centre the glyph on the finger.
                            val x = (dragStart.x + dragOffset.x - squareSizePx / 2).roundToInt()
                            val y = (dragStart.y + dragOffset.y - squareSizePx / 2).roundToInt()
                            IntOffset(x, y)
                        }
                    )
                }
            }
        }
    }
}

/** Promotion picker — shown when a pawn reaches the last rank. */
@Composable
fun PromotionDialog(
    onChoose: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote pawn to") },
        text = { Text("Choose a piece. Queen is usually best for beginners.") },
        confirmButton = {
            TextButton(onClick = { onChoose(PieceType.QUEEN) }) { Text("Queen") }
            TextButton(onClick = { onChoose(PieceType.ROOK) }) { Text("Rook") }
        },
        dismissButton = {
            TextButton(onClick = { onChoose(PieceType.BISHOP) }) { Text("Bishop") }
            TextButton(onClick = { onChoose(PieceType.KNIGHT) }) { Text("Knight") }
        }
    )
}
