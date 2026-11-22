package com.chessmentor.chess.model

/**
 * Phase 1 — complete legal-move engine (no Stockfish yet).
 *
 * GothamChess principle in code form: the engine doesn't memorise
 * lines, it derives everything from three questions —
 *  1. Where can this piece go? (pseudo-legal)
 *  2. Would my king still be safe? (legal filter)
 *  3. Is the game over? (check / mate / stalemate)
 *
 * That is the same thinking system taught in Stage 3 of the curriculum.
 *
 * State is kept in plain arrays so a copy is cheap — the ViewModel
 * snapshots the game before every move to support Undo.
 */
enum class GameStatus { ONGOING, CHECK, CHECKMATE, STALEMATE }

class ChessGame {

    // board[rank][file], rank 0 = White back rank.
    private val board: Array<Array<Piece?>> = Array(8) { arrayOfNulls<Piece>(8) }

    var turn: PieceColor = PieceColor.WHITE
        private set

    var enPassantTarget: Square? = null
        private set

    var whiteKingside = true
        private set
    var whiteQueenside = true
        private set
    var blackKingside = true
        private set
    var blackQueenside = true
        private set

    var status: GameStatus = GameStatus.ONGOING
        private set

    var halfmoveClock = 0
        private set
    var fullmoveNumber = 1
        private set

    /** SAN strings in order, for the move list UI. */
    val moveHistorySan = mutableListOf<String>()
    private val moveHistory = mutableListOf<Move>()

    /** FEN before move 1, then after every move. Review (Phase 4) replays this. */
    val fenHistory = mutableListOf<String>()
    /** UCI strings in order, parallel to [moveHistorySan]. */
    val uciHistory = mutableListOf<String>()

    init { reset() }

    fun reset() {
        for (r in 0..7) for (f in 0..7) board[r][f] = null
        val back = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        for (f in 0..7) {
            board[0][f] = Piece(back[f], PieceColor.WHITE)
            board[1][f] = Piece(PieceType.PAWN, PieceColor.WHITE)
            board[6][f] = Piece(PieceType.PAWN, PieceColor.BLACK)
            board[7][f] = Piece(back[f], PieceColor.BLACK)
        }
        turn = PieceColor.WHITE
        enPassantTarget = null
        whiteKingside = true; whiteQueenside = true
        blackKingside = true; blackQueenside = true
        status = GameStatus.ONGOING
        halfmoveClock = 0; fullmoveNumber = 1
        moveHistorySan.clear(); moveHistory.clear()
        fenHistory.clear(); uciHistory.clear()
        fenHistory += toFen()
    }

    fun pieceAt(s: Square): Piece? = board[s.rank][s.file]
    fun pieceAt(file: Int, rank: Int): Piece? =
        if (file in 0..7 && rank in 0..7) board[rank][file] else null

    /** Snapshot for the UI — a copy so Compose can't mutate the game. */
    fun boardSnapshot(): List<List<Piece?>> =
        board.map { row -> row.toList() }

    fun lastMove(): Move? = moveHistory.lastOrNull()
    fun history(): List<Move> = moveHistory.toList()

    fun copy(): ChessGame {
        val g = ChessGame()
        for (r in 0..7) for (f in 0..7) g.board[r][f] = board[r][f]
        g.turn = turn; g.enPassantTarget = enPassantTarget
        g.whiteKingside = whiteKingside; g.whiteQueenside = whiteQueenside
        g.blackKingside = blackKingside; g.blackQueenside = blackQueenside
        g.status = status; g.halfmoveClock = halfmoveClock; g.fullmoveNumber = fullmoveNumber
        g.moveHistorySan.addAll(moveHistorySan); g.moveHistory.addAll(moveHistory)
        // reset() already seeded one FEN — clear before copying the real history.
        g.fenHistory.clear(); g.uciHistory.clear()
        g.fenHistory.addAll(fenHistory); g.uciHistory.addAll(uciHistory)
        return g
    }

