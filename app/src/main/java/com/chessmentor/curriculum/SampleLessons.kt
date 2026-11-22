package com.chessmentor.curriculum

/**
 * Sample content — Stages 1 and 2 only (Phase 3 will load the full
 * 7-stage curriculum from this shape, ideally from Room / JSON).
 *
 * Teaching voice:
 *  - Levy / DaVaun: energy + principles, not memorisation.
 *  - Rosen: explain the *why*.
 *  - Naroditsky: ask "What was I thinking?" after every exercise.
 *  - Smirnov: one system — think, candidate moves, check, decide.
 */

data class Lesson(
    val id: String,
    val stage: Int,
    val title: String,
    val body: String,          // Markdown-lite text shown in the lesson screen
    val fen: String? = null,   // Starting position for the interactive diagram
    val exercise: String? = null
)

data class Puzzle(
    val id: String,
    val title: String,
    val fen: String,
    /** UCI moves, e.g. listOf("e2e4", "e7e5") — the solution line. */
    val solutionUci: List<String>,
    val hint: String,
    /** True for checkmate puzzles — drives the "First mate!" achievement. */
    val isMate: Boolean = false
)

/** Stage titles for the lesson list screen. */
val stageTitles = mapOf(
    1 to "Stage 1 — Absolute Basics",
    2 to "Stage 2 — Checkmate Before Openings",
    3 to "Stage 3 — Core Principles",
    4 to "Stage 4 — Tactics & Calculation",
    5 to "Stage 5 — Opening Repertoires",
    6 to "Stage 6 — Endgame Fundamentals",
    7 to "Stage 7 — Review & Improve"
)

fun allLessons(): List<Lesson> =
    stage1Lessons + stage2Lessons + stage3Lessons + stage4Lessons +
        stage5Lessons + stage6Lessons + stage7Lessons

fun lessonsForStage(stage: Int): List<Lesson> = allLessons().filter { it.stage == stage }

val stage1Lessons = listOf(
    Lesson(
        id = "1.1",
        stage = 1,
        title = "How the pieces move — the board",
        body = "The board is 8×8. White moves first. " +
            "Control the centre (e4, d4, e5, d5) — that's the Levy principle: " +
            "pieces in the centre control more squares.\n\n" +
            "Tap a piece on the board to see where it can go. " +
            "That dot is a legal move — the game will not let you play an illegal one.",
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    ),
    Lesson(
        id = "1.2",
        stage = 1,
        title = "Check, checkmate and stalemate",
        body = "Check = your king is attacked. You must get out of check.\n\n" +
            "Checkmate = in check and no legal move. Stalemate = NOT in check " +
            "and no legal move — that's a draw, not a win. Beginners lose " +
            "many won games by stalemating instead of mating.",
        fen = "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1",
        exercise = "It is White to move. Find checkmate in 1. (Hint: Qg7#)"
    ),
    Lesson(
        id = "1.3",
        stage = 1,
        title = "Castling, en passant, promotion",
        body = "Castling: king + rook, once per game, if neither has moved, " +
            "no pieces between them, and the king is not in / through check.\n\n" +
            "En passant: if a pawn jumps two squares past your pawn, you may " +
            "capture it as if it had moved one square — only on the very next move.\n\n" +
            "Promotion: a pawn reaching the last rank becomes a queen, rook, " +
            "bishop or knight — usually a queen.",
        fen = "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1"
    )
)

val stage2Lessons = listOf(
    Lesson(
        id = "2.1",
        stage = 2,
        title = "King + Queen vs King — the box",
        body = "Don't chase the king. Shrink the box: put your queen a knight's " +
            "move away, force the king to the edge, then bring your king in. " +
            "Checkmate happens on the edge, with your king covering the escape squares.",
        fen = "8/8/8/8/8/8/5K2/4Q2k w - - 0 1",
        exercise = "White to move. Play Qe1+, then follow the king to the edge."
    ),
    Lesson(
        id = "2.2",
        stage = 2,
        title = "King + Rook vs King — the ladder",
        body = "Use the rook to cut the board in half, then walk your king up " +
            "behind it. Never stalemate: always leave the enemy king a square.",
        fen = "8/8/8/8/8/8/5K2/4R2k w - - 0 1"
    ),
    Lesson(
        id = "2.3",
        stage = 2,
        title = "Back-rank mate",
        body = "A king trapped behind its own pawns on the back rank is mated " +
            "by a rook or queen on that rank. Ask before every move: " +
            "\"Can my king escape?\" If not, watch the back rank.",
        fen = "6k1/5ppp/8/8/8/8/8/4R2K w - - 0 1",
        exercise = "White to move. Find mate in 1. (Hint: Re8#)"
    )
)

