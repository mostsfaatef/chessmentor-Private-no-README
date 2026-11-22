package com.chessmentor.ui.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chessmentor.data.Achievements
import com.chessmentor.notifications.DailyPuzzleScheduler

/**
 * Phase 3 — progress, achievements and recent games.
 * The daily-reminder button (Phase 4) schedules a 9:00 puzzle
 * reminder with AlarmManager — no extra dependency needed.
 */
@Composable
fun ProgressScreen(viewModel: ProgressViewModel, modifier: Modifier = Modifier) {
    val lessons by viewModel.lessonProgress.collectAsState()
    val attempts by viewModel.puzzleAttempts.collectAsState()
    val games by viewModel.games.collectAsState()
    val unlocked by viewModel.achievements.collectAsState()
    val context = LocalContext.current
    var reminderMessage by remember { mutableStateOf<String?>(null) }

    val completedCount = lessons.count { it.completed }
    val solvedCount = attempts.filter { it.solved }.map { it.puzzleId }.distinct().size
    val unlockedIds = unlocked.map { it.id }.toSet()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Your progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Lessons completed: $completedCount / ${viewModel.totalLessons}")
        Text("Puzzles solved: $solvedCount")
        Text("Games recorded: ${games.size}")
        Spacer(Modifier.height(16.dp))
        Text("Achievements", fontWeight = FontWeight.Bold)
        for (achievement in Achievements.ALL) {
            val hasIt = achievement.id in unlockedIds
            Text(
                "${if (hasIt) "✓" else "○"} ${achievement.title} — ${achievement.description}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Recent games", fontWeight = FontWeight.Bold)
        if (games.isEmpty()) {
            Text("No finished games yet — play vs Stockfish to record one.")
        } else {
            for (game in games.take(5)) {
                Text("${game.result} — vs Stockfish L${game.stockfishLevel}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            DailyPuzzleScheduler.scheduleDaily(context)
            reminderMessage = "Daily puzzle reminder set for 9:00."
        }) { Text("Remind me daily at 9:00") }
        reminderMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
            Text(
                "On Android 13+, allow notifications when asked — otherwise the reminder can't appear.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
