package com.groupcalendar.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.*
import com.groupcalendar.app.data.local.PreferencesManager
import com.groupcalendar.app.data.sync.CalendarSyncWorker
import java.util.concurrent.TimeUnit

class GroupCalendarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupBackgroundSync()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SYNC_CHANNEL_ID,
                getString(R.string.sync_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.sync_channel_description)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupBackgroundSync() {
        val prefs = PreferencesManager(this)
        if (prefs.isAutoSyncEnabled && prefs.getBotToken().isNotBlank() && prefs.getChatId().isNotBlank()) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
                prefs.getSyncIntervalMinutes().toLong().coerceAtLeast(15L),
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                CalendarSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        }
    }

    companion object {
        const val SYNC_CHANNEL_ID = "group_calendar_sync_channel"
    }
}
