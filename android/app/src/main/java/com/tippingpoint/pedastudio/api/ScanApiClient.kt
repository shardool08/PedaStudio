package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.BuildConfig
import com.tippingpoint.pedastudio.data.TeacherAccount
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ScanAnalysis(
    val detectedLesson: String,
    val detectedPage: String,
    val summary: String,
    val vocabulary: List<String>,
    val suggestedActions: List<String>,
    val actionHints: Map<String, String>,
    val account: TeacherAccount?,
)

object ScanApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun analyzePage(
        imageBase64: String,
        mediaType: String,
        lessonId: String?,
        idToken: String?,
    ): Result<ScanAnalysis> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.failure(IllegalStateException("API URL not configured"))

        val body = JSONObject().apply {
            put("imageBase64", imageBase64)
            put("mediaType", mediaType)
            if (!lessonId.isNullOrBlank()) put("lessonId", lessonId)
        }

        val requestBuilder = Request.Builder()
            .url("$base/api/scan")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        if (!idToken.isNullOrBlank()) requestBuilder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) return Result.failure(parseApiError(text, response.code))
                val root = JSONObject(text)
                val a = root.optJSONObject("analysis") ?: return Result.failure(Exception("No analysis"))
                val hintsObj = a.optJSONObject("actionHints")
                val hints = buildMap {
                    hintsObj?.keys()?.forEach { key ->
                        put(key, hintsObj.optString(key))
                    }
                }
                val vocab = a.optJSONArray("vocabulary")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it) }
                } ?: emptyList()
                val actions = a.optJSONArray("suggestedActions")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it) }
                } ?: emptyList()
                Result.success(
                    ScanAnalysis(
                        detectedLesson = a.optString("detectedLesson"),
                        detectedPage = a.optString("detectedPage"),
                        summary = a.optString("summary"),
                        vocabulary = vocab,
                        suggestedActions = actions,
                        actionHints = hints,
                        account = root.optJSONObject("account")?.let { AccountApiClient.parseAccount(it) },
                    ),
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Could not analyze scan"))
        }
    }
}
