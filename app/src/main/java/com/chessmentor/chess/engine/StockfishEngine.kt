package com.chessmentor.chess.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Phase 2 — Stockfish UCI client (Kotlin side only).
 *
 * The native library is built from the official Stockfish source —
 * see README §5 and app/src/main/cpp/CMakeLists.txt. No engine is
 * invented here and no binary is vendored.
 *
 * Threading rule: every native call runs on Dispatchers.Default,
 * behind a single [Mutex], because there is exactly one engine
 * and one search at a time. The UI thread never touches UCI.
 *
 * Fallback: if libstockfish.so is missing (not built yet), [available]
 * is false and every method returns null / an empty result instead
 * of crashing. The UI shows "Stockfish not built yet — see README §5".
 */
data class EvalResult(
    /** Centipawn score from White's point of view; null for mate scores. */
    val cp: Int?,
    /** Mate in N moves from White's point of view; null if not a mate score. */
    val mate: Int?,
    val bestMoveUci: String?
)

class StockfishEngine {

    /** True only when the native library loaded successfully. */
    val available: Boolean

    private val mutex = Mutex()
    private var started = false

    init {
        var loaded = false
        try {
            System.loadLibrary("stockfish")
            // The stub build (no Stockfish sources) loads fine but reports false.
            loaded = try { nativeIsAvailable() } catch (_: UnsatisfiedLinkError) { false }
        } catch (_: UnsatisfiedLinkError) {
            loaded = false
        }
        available = loaded
    }

    // --- JNI surface (implemented in app/src/main/cpp/stockfish_jni.cpp) ---
    private external fun nativeStart()
    private external fun nativeStop()
    private external fun nativeSendCommand(command: String)
    private external fun nativeReadLine(): String?
    private external fun nativeIsAvailable(): Boolean

    /** Level 1–10 → Stockfish Skill Level 0–20. Level 1 ≈ Skill 0. */
    fun skillForLevel(level: Int): Int =
        ((level.coerceIn(1, 10) - 1) * 20 / 9).coerceIn(0, 20)

    /** Search depth per level: shallow for beginners, deeper for strong play. */
    fun depthForLevel(level: Int): Int = when (level.coerceIn(1, 10)) {
        1 -> 2; 2 -> 3; 3 -> 4; 4 -> 6; 5 -> 8
        6 -> 10; 7 -> 12; 8 -> 14; 9 -> 16; else -> 18
    }

    private fun ensureStarted() {
        if (available && !started) {
            nativeStart()
            started = true
        }
    }

    /** Read lines until [predicate] matches, or the timeout fires. Null on timeout. */
    private fun readUntil(timeoutMs: Long, predicate: (String) -> Boolean): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val line = try { nativeReadLine() } catch (_: UnsatisfiedLinkError) { null } ?: continue
            if (predicate(line)) return line
            // Keep the last info line for eval parsing — callers re-read via [lastInfo].
            lastInfoLine = line
        }
        return null
    }

    @Volatile
    private var lastInfoLine: String? = null

    /**
     * Best move for [fen] at [level]. Returns a UCI string such as
     * "e2e4" / "e7e8q", or null when the engine is unavailable or times out.
     */
    suspend fun bestMove(fen: String, level: Int): String? = withContext(Dispatchers.Default) {
        if (!available) return@withContext null
        mutex.withLock {
            ensureStarted()
            lastInfoLine = null
            nativeSendCommand("uci")
            readUntil(2_000) { it.startsWith("uciok") }
            nativeSendCommand("setoption name Skill Level value ${skillForLevel(level)}")
            nativeSendCommand("isready")
            readUntil(2_000) { it.startsWith("readyok") }
            nativeSendCommand("position fen $fen")
            nativeSendCommand("go depth ${depthForLevel(level)}")
            val best = withTimeoutOrNull(15_000) {
                readUntil(15_000) { it.startsWith("bestmove") }
            } ?: return@withLock null
            parseBestMove(best)
        }
    }

    /** Hint = best move at full strength (level 10). */
    suspend fun hint(fen: String): String? = bestMove(fen, level = 10)

    /**
     * Evaluate [fen] at [depth] and return the score + best move.
     * Used by game review (Phase 4). Null when the engine is unavailable.
     */
    suspend fun eval(fen: String, depth: Int = 12): EvalResult? = withContext(Dispatchers.Default) {
        if (!available) return@withContext null
        mutex.withLock {
            ensureStarted()
            lastInfoLine = null
            var bestInfo: String? = null
            nativeSendCommand("uci")
            readUntil(2_000) { it.startsWith("uciok") }
            nativeSendCommand("isready")
            readUntil(2_000) { it.startsWith("readyok") }
            nativeSendCommand("position fen $fen")
            nativeSendCommand("go depth $depth")
            val deadline = System.currentTimeMillis() + 15_000
            var best: String? = null
            while (System.currentTimeMillis() < deadline) {
                val line = try { nativeReadLine() } catch (_: UnsatisfiedLinkError) { null } ?: continue
                if (line.startsWith("info depth") && " score " in line) bestInfo = line
                if (line.startsWith("bestmove")) { best = line; break }
            }
            val bestMove = best?.let { parseBestMove(it) }
            val (cp, mate) = bestInfo?.let { parseScore(it) } ?: (null to null)
            EvalResult(cp = cp, mate = mate, bestMoveUci = bestMove)
        }
    }

    fun stop() {
        if (available && started) {
            try { nativeSendCommand("quit"); nativeStop() } catch (_: UnsatisfiedLinkError) { }
            started = false
        }
    }

    companion object {
        /** "bestmove e2e4 ponder e7e5" -> "e2e4". "(none)" -> null. */
        fun parseBestMove(line: String): String? {
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 2 || parts[0] != "bestmove") return null
            return parts[1].takeIf { it != "(none)" && it.length >= 4 }
        }

        /**
         * "info depth 12 ... score cp 35 ..." -> (35, null).
         * "info depth 12 ... score mate 3 ..." -> (null, 3).
         * Scores are from the side-to-move's point of view in UCI;
         * callers convert to White's point of view using the FEN turn.
         * Returns (null, null) when no score is present.
         */
        fun parseScore(line: String): Pair<Int?, Int?> {
            val cp = Regex("score cp (-?\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
            if (cp != null) return cp to null
            val mate = Regex("score mate (-?\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
            if (mate != null) return null to mate
            return null to null
        }

        /** Convert a side-to-move score to White's point of view. */
        fun toWhitePov(cp: Int?, whiteToMove: Boolean): Int? =
            cp?.let { if (whiteToMove) it else -it }

        /** Mate score to White's POV, same convention as [toWhitePov]. */
        fun mateToWhitePov(mate: Int?, whiteToMove: Boolean): Int? =
            mate?.let { if (whiteToMove) it else -it }
    }
}
