package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.BuildConfig
import com.tippingpoint.pedastudio.data.TeacherAccount
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WorksheetItem(
    val type: String,
    val question: String,
    val options: List<String>,
    val answer: String,
)

data class WorksheetResult(
    val title: String,
    val instructions: String,
    val items: List<WorksheetItem>,
    val teacherNotes: String,
    val account: TeacherAccount?,
)

object WorksheetApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun generate(
        lessonId: String,
        day: Int,
        idToken: String?,
    ): Result<WorksheetResult> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.failure(IllegalStateException("API URL not configured"))

        val body = JSONObject().apply {
            put("lessonId", lessonId)
            put("day", day)
        }

        val requestBuilder = Request.Builder()
            .url("$base/api/worksheet")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        if (!idToken.isNullOrBlank()) requestBuilder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (!response.isSuccessful) {
                    return Result.failure(parseApiError(text, response.code))
                }
                val root = json ?: return Result.failure(Exception("Empty response"))
                val ws = root.optJSONObject("worksheet") ?: return Result.failure(Exception("No worksheet"))
                val items = ws.optJSONArray("items")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        WorksheetItem(
                            type = o.optString("type"),
                            question = o.optString("question"),
                            options = o.optJSONArray("options")?.let { opts ->
                                (0 until opts.length()).map { j -> opts.optString(j) }
                            } ?: emptyList(),
                            answer = o.optString("answer"),
                        )
                    }
                } ?: emptyList()
                Result.success(
                    WorksheetResult(
                        title = ws.optString("title"),
                        instructions = ws.optString("instructions"),
                        items = items,
                        teacherNotes = ws.optString("teacherNotes"),
                        account = root.optJSONObject("account")?.let { AccountApiClient.parseAccount(it) },
                    ),
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Could not generate worksheet"))
        }
    }
}
