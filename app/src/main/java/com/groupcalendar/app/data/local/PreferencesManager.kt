package com.groupcalendar.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.groupcalendar.app.BuildConfig
import com.groupcalendar.app.data.model.MemberAvailability
import com.groupcalendar.app.data.model.TimeSlot
import java.util.UUID

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        // Initialize default user ID if not already generated
        if (getUserId().isBlank()) {
            val randomId = "user_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString(KEY_USER_ID, randomId).apply()
        }
    }

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "My Schedule") ?: "My Schedule"
    fun setUserName(name: String) = prefs.edit().putString(KEY_USER_NAME, name).apply()

    fun getUserColorHex(): String = prefs.getString(KEY_USER_COLOR, "#3B82F6") ?: "#3B82F6"
    fun setUserColorHex(hex: String) = prefs.edit().putString(KEY_USER_COLOR, hex).apply()

    // Bot Token (Falls back to BuildConfig if not configured by user)
    fun getBotToken(): String {
        val stored = prefs.getString(KEY_BOT_TOKEN, null)
        val raw = if (!stored.isNullOrBlank()) stored else BuildConfig.DEFAULT_BOT_TOKEN
        return sanitizeBotToken(raw)
    }
    fun setBotToken(token: String) = prefs.edit().putString(KEY_BOT_TOKEN, sanitizeBotToken(token)).apply()

    // Group Chat ID (Falls back to BuildConfig)
    fun getChatId(): String {
        val stored = prefs.getString(KEY_CHAT_ID, null)
        val raw = if (!stored.isNullOrBlank()) stored else BuildConfig.DEFAULT_CHAT_ID
        return sanitizeChatId(raw)
    }
    fun setChatId(id: String) = prefs.edit().putString(KEY_CHAT_ID, sanitizeChatId(id)).apply()

    // Group Invite Link
    fun getGroupInviteLink(): String {
        val stored = prefs.getString(KEY_GROUP_INVITE_LINK, null)
        return if (!stored.isNullOrBlank()) stored else BuildConfig.DEFAULT_INVITE_LINK
    }
    fun setGroupInviteLink(link: String) = prefs.edit().putString(KEY_GROUP_INVITE_LINK, link.trim()).apply()

    // Busy Slots for local user
    fun getMyBusySlots(): List<TimeSlot> {
        val json = prefs.getString(KEY_MY_BUSY_SLOTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TimeSlot>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    fun setMyBusySlots(slots: List<TimeSlot>) {
        val json = gson.toJson(slots)
        prefs.edit().putString(KEY_MY_BUSY_SLOTS, json).apply()
    }

    // Cached Roster
    fun getCachedRoster(): Map<String, MemberAvailability> {
        val json = prefs.getString(KEY_CACHED_ROSTER, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, MemberAvailability>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    fun setCachedRoster(roster: Map<String, MemberAvailability>) {
        val json = gson.toJson(roster)
        prefs.edit().putString(KEY_CACHED_ROSTER, json).apply()
    }

    // Last Sync Timestamp
    fun getLastSyncTimestamp(): String = prefs.getString(KEY_LAST_SYNC, "Never") ?: "Never"
    fun setLastSyncTimestamp(timestamp: String) = prefs.edit().putString(KEY_LAST_SYNC, timestamp).apply()

    // Auto Sync Settings
    val isAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
    fun setAutoSyncEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()

    fun getSyncIntervalMinutes(): Int = prefs.getInt(KEY_SYNC_INTERVAL, 15)
    fun setSyncIntervalMinutes(minutes: Int) = prefs.edit().putInt(KEY_SYNC_INTERVAL, minutes).apply()

    fun isConfigured(): Boolean {
        return getBotToken().isNotBlank() && getChatId().isNotBlank()
    }

    companion object {
        private const val PREFS_NAME = "group_calendar_preferences"
        private const val KEY_USER_ID = "pref_user_id"
        private const val KEY_USER_NAME = "pref_user_name"
        private const val KEY_USER_COLOR = "pref_user_color"
        private const val KEY_BOT_TOKEN = "pref_bot_token"
        private const val KEY_CHAT_ID = "pref_chat_id"
        private const val KEY_GROUP_INVITE_LINK = "pref_invite_link"
        private const val KEY_MY_BUSY_SLOTS = "pref_my_busy_slots"
        private const val KEY_CACHED_ROSTER = "pref_cached_roster"
        private const val KEY_LAST_SYNC = "pref_last_sync"
        private const val KEY_AUTO_SYNC = "pref_auto_sync"
        private const val KEY_SYNC_INTERVAL = "pref_sync_interval"

        fun sanitizeBotToken(input: String): String {
            var token = input.trim()
            if (token.contains("/bot")) {
                token = token.substringAfter("/bot")
            } else if (token.startsWith("bot", ignoreCase = true)) {
                token = token.substring(3)
            }
            if (token.contains("/")) {
                token = token.substringBefore("/")
            }
            return token.trim('/', ' ', '\t', '\n', '\r')
        }

        fun sanitizeChatId(input: String): String {
            var id = input.trim().replace("\"", "").replace("'", "")
            if (id.contains("chat_id=")) {
                id = id.substringAfter("chat_id=")
            }
            if (id.contains("&")) {
                id = id.substringBefore("&")
            }
            return id.trim()
        }
    }
}
