package com.groupcalendar.app.data.sync

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.groupcalendar.app.data.api.TelegramCalendarEngine
import com.groupcalendar.app.data.model.ManifestEntry
import com.groupcalendar.app.data.model.MemberAvailability
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
    rawBotToken: String,
    rawChatId: String
) {
    // Colon must be percent-encoded: Retrofit leaves ':' unescaped in path segments, and
    // a literal colon right after "bot<digits>" gets misread by OkHttp as a URI scheme,
    // causing a "Malformed URL" resolve failure.
    private val botToken: String = com.groupcalendar.app.data.local.PreferencesManager
        .sanitizeBotToken(rawBotToken)
        .replace(":", "%3A")
    private val chatId: String = com.groupcalendar.app.data.local.PreferencesManager.sanitizeChatId(rawChatId)
    private val gson = Gson()
    private val localProfilesDir = File(context.filesDir, "member_profiles").apply { mkdirs() }

    /**
     * Serializes current user's availability to JSON, uploads it as user_{id}.json to the
     * Telegram group, then records its file_id in the pinned manifest message.
     *
     * Documents are always sent *by the bot*, and Telegram's getUpdates never echoes back
     * messages the bot itself sent - so discovery can't rely on polling updates. Instead we
     * maintain one pinned message holding a userId -> file_id manifest, which getChat() can
     * always read back regardless of who "sent" it.
     */
    suspend fun broadcastMyAvailability(myProfile: MemberAvailability): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (botToken.isBlank() || chatId.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Bot token or Chat ID is missing"))
                }

                val jsonString = gson.toJson(myProfile)
                val fileName = "${myProfile.userId}.json"
                val file = File(context.cacheDir, fileName)
                file.writeText(jsonString)

                // Cache own profile locally as well
                File(localProfilesDir, fileName).writeText(jsonString)

                val requestFile = file.asRequestBody("application/json".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("document", file.name, requestFile)
                val caption = "📅 Availability update from ${myProfile.userName}"

                val uploadResponse = api.uploadAvailability(
                    botToken = botToken,
                    chatId = chatId,
                    document = body,
                    caption = caption
                )

                if (!uploadResponse.isSuccessful || uploadResponse.body()?.ok != true) {
                    val errorBody = uploadResponse.errorBody()?.string()
                        ?: uploadResponse.body()?.description
                        ?: "Upload failed"
                    return@withContext Result.failure(Exception("Telegram API error (${uploadResponse.code()}): $errorBody"))
                }

                val uploadedFileId = uploadResponse.body()?.result?.document?.fileId
                    ?: return@withContext Result.failure(Exception("Upload succeeded but no file_id was returned"))

                updateManifestEntry(
                    userId = myProfile.userId,
                    entry = ManifestEntry(
                        fileId = uploadedFileId,
                        fileName = fileName,
                        lastUpdated = myProfile.lastUpdated
                    )
                )

                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Reads the pinned manifest message, downloads each member's latest file_id,
     * and maps each group member to their latest availability profile.
     */
    suspend fun fetchGroupAvailability(): Result<Map<String, MemberAvailability>> =
        withContext(Dispatchers.IO) {
            val activeRoster = mutableMapOf<String, MemberAvailability>()
            try {
                if (botToken.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Bot token not configured"))
                }

                val (manifest, _) = fetchManifest()

                for ((userId, entry) in manifest) {
                    val profile = downloadMemberProfile(entry.fileId, entry.fileName)
                    if (profile != null) {
                        activeRoster[userId] = profile
                    }
                }

                Result.success(activeRoster)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Reads the chat's pinned message and parses it as a userId -> ManifestEntry map.
     * Returns the parsed manifest alongside the pinned message's id (null if there isn't one yet).
     */
    private suspend fun fetchManifest(): Pair<Map<String, ManifestEntry>, Long?> {
        val chatResponse = api.getChat(botToken, chatId)
        val pinned = chatResponse.body()?.result?.pinnedMessage
        val text = pinned?.text
        val messageId = pinned?.messageId
        if (messageId == null || text.isNullOrBlank()) {
            return emptyMap<String, ManifestEntry>() to null
        }

        val manifest: Map<String, ManifestEntry> = try {
            val type = object : TypeToken<Map<String, ManifestEntry>>() {}.type
            gson.fromJson(text, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        return manifest to messageId
    }

    /**
     * Merges a single member's entry into the shared manifest and persists it:
     * edits the existing pinned message if there is one, otherwise sends and pins a new one.
     */
    private suspend fun updateManifestEntry(userId: String, entry: ManifestEntry) {
        val (manifest, pinnedMessageId) = fetchManifest()
        val updatedManifest = manifest + (userId to entry)
        val manifestJson = gson.toJson(updatedManifest)

        if (pinnedMessageId != null) {
            api.editMessageText(botToken, chatId, pinnedMessageId, manifestJson)
        } else {
            val sendResponse = api.sendMessage(botToken, chatId, manifestJson)
            val newMessageId = sendResponse.body()?.result?.messageId
            if (newMessageId != null) {
                api.pinChatMessage(botToken, chatId, newMessageId)
            }
        }
    }

    /**
     * Downloads and parses an availability profile JSON file by file_id
     */
    private suspend fun downloadMemberProfile(fileId: String, fileName: String?): MemberAvailability? =
        withContext(Dispatchers.IO) {
            try {
                val pathResponse = api.getFilePath(botToken, fileId)
                val filePath = pathResponse.body()?.result?.filePath ?: return@withContext null

                val fileContentResponse = api.downloadFileDirectly(botToken, filePath)
                val jsonString = fileContentResponse.body()?.string() ?: return@withContext null

                // Save locally for offline cache
                fileName?.let { name ->
                    File(localProfilesDir, name).writeText(jsonString)
                }

                gson.fromJson(jsonString, MemberAvailability::class.java)
            } catch (e: Exception) {
                // If network fails, attempt to read from local disk cache
                val cachedFile = fileName?.let { File(localProfilesDir, it) }
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
