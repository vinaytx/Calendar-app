package com.groupcalendar.app.data.api

import com.groupcalendar.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface TelegramCalendarEngine {

    /**
     * Upload an availability document (e.g. user_12345.json) to the target chat
     */
    @Multipart
    @POST("bot{token}/sendDocument")
    suspend fun uploadAvailability(
        @Path("token") botToken: String,
        @Part("chat_id") chatId: RequestBody,
        @Part document: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): Response<ResponseBody>

    /**
     * Fetch recent updates / message logs from the Telegram chat
     */
    @GET("bot{token}/getUpdates")
    suspend fun getChatUpdates(
        @Path("token") botToken: String,
        @Query("offset") offset: Long? = null,
        @Query("limit") limit: Int? = 100,
        @Query("allowed_updates") allowedUpdates: String? = "[\"message\",\"channel_post\"]"
    ): Response<TgUpdateResponse>

    /**
     * Query Telegram for the download path of a given file_id
     */
    @GET("bot{token}/getFile")
    suspend fun getFilePath(
        @Path("token") botToken: String,
        @Query("file_id") fileId: String
    ): Response<TgFileResponse>

    /**
     * Stream and download the file payload directly from the Telegram CDN
     */
    @GET("file/bot{token}/{file_path}")
    @Streaming
    suspend fun downloadFileDirectly(
        @Path("token") botToken: String,
        @Path("file_path") filePath: String
    ): Response<ResponseBody>

    /**
     * Test authentication / verify bot token validity
     */
    @GET("bot{token}/getMe")
    suspend fun getMe(
        @Path("token") botToken: String
    ): Response<TgUserResponse>

    /**
     * Verify chat permissions and fetch group metadata
     */
    @GET("bot{token}/getChat")
    suspend fun getChat(
        @Path("token") botToken: String,
        @Query("chat_id") chatId: String
    ): Response<TgChatResponse>
}
