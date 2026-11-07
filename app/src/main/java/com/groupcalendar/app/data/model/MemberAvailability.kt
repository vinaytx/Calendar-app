package com.groupcalendar.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Busy time interval representation
 */
data class TimeSlot(
    @SerializedName("start") val start: String, // ISO-8601 string, e.g. "2026-11-05T09:00:00"
    @SerializedName("end") val end: String,     // ISO-8601 string, e.g. "2026-11-05T11:00:00"
    @SerializedName("title") val title: String? = null
)

/**
 * Member availability payload mapped to user_{id}.json
 */
data class MemberAvailability(
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("lastUpdated") val lastUpdated: String, // ISO-8601 UTC timestamp
    @SerializedName("busySlots") val busySlots: List<TimeSlot> = emptyList(),
    @SerializedName("colorHex") val colorHex: String? = null
)

// --- Telegram Bot API Response Models ---

data class TgUpdateResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: List<TgMessageWrapper> = emptyList(),
    @SerializedName("description") val description: String? = null
)

data class TgMessageWrapper(
    @SerializedName("update_id") val updateId: Long? = null,
    @SerializedName("message") val message: TgMessage? = null,
    @SerializedName("channel_post") val channelPost: TgMessage? = null
) {
    val actualMessage: TgMessage?
        get() = message ?: channelPost
}

data class TgMessage(
    @SerializedName("message_id") val messageId: Long? = null,
    @SerializedName("date") val date: Long? = null,
    @SerializedName("from") val from: TgUser? = null,
    @SerializedName("chat") val chat: TgChat? = null,
    @SerializedName("document") val document: TgDocument? = null,
    @SerializedName("text") val text: String? = null
)

data class TgDocument(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null
)

data class TgFileResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: TgFilePath? = null,
    @SerializedName("description") val description: String? = null
)

data class TgFilePath(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("file_path") val filePath: String? = null
)

data class TgUser(
    @SerializedName("id") val id: Long,
    @SerializedName("is_bot") val isBot: Boolean,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("username") val username: String? = null
)

data class TgChat(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String? = null,
    @SerializedName("type") val type: String? = null
)

data class TgUserResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: TgUser? = null,
    @SerializedName("description") val description: String? = null
)

data class TgChatResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: TgChat? = null,
    @SerializedName("description") val description: String? = null
)
