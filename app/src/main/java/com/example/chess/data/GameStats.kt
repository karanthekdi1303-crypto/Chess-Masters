package com.example.chess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val id: Int = 1, // Only 1 row needed for active local profile
    val username: String = "Grandmaster",
    val avatar: String = "♔",
    val theme: String = "Frosted Glass",
    val language: String = "English",
    val commentaryEnabled: Boolean = true,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val winStreak: Int = 0,
    val gameHistory: String = "", // Comma separated: e.g. "W,L,D"
    val pieceStyle: String = "Classic Outline",
    val pieceColorStyle: String = "Standard Crisp",
    
    // Cloud profile fields requested of player's secure profile
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val dob: String = "",
    val playerId: String = "",
    val lastLogin: String = "",
    val deviceInformation: String = "",
    val cloudSynced: Boolean = false,
    val cloudUserId: String = "",
    val creationDate: String = "",
    val isDatabaseAdmin: Boolean = false,
    val emailVerified: Boolean = false,
    
    // Admin, Moderation, and Fraud Detection
    val accountStatus: String = "Active", // "Active", "Inactive", "Banned"
    val warningLevel: Int = 0,
    val fraudFlags: Int = 0
) {
    val battleScore: Int
        get() = (wins * 5) + (draws * 2) + (losses * -2)

    val winRate: Float
        get() = if (gamesPlayed > 0) (wins.toFloat() / gamesPlayed) * 100f else 0f

    val krRatio: Float
        get() = if (losses > 0) wins.toFloat() / losses else wins.toFloat()

    val playerTitle: String
        get() = when {
            wins == 0 && losses == 0 -> "BATTLE SCORE"
            winRate >= 70f && gamesPlayed >= 10 -> "Rising Grandmaster"
            winRate >= 50f && winStreak >= 3 -> "Aggressive Player"
            winRate >= 50f && draws > losses -> "Defensive Wall"
            battleScore < 0 -> "Risk Player"
            wins > losses * 2 -> "Attack Specialist"
            else -> "Tactical Mind"
        }
    
    val historyList: List<String>
        get() = gameHistory.split(",").filter { it.isNotEmpty() }
}