val stage1Puzzles = listOf(
    Puzzle(
        id = "p1.1",
        title = "Mate in 1 — queen",
        fen = "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1",
        solutionUci = listOf("f7g7"),
        hint = "The queen needs to cover g8 and h7 at the same time.",
        isMate = true
    ),
    Puzzle(
        id = "p2.1",
        title = "Mate in 1 — back rank",
        fen = "6k1/5ppp/8/8/8/8/8/4R2K w - - 0 1",
        solutionUci = listOf("e1e8"),
        hint = "The black king has no escape square — its own pawns block it.",
        isMate = true
    )
)

// ------------------------------------------------------------------
// Stages 3–7 — full outlines, at least 2 lessons each (Phase 3).
// ------------------------------------------------------------------

val stage3Lessons = listOf(
    Lesson(
        id = "3.1",
        stage = 3,
        title = "Control the centre",
        body = "Levy's first principle: put pawns and pieces on e4, d4, e5, d5. " +
            "A piece in the centre controls more squares and can go either way.\n\n" +
            "Ask before every move: \"Does this help me control the centre?\" " +
            "If not, is there a move that does?",
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
        exercise = "Black to move. Play e5 or d5 and fight for the centre — don't just develop to the edge."
    ),
    Lesson(
        id = "3.2",
        stage = 3,
        title = "Develop pieces, keep your king safe",
        body = "Develop knights and bishops before your queen. Castle early — " +
            "usually by move 10. Don't move the same piece twice in the opening " +
            "unless you have to.\n\n" +
            "King safety is not a suggestion: an uncastled king in the centre " +
            "is the most common way beginners lose fast.",
        fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 1",
        exercise = "White to move. Develop: Bc4, d3, O-O. That's the whole plan."
    )
)

val stage4Lessons = listOf(
    Lesson(
        id = "4.1",
        stage = 4,
        title = "Forks — one piece attacks two",
        body = "A fork is one piece attacking two (or more) enemy pieces at once. " +
            "Knights are the classic forkers, but even a pawn can fork. " +
            "Smirnov's system: before moving, look for checks, captures and " +
            "attacks — forks hide in that list.\n\n" +
            "Do 10–15 minutes of puzzles a day. Speed matters less than accuracy.",
        fen = "4k3/3n1r2/8/4P3/8/8/8/4K3 w - - 0 1",
        exercise = "White to move. Push e6 — the pawn attacks the knight on d7 and the rook on f7 at once."
    ),
    Lesson(
        id = "4.2",
        stage = 4,
        title = "Pins and skewers",
        body = "Pin: a piece can't move because a more valuable piece is behind it. " +
            "Skewer: the valuable piece is in front, and it must move, exposing " +
            "the piece behind it. Same idea, opposite order.\n\n" +
            "Ask: \"If I move this piece, what is behind it?\"",
        fen = "5rk1/8/8/8/8/8/8/4K2R w K - 0 1",
        exercise = "White to move. Play Rh8+ — the king must move, then take the rook on f8 behind it."
    )
)

