package com.example.gameforpmd.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScoreDao {
    @Insert
    suspend fun insert(score: Score)

//    @Query("SELECT * FROM scores ORDER BY points DESC")
//    suspend fun getAll(): List<Score>

    @Query("SELECT * FROM scores WHERE userId = :userId ORDER BY points DESC")
    suspend fun getByUser(userId: Int): List<Score>

    @Query("""
    SELECT s.* FROM scores s
    INNER JOIN (
        SELECT userId, MAX(points) as maxPoints
        FROM scores
        GROUP BY userId
    ) grouped 
    ON s.userId = grouped.userId 
       AND s.points = grouped.maxPoints
    ORDER BY s.points DESC
""")
    suspend fun getBestScores(): List<Score>
}
