package com.groupcalendar.app.data.repository

import android.content.Context
import com.groupcalendar.app.data.api.RetrofitClient
import com.groupcalendar.app.data.local.PreferencesManager
import com.groupcalendar.app.data.model.MemberAvailability
import com.groupcalendar.app.data.model.TimeSlot
import com.groupcalendar.app.data.sync.GroupCalendarSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class CalendarRepository(private val context: Context) {

    val preferences = PreferencesManager(context)

    private val _rosterState = MutableStateFlow<Map<String, MemberAvailability>>(preferences.getCachedRoster())
    val rosterState: StateFlow<Map<String, MemberAvailability>> = _rosterState.asStateFlow()

    private val _myProfileState = MutableStateFlow(buildMyProfile())
    val myProfileState: StateFlow<MemberAvailability> = _myProfileState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private fun getSyncManager(): GroupCalendarSyncManager {
        return GroupCalendarSyncManager(
            context = context,
            api = RetrofitClient.engine,
            rawBotToken = preferences.getBotToken(),
            rawChatId = preferences.getChatId()
        )
    }

    private fun buildMyProfile(): MemberAvailability {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        return MemberAvailability(
            userId = preferences.getUserId(),
            userName = preferences.getUserName(),
            lastUpdated = nowIso,
            busySlots = preferences.getMyBusySlots(),
            colorHex = preferences.getUserColorHex()
        )
    }

    fun refreshMyProfile() {
        val updated = buildMyProfile()
        _myProfileState.value = updated
    }

    suspend fun saveBusySlots(slots: List<TimeSlot>, broadcastNow: Boolean = true): Result<Boolean> {
        preferences.setMyBusySlots(slots)
        refreshMyProfile()

        if (broadcastNow && preferences.isConfigured()) {
            return broadcastMyAvailability()
        }
        return Result.success(true)
    }

    suspend fun broadcastMyAvailability(): Result<Boolean> {
        _isSyncing.value = true
        return try {
            val profile = buildMyProfile()
            val result = getSyncManager().broadcastMyAvailability(profile)
            if (result.isSuccess) {
                // Update our own profile in the active roster map
                val current = _rosterState.value.toMutableMap()
                current[profile.userId] = profile
                _rosterState.value = current
                preferences.setCachedRoster(current)
            }
            result
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncRoster(): Result<Map<String, MemberAvailability>> {
        _isSyncing.value = true
        return try {
            val result = getSyncManager().fetchGroupAvailability()
            if (result.isSuccess) {
                val fetched = result.getOrNull() ?: emptyMap()
                val merged = fetched.toMutableMap()
                // Ensure my own local profile is always included
                merged[preferences.getUserId()] = buildMyProfile()

                _rosterState.value = merged
                preferences.setCachedRoster(merged)

                val nowString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                preferences.setLastSyncTimestamp(nowString)
            }
            result
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun testTelegramConnection(): Result<String> {
        return getSyncManager().testConnection()
    }
}
