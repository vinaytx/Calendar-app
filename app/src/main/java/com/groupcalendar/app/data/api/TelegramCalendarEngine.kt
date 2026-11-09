package com.groupcalendar.app.data.api

import com.groupcalendar.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface TelegramCalendarEngine {

    /**
     * Upload an availability document (e.g. user_12345.json) to the target chat.
     * Returns the sent Message so the caller can read back the document's file_id.
     */
    @Multipart
    @POST("bot{token}/sendDocument")
    suspend fun uploadAvailability(
        @Path(value = "token", encoded = true) botToken: String,
        @Query("chat_id") chatId: String,
        @Part document: MultipartBody.Part,
        @Query("caption") caption: String? = null
    ): Response<TgMessageResponse>

    /**
     * Post a new text message (used to create the pinned manifest the first time)
     */
    @FormUrlEncoded
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path(value = "token", encoded = true) botToken: String,
        @Field("chat_id") chatId: String,
        @Field("text") text: String,
        @Field("disable_notification") disableNotification: Boolean = true
    ): Response<TgMessageResponse>

    /**
     * Overwrite the text of an existing message (used to update the pinned manifest)
     */
    @FormUrlEncoded
    @POST("bot{token}/editMessageText")
    suspend fun editMessageText(
        @Path(value = "token", encoded = true) botToken: String,
        @Field("chat_id") chatId: String,
        @Field("message_id") messageId: Long,
        @Field("text") text: String
    ): Response<TgMessageResponse>

    /**
     * Pin the manifest message so getChat() can always find it via pinned_message
     */
    @FormUrlEncoded
    @POST("bot{token}/pinChatMessage")
    suspend fun pinChatMessage(
        @Path(value = "token", encoded = true) botToken: String,
        @Field("chat_id") chatId: String,
        @Field("message_id") messageId: Long,
        @Field("disable_notification") disableNotification: Boolean = true
    ): Response<TgBooleanResponse>

    /**
     * Query Telegram for the download path of a given file_id
     */
    @GET("bot{token}/getFile")
    suspend fun getFilePath(
        @Path(value = "token", encoded = true) botToken: String,
        @Query("file_id") fileId: String
    ): Response<TgFileResponse>

    /**
     * Stream and download the file payload directly from the Telegram CDN
     */
    @GET("file/bot{token}/{file_path}")
    @Streaming
    suspend fun downloadFileDirectly(
        @Path(value = "token", encoded = true) botToken: String,
        @Path(value = "file_path", encoded = true) filePath: String
    ): Response<ResponseBody>

    /**
     * Test authentication / verify bot token validity
     */
    @GET("bot{token}/getMe")
    suspend fun getMe(
        @Path(value = "token", encoded = true) botToken: String
    ): Response<TgUserResponse>

    /**
     * Verify chat permissions and fetch group metadata
     */
    @GET("bot{token}/getChat")
    suspend fun getChat(
        @Path(value = "token", encoded = true) botToken: String,
        @Query("chat_id") chatId: String
    ): Response<TgChatResponse>
}
