package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.BuildConfig
import com.tippingpoint.pedastudio.data.TeacherAccount
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AssessmentGroup(
    val id: String,
    val name: String,
    val focus: String,
    val lessons: List<String>,
    val unit: Int,
)

data class AssessmentCatalog(
    val baselineTitle: String,
    val baselineDesc: String,
    val endlineTitle: String,
    val endlineDesc: String,
    val unitTests: List<AssessmentGroup>,
)

data class AssessmentScoreRecord(
    val id: String,
    val type: String,
    val groupId: String?,
    val groupName: String?,
    val scorePercent: Int,
    val studentsAssessed: Int,
)

data class AssessmentHubData(
    val catalog: AssessmentCatalog,
    val scores: List<AssessmentScoreRecord>,
    val account: TeacherAccount?,
)

object AssessmentApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun fetchCatalog(
        grade: Int,
        subject: String,
        medium: String,
        idToken: String?,
    ): Result<AssessmentHubData> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.failure(IllegalStateException("API URL not configured"))

        val url = "$base/api/assessment".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("grade", grade.toString())
            ?.addQueryParameter("subject", subject)
            ?.addQueryParameter("medium", medium)
            ?.build()
            ?: return Result.failure(IllegalStateException("Invalid API URL"))

        val requestBuilder = Request.Builder().url(url)
        if (!idToken.isNullOrBlank()) requestBuilder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(requestBuilder.get().build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) return Result.failure(parseApiError(text, response.code))
                Result.success(parseHub(JSONObject(text)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Could not load assessments"))
        }
    }

    fun saveScore(
        type: String,
        grade: Int,
        subject: String,
        groupId: String?,
        groupName: String?,
        scorePercent: Int,
        studentsAssessed: Int,
        notes: String,
        idToken: String?,
    ): Result<Pair<AssessmentScoreRecord, TeacherAccount?>> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.failure(IllegalStateException("API URL not configured"))

        val body = JSONObject().apply {
            put("type", type)
            put("grade", grade)
            put("subject", subject)
            put("scorePercent", scorePercent)
            put("studentsAssessed", studentsAssessed)
            put("notes", notes)
            if (!groupId.isNullOrBlank()) put("groupId", groupId)
            if (!groupName.isNullOrBlank()) put("groupName", groupName)
        }

        val requestBuilder = Request.Builder()
            .url("$base/api/assessment/scores")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        if (!idToken.isNullOrBlank()) requestBuilder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) return Result.failure(parseApiError(text, response.code))
                val root = JSONObject(text)
                val rec = root.optJSONObject("record") ?: return Result.failure(Exception("No record"))
                Result.success(
                    parseRecord(rec) to root.optJSONObject("account")?.let { AccountApiClient.parseAccount(it) },
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Could not save score"))
        }
    }

    private fun parseHub(root: JSONObject): AssessmentHubData {
        val cat = root.optJSONObject("catalog") ?: JSONObject()
        val baseline = cat.optJSONObject("baseline") ?: JSONObject()
        val endline = cat.optJSONObject("endline") ?: JSONObject()
        val unitArr = cat.optJSONArray("unitTests")
        val units = unitArr?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                AssessmentGroup(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    focus = o.optString("focus"),
                    lessons = o.optJSONArray("lessons")?.let { ls ->
                        (0 until ls.length()).map { j -> ls.optString(j) }
                    } ?: emptyList(),
                    unit = o.optInt("unit", 1),
                )
            }
        } ?: emptyList()
        val scoresArr = root.optJSONArray("scores")
        val scores = scoresArr?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { parseRecord(it) }
            }
        } ?: emptyList()
        return AssessmentHubData(
            catalog = AssessmentCatalog(
                baselineTitle = baseline.optString("title"),
                baselineDesc = baseline.optString("description"),
                endlineTitle = endline.optString("title"),
                endlineDesc = endline.optString("description"),
                unitTests = units,
            ),
            scores = scores,
            account = root.optJSONObject("account")?.let { AccountApiClient.parseAccount(it) },
        )
    }

    private fun parseRecord(o: JSONObject) = AssessmentScoreRecord(
        id = o.optString("id"),
        type = o.optString("type"),
        groupId = o.optString("groupId").ifBlank { null },
        groupName = o.optString("groupName").ifBlank { null },
        scorePercent = o.optInt("scorePercent"),
        studentsAssessed = o.optInt("studentsAssessed"),
    )
}
