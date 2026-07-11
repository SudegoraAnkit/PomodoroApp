package com.example.data

import kotlinx.coroutines.flow.Flow

class PomodoroRepository(private val pomodoroDao: PomodoroDao) {
    val allTasks: Flow<List<PomodoroTask>> = pomodoroDao.getTasksFlow()
    val allSessions: Flow<List<PomodoroSession>> = pomodoroDao.getSessionsFlow()

    suspend fun insertTask(task: PomodoroTask): Long {
        return pomodoroDao.insertTask(task)
    }

    suspend fun updateTask(task: PomodoroTask) {
        pomodoroDao.updateTask(task)
    }

    suspend fun deleteTask(task: PomodoroTask) {
        pomodoroDao.deleteTask(task)
    }

    suspend fun getTaskById(id: Int): PomodoroTask? {
        return pomodoroDao.getTaskById(id)
    }

    suspend fun insertSession(session: PomodoroSession): Long {
        return pomodoroDao.insertSession(session)
    }

    suspend fun clearAllSessions() {
        pomodoroDao.clearAllSessions()
    }
}
