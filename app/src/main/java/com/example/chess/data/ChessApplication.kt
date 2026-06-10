package com.example.chess.data

import android.app.Application
import androidx.room.Room

class ChessApplication : Application() {
    lateinit var database: AppDatabase
        private set
    
    lateinit var repository: GameStatsRepository
        private set
        
    override fun onCreate() {
        super.onCreate()
        
        // Pre-create WebView js and wasm HTTP cache directories to satisfy Chromium's internal file enumerators
        try {
            val webViewJsDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            if (!webViewJsDir.exists()) {
                webViewJsDir.mkdirs()
            }
            val webViewWasmDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!webViewWasmDir.exists()) {
                webViewWasmDir.mkdirs()
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewCachePrep", "Failed to pre-create directories: ${e.message}")
        }

        AdManager.initialize(this)
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "chess_database"
        ).fallbackToDestructiveMigration(true).build()
        repository = GameStatsRepository(database.gameStatsDao())
    }
}
