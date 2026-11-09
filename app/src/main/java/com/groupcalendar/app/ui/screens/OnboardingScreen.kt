package com.groupcalendar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.ui.unit.sp
import com.groupcalendar.app.ui.viewmodel.CalendarViewModel
import com.groupcalendar.app.util.TelegramDeepLinkHelper

@Composable
fun OnboardingScreen(
    viewModel: CalendarViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }

    var userName by remember { mutableStateOf(viewModel.preferences.getUserName()) }
    var userColor by remember { mutableStateOf(viewModel.preferences.getUserColorHex()) }
    var botToken by remember { mutableStateOf(viewModel.preferences.getBotToken()) }
    var chatId by remember { mutableStateOf(viewModel.preferences.getChatId()) }
    var inviteLink by remember { mutableStateOf(viewModel.preferences.getGroupInviteLink()) }

    val colors = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EC4899", "#14B8A6")

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome to Group Calendar",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Decentralized schedule coordination via Telegram",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator
                LinearProgressIndicator(
                    progress = currentStep / 3f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Step Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    1 -> {
                        // Step 1: Profile Setup
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Step 1: Set Up Your Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your name will appear on the group timeline so members can see when you are free or busy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = userName,
                                onValueChange = { userName = it },
                                label = { Text("Your Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Select an avatar color:", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                colors.forEach { hex ->
                                    val isSelected = hex.equals(userColor, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .clickable { userColor = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Step 2: Telegram Group Setup
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Step 2: Connect Telegram Group",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "The app synchronizes schedules across all members through a Telegram group.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (inviteLink.isNotBlank()) {
                                Button(
                                    onClick = { TelegramDeepLinkHelper.openTelegramLink(context, inviteLink) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Join Group on Telegram")
                                }
                            }

                            OutlinedTextField(
                                value = botToken,
                                onValueChange = { botToken = com.groupcalendar.app.data.local.PreferencesManager.sanitizeBotToken(it) },
                                label = { Text("Bot Token") },
                                supportingText = { Text("Enter token only, not URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = chatId,
                                onValueChange = { chatId = com.groupcalendar.app.data.local.PreferencesManager.sanitizeChatId(it) },
                                label = { Text("Group Chat ID") },
                                supportingText = { Text("e.g. -1004478792508") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    3 -> {
                        // Step 3: Confirmation
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "You're all set, $userName!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Whenever you update your busy hours, your availability file will automatically sync to the Telegram group. You can now view the entire team's roster in real time.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            viewModel.saveSettings(
                                name = userName,
                                colorHex = userColor,
                                botToken = botToken,
                                chatId = chatId,
                                inviteLink = inviteLink,
                                autoSync = true,
                                syncInterval = 15
                            )
                            onComplete()
                        }
                    }
                ) {
                    Text(if (currentStep == 3) "Start Using App" else "Next")
                }
            }
        }
    }
}
