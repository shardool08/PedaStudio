package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Profile, lesson plan and catalog sync against the PedaStudio API, which stores
 * everything in Supabase. Replaces the app's former direct Firestore access.
 */
object SyncApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    private fun base(): String = BuildConfig.API_BASE_URL.trimEnd('/')

    private fun request(path: String, idToken: String?): Request.Builder {
        val builder = Request.Builder().url("${base()}$path")
        if (!idToken.isNullOrBlank()) builder.addHeader("Authorization", "Bearer $idToken")
        return builder
    }

    private fun call(builder: Request.Builder): Result<JSONObject> = try {
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Result.failure(parseApiError(text, response.code))
            } else {
                Result.success(runCatching { JSONObject(text) }.getOrElse { JSONObject() })
            }
        }
    } catch (e: Exception) {
        Result.failure(Exception(e.message ?: "Could not reach the server"))
    }

    fun getProfile(idToken: String?): Result<JSONObject?> =
        call(request("/api/profile", idToken).get()).map { root ->
            if (root.optBoolean("exists")) root.optJSONObject("profile") else null
        }

    fun putProfile(profile: JSONObject, idToken: String?): Result<Unit> =
        call(
            request("/api/profile", idToken).put(profile.toString().toRequestBody(jsonType)),
        ).map { }

    /** Saved plans for this teacher, optionally narrowed to one lesson day. */
    fun getPlans(idToken: String?, lessonId: String? = null, day: Int? = null): Result<List<JSONObject>> {
        val url = "${base()}/api/plans".toHttpUrlOrNull()?.newBuilder()?.apply {
            if (!lessonId.isNullOrBlank()) addQueryParameter("lessonId", lessonId)
            if (day != null) addQueryParameter("day", day.toString())
        }?.build() ?: return Result.failure(IllegalStateException("API URL not configured"))

        val builder = Request.Builder().url(url).get()
        if (!idToken.isNullOrBlank()) builder.addHeader("Authorization", "Bearer $idToken")
        return call(builder).map { root ->
            val array = root.optJSONArray("plans") ?: return@map emptyList()
            (0 until array.length()).mapNotNull { array.optJSONObject(it) }
        }
    }

    fun putPlan(plan: JSONObject, idToken: String?): Result<JSONObject?> =
        call(
            request("/api/plans", idToken).put(plan.toString().toRequestBody(jsonType)),
        ).map { it.optJSONObject("plan") }

    /** Artwork URLs. Pass a lessonId to also get that lesson's flashcard images. */
    fun getCatalog(lessonId: String?, idToken: String?): Result<JSONObject> {
        val url = "${base()}/api/catalog".toHttpUrlOrNull()?.newBuilder()?.apply {
            if (!lessonId.isNullOrBlank()) addQueryParameter("lessonId", lessonId)
        }?.build() ?: return Result.failure(IllegalStateException("API URL not configured"))

        val builder = Request.Builder().url(url).get()
        if (!idToken.isNullOrBlank()) builder.addHeader("Authorization", "Bearer $idToken")
        return call(builder)
    }
}
