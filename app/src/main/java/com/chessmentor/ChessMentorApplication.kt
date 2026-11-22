package com.chessmentor

import android.app.Application
import com.chessmentor.data.ChessMentorDatabase
import com.chessmentor.data.ProgressRepository

/**
 * Phase 3 — holds the Room database and repository so every
 * ViewModel can share one instance. Declared in AndroidManifest.xml
 * via android:name=".ChessMentorApplication".
 */
class ChessMentorApplication : Application() {
    val database: ChessMentorDatabase by lazy { ChessMentorDatabase.getInstance(this) }
    val repository: ProgressRepository by lazy { ProgressRepository(database.progressDao()) }
}
