package com.myapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.myapp.data.AppDatabase
import com.myapp.data.PomodoroRepository

class PomodoroApplication : Application() {
    // Lazy initialization of database and repository
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { PomodoroRepository(database.pomodoroDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Alerts"
            val descriptionText = "Alerts when your work or break sessions end"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_timer_alerts"
    }
}