val stage5Lessons = listOf(
    Lesson(
        id = "5.1",
        stage = 5,
        title = "The London System — ideas, not lines",
        body = "Rosen's beginner opening: 1. d4, 2. Bf4, 3. e3, 4. Nf3, 5. c3, 6. Nbd2. " +
            "Same setup almost every game. The idea: a solid pawn centre, " +
            "bishop outside the pawn chain, and a safe king.\n\n" +
            "Don't memorise 20 moves. Know the idea: play Bf4 early, castle, " +
            "then push e4 or c4 when the time is right.",
        fen = "rnbqkbnr/ppp1pppp/8/3p4/3P1B2/8/PPP1PPPP/RN1QKBNR w KQkq - 0 1",
        exercise = "White to move. Play e3, Nf3, c3, Nbd2 — the London setup."
    ),
    Lesson(
        id = "5.2",
        stage = 5,
        title = "What to do when they don't cooperate",
        body = "Openings are ideas, not scripts. If Black plays something odd, " +
            "go back to principles: centre, development, king safety.\n\n" +
            "Never play a memorised line you don't understand — if you forget " +
            "move 12, you have nothing. If you understand the idea, you always " +
            "have a move.",
        fen = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1"
    )
)

val stage6Lessons = listOf(
    Lesson(
        id = "6.1",
        stage = 6,
        title = "Pawn promotion — the race",
        body = "In the endgame, a passed pawn (no enemy pawn in front of it) is " +
            "worth more than it looks. Push it, support it with your king, " +
            "and don't let the enemy king catch it.\n\n" +
            "Count: can your pawn queen before their king arrives? That's the race.",
        fen = "8/5P2/8/8/8/8/1k6/4K3 w - - 0 1",
        exercise = "White to move. Push f7, then f8=Q. Don't let the black king get close."
    ),
    Lesson(
        id = "6.2",
        stage = 6,
        title = "King and pawn vs king — opposition",
        body = "Opposition: kings facing each other with one square between them. " +
            "Whoever is NOT to move has the opposition — and wins.\n\n" +
            "Rule of thumb: keep your king in front of your pawn, and don't " +
            "let the enemy king in front of it.",
        fen = "8/8/8/3k4/8/3PK3/8/8 w - - 0 1",
        exercise = "White to move. Play Ke4, keep the king in front of the pawn."
    )
)

val stage7Lessons = listOf(
    Lesson(
        id = "7.1",
        stage = 7,
        title = "Review your games — the 3 questions",
        body = "Naroditsky's review, after every game:\n\n" +
            "1. Where did the game turn?\n" +
            "2. What was I thinking at that moment?\n" +
            "3. Have I made this mistake before?\n\n" +
            "Write the answers down. A mistake you name once is a mistake " +
            "you stop making.",
        exercise = "Open Game Review after your next game and answer all three."
    ),
    Lesson(
        id = "7.2",
        stage = 7,
        title = "Blunders vs mistakes — what to fix first",
        body = "A blunder loses material or the game at once. A mistake is a " +
            "small inaccuracy. Fix blunders first: they are usually one-move " +
            "tactics you missed — a hanging piece, a fork, a back-rank mate.\n\n" +
            "Use Game Review to find them, then do 5 puzzles on that tactic.",
        exercise = "After review, pick your worst blunder and find the tactic you missed."
    )
)

/** More puzzles for the daily puzzle screen (Stages 3–4 tactics). */
val stage4Puzzles = listOf(
    Puzzle(
        id = "p4.1",
        title = "Fork — pawn attacks two pieces",
        fen = "4k3/3n1r2/8/4P3/8/8/8/4K3 w - - 0 1",
        solutionUci = listOf("e5e6"),
        hint = "Push the pawn — it attacks the knight on d7 and the rook on f7 at once.",
        isMate = false
    ),
    Puzzle(
        id = "p4.2",
        title = "Skewer — win the rook",
        fen = "5rk1/8/8/8/8/8/8/4K2R w K - 0 1",
        solutionUci = listOf("h1h8"),
        hint = "Check along the back rank — the king must move, then take the rook behind it.",
        isMate = false
    ),
    Puzzle(
        id = "p4.3",
        title = "Mate in 1 — rook on the back rank",
        fen = "6k1/7R/6K1/8/8/8/8/8 w - - 0 1",
        solutionUci = listOf("h7h8"),
        hint = "The rook on h8 covers the whole back rank, and your king covers g7.",
        isMate = true
    )
)

fun allPuzzles(): List<Puzzle> = stage1Puzzles + stage4Puzzles
