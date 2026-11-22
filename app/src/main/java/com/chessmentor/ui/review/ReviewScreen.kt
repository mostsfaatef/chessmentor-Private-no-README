package com.chessmentor.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chessmentor.ui.board.FenBoard

/**
 * Phase 4 — game review (Naroditsky's 3 questions + Stockfish flags).
 * Tap a move to see the position before it was played.
 */
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Game review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (!state.engineAvailable) {
            Text(
                "Stockfish isn't built yet, so no engine evals — the move list and " +
                    "the 3 questions below still work. See README §5.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAnalyze, enabled = !state.analyzing) {
            Text(if (state.analyzing) "Analysing…" else "Analyse last game")
        }
        if (state.analyzed && state.moves.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("No moves to review yet — play a game first.")
        }
        for (move in state.moves) {
            val flag = when {
                move.isBlunder -> " — BLUNDER"
                move.isInaccuracy -> " — inaccuracy"
                else -> ""
            }
            val evalText = if (move.evalBeforeCpWhite != null && move.evalAfterCpWhite != null) {
                val before = move.evalBeforeCpWhite / 100.0
                val after = move.evalAfterCpWhite / 100.0
                " (eval ${if (before >= 0) "+" else ""}$before → ${if (after >= 0) "+" else ""}$after)"
            } else ""
            Text(
                text = "${move.number}. ${move.san}$flag$evalText" +
                    (move.bestMoveUci?.let { " — engine's choice: $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (move.isBlunder) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    .clickable { viewModel.selectMove(move.fenBefore) }
            )
        }
        state.selectedFen?.let { fen ->
            Spacer(Modifier.height(12.dp))
            Text("Position before that move:", fontWeight = FontWeight.Bold)
            FenBoard(fen = fen)
        }
        if (state.analyzed) {
            Spacer(Modifier.height(16.dp))
            Text("Naroditsky's 3 questions", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.question1,
                onValueChange = { viewModel.setQuestion(1, it) },
                label = { Text("1. Where did the game turn?") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.question2,
                onValueChange = { viewModel.setQuestion(2, it) },
                label = { Text("2. What was I thinking?") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.question3,
                onValueChange = { viewModel.setQuestion(3, it) },
                label = { Text("3. Have I made this mistake before?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = viewModel::saveNotes) { Text("Save notes") }
            if (state.notesSaved) Text("Notes saved for this review session.")
        }
    }
}
