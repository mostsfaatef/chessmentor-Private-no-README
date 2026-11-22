package com.chessmentor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Phase 1 theme — Material 3, dark-mode aware.
// Board square colours stay classic (Lichess-style) in both themes
// so the board is always readable; only the surrounding UI changes.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF6D4C41),
    background = Color(0xFFFAFAF5),
    surface = Color(0xFFFFFFFF)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFFBCAAA4),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

/** Board colours — keep these fixed, do not derive from Material theme. */
object BoardColors {
    val lightSquare = Color(0xFFF0D9B5)
    val darkSquare = Color(0xFFB58863)
    val selected = Color(0xFF7A9A5B)
    val lastMove = Color(0xFFCDD26A)
    val legalDot = Color(0xFF000000).copy(alpha = 0.22f)
    val captureRing = Color(0xFF000000).copy(alpha = 0.35f)
    val check = Color(0xFFE53935)
    /** Hint (Phase 2): Stockfish's suggested from/to squares. */
    val hint = Color(0xFF42A5F5)
}

@Composable
fun ChessMentorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
