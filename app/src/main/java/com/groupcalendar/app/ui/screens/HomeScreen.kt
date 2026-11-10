package com.groupcalendar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groupcalendar.app.ui.components.CalendarViewMode
import com.groupcalendar.app.ui.components.GroupCalendarTable
import com.groupcalendar.app.ui.viewmodel.CalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CalendarViewModel,
    onNavigateToEdit: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val roster by viewModel.rosterState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val myProfile by viewModel.myProfileState.collectAsState()

    var viewMode by remember { mutableStateOf(CalendarViewMode.DAY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Group Availability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Last synced: ${viewModel.preferences.getLastSyncTimestamp()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.syncNow() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync from Telegram")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToEdit,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Update My Schedule") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Snackbar / Banner if present
            statusMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearStatusMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Selectable, sortable roster table (Day or Week)
            GroupCalendarTable(
                roster = roster,
                myProfile = myProfile,
                selectedDate = selectedDate,
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                onPrev = { if (viewMode == CalendarViewMode.DAY) viewModel.prevDay() else viewModel.prevWeek() },
                onNext = { if (viewMode == CalendarViewMode.DAY) viewModel.nextDay() else viewModel.nextWeek() },
                onToday = { viewModel.today() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
