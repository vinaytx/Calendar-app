package com.groupcalendar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groupcalendar.app.ui.viewmodel.CalendarViewModel
import com.groupcalendar.app.util.TelegramDeepLinkHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var userName by remember { mutableStateOf(viewModel.preferences.getUserName()) }
    var userColor by remember { mutableStateOf(viewModel.preferences.getUserColorHex()) }
    var botToken by remember { mutableStateOf(viewModel.preferences.getBotToken()) }
    var chatId by remember { mutableStateOf(viewModel.preferences.getChatId()) }
    var inviteLink by remember { mutableStateOf(viewModel.preferences.getGroupInviteLink()) }
    var autoSync by remember { mutableStateOf(viewModel.preferences.isAutoSyncEnabled) }
    var syncInterval by remember { mutableStateOf(viewModel.preferences.getSyncIntervalMinutes().toFloat()) }

    var testStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    var apkUrlInput by remember { mutableStateOf("https://github.com/my-org/calendar-app/releases/latest/download/app-release.apk") }
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    val colorOptions = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EC4899", "#14B8A6")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Connection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveSettings(
                            name = userName,
                            colorHex = userColor,
                            botToken = botToken,
                            chatId = chatId,
                            inviteLink = inviteLink,
                            autoSync = autoSync,
                            syncInterval = syncInterval.toInt()
                        )
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: User Profile
            Text(
                text = "User Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Your User ID: ${viewModel.preferences.getUserId()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(text = "Roster Accent Color:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colorOptions.forEach { hex ->
                    val isSelected = hex.equals(userColor, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .clickable { userColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Divider()

            // Section 2: Telegram Bot API Configuration
            Text(
                text = "Telegram P2P Connection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = botToken,
                onValueChange = { botToken = com.groupcalendar.app.data.local.PreferencesManager.sanitizeBotToken(it) },
                label = { Text("Telegram Bot Token") },
                placeholder = { Text("123456789:ABCdef...") },
                supportingText = { Text("Enter token only (e.g. 123456:ABC...), not the URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { TelegramDeepLinkHelper.openBotFather(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Get Token (@BotFather)", fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = chatId,
                onValueChange = { chatId = com.groupcalendar.app.data.local.PreferencesManager.sanitizeChatId(it) },
                label = { Text("Group Chat ID") },
                placeholder = { Text("-1001234567890") },
                supportingText = { Text("Must start with -100 (e.g. -1004478792508)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = inviteLink,
                onValueChange = { inviteLink = it },
                label = { Text("Group Invite Link (Optional)") },
                placeholder = { Text("https://t.me/+AbCdEf...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (inviteLink.isNotBlank()) {
                Button(
                    onClick = { TelegramDeepLinkHelper.openTelegramLink(context, inviteLink) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Telegram Group")
                }
            }

            // Connection Test Button
            OutlinedButton(
                onClick = {
                    isTestingConnection = true
                    viewModel.testConnection { success, message ->
                        isTestingConnection = false
                        testStatus = success to message
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTestingConnection && botToken.isNotBlank()
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing Connection...")
                } else {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Telegram Connection")
                }
            }

            testStatus?.let { (success, msg) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (success) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (success) Color(0xFF065F46) else Color(0xFF991B1B)
                    )
                }
            }

            Divider()

            // Section 3: Background Sync
            Text(
                text = "Background Synchronization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Automatic Sync (WorkManager)", fontWeight = FontWeight.Medium)
                    Text(
                        text = "Polls Telegram for schedule updates in background",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoSync, onCheckedChange = { autoSync = it })
            }

            if (autoSync) {
                Text(
                    text = "Sync Frequency: ${syncInterval.toInt()} minutes",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = syncInterval,
                    onValueChange = { syncInterval = it },
                    valueRange = 15f..120f,
                    steps = 6
                )
            }

            Divider()

            // Section 4: Sideload & In-App APK Updates
            Text(
                text = "App Distribution & Updates (APK)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Download and self-update new versions without the Google Play Store.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = apkUrlInput,
                onValueChange = { apkUrlInput = it },
                label = { Text("Direct APK Download URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            downloadProgress?.let { progress ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloading APK: $progress%",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Button(
                onClick = { viewModel.triggerApkUpdate(apkUrlInput) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download & Install Update")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
