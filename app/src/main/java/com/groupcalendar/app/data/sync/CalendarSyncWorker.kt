package com.groupcalendar.app.data.sync

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.groupcalendar.app.GroupCalendarApp
import com.groupcalendar.app.R
import com.groupcalendar.app.data.api.RetrofitClient
import com.groupcalendar.app.data.local.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(context)
        val botToken = prefs.getBotToken()
        val chatId = prefs.getChatId()

        if (botToken.isBlank() || chatId.isBlank()) {
            return Result.failure()
        }

        val syncManager = GroupCalendarSyncManager(
            context = context,
            api = RetrofitClient.engine,
            botToken = botToken,
            chatId = chatId
        )

        return try {
            val result = syncManager.fetchGroupAvailability()
            if (result.isSuccess) {
                val newRoster = result.getOrNull() ?: emptyMap()
                val previousRoster = prefs.getCachedRoster()

                // Save new roster
                prefs.setCachedRoster(newRoster)
                val timeString = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
                prefs.setLastSyncTimestamp(timeString)

                // Detect if any group member updated their schedule
                if (newRoster.size > previousRoster.size || hasScheduleChanges(previousRoster, newRoster)) {
                    showSyncNotification("Calendar Synchronized", "${newRoster.size} member schedule(s) updated.")
                }

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun hasScheduleChanges(
        oldMap: Map<String, com.groupcalendar.app.data.model.MemberAvailability>,
        newMap: Map<String, com.groupcalendar.app.data.model.MemberAvailability>
    ): Boolean {
        for ((id, profile) in newMap) {
            val oldProfile = oldMap[id] ?: return true
            if (oldProfile.lastUpdated != profile.lastUpdated) {
                return true
            }
        }
        return false
    }

    private fun showSyncNotification(title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, GroupCalendarApp.SYNC_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }

    companion object {
        const val WORK_NAME = "periodic_calendar_sync_worker"
    }
}
