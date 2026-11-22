package com.chessmentor.chess.model

/**
 * Phase 1 — core model. No Android dependencies here so the rules
 * can be unit-tested on the JVM without an emulator.
 *
 * Board coordinates: file 0..7 = a..h, rank 0..7 = 1..8.
 * rank 0 is White's back rank, rank 7 is Black's back rank.
 * This matches how a human reads a board from White's side.
 */

enum class PieceColor { WHITE, BLACK }

fun PieceColor.opposite(): PieceColor =
    if (this == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE

enum class PieceType(val value: Int) {
    PAWN(1), KNIGHT(3), BISHOP(3), ROOK(5), QUEEN(9), KING(0)
}

/** Immutable piece. Copy it when promoting — never mutate in place. */
data class Piece(val type: PieceType, val color: PieceColor) {
    /** Unicode glyph used by the Compose board in Phase 1.
     *  Phase 4 can swap this for vector drawables without touching the model. */
    val symbol: String
        get() = when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"; PieceType.QUEEN -> "♕"; PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"; PieceType.KNIGHT -> "♘"; PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"; PieceType.QUEEN -> "♛"; PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"; PieceType.KNIGHT -> "♞"; PieceType.PAWN -> "♟"
            }
        }

    /** Single-letter FEN / SAN letter, e.g. N for knight, empty for pawn. */
    val letter: String
        get() = when (type) {
            PieceType.KING -> "K"; PieceType.QUEEN -> "Q"; PieceType.ROOK -> "R"
            PieceType.BISHOP -> "B"; PieceType.KNIGHT -> "N"; PieceType.PAWN -> ""
        }
}

data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7 && rank in 0..7) { "Square out of bounds: $file,$rank" }
    }

    val algebraic: String
        get() = "${('a' + file)}${rank + 1}"

    companion object {
        fun fromAlgebraic(s: String): Square {
            require(s.length == 2) { "Bad square: $s" }
            return Square(s[0] - 'a', s[1] - '1')
        }
    }
}
