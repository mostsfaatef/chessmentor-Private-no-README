package com.chessmentor.ui.elo

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
import com.chessmentor.curriculum.eloBands
import com.chessmentor.ui.board.FenBoard

/** Phase 4 — Guess the Elo mini-game. */
@Composable
fun EloScreen(viewModel: EloViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val game = state.game

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Guess the Elo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Score: ${state.score} / ${state.played}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(game.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(game.moves.joinToString("  "), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        FenBoard(fen = game.fen)
        Spacer(Modifier.height(12.dp))
        Text("What Elo band played this?", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for (band in eloBands) {
                OutlinedButton(
                    onClick = { viewModel.guess(band) },
                    enabled = !state.revealed
                ) { Text(band) }
            }
        }
        if (state.revealed) {
            Spacer(Modifier.height(12.dp))
            val correct = state.selectedBand == game.actualBand
            Text(
                if (correct) "Correct — ${game.actualBand}" else "Not quite — it was ${game.actualBand}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(game.explanation, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = viewModel::next) { Text("Next game") }
        }
    }
}
