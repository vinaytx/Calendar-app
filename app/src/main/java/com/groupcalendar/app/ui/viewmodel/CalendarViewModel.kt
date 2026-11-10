package com.groupcalendar.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.groupcalendar.app.data.model.MemberAvailability
import com.groupcalendar.app.data.model.TimeSlot
import com.groupcalendar.app.data.repository.CalendarRepository
import com.groupcalendar.app.util.ApkUpdateHelper
import com.groupcalendar.app.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application)
    val preferences = repository.preferences

    val rosterState: StateFlow<Map<String, MemberAvailability>> = repository.rosterState
    val myProfileState: StateFlow<MemberAvailability> = repository.myProfileState
    val isSyncing: StateFlow<Boolean> = repository.isSyncing

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    init {
        // Initial sync on startup if configured
        if (preferences.isConfigured()) {
            syncNow()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
    }

    fun nextDay() {
        val cal = Calendar.getInstance().apply {
            time = _selectedDate.value
            add(Calendar.DAY_OF_YEAR, 1)
        }
        _selectedDate.value = cal.time
    }

    fun prevDay() {
        val cal = Calendar.getInstance().apply {
            time = _selectedDate.value
            add(Calendar.DAY_OF_YEAR, -1)
        }
        _selectedDate.value = cal.time
    }

    fun today() {
        _selectedDate.value = Date()
    }

    fun nextWeek() {
        val cal = Calendar.getInstance().apply {
            time = _selectedDate.value
            add(Calendar.DAY_OF_YEAR, 7)
        }
        _selectedDate.value = cal.time
    }

    fun prevWeek() {
        val cal = Calendar.getInstance().apply {
            time = _selectedDate.value
            add(Calendar.DAY_OF_YEAR, -7)
        }
        _selectedDate.value = cal.time
    }

    fun addBusySlot(startIso: String, endIso: String, title: String?) {
        viewModelScope.launch {
            val currentSlots = myProfileState.value.busySlots.toMutableList()
            currentSlots.add(TimeSlot(start = startIso, end = endIso, title = title))
            // Sort by start time
            currentSlots.sortBy { it.start }

            val result = repository.saveBusySlots(currentSlots, broadcastNow = true)
            if (result.isSuccess) {
                _statusMessage.value = "Schedule updated & broadcasted to Telegram!"
            } else {
                _statusMessage.value = "Saved locally. (Broadcast failed: ${result.exceptionOrNull()?.message})"
            }
        }
    }

    fun removeBusySlot(slot: TimeSlot) {
        viewModelScope.launch {
            val currentSlots = myProfileState.value.busySlots.toMutableList()
            currentSlots.remove(slot)

            val result = repository.saveBusySlots(currentSlots, broadcastNow = true)
            if (result.isSuccess) {
                _statusMessage.value = "Slot removed and synced to Telegram!"
            } else {
                _statusMessage.value = "Slot removed locally."
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val result = repository.syncRoster()
            if (result.isSuccess) {
                val count = result.getOrNull()?.size ?: 0
                _statusMessage.value = "Synced successfully ($count active member profiles)"
            } else {
                _statusMessage.value = "Sync failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun broadcastNow() {
        viewModelScope.launch {
            val result = repository.broadcastMyAvailability()
            if (result.isSuccess) {
                _statusMessage.value = "Availability broadcasted to Telegram group!"
            } else {
                _statusMessage.value = "Broadcast error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun testConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.testTelegramConnection()
            if (result.isSuccess) {
                onResult(true, result.getOrNull() ?: "Connection valid!")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Connection test failed")
            }
        }
    }

    fun saveSettings(
        name: String,
        colorHex: String,
        botToken: String,
        chatId: String,
        inviteLink: String,
        autoSync: Boolean,
        syncInterval: Int
    ) {
        preferences.setUserName(name)
        preferences.setUserColorHex(colorHex)
        preferences.setBotToken(botToken)
        preferences.setChatId(chatId)
        preferences.setGroupInviteLink(inviteLink)
        preferences.setAutoSyncEnabled(autoSync)
        preferences.setSyncIntervalMinutes(syncInterval)

        repository.refreshMyProfile()

        viewModelScope.launch {
            if (preferences.isConfigured()) {
                val result = repository.broadcastMyAvailability()
                _statusMessage.value = if (result.isSuccess) {
                    "Settings saved and synced to the group!"
                } else {
                    "Settings saved locally. (Broadcast failed: ${result.exceptionOrNull()?.message})"
                }
            } else {
                _statusMessage.value = "Settings saved successfully!"
            }
        }
    }

    fun triggerApkUpdate(apkUrl: String) {
        viewModelScope.launch {
            _statusMessage.value = "Downloading APK update..."
            val result = ApkUpdateHelper.downloadAndInstallApk(
                context = getApplication(),
                apkDownloadUrl = apkUrl,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                }
            )
            _downloadProgress.value = null
            if (result.isFailure) {
                _statusMessage.value = "Update failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}
