package com.myapp.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myapp.MainActivity
import com.myapp.PomodoroApplication
import com.myapp.data.PomodoroRepository
import com.myapp.data.PomodoroSession
import com.myapp.data.PomodoroTask
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TimerMode(val displayName: String, val defaultMinutes: Int) {
    FOCUS("Focus", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

class PomodoroViewModel(private val repository: PomodoroRepository) : ViewModel() {

    // Task & Session flows from DB
    val tasks: StateFlow<List<PomodoroTask>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sessions: StateFlow<List<PomodoroSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Timer settings
    private val _focusDurationMinutes = MutableStateFlow(25)
    val focusDurationMinutes = _focusDurationMinutes.asStateFlow()

    private val _shortBreakDurationMinutes = MutableStateFlow(5)
    val shortBreakDurationMinutes = _shortBreakDurationMinutes.asStateFlow()

    private val _longBreakDurationMinutes = MutableStateFlow(15)
    val longBreakDurationMinutes = _longBreakDurationMinutes.asStateFlow()

    // Timer active states
    private val _timerMode = MutableStateFlow(TimerMode.FOCUS)
    val timerMode = _timerMode.asStateFlow()

    private val _timerSecondsLeft = MutableStateFlow(25 * 60)
    val timerSecondsLeft = _timerSecondsLeft.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(25 * 60)
    val timerTotalSeconds = _timerTotalSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _activeTask = MutableStateFlow<PomodoroTask?>(null)
    val activeTask = _activeTask.asStateFlow()

    // Filter and Navigation UI states
    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private var timerJob: Job? = null

    init {
        resetTimerState()
    }

    fun setTimerMode(mode: TimerMode) {
        _timerMode.value = mode
        _isTimerRunning.value = false
        timerJob?.cancel()
        val minutes = when (mode) {
            TimerMode.FOCUS -> _focusDurationMinutes.value
            TimerMode.SHORT_BREAK -> _shortBreakDurationMinutes.value
            TimerMode.LONG_BREAK -> _longBreakDurationMinutes.value
        }
        _timerSecondsLeft.value = minutes * 60
        _timerTotalSeconds.value = minutes * 60
    }

    fun startTimer(context: Context? = null) {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_timerSecondsLeft.value > 0) {
                delay(1000)
                _timerSecondsLeft.value -= 1
            }
            onTimerFinished(context)
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimerState() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        val minutes = when (_timerMode.value) {
            TimerMode.FOCUS -> _focusDurationMinutes.value
            TimerMode.SHORT_BREAK -> _shortBreakDurationMinutes.value
            TimerMode.LONG_BREAK -> _longBreakDurationMinutes.value
        }
        _timerSecondsLeft.value = minutes * 60
        _timerTotalSeconds.value = minutes * 60
    }

    fun skipSession(context: Context? = null) {
        _isTimerRunning.value = false
        timerJob?.cancel()
        autoTransitionMode()
    }

    private fun autoTransitionMode() {
        when (_timerMode.value) {
            TimerMode.FOCUS -> {
                // Default transition to short break (can be long break if 4 sessions are completed)
                setTimerMode(TimerMode.SHORT_BREAK)
            }
            TimerMode.SHORT_BREAK, TimerMode.LONG_BREAK -> {
                setTimerMode(TimerMode.FOCUS)
            }
        }
    }

    private suspend fun onTimerFinished(context: Context?) {
        _isTimerRunning.value = false
        timerJob?.cancel()

        val currentMode = _timerMode.value
        val durationMin = when (currentMode) {
            TimerMode.FOCUS -> _focusDurationMinutes.value
            TimerMode.SHORT_BREAK -> _shortBreakDurationMinutes.value
            TimerMode.LONG_BREAK -> _longBreakDurationMinutes.value
        }

        // 1. Save Session to DB
        val associatedTaskId = if (currentMode == TimerMode.FOCUS) _activeTask.value?.id else null
        val session = PomodoroSession(
            taskId = associatedTaskId,
            sessionType = currentMode.name,
            durationMinutes = durationMin,
            timestamp = System.currentTimeMillis(),
            completed = true
        )
        repository.insertSession(session)

        // 2. If FOCUS, increment task pomodoros if active
        if (currentMode == TimerMode.FOCUS) {
            _activeTask.value?.let { task ->
                val updatedTask = task.copy(completedPomodoros = task.completedPomodoros + 1)
                repository.updateTask(updatedTask)
                // Keep activeTask reference updated
                _activeTask.value = updatedTask
            }
        }

        // 3. Play sound and trigger vibration
        if (context != null) {
            playAlertSound()
            triggerVibration(context)

            // Dynamic local notifications
            val title = when (currentMode) {
                TimerMode.FOCUS -> "Focus Session Finished!"
                TimerMode.SHORT_BREAK -> "Short Break Finished!"
                TimerMode.LONG_BREAK -> "Long Break Finished!"
            }

            val activeTaskTitle = _activeTask.value?.title
            val text = when (currentMode) {
                TimerMode.FOCUS -> {
                    if (activeTaskTitle != null) {
                        "Nicely done! Focus session for '$activeTaskTitle' complete. Time for a break!"
                    } else {
                        "Nicely done! Focus session complete. Time for a break!"
                    }
                }
                TimerMode.SHORT_BREAK -> "Break is over! Ready to focus?"
                TimerMode.LONG_BREAK -> "Long break completed! Ready to start another focus session?"
            }

            showNotification(context, title, text)
        }

        // 4. Move to next cycle automatically
        autoTransitionMode()
    }

    fun updateSettings(focusMin: Int, shortMin: Int, longMin: Int) {
        _focusDurationMinutes.value = focusMin
        _shortBreakDurationMinutes.value = shortMin
        _longBreakDurationMinutes.value = longMin
        // Re-apply to active timer if not running
        if (!_isTimerRunning.value) {
            resetTimerState()
        }
    }

    // Task operations
    fun selectTask(task: PomodoroTask?) {
        _activeTask.value = task
    }

    fun addTask(title: String, notes: String, plannedPomos: Int, category: String) {
        viewModelScope.launch {
            val task = PomodoroTask(
                title = title,
                notes = notes,
                plannedPomodoros = plannedPomos,
                category = category
            )
            repository.insertTask(task)
        }
    }

    fun toggleTaskCompletion(task: PomodoroTask) {
        viewModelScope.launch {
            val isCompleting = !task.isCompleted
            val updatedTask = task.copy(
                isCompleted = isCompleting,
                completedAt = if (isCompleting) System.currentTimeMillis() else null
            )
            repository.updateTask(updatedTask)

            // If active task is completed, clear active selection or update
            if (_activeTask.value?.id == task.id) {
                _activeTask.value = updatedTask
            }
        }
    }

    fun incrementCompletedPomodoro(task: PomodoroTask) {
        viewModelScope.launch {
            val updated = task.copy(completedPomodoros = task.completedPomodoros + 1)
            repository.updateTask(updated)
            if (_activeTask.value?.id == task.id) {
                _activeTask.value = updated
            }
        }
    }

    fun deleteTask(task: PomodoroTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            if (_activeTask.value?.id == task.id) {
                _activeTask.value = null
            }
        }
    }

    fun updateTaskDetails(task: PomodoroTask) {
        viewModelScope.launch {
            repository.updateTask(task)
            if (_activeTask.value?.id == task.id) {
                _activeTask.value = task
            }
        }
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllSessions()
        }
    }

    // Tone Alert Synthesis (Sweet notification sound)
    private fun playAlertSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 500) // Beautiful prompt tone
        } catch (e: Exception) {
            Log.e("PomodoroViewModel", "Error playing tone: ${e.message}")
        }
    }

    // Dynamic Haptic Feedback Vibration
    private fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(800)
            }
        } catch (e: Exception) {
            Log.e("PomodoroViewModel", "Vibration failed: ${e.message}")
        }
    }

    private fun showNotification(context: Context, title: String, text: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

            val builder = NotificationCompat.Builder(context, PomodoroApplication.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            val notificationManager = NotificationManagerCompat.from(context)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(1, builder.build())
            }
        } catch (e: Exception) {
            Log.e("PomodoroViewModel", "Failed to show notification: ${e.message}")
        }
    }
}

class PomodoroViewModelFactory(private val repository: PomodoroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PomodoroViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
