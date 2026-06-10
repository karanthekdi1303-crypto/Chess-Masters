package com.example.chess.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameStatsRepository(private val dao: GameStatsDao) {
    val stats: Flow<GameStats> = dao.getStats().map { it ?: GameStats() }

    suspend fun updateStats(stats: GameStats) {
        dao.insertStats(stats)
    }
}
