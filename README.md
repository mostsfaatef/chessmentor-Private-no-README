# ChessMentor — Learn Chess from Scratch

**Name:** ChessMentor (kept — clear, beginner-friendly; alternatives: `GothamLearn`, `ChessPath`).
**Stack:** Kotlin • Jetpack Compose (100% UI) • MVVM + StateFlow • Room • Stockfish
**Min SDK:** 24 (Android 7.0) • Target SDK: 34 • Compile SDK: 34

> **Status: Phases 1–4 implemented.** Phase 1 board + legal moves, Phase 2 Stockfish,
> Phase 3 lessons / puzzles / Room, Phase 4 Guess the Elo / game review / daily reminders.
> Not compiled locally — there is no Android SDK/Gradle in this VM (see §10).

## 1. Teaching philosophy — the "Dream Team"

| Tutor | What we steal | Where it lives |
|---|---|---|
| **Levy Rozman (GothamChess)** | Principles over memorisation: centre, development, king safety. "Guess the Elo", game review | Stage 3 prompts in Play screen, Stage 7 review, `ui/elo/` |
| **Igor Smirnov** | Systematic thinking: candidate moves → checks → decide. Break plateaus | `ChessGame.legalMoves()` filter = the system in code; Stage 4 tactics |
| **Eric Rosen** | Calm "why", simple openings (London System) | Stage 5 — ideas, not lines |
| **Daniel Naroditsky** | Speedrun over-explanation + 3 review questions: Where did it turn? What was I thinking? Have I made this mistake before? | Stage 7, `ui/review/` |
| **DaVaun Williams (H1Chess)** | Energy + fundamentals: centre, development, safety, tactics, pawn structure, endgames | Stages 1–6 order |

## 2. Architecture (MVVM)

```
MainActivity (Compose entry, hand-rolled Screen navigation)
  ├─ ChessViewModel : ViewModel  (Play vs Stockfish, Hint, Undo, FEN history)
  ├─ PuzzleViewModel               (solution checking, Room recording)
  ├─ ProgressViewModel             (Room Flows → progress / achievements)
  ├─ EloViewModel                  (Guess the Elo, in-memory score)
  └─ ReviewViewModel               (Stockfish eval per move + 3 questions)
       └─ ChessGame (pure Kotlin, no Android deps)
            ├─ Piece / Square / Move
            └─ legalMoves(), makeMove(), loadFen(), toFen(), isInCheck()

UI: ui/theme/Theme.kt (Material 3, dark mode)
    ui/board/ChessBoard.kt (tap + drag, highlights, hint squares, promotion dialog)
    ui/board/BoardFromFen.kt (read-only diagrams for lessons / Elo / review)
    ui/lessons/, ui/puzzle/, ui/progress/, ui/elo/, ui/review/ (one screen per file)
Model: chess/model/ — unit-testable on JVM, no emulator needed
Engine: chess/engine/StockfishEngine.kt + app/src/main/cpp/ (see §5)
Data: data/ — Room entities, DAO, database, repository, achievements
Notifications: notifications/ — AlarmManager scheduler + receiver (see §9)
```

**Rules:**
- UI never mutates the game. Only ViewModels call `ChessGame.makeMove()` / `applyUci()`.
- Board state is a snapshot (`List<List<Piece?>>`), so Compose can't corrupt the model.
- `ChessGame.copy()` before every move → Undo is a snapshot restore.
- FEN is the contract between model ↔ Stockfish ↔ puzzles ↔ review.
- Navigation is hand-rolled (`enum class Screen` in MainActivity) — no navigation-compose dependency.

## 3. File structure

