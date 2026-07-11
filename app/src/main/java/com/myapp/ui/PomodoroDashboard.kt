package com.myapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myapp.PomodoroApplication
import com.myapp.ui.screens.StatsScreen
import com.myapp.ui.screens.TasksScreen
import com.myapp.ui.screens.TimerScreen
import com.myapp.ui.theme.SophisticatedDarkNav
import com.myapp.ui.theme.SophisticatedPurple
import com.myapp.ui.theme.SophisticatedOnPurple

enum class DashboardTab(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TIMER("Timer", Icons.Rounded.HourglassEmpty),
    TASKS("Tasks", Icons.Rounded.ListAlt),
    STATS("Stats", Icons.Rounded.BarChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as PomodoroApplication
    
    // Inject ViewModel using manual DI Factory
    val viewModel: PomodoroViewModel = viewModel(
        factory = PomodoroViewModelFactory(app.repository)
    )

    var currentTab by remember { mutableStateOf(DashboardTab.TIMER) }

    val modeColor = when (viewModel.timerMode.collectAsState().value) {
        TimerMode.FOCUS -> MaterialTheme.colorScheme.primary
        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            text = "Focus Tomato",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = modeColor
                            )
                        )
                    }
                },
                actions = {
                    // Profile initials badge
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SophisticatedPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedOnPurple,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SophisticatedDarkNav,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                DashboardTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.displayName,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SophisticatedPurple,
                            selectedTextColor = SophisticatedPurple,
                            indicatorColor = Color(0xFF4A4458),
                            unselectedIconColor = Color(0xFFC9C5D0),
                            unselectedTextColor = Color(0xFFC9C5D0)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                DashboardTab.TIMER -> {
                    TimerScreen(
                        viewModel = viewModel,
                        onNavigateToTasks = { currentTab = DashboardTab.TASKS }
                    )
                }
                DashboardTab.TASKS -> {
                    TasksScreen(
                        viewModel = viewModel,
                        onNavigateToTimer = { currentTab = DashboardTab.TIMER }
                    )
                }
                DashboardTab.STATS -> {
                    StatsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
