package com.example.gameforpmd.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [User::class, Score::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scoreDao(): ScoreDao
}
