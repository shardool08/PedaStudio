package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.BuildConfig
import com.tippingpoint.pedastudio.data.DayInfo
import com.tippingpoint.pedastudio.data.LessonItem
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.UserPreferences
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object PlanApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean get() = BuildConfig.API_BASE_URL.isNotBlank()

    fun generatePlan(
        lesson: LessonItem,
        day: Int,
        dayInfo: DayInfo?,
        selections: Map<String, String>,
        prefs: UserPreferences,
        idToken: String?,
        mode: String = "",
        reteachNotes: String = "",
    ): Result<PlanGenerationResult> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) {
            return Result.failure(IllegalStateException("API URL not set. Add pedastudio.api.url=http://10.0.2.2:3000 to android/local.properties"))
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            return Result.failure(
                IllegalStateException("Invalid API URL \"$base\". Fix android/local.properties — use pedastudio.api.url=http://10.0.2.2:3000"),
            )
        }

        val teacherProfile = JSONObject().apply {
            put("name", prefs.teacherName)
            put("medium", prefs.medium)
            put("studentCount", prefs.studentCount.toString())
            put("seating", "Rows")
            put("resources", selections["tlms"] ?: prefs.getTeacherResources().joinToString())
            put("language", prefs.language)
            put("comfort", prefs.englishComfort)
        }

        val mergedNotes = buildString {
            if (reteachNotes.isNotBlank()) {
                append("What didn't work in class: ")
                append(reteachNotes.trim())
            }
            val extra = selections["notes"].orEmpty().trim()
            if (extra.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append(extra)
            }
        }
        val mergedSelections = selections.toMutableMap().apply {
            if (mergedNotes.isNotBlank()) put("notes", mergedNotes)
        }

        val body = JSONObject().apply {
            put("lessonId", lesson.id)
            put("day", day)
            put("selections", JSONObject(mergedSelections))
            put("teacherProfile", teacherProfile)
            if (mode == "practice" || mode == "reteach" || mode == "continue") {
                put("mode", mode)
            }
        }

        val requestBuilder = Request.Builder()
            .url("$base/api/generate-plan")
            .post(body.toString().toRequestBody("application/json".toMediaType()))

        if (!idToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $idToken")
        }

        var lastError: Exception? = null
        repeat(2) { attempt ->
            val result = executeGeneratePlan(requestBuilder.build(), base)
            if (result.isSuccess) return result
            val error = result.exceptionOrNull() ?: return result
            lastError = error as? Exception ?: Exception(error.message)
            if (attempt == 0 && isTransientConnectionError(error.message.orEmpty())) {
                Thread.sleep(2000)
                return@repeat
            }
            return result
        }
        return Result.failure(lastError ?: Exception("Could not generate plan"))
    }

    private fun executeGeneratePlan(request: Request, base: String): Result<PlanGenerationResult> {
        return try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (!response.isSuccessful) {
                    val serverError = json?.optString("error", text)?.takeIf { it.isNotBlank() } ?: text
                    val friendly = when (response.code) {
                        401 -> "Please sign in again, then try generating the plan."
                        403, 429 -> serverError.ifBlank { "This feature needs a higher plan." }
                        503 -> when {
                            serverError.contains("API key", ignoreCase = true) ->
                                "Server missing Claude API key. Redeploy App Hosting after setting ANTHROPIC_API_KEY secret."
                            serverError.isNotBlank() && serverError.length < 200 -> serverError
                            else -> "Plan service is starting up. Wait a minute and try again."
                        }
                        else -> "Server error ${response.code}: $serverError"
                    }
                    return Result.failure(Exception(friendly))
                }
                val root = json ?: return Result.failure(Exception("No plan in response"))
                val plan = root.optJSONObject("plan")
                    ?: return Result.failure(Exception("No plan in response"))
                val account = root.optJSONObject("account")?.let { AccountApiClient.parseAccount(it) }
                Result.success(PlanGenerationResult(plan, account))
            }
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            val friendly = when {
                isTransientConnectionError(msg) -> buildConnectionHelp(base)
                else -> msg.ifBlank { "Could not generate plan" }
            }
            Result.failure(Exception(friendly))
        }
    }

    private fun isTransientConnectionError(message: String): Boolean =
        message.contains("upstream connect", ignoreCase = true) ||
            message.contains("connection termination", ignoreCase = true) ||
            message.contains("connection reset", ignoreCase = true) ||
            message.contains("Failed to connect", ignoreCase = true) ||
            message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("ECONNREFUSED", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true)

    private fun buildConnectionHelp(base: String): String {
        val isProduction = base.contains("hosted.app", ignoreCase = true) ||
            base.contains("firebaseapp.com", ignoreCase = true)
        val usingEmulatorHost = base.contains("10.0.2.2")
        return buildString {
            append("Cannot reach plan server at $base. ")
            if (isProduction) {
                append("The cloud API may be restarting — wait 30 seconds and try again. ")
                append("If it keeps failing, check Firebase Console → App Hosting → pedastudio-api for errors.")
            } else {
                append("On your PC run: npm run dev (must stay open). ")
                if (usingEmulatorHost) {
                    append("Emulator: use http://10.0.2.2:3000 (not https). ")
                    append("Physical phone: use http://YOUR_PC_LAN_IP:3000 instead of 10.0.2.2. ")
                } else {
                    append("Use http:// not https:// for local dev. ")
                }
                append("Then rebuild the Android app.")
            }
        }
    }
}
