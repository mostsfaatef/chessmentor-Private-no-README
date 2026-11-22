package com.chessmentor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Phase 3 — Room database. Version 1. No migrations yet; see README §7. */
@Database(
    entities = [LessonProgress::class, PuzzleAttempt::class, GameRecord::class, Achievement::class],
    version = 1,
    exportSchema = false
)
abstract class ChessMentorDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var instance: ChessMentorDatabase? = null

        fun getInstance(context: Context): ChessMentorDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChessMentorDatabase::class.java,
                    "chessmentor.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