```
ChessMentor/
├── settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml
├── README.md
└── app/
    ├── build.gradle.kts        # kapt + Room, cmake block wired (see §5)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml # ChessMentorApplication, POST_NOTIFICATIONS, receiver
        ├── cpp/CMakeLists.txt
        ├── cpp/stockfish_jni.cpp
        ├── res/values/themes.xml
        └── java/com/chessmentor/
            ├── MainActivity.kt                 # Nav: Play / Lessons / Puzzles / Elo / Progress / Review
            ├── ChessMentorApplication.kt       # Room database + repository singletons
            ├── chess/model/ Piece.kt, Move.kt, ChessGame.kt
            │                                   # + loadFen(), applyUci(), fen/uci history
            ├── chess/engine/StockfishEngine.kt # UCI client, Skill mapping, eval parsing
            ├── ui/theme/Theme.kt               # Material 3, dark mode, board + hint colours
            ├── ui/board/ ChessViewModel.kt, ChessBoard.kt, BoardFromFen.kt
            ├── ui/lessons/LessonScreens.kt     # List (7 stages) + detail
            ├── ui/puzzle/ PuzzleViewModel.kt, PuzzleScreen.kt
            ├── ui/progress/ ProgressViewModel.kt, ProgressScreen.kt
            ├── ui/elo/ EloViewModel.kt, EloScreen.kt
            ├── ui/review/ ReviewViewModel.kt, ReviewScreen.kt
            ├── data/ ProgressModels.kt, ProgressDao.kt, ChessMentorDatabase.kt,
            │         ProgressRepository.kt, Achievements.kt
            ├── notifications/ DailyPuzzleScheduler.kt, DailyPuzzleReceiver.kt
            └── curriculum/ SampleLessons.kt (Stages 1–7, puzzles), EloGames.kt (6 games)
```

## 4. Setup — run it

1. Android Studio Hedgehog+ (AGP 8.5.2, Kotlin 2.0.20, JDK 17). **NDK required** for the
   native build (see §5); without it, comment out `externalNativeBuild` in `app/build.gradle.kts`.
