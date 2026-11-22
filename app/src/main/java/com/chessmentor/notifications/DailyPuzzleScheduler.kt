package com.chessmentor.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Phase 4 — daily puzzle reminder, AlarmManager version.
 *
 * Why AlarmManager and not WorkManager: it needs no extra dependency,
 * so it cannot break the Phase 1 build. It uses setInexactRepeating
 * (no SCHEDULE_EXACT_ALARM permission needed) at 9:00 local time.
 * If you prefer WorkManager, add androidx.work:work-runtime-ktx to
 * app/build.gradle.kts and replace this with a PeriodicWorkRequest —
 * the receiver / notification code stays the same (see README §9).
 */
object DailyPuzzleScheduler {
    private const val REQUEST_CODE = 1001

    fun scheduleDaily(context: Context, hour: Int = 9, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyPuzzleReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, trigger, AlarmManager.INTERVAL_DAY, pending
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyPuzzleReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
