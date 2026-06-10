package com.example.chess.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GameStats::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameStatsDao(): GameStatsDao
}
