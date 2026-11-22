package com.chessmentor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chessmentor.chess.engine.StockfishEngine
import com.chessmentor.chess.model.GameStatus
import com.chessmentor.chess.model.PieceColor
import com.chessmentor.curriculum.Lesson
import com.chessmentor.ui.board.ChessBoard
import com.chessmentor.ui.board.ChessViewModel
import com.chessmentor.ui.board.PromotionDialog
import com.chessmentor.ui.elo.EloScreen
import com.chessmentor.ui.elo.EloViewModel
import com.chessmentor.ui.lessons.LessonDetailScreen
import com.chessmentor.ui.lessons.LessonListScreen
import com.chessmentor.ui.progress.ProgressScreen
import com.chessmentor.ui.progress.ProgressViewModel
import com.chessmentor.ui.puzzle.PuzzleScreen
import com.chessmentor.ui.puzzle.PuzzleViewModel
import com.chessmentor.ui.review.ReviewScreen
import com.chessmentor.ui.review.ReviewViewModel
import com.chessmentor.ui.theme.ChessMentorTheme
import com.chessmentor.ui.viewModelFactory

/** Simple hand-rolled navigation — no navigation-compose dependency needed. */
enum class Screen { Play, Lessons, LessonDetail, Puzzles, Progress, Elo, Review }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessMentorTheme {
                val app = application as ChessMentorApplication
                val repository = app.repository
                // One engine for the whole activity: Play + Review share it.
                val engine = remember { StockfishEngine() }
                val boardViewModel: ChessViewModel = viewModel(
                    factory = viewModelFactory { ChessViewModel(engine, repository) }
                )
                val puzzleViewModel: PuzzleViewModel = viewModel(
                    factory = viewModelFactory { PuzzleViewModel(repository) }
                )
                val progressViewModel: ProgressViewModel = viewModel(
                    factory = viewModelFactory { ProgressViewModel(repository) }
                )
                val eloViewModel: EloViewModel = viewModel()
                val reviewViewModel: ReviewViewModel = viewModel(
                    factory = viewModelFactory { ReviewViewModel(engine) }
                )
                ChessMentorApp(
                    boardViewModel, puzzleViewModel, progressViewModel,
                    eloViewModel, reviewViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessMentorApp(
    boardViewModel: ChessViewModel,
    puzzleViewModel: PuzzleViewModel,
    progressViewModel: ProgressViewModel,
    eloViewModel: EloViewModel,
    reviewViewModel: ReviewViewModel
) {
    var screen by remember { mutableStateOf(Screen.Play) }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    val progress by progressViewModel.lessonProgress.collectAsState()
    val completedIds = progress.filter { it.completed }.map { it.lessonId }.toSet()

    // Phase 4: ask once, on first launch, for notification permission (Android 13+).
    // If the user declines, nothing nags them again — the daily reminder
    // button in Progress explains why notifications are needed.
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result is not needed — the reminder button explains the state */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = screen != Screen.Play) {
        screen = when (screen) {
            Screen.LessonDetail -> Screen.Lessons
            else -> Screen.Play
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ChessMentor — Play & Learn") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Top nav — one row, horizontally scrollable on small screens.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Start
            ) {
                for ((label, target) in listOf(
                    "Play" to Screen.Play,
                    "Lessons" to Screen.Lessons,
                    "Puzzles" to Screen.Puzzles,
                    "Guess the Elo" to Screen.Elo,
                    "Progress" to Screen.Progress,
                    "Review" to Screen.Review
                )) {
                    TextButton(onClick = { screen = target }) {
                        Text(label, fontWeight = if (screen == target) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            when (screen) {
                Screen.Play -> PlayScreen(
                    viewModel = boardViewModel,
                    onReviewClick = { screen = Screen.Review }
                )
                Screen.Lessons -> LessonListScreen(
                    completedIds = completedIds,
                    onLessonClick = { lesson -> selectedLesson = lesson; screen = Screen.LessonDetail }
                )
                Screen.LessonDetail -> selectedLesson?.let { lesson ->
                    LessonDetailScreen(
                        lesson = lesson,
                        isCompleted = lesson.id in completedIds,
                        onMarkComplete = { progressViewModel.completeLesson(lesson.id) },
                        onTryOnBoard = { fen ->
                            boardViewModel.loadFen(fen)
                            screen = Screen.Play
                        }
                    )
                } ?: LessonListScreen(completedIds = completedIds, onLessonClick = {
                    selectedLesson = it; screen = Screen.LessonDetail
                })
                Screen.Puzzles -> PuzzleScreen(viewModel = puzzleViewModel)
                Screen.Elo -> EloScreen(viewModel = eloViewModel)
                Screen.Progress -> ProgressScreen(viewModel = progressViewModel)
                Screen.Review -> ReviewScreen(
                    viewModel = reviewViewModel,
                    onAnalyze = {
                        val (fens, ucis, sans) = boardViewModel.reviewData()
                        reviewViewModel.analyze(fens, ucis, sans)
                    }
                )
            }
        }
    }

    val boardState by boardViewModel.uiState.collectAsState()
    if (boardState.pendingPromotion != null) {
        PromotionDialog(
            onChoose = boardViewModel::choosePromotion,
            onDismiss = boardViewModel::cancelPromotion
        )
    }
}

/** Phase 1 board screen, extended in Phase 2 with engine controls. */
@Composable
private fun PlayScreen(viewModel: ChessViewModel, onReviewClick: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        val statusText = when (state.status) {
            GameStatus.CHECKMATE -> "Checkmate"
            GameStatus.STALEMATE -> "Stalemate — draw"
            GameStatus.CHECK -> "Check! Protect your king"
            GameStatus.ONGOING -> if (state.turn == PieceColor.WHITE) "White to move" else "Black to move"
        }
        Text(statusText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.engineThinking) Text("Stockfish is thinking…")
        Text(
            "Principles: control the centre • develop pieces • keep your king safe",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        ChessBoard(
            state = state,
            onSquareTapped = viewModel::onSquareTapped,
            onDragMove = viewModel::tryMove
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = viewModel::newGame) { Text("New game") }
            OutlinedButton(onClick = viewModel::undo) { Text("Undo") }
            OutlinedButton(onClick = viewModel::flipBoard) { Text("Flip board") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.setVsEngine(!state.vsEngine) }) {
                Text(if (state.vsEngine) "vs Stockfish: On" else "vs Stockfish: Off")
            }
            OutlinedButton(
                onClick = viewModel::requestHint,
                enabled = !state.engineThinking && state.status == GameStatus.ONGOING
            ) { Text("Hint") }
            OutlinedButton(onClick = onReviewClick) { Text("Review game") }
        }
        if (state.vsEngine) {
            Spacer(Modifier.height(8.dp))
            Text("Engine level: ${state.engineLevel} (1 = easiest, 10 = strongest)")
            Slider(
                value = state.engineLevel.toFloat(),
                onValueChange = { viewModel.setLevel(it.roundToInt().coerceIn(1, 10)) },
                valueRange = 1f..10f,
                steps = 8
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.setPlayAs(PieceColor.WHITE) }) { Text("Play White") }
                OutlinedButton(onClick = { viewModel.setPlayAs(PieceColor.BLACK) }) { Text("Play Black") }
            }
            if (!state.engineAvailable) {
                Text(
                    "Stockfish not built yet — see README §5. Play vs a friend on this board for now.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Moves", fontWeight = FontWeight.Bold)
        Text(
            text = if (state.moveList.isEmpty()) "No moves yet — tap a piece to see where it can go."
            else state.moveList.chunked(2).mapIndexed { i, pair ->
                "${i + 1}. ${pair.joinToString(" ")}"
            }.joinToString("  "),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
