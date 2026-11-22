package com.chessmentor.ui.lessons

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chessmentor.curriculum.Lesson
import com.chessmentor.curriculum.allLessons
import com.chessmentor.curriculum.stageTitles
import com.chessmentor.ui.board.FenBoard

/**
 * Phase 3 — lesson list + detail. All screens use ChessMentorTheme
 * (applied in MainActivity), so dark mode works here too.
 */
@Composable
fun LessonListScreen(
    completedIds: Set<String>,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Lessons — 7 stages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Principles first, memorisation last. Work through a stage, then play.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        for (stage in 1..7) {
            val lessons = allLessons().filter { it.stage == stage }
            if (lessons.isEmpty()) continue
            Text(
                stageTitles[stage] ?: "Stage $stage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            for (lesson in lessons) {
                val done = lesson.id in completedIds
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { onLessonClick(lesson) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${lesson.id} — ${lesson.title}", style = MaterialTheme.typography.bodyLarge)
                        Text(if (done) "✓" else "", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonDetailScreen(
    lesson: Lesson,
    isCompleted: Boolean,
    onMarkComplete: () -> Unit,
    onTryOnBoard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text(lesson.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            stageTitles[lesson.stage] ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        lesson.fen?.let { fen ->
            FenBoard(fen = fen)
            Spacer(Modifier.height(8.dp))
            Text("Diagram — the position above.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        Text(lesson.body, style = MaterialTheme.typography.bodyLarge)
        lesson.exercise?.let {
            Spacer(Modifier.height(12.dp))
            Text("Try it", fontWeight = FontWeight.Bold)
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onMarkComplete, enabled = !isCompleted) {
                Text(if (isCompleted) "Completed ✓" else "Mark complete")
            }
            lesson.fen?.let { fen ->
                OutlinedButton(onClick = { onTryOnBoard(fen) }) { Text("Try it on the board") }
            }
        }
    }
}
