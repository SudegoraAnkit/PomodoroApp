package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PomodoroViewModel
import com.example.ui.TimerMode
import com.example.data.PomodoroSession
import com.example.data.PomodoroTask
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: PomodoroViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    var showClearConfirm by remember { mutableStateOf(false) }

    // Focus calculations
    val focusSessions = sessions.filter { it.sessionType == "FOCUS" }
    val totalFocusMinutes = focusSessions.sumOf { it.durationMinutes }
    val totalSessionsCount = sessions.size
    val totalCompletedTasks = tasks.count { it.isCompleted }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            if (sessions.isNotEmpty()) {
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Clear Session History",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Summary KPI Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                value = "$totalFocusMinutes",
                label = "Focus Mins",
                icon = Icons.Rounded.Timer,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                value = "$totalSessionsCount",
                label = "Total Cycles",
                icon = Icons.Rounded.TrendingUp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                value = "$totalCompletedTasks",
                label = "Tasks Done",
                icon = Icons.Rounded.TaskAlt,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        // 7-Day Custom Canvas Bar Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Weekly Focus Progress (Minutes)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                WeeklyFocusBarChart(sessions = focusSessions)
            }
        }

        // Category Breakdown Card with Custom Donut Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Sessions by Category",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                CategoryBreakdownSection(sessions = focusSessions, tasks = tasks)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Statistics?") },
            text = { Text("Are you sure you want to clear your entire Focus history? This action is offline-permanent and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun KpiCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WeeklyFocusBarChart(sessions: List<PomodoroSession>) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    // Compute past 7 days mapping (Today back to 6 days ago)
    val chartData = remember(sessions) {
        val calendar = Calendar.getInstance()
        val days = mutableListOf<Pair<String, Int>>() // Day Name -> Minutes

        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 7 = Sat
            val dayLabel = dayNames[dayOfWeek - 1]

            // Start of day
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startMs = cal.timeInMillis

            // End of day
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endMs = cal.timeInMillis

            val minsInDay = sessions
                .filter { it.timestamp in startMs..endMs }
                .sumOf { it.durationMinutes }

            days.add(Pair(dayLabel, minsInDay))
        }
        days
    }

    val maxVal = remember(chartData) {
        val rawMax = chartData.maxOfOrNull { it.second } ?: 0
        if (rawMax < 60) 60 else (((rawMax + 29) / 30) * 30) // nice grid interval snaps
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        val labelAreaHeight = 24.dp.toPx()
        val chartHeight = height - labelAreaHeight
        val barWidth = 16.dp.toPx()
        val spacing = (width - (barWidth * chartData.size)) / (chartData.size + 1)

        // Draw horizontal grid lines (0%, 50%, 100%)
        val gridLines = listOf(0f, 0.5f, 1f)
        gridLines.forEach { frac ->
            val y = chartHeight * (1f - frac)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw Bars & Labels
        chartData.forEachIndexed { index, pair ->
            val x = spacing + index * (barWidth + spacing)
            val minutes = pair.second
            val fraction = if (maxVal > 0) minutes.toFloat() / maxVal.toFloat() else 0f
            val barHeight = chartHeight * fraction

            // Draw Bar
            if (barHeight > 0) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Draw label below bar
            // We draw labels and text simple using native canvas to avoid library errors
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (textColor.alpha * 255).toInt(),
                    (textColor.red * 255).toInt(),
                    (textColor.green * 255).toInt(),
                    (textColor.blue * 255).toInt()
                )
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            drawContext.canvas.nativeCanvas.drawText(
                pair.first,
                x + barWidth / 2,
                chartHeight + 16.dp.toPx(),
                paint
            )

            // Draw minute value atop bar if > 0
            if (minutes > 0) {
                val valuePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "${minutes}m",
                    x + barWidth / 2,
                    (chartHeight - barHeight - 6.dp.toPx()).coerceAtLeast(10.dp.toPx()),
                    valuePaint
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownSection(sessions: List<PomodoroSession>, tasks: List<PomodoroTask>) {
    val categoryCounts = remember(sessions, tasks) {
        // Build task mapping
        val taskMap = tasks.associate { it.id to it.category }
        val counts = mutableMapOf<String, Int>()

        sessions.forEach { sess ->
            val cat = if (sess.taskId != null) {
                taskMap[sess.taskId] ?: "Work"
            } else {
                "Work" // Default fallback category for manual / direct timer logs
            }
            counts[cat] = (counts[cat] ?: 0) + 1
        }
        counts.toList().sortedByDescending { it.second }
    }

    if (categoryCounts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log some focus sessions to see category insights!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Colors for category breakdown
            val colorPalette = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                Color(0xFFFB923C), // Orange
                Color(0xFFA78BFA)  // Purple
            )

            // Custom Donut Canvas
            val totalCount = categoryCounts.sumOf { it.second }

            Box(
                modifier = Modifier
                    .size(130.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var currentAngle = -90f
                    val strokeWidth = 14.dp.toPx()

                    categoryCounts.forEachIndexed { index, pair ->
                        val sweep = (pair.second.toFloat() / totalCount.toFloat()) * 360f
                        val color = colorPalette[index % colorPalette.size]

                        drawArc(
                            color = color,
                            startAngle = currentAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth)
                        )
                        currentAngle += sweep
                    }
                }

                // Center Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalCount",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Cycles",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 9.sp
                        )
                    )
                }
            }

            // Legend Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categoryCounts.take(5).forEachIndexed { index, pair ->
                    val color = colorPalette[index % colorPalette.size]
                    val percentage = ((pair.second.toFloat() / totalCount.toFloat()) * 100).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pair.first,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "${pair.second} ($percentage%)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}
