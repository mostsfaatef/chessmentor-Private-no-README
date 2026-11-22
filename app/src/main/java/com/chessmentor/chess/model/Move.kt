package com.chessmentor.chess.model

/**
 * One move. `promotion` is null except for a pawn reaching the last rank.
 * `isCastling` / `isEnPassant` are set by the generator so the UI and
 * the Stockfish layer (Phase 2) don't have to re-derive them.
 */
data class Move(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val captured: Piece? = null,
    val promotion: PieceType? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    val isDoublePawnPush: Boolean = false
) {
    /** Simple long-algebraic form, e.g. e2e4 / e7e8q — exactly what UCI expects. */
    val uci: String
        get() = from.algebraic + to.algebraic + when (promotion) {
            PieceType.QUEEN -> "q"; PieceType.ROOK -> "r"
            PieceType.BISHOP -> "b"; PieceType.KNIGHT -> "n"
            else -> ""
        }

    /** Human-readable for the move list, e.g. Nf3, exd5, O-O. */
    fun toSan(isCapture: Boolean = captured != null || isEnPassant): String {
        if (isCastling) return if (to.file == 6) "O-O" else "O-O-O"
        val dest = to.algebraic
        return when (piece.type) {
            PieceType.PAWN -> if (isCapture) "${('a' + from.file)}x$dest" else dest
            else -> piece.letter + (if (isCapture) "x" else "") + dest
        } + when (promotion) {
            PieceType.QUEEN -> "=Q"; PieceType.ROOK -> "=R"
            PieceType.BISHOP -> "=B"; PieceType.KNIGHT -> "=N"
            else -> ""
        }
    }
}