2. Open this folder → let Gradle sync.
3. Run on emulator / device (API 24+).
4. Play tab: tap a piece → legal dots. Toggle "vs Stockfish: On", pick a level (1–10),
   play White or Black, use Hint. Lessons tab: work a stage, "Try it on the board".
   Puzzles tab: solve, Hint if stuck. Progress tab: lessons / puzzles / achievements.
   Guess the Elo tab: judge 6 games. Review tab: "Analyse last game".

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest   # add tests under app/src/test/ — see §10
```

### Get an APK without building locally (GitHub Actions)

1. Push this repo to GitHub (branch `main`).
2. Open the repo → Actions → "Build debug APK" → Run workflow (or just push — it runs automatically).
3. When the run finishes, open it → Artifacts → download `chessmentor-debug-apk`.
4. Copy `app-debug.apk` to your phone and tap it to install (allow "Install unknown apps" for your browser/files app if asked).
5. It installs on Android 7.0+ (min SDK 24); the first run works without Stockfish — the engine shows "not built yet" until the real Stockfish sources are wired in (§5.3).

## 5. Phase 2 — Stockfish (implemented, one wiring step left)

Stockfish is GPLv3, open source. No engine is invented here, no binary is vendored.

### 5.1 Get Stockfish

```bash
git clone https://github.com/official-stockfish/Stockfish.git
# Use a tagged release, e.g. Stockfish 16/17. Do not vendor a random binary.
```

### 5.2 What is already in the repo

- `app/src/main/cpp/CMakeLists.txt` — builds `libstockfish.so`. If `STOCKFISH_DIR`
  is not set it builds a **stub** (`STOCKFISH_AVAILABLE=0`) so the app still
  compiles and runs; `StockfishEngine.available` is then false and the UI shows
  "Stockfish not built yet — see README §5" instead of crashing.
  Pass `-DSTOCKFISH_DIR=/path/to/Stockfish` to build the real engine.
- `app/src/main/cpp/stockfish_jni.cpp` — tiny JNI surface:
  `nativeStart()` / `nativeStop()` / `nativeSendCommand(String)` /
  `nativeReadLine(): String?` / `nativeIsAvailable(): Boolean`.
  One worker thread owns the engine; Kotlin sends UCI lines into a command
  queue and drains replies. All UCI parsing lives in Kotlin.
- `chess/engine/StockfishEngine.kt` — `bestMove(fen, level)`, `hint(fen)`,
  `eval(fen, depth)` (parses `info … score cp/mate` + `bestmove`), Skill Level
  mapping 1–10 → 0–20, depth 2–18 by level. Everything runs on
  `Dispatchers.Default` behind a `Mutex` (one engine, one search at a time).
- `ChessViewModel` — level selector 1–10, Play White/Black, engine moves
  automatically after your move, Hint highlights from/to squares in blue,
  Undo takes back two plies vs the engine (yours + its reply).
- `app/build.gradle.kts` — `externalNativeBuild { cmake … }` is wired, with
  `ndk { abiFilters += ["arm64-v8a", "armeabi-v7a", "x86_64"] }`.

### 5.3 The one remaining wiring step (release-specific)

The UCI entry-point symbol differs by Stockfish release. In
`stockfish_jni.cpp`, in the `HAVE_STOCKFISH` block, wire your tagged
release's loop (recent releases: `Stockfish::UCI::loop(...)` — verify the
symbol for your tag) so the worker thread feeds it lines from the command
queue and pushes replies with `pushResponse()`. Until that one line is
wired, the library builds and loads, and Kotlin correctly reports the
engine as unavailable. Do not ship the stub as "Stockfish works".

### 5.4 Alternative (no NDK)
Ship a Stockfish binary as an asset and run it with `ProcessBuilder` +
UCI over stdin/stdout. Same Kotlin API shape, worse startup / APK size.
Prefer the JNI route above for production.

## 6. Curriculum — 7 stages (all in `curriculum/SampleLessons.kt`)

1. **Absolute Basics** — movement, castling, en passant, promotion, check/mate/stalemate.
2. **Checkmate Before Openings** — K+Q vs K, K+R vs K, back-rank mate.
3. **Core Principles** — centre, development, king safety. "What to think about."
4. **Tactics & Calculation** — forks, pins, skewers. Daily puzzles, 10–15 min.
5. **Opening Repertoires** — London System etc. Ideas, not long lines.
6. **Endgame Fundamentals** — pawn promotion, essential techniques.
7. **Review & Improve** — 3 questions: Where did it turn? What was I thinking? Have I made this mistake before?

Stages 1–2 are full lessons; Stages 3–7 have at least 2 lessons each.
Puzzles: `stage1Puzzles` + `stage4Puzzles` (fork, skewer, mate in 1 — all
positions were hand-checked for legality and for the stated solution).

## 7. Phase 3 — Lessons, puzzles, Room (implemented)

- **Room:** `LessonProgress`, `PuzzleAttempt`, `GameRecord`, `Achievement`
  entities; `ProgressDao` (Flows + suspend queries); `ChessMentorDatabase`
  v1 (`chessmentor.db`, `fallbackToDestructiveMigration`); `ProgressRepository`
  (complete lesson, record attempt/game, unlock-once achievements).
  Enabled with **kapt** (`id("org.jetbrains.kotlin.kapt")` + `kapt(room-compiler)`):
  the version catalog has no KSP plugin entry, so kapt is the documented
  fallback — swap to KSP if you add it to the catalog.
- **Lessons:** list grouped by stage with ✓ for completed, detail screen with
  text + FEN diagram (`FenBoard`), "Mark complete", "Try it on the board"
  (loads the FEN into the Play board).
- **Puzzles:** FEN position, play the solution UCI line, wrong moves are
  rejected and counted, opponent replies play automatically, Hint shows the
  lesson hint, solving records a `PuzzleAttempt` in Room.
- **Achievements:** First lesson, First mate, Puzzle solver (5 distinct
  puzzles), First win (as White). Shown locked/unlocked in Progress.

## 8. Phase 4 — Guess the Elo, game review, notifications (implemented)

- **Guess the Elo** (`curriculum/EloGames.kt`, `ui/elo/`): 6 games with a
  midgame FEN, the moves so far, a known Elo band (`<1000`, `1000–1400`,
  `1400–1800`, `1800+`), and an explanation of what gives the level away.
  Guess → reveal → score kept for the session.
- **Game review** (`ui/review/`): replays `fenHistory` / `uciHistory` /
  `moveHistorySan` from the last game. With Stockfish built, each move is
  evaluated before/after (depth 8, White's POV); a drop > 200cp from the
  mover's point of view is a **blunder**, 50–200cp an **inaccuracy**, and the
  engine's best move is shown. Without Stockfish, review still lists the
  moves and asks Naroditsky's 3 questions — no eval is ever invented.
  Notes are kept in memory for the review session only.
- **Dark mode:** already in `Theme.kt`; all new screens use `ChessMentorTheme`.

## 9. Daily puzzle notifications (implemented, AlarmManager)

- `notifications/DailyPuzzleScheduler.kt` — `scheduleDaily(context, 9, 0)`
  with `AlarmManager.setInexactRepeating(RTC_WAKEUP, 9:00, INTERVAL_DAY)`.
  No `SCHEDULE_EXACT_ALARM` permission needed. Cancel with `cancel(context)`.
- `notifications/DailyPuzzleReceiver.kt` — posts one notification at 9:00
  (channel `daily_puzzle`), opens MainActivity, text is a random puzzle title.
- **Permission flow (Android 13+):** `POST_NOTIFICATIONS` is declared in the
  manifest and requested once on first launch in MainActivity. If declined,
  the app works normally and the Progress screen's "Remind me daily at 9:00"
  button explains why notifications are needed. No nagging on re-launch.
- **WorkManager alternative:** if you prefer it, add
  `androidx.work:work-runtime-ktx` to `app/build.gradle.kts` and replace the
  scheduler with a `PeriodicWorkRequest` — the receiver/notification code
  stays the same. It was deliberately *not* added as a dependency, so the
  Phase 1 build cannot break on it.

## 10. What is still unverified — run this in Android Studio

**Local build status (2026-11-22): not compiled yet.** JDK 17 (Temurin),
Android SDK (platform-tools, android-34, build-tools 34.0.0), Gradle 8.7,
CMake 3.22.1 and NDK 26.3.11579264 are all installed in this VM
(`~/jdk-17`, `~/Android/Sdk`, `~/gradle-8.7`), and a `gradlew` wrapper
was added. But the build still cannot run here: the Gradle daemon needs
loopback TCP between its client and daemon JVMs, and this VM's sandbox
blocks direct TCP from Java processes ("Other TCP connections is turned
off … switch other_tcp from Deny to Ask" in Muse settings → Permissions →
Direct network protocols). The failure is
`DaemonConnectionException: Could not receive a message from the daemon`,
caused by `IllegalArgumentException: Unexpected type tag 109 found`
(tag 109 = `m`, the first byte of that block message). Python loopback
sockets work; Java sockets do not. If that permission is enabled, retry
`./gradlew assembleDebug --no-daemon` here — everything else is ready.
Until then, use the GitHub Actions workflow in §4 or Android Studio.

In Android Studio, run first:

```bash
./gradlew assembleDebug
```

Likely first-run issues to expect (not hidden):
1. NDK missing → comment out `externalNativeBuild` (app builds, Stockfish reports unavailable).
2. `STOCKFISH_DIR` not set → stub lib builds; wire §5.3 for the real engine.
3. kapt/Room: first sync downloads `room-compiler`; if kapt is removed in a future Kotlin version, move to KSP.

**Unit tests to add first (`app/src/test/.../ChessGameTest.kt`):**
- Fool's mate is checkmate in 2, Scholar's mate works.
- Castling blocked by check / through check is illegal.
- En passant only on the immediate next move.
- Stalemate position returns `STALEMATE`, not checkmate.
- Promotion returns 4 legal moves, queen by default.
- `loadFen` round-trips: `loadFen(fen); toFen() == fen` for the puzzle FENs.
- `StockfishEngine.parseBestMove` / `parseScore` on sample UCI lines.

## 11. What Phase 1 deliberately left out (now done)

- Stockfish binary, Room DB, navigation — each is now an isolated addition;
  the model and ViewModel APIs already supported them (`toFen()`, `moveHistorySan`, `BoardUiState`).
- Piece graphics are Unicode glyphs. Swap `Piece.symbol` for vector drawables without touching the rules.