    /**
     * Load a position from FEN (puzzles, lesson diagrams, review).
     * Accepts 1–6 fields; missing fields default to "w - - 0 1".
     * Returns false and leaves the game unchanged on bad input.
     */
    fun loadFen(fen: String): Boolean {
        val parts = fen.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) return false
        val rows = parts[0].split("/")
        if (rows.size != 8) return false
        val newBoard: Array<Array<Piece?>> = Array(8) { arrayOfNulls<Piece>(8) }
        for ((rowIndex, row) in rows.withIndex()) {
            val rank = 7 - rowIndex
            var file = 0
            for (ch in row) {
                if (ch.isDigit()) {
                    file += ch.digitToInt()
                } else {
                    if (file !in 0..7) return false
                    val type = when (ch.lowercaseChar()) {
                        'p' -> PieceType.PAWN; 'n' -> PieceType.KNIGHT
                        'b' -> PieceType.BISHOP; 'r' -> PieceType.ROOK
                        'q' -> PieceType.QUEEN; 'k' -> PieceType.KING
                        else -> return false
                    }
                    val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    newBoard[rank][file] = Piece(type, color)
                    file++
                }
            }
            if (file != 8) return false
        }
        // Only commit once the whole FEN has parsed.
        for (r in 0..7) for (f in 0..7) board[r][f] = newBoard[r][f]
        turn = if (parts.getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE
        val castling = parts.getOrNull(2) ?: "-"
        whiteKingside = 'K' in castling; whiteQueenside = 'Q' in castling
        blackKingside = 'k' in castling; blackQueenside = 'q' in castling
        val ep = parts.getOrNull(3) ?: "-"
        enPassantTarget = if (ep == "-") null else runCatching { Square.fromAlgebraic(ep) }.getOrNull()
        halfmoveClock = parts.getOrNull(4)?.toIntOrNull() ?: 0
        fullmoveNumber = parts.getOrNull(5)?.toIntOrNull() ?: 1
        moveHistorySan.clear(); moveHistory.clear()
        fenHistory.clear(); uciHistory.clear()
        fenHistory += toFen()
        updateStatus()
        return true
    }

    /** Apply a UCI move such as "e2e4" or "e7e8q". Returns false if illegal. */
    fun applyUci(uci: String): Boolean {
        if (uci.length < 4) return false
        val from = runCatching { Square.fromAlgebraic(uci.substring(0, 2)) }.getOrNull() ?: return false
        val to = runCatching { Square.fromAlgebraic(uci.substring(2, 4)) }.getOrNull() ?: return false
        val promotion = when (uci.getOrNull(4)?.lowercaseChar()) {
            'q' -> PieceType.QUEEN; 'r' -> PieceType.ROOK
            'b' -> PieceType.BISHOP; 'n' -> PieceType.KNIGHT
            else -> null
        }
        return makeMove(from, to, promotion)
    }

    // ------------------------------------------------------------------
    // Attack / check
    // ------------------------------------------------------------------

    fun isSquareAttacked(s: Square, by: PieceColor): Boolean {
        // Pawns attack diagonally forward.
        val pawnDir = if (by == PieceColor.WHITE) -1 else 1 // look *behind* the target
        for (df in listOf(-1, 1)) {
            val p = pieceAt(s.file + df, s.rank + pawnDir)
            if (p?.type == PieceType.PAWN && p.color == by) return true
        }
        // Knights
        for ((df, dr) in listOf(
            1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2
        )) {
            val p = pieceAt(s.file + df, s.rank + dr)
            if (p?.type == PieceType.KNIGHT && p.color == by) return true
        }
        // King
        for (df in -1..1) for (dr in -1..1) {
            if (df == 0 && dr == 0) continue
            val p = pieceAt(s.file + df, s.rank + dr)
            if (p?.type == PieceType.KING && p.color == by) return true
        }
        // Sliding pieces: rook/queen on ranks/files, bishop/queen on diagonals.
        val sliders = listOf(
            Triple(1, 0, listOf(PieceType.ROOK, PieceType.QUEEN)),
            Triple(-1, 0, listOf(PieceType.ROOK, PieceType.QUEEN)),
            Triple(0, 1, listOf(PieceType.ROOK, PieceType.QUEEN)),
            Triple(0, -1, listOf(PieceType.ROOK, PieceType.QUEEN)),
            Triple(1, 1, listOf(PieceType.BISHOP, PieceType.QUEEN)),
            Triple(1, -1, listOf(PieceType.BISHOP, PieceType.QUEEN)),
            Triple(-1, 1, listOf(PieceType.BISHOP, PieceType.QUEEN)),
            Triple(-1, -1, listOf(PieceType.BISHOP, PieceType.QUEEN))
        )
        for ((df, dr, types) in sliders) {
            var f = s.file + df; var r = s.rank + dr
            while (f in 0..7 && r in 0..7) {
                val p = board[r][f]
                if (p != null) {
                    if (p.color == by && p.type in types) return true
                    break
                }
                f += df; r += dr
            }
        }
        return false
    }

