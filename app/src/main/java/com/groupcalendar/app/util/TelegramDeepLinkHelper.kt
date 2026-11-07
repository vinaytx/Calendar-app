package com.groupcalendar.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object TelegramDeepLinkHelper {

    /**
     * Attempts to open the group link in Telegram directly, or falls back to standard web browser.
     */
    fun openTelegramLink(context: Context, linkUrl: String) {
        if (linkUrl.isBlank()) return

        val cleanUrl = linkUrl.trim()
        val tgUri = if (cleanUrl.startsWith("https://t.me/")) {
            // Can be converted to tg:// or kept as https for Android app resolution
            Uri.parse(cleanUrl)
        } else if (cleanUrl.startsWith("tg://")) {
            Uri.parse(cleanUrl)
        } else {
            Uri.parse("https://t.me/$cleanUrl")
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: force web intent
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
            }
        }
    }

    /**
     * Opens BotFather to create or configure a bot
     */
    fun openBotFather(context: Context) {
        openTelegramLink(context, "https://t.me/BotFather")
    }
}
