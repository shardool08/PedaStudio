package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.BuildConfig
import com.tippingpoint.pedastudio.data.TeacherAccount
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AssessmentGroup(
    val id: String,
    val name: String,
    val focus: String,
    val lessons: List<String>,
    val unit: Int,
    val hasTool: Boolean = false,
)

data class AssessmentCatalog(
    val baselineTitle: String,
    val baselineDesc: String,
    val baselineHasTool: Boolean = false,
    val endlineTitle: String,
    val endlineDesc: String,
    val endlineHasTool: Boolean = false,
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

data class AssessmentToolItem(
    val id: String,
    val section: String,
    val format: String,
    val marks: Int,
    val stem: String,
    val flnStrand: String,
    val competency: String,
    val deliveryMode: String,
    val lessonIds: List<String> = emptyList(),
    val options: List<Pair<String, String>>,
    val correctOption: String?,
    val markingScheme: String,
    val stimulusNote: String?,
)

data class AssessmentToolData(
    val id: String,
    val title: String,
    val totalMarks: Int,
    val recommendedMinutes: Int,
    val administrationNotes: List<String>,
    val items: List<AssessmentToolItem>,
)

data class ItemTallyPayload(
    val itemId: String,
    val format: String,
    val countA: Int = 0,
    val countB: Int = 0,
    val countC: Int = 0,
    val countD: Int = 0,
    val correctCount: Int = 0,
    val notAssessed: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("itemId", itemId)
        put("format", format)
        if (format == "mcq") {
            put("countA", countA)
            put("countB", countB)
            put("countC", countC)
            put("countD", countD)
        } else {
            put("correctCount", correctCount)
        }
        if (notAssessed > 0) put("notAssessed", notAssessed)
    }
}

data class SavedAssessmentData(
    val studentsAssessed: Int,
    val scorePercent: Int,
    val notes: String,
    val tallies: List<ItemTallyPayload>,
    val weakItems: List<String>,
)

data class AssessmentToolResponse(
    val available: Boolean,
    val tool: AssessmentToolData?,
    val saved: SavedAssessmentData?,
    val account: TeacherAccount?,
)

data class AssessmentSaveResult(
    val record: AssessmentScoreRecord,
    val reportScorePercent: Double?,
    val weakItems: List<String> = emptyList(),
    val account: TeacherAccount?,
)

data class PaperScanResult(
    val studentsProcessed: Int,
    val tallies: List<ItemTallyPayload>,
    val warnings: List<String>,
    val account: TeacherAccount?,
)

object AssessmentApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
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

    fun fetchTool(
        grade: Int,
        subject: String,
        medium: String,
        type: String,
        groupId: String?,
        idToken: String?,
    ): Result<AssessmentToolResponse> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.failure(IllegalStateException("API URL not configured"))

        val builder = "$base/api/assessment/tool".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("grade", grade.toString())
            ?.addQueryParameter("subject", subject)
            ?.addQueryParameter("medium", medium)
            ?.addQueryParameter("type", type)
            ?: return Result.failure(IllegalStateException("Invalid API URL"))

        if (!groupId.isNullOrBlank()) builder.addQueryParameter("groupId", groupId)

        val requestBuilder = Request.Builder().url(builder.build())
        if (!idToken.isNullOrBlank()) requestBuilder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(requestBuilder.get().build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) return Result.failure(parseApiError(text, response.code))
                Result.success(parseToolResponse(JSONObject(text)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Could not load assessment tool"))
        }
    }

    fun downloadToolMarkdown(
        grade: Int,
        subject: String,
        medium: String,
        type: String,
        groupId: String?,
        copy: String,
        format: String = "html",
        idToken: String?,
    ): Result<String> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.failure(IllegalStateException("API URL not configured"))

        val builder = "$base/api/assessment/tool/download".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("grade", grade.toString())
            ?.addQueryParameter("subject", subject)
            ?.addQueryParameter("medium", medium)
            ?.addQueryParameter("type", type)
            ?.addQueryParameter("copy", copy)
            ?.addQueryParameter("format", format)
            ?: return Result.failure(IllegalStateException("Invalid API URL"))

        if (!groupId.isNullOrBlank()) builder.addQueryParameter("groupId", groupId)

        val requestBuilder = Request.Builder().url(builder.build())
        if (!idToken.isNullOrBlank()) requestBuilder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(requestBuilder.get().build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) return Result.failure(parseApiError(text, response.code))
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Could not download"))
        }
    }

    fun saveScore(
        type: String,
        grade: Int,
        subject: String,
        medium: String,
        groupId: String?,
        groupName: String?,
        scorePercent: Int,
        studentsAssessed: Int,
        notes: String,
        tallies: List<ItemTallyPayload>?,
        idToken: String?,
    ): Result<AssessmentSaveResult> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.failure(IllegalStateException("API URL not configured"))

        val body = JSONObject().apply {
            put("type", type)
            put("grade", grade)
            put("subject", subject)
            put("medium", medium)
            put("studentsAssessed", studentsAssessed)
            put("notes", notes)
            if (!groupId.isNullOrBlank()) put("groupId", groupId)
            if (!groupName.isNullOrBlank()) put("groupName", groupName)
            if (tallies != null) {
                put("tallies", JSONArray().apply { tallies.forEach { put(it.toJson()) } })
            } else {
                put("scorePercent", scorePercent)
            }
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
                val report = root.optJSONObject("report")
                val weakArr = report?.optJSONArray("weakItems")
                val weakItems = weakArr?.let { arr ->
                    (0 until arr.length()).map { i -> arr.optString(i) }.filter { it.isNotBlank() }
                } ?: emptyList()
                Result.success(
                    AssessmentSaveResult(
                        record = parseRecord(rec),
                        reportScorePercent = report?.optDouble("scorePercent"),
                        weakItems = weakItems,
                        account = root.optJSONObject("account")?.let { AccountApiClient.parseAccount(it) },
                    ),
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Could not save scores"))
        }
    }

    fun scanMarkPapers(
        grade: Int,
        subject: String,
        medium: String,
        type: String,
        groupId: String?,
        scanMode: String,
        images: List<Pair<String, String>>,
        idToken: String?,
    ): Result<PaperScanResult> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.failure(IllegalStateException("API URL not configured"))

        val imagesJson = JSONArray().apply {
            images.forEach { (b64, mime) ->
                put(JSONObject().apply {
                    put("base64", b64)
                    put("mediaType", mime)
                })
            }
        }
        val body = JSONObject().apply {
            put("grade", grade)
            put("subject", subject)
            put("medium", medium)
            put("type", type)
            put("scanMode", scanMode)
            put("images", imagesJson)
            if (!groupId.isNullOrBlank()) put("groupId", groupId)
        }

        val requestBuilder = Request.Builder()
            .url("$base/api/assessment/scan-mark")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        if (!idToken.isNullOrBlank()) requestBuilder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) return Result.failure(parseApiError(text, response.code))
                val root = JSONObject(text)
                val talliesArr = root.optJSONArray("tallies") ?: JSONArray()
                val tallies = (0 until talliesArr.length()).mapNotNull { i ->
                    val t = talliesArr.optJSONObject(i) ?: return@mapNotNull null
                    ItemTallyPayload(
                        itemId = t.optString("itemId"),
                        format = t.optString("format"),
                        countA = t.optInt("countA"),
                        countB = t.optInt("countB"),
                        countC = t.optInt("countC"),
                        countD = t.optInt("countD"),
                        correctCount = t.optInt("correctCount"),
                        notAssessed = t.optInt("notAssessed"),
                    )
                }
                val warningsArr = root.optJSONArray("warnings")
                val warnings = warningsArr?.let { arr ->
                    (0 until arr.length()).map { i -> arr.optString(i) }
                } ?: emptyList()
                Result.success(
                    PaperScanResult(
                        studentsProcessed = root.optInt("studentsProcessed"),
                        tallies = tallies,
                        warnings = warnings,
                        account = root.optJSONObject("account")?.let { AccountApiClient.parseAccount(it) },
                    ),
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Could not mark papers"))
        }
    }

    fun downloadReportHtml(
        grade: Int,
        subject: String,
        medium: String,
        type: String,
        groupId: String?,
        idToken: String?,
    ): Result<String> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.failure(IllegalStateException("API URL not configured"))

        val builder = "$base/api/assessment/report".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("grade", grade.toString())
            ?.addQueryParameter("subject", subject)
            ?.addQueryParameter("medium", medium)
            ?.addQueryParameter("type", type)
            ?: return Result.failure(IllegalStateException("Invalid API URL"))
        if (!groupId.isNullOrBlank()) builder.addQueryParameter("groupId", groupId)

        val requestBuilder = Request.Builder().url(builder.build())
        if (!idToken.isNullOrBlank()) requestBuilder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(requestBuilder.get().build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) return Result.failure(parseApiError(text, response.code))
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Could not export report"))
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
                    hasTool = o.optBoolean("hasTool", false),
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
                baselineHasTool = baseline.optBoolean("hasTool", false),
                endlineTitle = endline.optString("title"),
                endlineDesc = endline.optString("description"),
                endlineHasTool = endline.optBoolean("hasTool", false),
                unitTests = units,
            ),
            scores = scores,
            account = root.optJSONObject("account")?.let { AccountApiClient.parseAccount(it) },
        )
    }

    private fun parseToolResponse(root: JSONObject): AssessmentToolResponse {
        val available = root.optBoolean("available", false)
        val toolObj = root.optJSONObject("tool")
        val savedObj = root.optJSONObject("saved")
        return AssessmentToolResponse(
            available = available,
            tool = toolObj?.let { parseTool(it) },
            saved = savedObj?.let { parseSaved(it) },
            account = root.optJSONObject("account")?.let { AccountApiClient.parseAccount(it) },
        )
    }

    private fun parseTool(o: JSONObject): AssessmentToolData {
        val itemsArr = o.optJSONArray("items") ?: JSONArray()
        val items = (0 until itemsArr.length()).mapNotNull { i ->
            val item = itemsArr.optJSONObject(i) ?: return@mapNotNull null
            val optsArr = item.optJSONArray("options")
            val options = optsArr?.let { arr ->
                (0 until arr.length()).mapNotNull { j ->
                    val opt = arr.optJSONObject(j) ?: return@mapNotNull null
                    opt.optString("key") to opt.optString("text")
                }
            } ?: emptyList()
            val lessonIdsArr = item.optJSONArray("lessonIds")
            val lessonIds = lessonIdsArr?.let { arr ->
                (0 until arr.length()).map { j -> arr.optString(j) }.filter { it.isNotBlank() }
            } ?: emptyList()
            AssessmentToolItem(
                id = item.optString("id"),
                section = item.optString("section"),
                format = item.optString("format"),
                marks = item.optInt("marks", 1),
                stem = item.optString("stem"),
                flnStrand = item.optString("flnStrand"),
                competency = item.optString("competency"),
                deliveryMode = item.optString("deliveryMode"),
                lessonIds = lessonIds,
                options = options,
                correctOption = item.optString("correctOption").ifBlank { null },
                markingScheme = item.optString("markingScheme"),
                stimulusNote = item.optString("stimulusNote").ifBlank { null },
            )
        }
        val notesArr = o.optJSONArray("administrationNotes")
        val adminNotes = notesArr?.let { arr ->
            (0 until arr.length()).map { i -> arr.optString(i) }
        } ?: emptyList()
        return AssessmentToolData(
            id = o.optString("id"),
            title = o.optString("title"),
            totalMarks = o.optInt("totalMarks"),
            recommendedMinutes = o.optInt("recommendedMinutes"),
            administrationNotes = adminNotes,
            items = items,
        )
    }

    private fun parseSaved(o: JSONObject): SavedAssessmentData {
        val talliesArr = o.optJSONArray("tallies") ?: JSONArray()
        val tallies = (0 until talliesArr.length()).mapNotNull { i ->
            val t = talliesArr.optJSONObject(i) ?: return@mapNotNull null
            ItemTallyPayload(
                itemId = t.optString("itemId"),
                format = t.optString("format"),
                countA = t.optInt("countA"),
                countB = t.optInt("countB"),
                countC = t.optInt("countC"),
                countD = t.optInt("countD"),
                correctCount = t.optInt("correctCount"),
                notAssessed = t.optInt("notAssessed"),
            )
        }
        val weakArr = o.optJSONArray("weakItems")
        val weak = weakArr?.let { arr -> (0 until arr.length()).map { i -> arr.optString(i) } } ?: emptyList()
        return SavedAssessmentData(
            studentsAssessed = o.optInt("studentsAssessed"),
            scorePercent = o.optInt("scorePercent"),
            notes = o.optString("notes"),
            tallies = tallies,
            weakItems = weak,
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
