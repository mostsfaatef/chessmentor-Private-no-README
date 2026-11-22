package com.chessmentor.ui.puzzle

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chessmentor.ui.board.ChessBoard

/**
 * Phase 3 — puzzle screen. Reuses the Phase 1 board; the only
 * difference from Play is the ViewModel checking the solution.
 * Daily practice: 10–15 minutes (Stage 4).
 */
@Composable
fun PuzzleScreen(viewModel: PuzzleViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val puzzle = state.puzzle

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Daily puzzles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (puzzle != null) {
            Text(puzzle.title, style = MaterialTheme.typography.titleMedium)
            Text("Puzzle ${state.puzzleIndex + 1} of ${viewModel.puzzles.size} • Attempts: ${state.attempts}",
                style = MaterialTheme.typography.bodySmall)
        }
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        ChessBoard(
            state = state.board,
            onSquareTapped = viewModel::onSquareTapped,
            onDragMove = viewModel::tryMove
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = viewModel::showHint) { Text("Hint") }
            Button(onClick = viewModel::nextPuzzle) { Text(if (state.solved) "Next puzzle" else "Skip") }
        }
        if (state.hintShown && puzzle != null) {
            Spacer(Modifier.height(8.dp))
            Text("Hint: ${puzzle.hint}", style = MaterialTheme.typography.bodyMedium)
        }
        if (state.solved) {
            Spacer(Modifier.height(8.dp))
            Text("Nice — that one is saved to your puzzle history.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
    // Note: puzzles promote to queen automatically (PuzzleViewModel),
    // so no promotion dialog is needed here — unlike the Play screen.
}
