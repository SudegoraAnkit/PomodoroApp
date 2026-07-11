package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.PomodoroRepository

class PomodoroApplication : Application() {
    // Lazy initialization of database and repository
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { PomodoroRepository(database.pomodoroDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
