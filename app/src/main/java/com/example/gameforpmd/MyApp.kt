package com.example.gameforpmd

import android.app.Application
import androidx.room.Room
import com.example.gameforpmd.data.db.AppDatabase

class MyApp : Application() {
    companion object {
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "game_database"
        ).build()
    }
}
