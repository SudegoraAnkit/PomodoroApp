# Copilot Instructions for PomodoroApp

## Build, Test, and Lint Commands

**Build the app:**
```bash
./gradlew build
```

**Run unit tests:**
```bash
./gradlew testDebugUnitTest
```

**Run a specific test:**
```bash
./gradlew testDebugUnitTest --tests "com.myapp.ExampleUnitTest"
```

**Run instrumented tests on emulator:**
```bash
./gradlew connectedAndroidTest
```

**Build the debug APK:**
```bash
./gradlew assembleDebug
```

**Build the release APK** (requires signing configuration):
```bash
./gradlew assembleRelease
```

**Clean build:**
```bash
./gradlew clean build
```

## Project Overview

PomodoroApp is an Android app built with **Jetpack Compose** and **Kotlin** that implements the Pomodoro time management technique. The app tracks focused work sessions (25 min default), short breaks (5 min), and long breaks (15 min), with local data persistence and notifications.

### Architecture

- **UI Layer**: Jetpack Compose with three main screens (Timer, Tasks, Stats) in a tabbed dashboard
- **ViewModel**: `PomodoroViewModel` manages all UI state using `StateFlow`
- **Data Layer**: 
  - `PomodoroRepository` provides data access abstraction
  - `PomodoroDao` handles Room Database operations
  - Two entities: `PomodoroTask` (work items) and `PomodoroSession` (timer history)
- **Database**: Room (local SQLite) with automatic schema migrations

### Key Flows

1. **Timer Flow**: `TimerMode` (FOCUS/SHORT_BREAK/LONG_BREAK) → countdown → `onTimerFinished()` → saves session, updates task, triggers notification, auto-transitions to next mode
2. **Task Flow**: Tasks are created with planned pomodoro count → when timer finishes on FOCUS mode, `completedPomodoros` increments → task can be marked complete manually
3. **State Management**: ViewModel exposes `StateFlow<T>` for reactive UI updates via Compose's `collectAsStateWithLifecycle()`

## Code Conventions

### Package Structure
- `com.myapp` - Entry points (MainActivity, PomodoroApplication)
- `com.myapp.ui` - Compose composables and ViewModels
- `com.myapp.ui.screens` - Individual screen composables (TimerScreen, TasksScreen, StatsScreen)
- `com.myapp.ui.theme` - Material3 theme colors and typography
- `com.myapp.data` - Database entities, DAOs, Repository

### ViewModel State Management
- Private mutable state: `MutableStateFlow<T>` (internal, prefixed with `_`)
- Public exposed state: `StateFlow<T>` via `asStateFlow()` (read-only)
- All database operations use `viewModelScope.launch` to handle suspend functions safely
- UI collects state using `collectAsStateWithLifecycle()` to avoid memory leaks

### Compose Patterns
- Screens are top-level `@Composable` functions with a ViewModel parameter
- State flows are collected into local `by` properties for reactivity
- Use `LocalContext.current` to access Android context (notifications, vibration)
- Button click handlers use `viewModel.methodName(context)` pattern for operations requiring Android context

### Database Entities
- Auto-incrementing primary keys: `@PrimaryKey(autoGenerate = true)`
- Timestamps stored as Long (milliseconds): `System.currentTimeMillis()`
- Default values in constructor: `category: String = "Work"`, `isCompleted: Boolean = false`
- Use `.copy()` for immutable updates before passing to repository

### Notifications & Haptics
- Notifications use `NotificationCompat.Builder` with `PomodoroApplication.CHANNEL_ID`
- Vibration handled differently for API 31+ (VibratorManager) vs older versions
- Permission checks required for POST_NOTIFICATIONS on Android 13+
- Tone alerts use `ToneGenerator.TONE_CDMA_PIP` for timer completion

### Gradle Configuration
- Gradle version: 8.x (uses `.kts` syntax)
- Uses Gradle Version Catalog (`libs.plugins.*` and `libs.*` references in `build.gradle.kts`)
- Secrets plugin loads environment variables from `.env` file
- KSP (Kotlin Symbol Processing) enabled for Room code generation and Moshi serialization

### Async Patterns
- Timer countdown: `while (secondsLeft > 0) { delay(1000) }` in a coroutine Job
- Job cancellation: `timerJob?.cancel()` to stop timer
- Repository wraps DAO suspend functions for cleaner API

### Testing
- Unit tests in `app/src/test/` use Robolectric for Android framework simulation
- Instrumented tests in `app/src/androidTest/` run on emulator/device
- Roborazzi used for visual regression testing with Compose

## Key Configuration Files

- `.env` - Stores `GEMINI_API_KEY` (required, not checked in)
- `.env.example` - Template for required secrets
- `app/build.gradle.kts` - App-level Gradle config with dependencies and signing configs
- `build.gradle.kts` - Root Gradle config with plugin aliases

## Important Notes

- **Signing Config**: Debug builds use `debug.keystore` with hardcoded credentials. Release config requires environment variables: `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`
- **Target SDK**: 36 (Android 15)
- **Min SDK**: 24 (Android 7.0)
- **Theme**: Material3 with custom `SophisticatedDarkNav` and `SophisticatedPurple` colors defined in `ui/theme/`
- **Database Migration**: Uses `.fallbackToDestructiveMigration()` during dev — clear app data on schema changes
