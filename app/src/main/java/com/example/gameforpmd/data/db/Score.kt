package com.example.gameforpmd.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val points: Int,
    val difficulty: Int,
    val date: Long = System.currentTimeMillis(),

    // новые поля
    val roundDuration: Int,     // в секундах
    val maxBugs: Int,
    val bonusInterval: Int
)

