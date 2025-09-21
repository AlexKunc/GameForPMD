package com.example.gameforpmd

import android.app.Application
import androidx.room.Room
import com.example.gameforpmd.data.db.AppDatabase
import com.example.gameforpmd.ui.game.GameViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MyApp : Application() {
    companion object {
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Room init
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "game_database"
        ).fallbackToDestructiveMigration().build()

        // Koin module
        val appModule = module {
            viewModel { GameViewModel() }
        }

        // Start Koin
        startKoin {
            androidContext(this@MyApp)
            modules(appModule)
        }
    }
}
