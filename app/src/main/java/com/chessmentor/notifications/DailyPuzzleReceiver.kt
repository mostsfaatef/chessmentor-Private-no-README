package com.chessmentor.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.chessmentor.MainActivity
import com.chessmentor.curriculum.allPuzzles

/**
 * Phase 4 — fires at 9:00, posts one notification, done.
 * The notification opens MainActivity; the daily puzzle lives on the
 * Puzzles tab. No notification is posted without the
 * POST_NOTIFICATIONS permission (Android 13+) — see MainActivity,
 * which asks once on first launch.
 */
class DailyPuzzleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val puzzle = allPuzzles().randomOrNull()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_puzzle"
        val channel = NotificationChannel(
            channelId, "Daily puzzle", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Your daily ChessMentor puzzle reminder" }
        manager.createNotificationChannel(channel)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Your daily puzzle is ready")
            .setContentText(puzzle?.title ?: "Solve today's puzzle — 10 minutes.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(2001, notification)
    }
}
