package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int? = null,
    val sessionType: String = "FOCUS", // "FOCUS", "SHORT_BREAK", "LONG_BREAK"
    val durationMinutes: Int = 25,
    val timestamp: Long = System.currentTimeMillis(),
    val completed: Boolean = true
)
