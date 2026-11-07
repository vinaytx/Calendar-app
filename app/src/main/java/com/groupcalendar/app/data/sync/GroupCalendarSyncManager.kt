package com.groupcalendar.app.data.sync

import android.content.Context
import com.google.gson.Gson
import com.groupcalendar.app.data.api.TelegramCalendarEngine
import com.groupcalendar.app.data.model.MemberAvailability
import com.groupcalendar.app.data.model.TgDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class GroupCalendarSyncManager(
    private val context: Context,
    private val api: TelegramCalendarEngine,
    private val botToken: String,
    private val chatId: String
) {
    private val gson = Gson()
    private val localProfilesDir = File(context.filesDir, "member_profiles").apply { mkdirs() }

    /**
     * Serializes current user's availability to JSON and uploads it as user_{id}.json to the Telegram group
     */
    suspend fun broadcastMyAvailability(myProfile: MemberAvailability): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (botToken.isBlank() || chatId.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Bot token or Chat ID is missing"))
                }

                val jsonString = gson.toJson(myProfile)
                val file = File(context.cacheDir, "${myProfile.userId}.json")
                file.writeText(jsonString)

                // Cache own profile locally as well
                File(localProfilesDir, "${myProfile.userId}.json").writeText(jsonString)

                val requestFile = file.asRequestBody("application/json".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("document", file.name, requestFile)
                val chatIdBody = chatId.toRequestBody("text/plain".toMediaTypeOrNull())
                val captionBody = "📅 Availability update from ${myProfile.userName}"
                    .toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.uploadAvailability(
                    botToken = botToken,
                    chatId = chatIdBody,
                    document = body,
                    caption = captionBody
                )

                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Upload failed"
                    Result.failure(Exception("Telegram API error (${response.code()}): $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Polls the Telegram chat log, extracts all user_*.json files, downloads them,
     * and maps each group member to their latest availability profile.
     */
    suspend fun fetchGroupAvailability(): Result<Map<String, MemberAvailability>> =
        withContext(Dispatchers.IO) {
            val activeRoster = mutableMapOf<String, MemberAvailability>()
            try {
                if (botToken.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Bot token not configured"))
                }

                val updatesResponse = api.getChatUpdates(botToken)
                if (!updatesResponse.isSuccessful || updatesResponse.body() == null) {
                    val err = updatesResponse.errorBody()?.string() ?: "Failed to get updates"
                    return@withContext Result.failure(Exception("Telegram updates error: $err"))
                }

                val updates = updatesResponse.body()!!.result

                // Find all valid JSON documents from message history
                val documentMessages = updates
                    .mapNotNull { it.actualMessage?.document }
                    .filter { doc ->
                        val name = doc.fileName ?: ""
                        name.startsWith("user_") && name.endsWith(".json")
                    }

                // Parse documents. Chronological updates naturally overwrite older profiles in the map
                for (doc in documentMessages) {
                    val userId = doc.fileName?.removeSuffix(".json") ?: continue
                    val profile = downloadMemberProfile(doc)
                    if (profile != null) {
                        val existing = activeRoster[userId]
                        if (existing == null || profile.lastUpdated >= existing.lastUpdated) {
                            activeRoster[userId] = profile
                        }
                    }
                }

                Result.success(activeRoster)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Downloads and parses an availability profile JSON file by file_id
     */
    private suspend fun downloadMemberProfile(doc: TgDocument): MemberAvailability? =
        withContext(Dispatchers.IO) {
            try {
                val pathResponse = api.getFilePath(botToken, doc.fileId)
                val filePath = pathResponse.body()?.result?.filePath ?: return@withContext null

                val fileContentResponse = api.downloadFileDirectly(botToken, filePath)
                val jsonString = fileContentResponse.body()?.string() ?: return@withContext null

                // Save locally for offline cache
                doc.fileName?.let { name ->
                    File(localProfilesDir, name).writeText(jsonString)
                }

                gson.fromJson(jsonString, MemberAvailability::class.java)
            } catch (e: Exception) {
                // If network fails, attempt to read from local disk cache
                val cachedFile = doc.fileName?.let { File(localProfilesDir, it) }
                if (cachedFile != null && cachedFile.exists()) {
                    try {
                        gson.fromJson(cachedFile.readText(), MemberAvailability::class.java)
                    } catch (parseEx: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
        }

    /**
     * Validates bot token and tests access to target Telegram chat
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (botToken.isBlank()) {
                return@withContext Result.failure(Exception("Bot token cannot be empty"))
            }

            // Test Bot Identity
            val meResponse = api.getMe(botToken)
            if (!meResponse.isSuccessful || meResponse.body()?.ok != true) {
                val err = meResponse.errorBody()?.string() ?: "Invalid bot token"
                return@withContext Result.failure(Exception("Bot verification failed: $err"))
            }
            val botName = meResponse.body()?.result?.firstName ?: "Bot"

            // Test Chat Access if chatId is provided
            if (chatId.isNotBlank()) {
                val chatResponse = api.getChat(botToken, chatId)
                if (!chatResponse.isSuccessful || chatResponse.body()?.ok != true) {
                    val err = chatResponse.errorBody()?.string() ?: "Chat ID not accessible"
                    return@withContext Result.failure(Exception("Connected to @$botName, but cannot access Chat ID: $err"))
                }
                val chatTitle = chatResponse.body()?.result?.title ?: "Chat $chatId"
                return@withContext Result.success("Connected successfully to @$botName in group \"$chatTitle\"")
            }

            Result.success("Bot @$botName is valid! (Please specify a Chat ID to link the group)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
