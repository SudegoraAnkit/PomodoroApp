package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getTasksFlow(): Flow<List<PomodoroTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PomodoroTask): Long

    @Update
    suspend fun updateTask(task: PomodoroTask)

    @Delete
    suspend fun deleteTask(task: PomodoroTask)

    @Query("SELECT * FROM pomodoro_tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): PomodoroTask?

    @Query("SELECT * FROM pomodoro_sessions ORDER BY timestamp DESC")
    fun getSessionsFlow(): Flow<List<PomodoroSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PomodoroSession): Long

    @Query("DELETE FROM pomodoro_sessions")
    suspend fun clearAllSessions()
}
