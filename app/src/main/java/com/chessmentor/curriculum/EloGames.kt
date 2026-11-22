package com.chessmentor.curriculum

/**
 * Phase 4 — "Guess the Elo" dataset.
 *
 * Each entry is a real-feeling position with a known Elo *band*.
 * Bands are coarse on purpose: the game is about recognising the
 * *level of play*, not guessing an exact number.
 *
 * Elo bands used as answers: "<1000", "1000–1400", "1400–1800", "1800+".
 * The `moves` are the short game so far (SAN), shown so the user can
 * judge the quality of play, not just the position.
 */
data class EloGame(
    val id: String,
    val title: String,
    /** Position to judge — midgame, White to move unless stated. */
    val fen: String,
    val moves: List<String>,
    val actualBand: String,
    val explanation: String
)

val eloBands = listOf("<1000", "1000–1400", "1400–1800", "1800+")

val eloGames = listOf(
    EloGame(
        id = "elo1",
        title = "Game 1 — the hanging queen",
        fen = "r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 1",
        moves = listOf("1. e4 e5", "2. Bc4 Nc6", "3. Qh5"),
        actualBand = "<1000",
        explanation = "White's queen is out far too early and Black can already " +
            "attack it with ...g6. Beginners love early queen raids — " +
            "strong players develop first."
    ),
    EloGame(
        id = "elo2",
        title = "Game 2 — Scholar's mate tried",
        fen = "r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 0 1",
        moves = listOf("1. e4 e5", "2. Bc4 Nc6", "3. Qh5 Nf6"),
        actualBand = "1000–1400",
        explanation = "Black calmly defends with ...Nf6 instead of panicking. " +
            "The idea is known, the defence is sound — typical club-player level."
    ),
    EloGame(
        id = "elo3",
        title = "Game 3 — solid London setup",
        fen = "rnbqkb1r/pp2pppp/5n2/2pp4/3P1B2/2P1PN2/PP3PPP/RN1QKB1R b KQkq - 0 1",
        moves = listOf("1. d4 d5", "2. Bf4 Nf6", "3. e3 c5", "4. c3 Nc6", "5. Nf3"),
        actualBand = "1000–1400",
        explanation = "A tidy London System setup: bishop out, pawns solid, " +
            "king safe. No fireworks, no mistakes — solid beginner-to-club play."
    ),
    EloGame(
        id = "elo4",
        title = "Game 4 — a clean fork",
        fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 0 1",
        moves = listOf("1. e4 e5", "2. Nf3 Nc6", "3. Bc4 Bc5", "4. O-O Nf6", "5. Ng5"),
        actualBand = "1400–1800",
        explanation = "White plays the Fried Liver idea (Ng5) — a known, sharp " +
            "tactic that needs real calculation. This is not a beginner's move."
    ),
    EloGame(
        id = "elo5",
        title = "Game 5 — quiet positional squeeze",
        fen = "r1bq1rk1/pp2ppbp/2np1np1/8/3P4/2N1PN2/PP3PPP/R1BQKB1R w KQ - 0 1",
        moves = listOf("1. d4 Nf6", "2. c4 g6", "3. Nc3 Bg7", "4. e4 d6", "5. Nf3 O-O", "6. Be2 e5"),
        actualBand = "1400–1800",
        explanation = "A King's Indian setup with a clear plan: centre pawns, " +
            "kings castled, pieces coordinated. No piece is hanging, " +
            "no move is wasted — strong club play."
    ),
    EloGame(
        id = "elo6",
        title = "Game 6 — precise endgame technique",
        fen = "8/5pk1/6p1/3P4/8/6KP/8/8 w - - 0 1",
        moves = listOf("... rook endgame simplified", "White has an outside passed pawn"),
        actualBand = "1800+",
        explanation = "An outside passed pawn with correct king placement — " +
            "the kind of endgame only strong players steer towards " +
            "deliberately. Technique, not tactics, decides this one."
    )
)
