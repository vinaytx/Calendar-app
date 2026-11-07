package com.groupcalendar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.groupcalendar.app.data.model.TimeSlot
import com.groupcalendar.app.ui.viewmodel.CalendarViewModel
import com.groupcalendar.app.util.DateTimeUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    val myProfile by viewModel.myProfileState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val dateOnlyString = remember(selectedDate) {
        DateTimeUtils.formatDateOnly(selectedDate)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Busy Schedule") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.broadcastNow() },
                        enabled = !isSyncing
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Broadcast to Telegram")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Busy Slot") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Profile Card Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Schedule for ${myProfile.userName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unique ID: ${myProfile.userId} • Total busy slots: ${myProfile.busySlots.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Configured Busy Blocks:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (myProfile.busySlots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You currently have no busy slots configured.\nYou are shown as free all day!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(myProfile.busySlots) { slot ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slot.title ?: "Busy Window",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${DateTimeUtils.formatDisplayTime(slot.start)} → ${DateTimeUtils.formatDisplayTime(slot.end)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = slot.start.take(10),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.removeBusySlot(slot) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Slot", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom CTA to Broadcast
            Button(
                onClick = { viewModel.broadcastNow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcasting to Telegram...")
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast Availability to Group")
                }
            }
        }
    }

    // Add Slot Modal Dialog
    if (showAddDialog) {
        AddBusySlotDialog(
            defaultDateString = dateOnlyString,
            onDismiss = { showAddDialog = false },
            onConfirm = { startIso, endIso, title ->
                viewModel.addBusySlot(startIso, endIso, title)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddBusySlotDialog(
    defaultDateString: String,
    onDismiss: () -> Unit,
    onConfirm: (startIso: String, endIso: String, title: String?) -> Unit
) {
    var dateString by remember { mutableStateOf(defaultDateString) }
    var startHour by remember { mutableStateOf("09") }
    var startMinute by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("11") }
    var endMinute by remember { mutableStateOf("00") }
    var title by remember { mutableStateOf("Work / Meeting") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Busy Slot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title / Reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { startHour = it.take(2) },
                        label = { Text("Start Hr") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = startMinute,
                        onValueChange = { startMinute = it.take(2) },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = endHour,
                        onValueChange = { endHour = it.take(2) },
                        label = { Text("End Hr") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endMinute,
                        onValueChange = { endMinute = it.take(2) },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val sHour = startHour.padStart(2, '0')
                val sMin = startMinute.padStart(2, '0')
                val eHour = endHour.padStart(2, '0')
                val eMin = endMinute.padStart(2, '0')

                val startIso = "${dateString}T$sHour:$sMin:00"
                val endIso = "${dateString}T$eHour:$eMin:00"
                onConfirm(startIso, endIso, title.ifBlank { null })
            }) {
                Text("Add Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