    private fun kingSquare(color: PieceColor): Square? {
        for (r in 0..7) for (f in 0..7) {
            val p = board[r][f]
            if (p?.type == PieceType.KING && p.color == color) return Square(f, r)
        }
        return null
    }

    fun isInCheck(color: PieceColor): Boolean {
        val k = kingSquare(color) ?: return false
        return isSquareAttacked(k, color.opposite())
    }

    // ------------------------------------------------------------------
    // Move generation
    // ------------------------------------------------------------------

    /** Pseudo-legal: piece movement only, king safety not yet checked. */
    fun pseudoLegalMoves(from: Square): List<Move> {
        val piece = pieceAt(from) ?: return emptyList()
        val out = mutableListOf<Move>()
        val dir = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
        val promoRank = if (piece.color == PieceColor.WHITE) 7 else 0

        fun addSliding(dirs: List<Pair<Int, Int>>) {
            for ((df, dr) in dirs) {
                var f = from.file + df; var r = from.rank + dr
                while (f in 0..7 && r in 0..7) {
                    val target = board[r][f]
                    if (target == null) {
                        out += Move(from, Square(f, r), piece)
                    } else {
                        if (target.color != piece.color)
                            out += Move(from, Square(f, r), piece, captured = target)
                        break
                    }
                    f += df; r += dr
                }
            }
        }
        fun addStep(df: Int, dr: Int) {
            val f = from.file + df; val r = from.rank + dr
            if (f !in 0..7 || r !in 0..7) return
            val target = board[r][f]
            if (target == null) out += Move(from, Square(f, r), piece)
            else if (target.color != piece.color)
                out += Move(from, Square(f, r), piece, captured = target)
        }
        fun addPawnMove(to: Square, captured: Piece?, isEnPassant: Boolean = false) {
            if (to.rank == promoRank) {
                for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT))
                    out += Move(from, to, piece, captured = captured, promotion = promo, isEnPassant = isEnPassant)
            } else {
                out += Move(from, to, piece, captured = captured, isEnPassant = isEnPassant)
            }
        }

        when (piece.type) {
            PieceType.PAWN -> {
                // A pawn on the last rank should already have promoted;
                // guard the bounds so a corrupt position can't crash the app.
                if (from.rank + dir !in 0..7) return out
                val one = Square(from.file, from.rank + dir)
                if (pieceAt(one) == null) {
                    addPawnMove(one, null)
                    if (from.rank == startRank) {
                        val two = Square(from.file, from.rank + 2 * dir)
                        if (pieceAt(two) == null)
                            out += Move(from, two, piece, isDoublePawnPush = true)
                    }
                }
                for (df in listOf(-1, 1)) {
                    val f = from.file + df; val r = from.rank + dir
                    if (f !in 0..7 || r !in 0..7) continue
                    val target = board[r][f]
                    if (target != null && target.color != piece.color) {
                        addPawnMove(Square(f, r), target)
                    } else if (enPassantTarget == Square(f, r)) {
                        // Captured pawn sits beside us, not on the target square.
                        val victim = board[from.rank][f]
                        if (victim?.type == PieceType.PAWN && victim.color != piece.color)
                            addPawnMove(Square(f, r), victim, isEnPassant = true)
                    }
                }
            }
            PieceType.KNIGHT -> for ((df, dr) in listOf(
                1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2
            )) addStep(df, dr)

            PieceType.BISHOP -> addSliding(listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1))
            PieceType.ROOK -> addSliding(listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1))
            PieceType.QUEEN -> addSliding(listOf(
                1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1
            ))
            PieceType.KING -> {
                for (df in -1..1) for (dr in -1..1) {
                    if (df == 0 && dr == 0) continue
                    addStep(df, dr)
                }
                // Castling — king may not be in check, pass through check, or land in check.
                val rank = if (piece.color == PieceColor.WHITE) 0 else 7
                val kingside = if (piece.color == PieceColor.WHITE) whiteKingside else blackKingside
                val queenside = if (piece.color == PieceColor.WHITE) whiteQueenside else blackQueenside
                if (from == Square(4, rank) && !isInCheck(piece.color)) {
                    if (kingside && board[rank][5] == null && board[rank][6] == null &&
                        board[rank][7]?.type == PieceType.ROOK &&
                        board[rank][7]?.color == piece.color &&
                        !isSquareAttacked(Square(5, rank), piece.color.opposite()) &&
                        !isSquareAttacked(Square(6, rank), piece.color.opposite())
                    ) out += Move(from, Square(6, rank), piece, isCastling = true)
                    if (queenside && board[rank][3] == null && board[rank][2] == null && board[rank][1] == null &&
                        board[rank][0]?.type == PieceType.ROOK &&
                        board[rank][0]?.color == piece.color &&
                        !isSquareAttacked(Square(3, rank), piece.color.opposite()) &&
                        !isSquareAttacked(Square(2, rank), piece.color.opposite())
                    ) out += Move(from, Square(2, rank), piece, isCastling = true)
                }
            }
        }
        return out
    }

    /** Legal = pseudo-legal and the king is safe after the move. */
    fun legalMoves(from: Square): List<Move> {
        val piece = pieceAt(from) ?: return emptyList()
        if (piece.color != turn) return emptyList()
        return pseudoLegalMoves(from).filter { move ->
            val trial = copy()
            trial.applyMoveUnchecked(move)
            !trial.isInCheck(piece.color)
        }
    }

    fun allLegalMoves(color: PieceColor): List<Move> {
        val out = mutableListOf<Move>()
        for (r in 0..7) for (f in 0..7) {
            val p = board[r][f]
            if (p != null && p.color == color) out += legalMoves(Square(f, r))
        }
        return out
    }

    /** Find a legal move by squares. Promotion defaults to queen. */
    fun findLegalMove(from: Square, to: Square, promotion: PieceType? = null): Move? {
        val candidates = legalMoves(from).filter { it.to == to }
        if (candidates.isEmpty()) return null
        // Promotion moves come in four flavours; pick the requested one.
        return promotion?.let { p -> candidates.firstOrNull { it.promotion == p } }
            ?: candidates.firstOrNull { it.promotion == PieceType.QUEEN }
            ?: candidates.firstOrNull()
    }

    fun isPromotionMove(from: Square, to: Square): Boolean =
        legalMoves(from).any { it.to == to && it.promotion != null }

    // ------------------------------------------------------------------
    // Making moves
    // ------------------------------------------------------------------

    /** Caller must have validated with [findLegalMove] / [legalMoves]. */
    private fun applyMoveUnchecked(move: Move) {
        // En passant: remove the pawn that was bypassed.
        if (move.isEnPassant) board[move.from.rank][move.to.file] = null
        // Castling: move the rook too.
        if (move.isCastling) {
            val rank = move.from.rank
            if (move.to.file == 6) { // kingside: h-file rook -> f-file
                board[rank][5] = board[rank][7]; board[rank][7] = null
            } else { // queenside: a-file rook -> d-file
                board[rank][3] = board[rank][0]; board[rank][0] = null
            }
        }
        board[move.from.rank][move.from.file] = null
        val placed = if (move.promotion != null)
            Piece(move.promotion, move.piece.color) else move.piece
        board[move.to.rank][move.to.file] = placed

        // Castling rights: king move kills both sides; rook move / capture kills one.
        when (move.piece.type) {
            PieceType.KING -> when (move.piece.color) {
                PieceColor.WHITE -> { whiteKingside = false; whiteQueenside = false }
                PieceColor.BLACK -> { blackKingside = false; blackQueenside = false }
            }
            PieceType.ROOK -> when (move.from) {
                Square(0, 0) -> whiteQueenside = false
                Square(7, 0) -> whiteKingside = false
                Square(0, 7) -> blackQueenside = false
                Square(7, 7) -> blackKingside = false
                else -> {}
            }
            else -> {}
        }
        if (move.captured?.type == PieceType.ROOK) when (move.to) {
            Square(0, 0) -> whiteQueenside = false
            Square(7, 0) -> whiteKingside = false
            Square(0, 7) -> blackQueenside = false
            Square(7, 7) -> blackKingside = false
            else -> {}
        }

        enPassantTarget = if (move.isDoublePawnPush)
            Square(move.from.file, (move.from.rank + move.to.rank) / 2) else null

        halfmoveClock = if (move.piece.type == PieceType.PAWN || move.captured != null) 0
        else halfmoveClock + 1
        if (turn == PieceColor.BLACK) fullmoveNumber++
        turn = turn.opposite()
    }

    /** Public entry point. Returns false if the move is illegal. */
    fun makeMove(from: Square, to: Square, promotion: PieceType? = null): Boolean {
        val move = findLegalMove(from, to, promotion) ?: return false
        val san = move.toSan()
        applyMoveUnchecked(move)
        moveHistory += move
        uciHistory += move.uci
        fenHistory += toFen()
        // Append + / # after the position is updated.
        val suffix = when {
            statusAfterMove() == GameStatus.CHECKMATE -> "#"
            isInCheck(turn) -> "+"
            else -> ""
        }
        moveHistorySan += san + suffix
        updateStatus()
        return true
    }

    private fun statusAfterMove(): GameStatus {
        if (allLegalMoves(turn).isEmpty())
            return if (isInCheck(turn)) GameStatus.CHECKMATE else GameStatus.STALEMATE
        return if (isInCheck(turn)) GameStatus.CHECK else GameStatus.ONGOING
    }

    private fun updateStatus() { status = statusAfterMove() }

    /** FEN for Stockfish (Phase 2) and for puzzle storage. */
    fun toFen(): String {
        val sb = StringBuilder()
        for (r in 7 downTo 0) {
            var empty = 0
            for (f in 0..7) {
                val p = board[r][f]
                if (p == null) empty++
                else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    val c = when (p.type) {
                        PieceType.KING -> 'k'; PieceType.QUEEN -> 'q'; PieceType.ROOK -> 'r'
                        PieceType.BISHOP -> 'b'; PieceType.KNIGHT -> 'n'; PieceType.PAWN -> 'p'
                    }
                    sb.append(if (p.color == PieceColor.WHITE) c.uppercaseChar() else c)
                }
            }
            if (empty > 0) sb.append(empty)
            if (r > 0) sb.append('/')
        }
        sb.append(if (turn == PieceColor.WHITE) " w " else " b ")
        var castling = ""
        if (whiteKingside) castling += "K"
        if (whiteQueenside) castling += "Q"
        if (blackKingside) castling += "k"
        if (blackQueenside) castling += "q"
        sb.append(if (castling.isEmpty()) "-" else castling).append(' ')
        sb.append(enPassantTarget?.algebraic ?: "-")
        sb.append(" $halfmoveClock $fullmoveNumber")
        return sb.toString()
    }
}
