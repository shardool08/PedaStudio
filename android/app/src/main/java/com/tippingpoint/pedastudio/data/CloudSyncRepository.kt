package com.tippingpoint.pedastudio.data

import com.tippingpoint.pedastudio.api.SyncApiClient
import com.tippingpoint.pedastudio.auth.PhoneAuthController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cloud sync for profile and lesson plans, over the PedaStudio API (Supabase behind it).
 * Replaces the old FirestoreRepository — the app no longer talks to a database directly.
 */
class CloudSyncRepository(
    private val auth: PhoneAuthController,
) {
    private suspend fun token(): String? = auth.getIdToken()

    suspend fun pushProfile(prefs: UserPreferences): Result<Unit> = withContext(Dispatchers.IO) {
        SyncApiClient.putProfile(prefs.toProfileJson(), token())
    }

    suspend fun pullProfile(prefs: UserPreferences): Result<Boolean> = withContext(Dispatchers.IO) {
        SyncApiClient.getProfile(token()).map { profile ->
            if (profile == null) return@map false
            prefs.applyProfileJson(profile)
            true
        }
    }

    suspend fun pushPlan(
        lessonId: String,
        day: Int,
        planJson: String,
        selections: Map<String, String>,
        planStorage: PlanStorage,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val meta = planStorage.getDayMeta(lessonId, day)
            val selectionsJson = JSONObject()
            selections.forEach { (key, value) -> selectionsJson.put(key, value) }
            val body = JSONObject().apply {
                put("lessonId", lessonId)
                put("day", day)
                put("plan", JSONObject(planJson))
                put("selections", selectionsJson)
                put("status", meta.status.key)
                meta.feedback?.let { put("feedback", it.key) }
                if (meta.savedAt > 0L) put("savedAt", meta.savedAt)
                if (meta.completedAt > 0L) put("completedAt", meta.completedAt)
            }
            SyncApiClient.putPlan(body, token()).getOrThrow()
            Unit
        }
    }

    suspend fun pushPlanMeta(
        lessonId: String,
        day: Int,
        planStorage: PlanStorage,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val meta = planStorage.getDayMeta(lessonId, day)
            val body = JSONObject().apply {
                put("lessonId", lessonId)
                put("day", day)
                put("status", meta.status.key)
                meta.feedback?.let { put("feedback", it.key) }
                if (meta.savedAt > 0L) put("savedAt", meta.savedAt)
                if (meta.completedAt > 0L) put("completedAt", meta.completedAt)
            }
            SyncApiClient.putPlan(body, token()).getOrThrow()
            Unit
        }
    }

    suspend fun pullPlan(lessonId: String, day: Int, planStorage: PlanStorage): Result<Boolean> =
        withContext(Dispatchers.IO) {
            SyncApiClient.getPlans(token(), lessonId, day).map { plans ->
                val record = plans.firstOrNull() ?: return@map false
                applyPlanRecord(record, planStorage)
                true
            }
        }

    /** Pull cloud plans, then push any local plans that are newer or missing in cloud. */
    suspend fun syncAllPlans(planStorage: PlanStorage): Result<PlanSyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cloudPlans = SyncApiClient.getPlans(token()).getOrThrow()
                val cloudUpdated = mutableMapOf<String, Long>()
                var pulled = 0

                for (record in cloudPlans) {
                    val lessonId = record.optString("lessonId").takeIf { it.isNotBlank() } ?: continue
                    val day = record.optInt("day", 0).takeIf { it > 0 } ?: continue
                    val cloudTs = record.optLong("updatedAt", 0L)
                    cloudUpdated[planKey(lessonId, day)] = cloudTs
                    if (cloudTs >= planStorage.getPlanUpdatedAt(lessonId, day)) {
                        applyPlanRecord(record, planStorage)
                        pulled++
                    }
                }

                var pushed = 0
                for (ref in planStorage.listLocalPlans()) {
                    val localTs = planStorage.getPlanUpdatedAt(ref.lessonId, ref.day)
                    val cloudTs = cloudUpdated[planKey(ref.lessonId, ref.day)] ?: 0L
                    if (localTs > cloudTs) {
                        val plan = planStorage.getPlan(ref.lessonId, ref.day) ?: continue
                        pushPlan(
                            ref.lessonId,
                            ref.day,
                            plan.toString(),
                            planStorage.getPlanSelections(ref.lessonId, ref.day),
                            planStorage,
                        ).getOrThrow()
                        pushed++
                    }
                }

                PlanSyncResult(pulled = pulled, pushed = pushed, totalCloud = cloudPlans.size)
            }
        }

    suspend fun loadFlashcardImageUrls(lessonId: String): Map<String, String> =
        withContext(Dispatchers.IO) {
            SyncApiClient.getCatalog(lessonId, token())
                .map { jsonToStringMap(it.optJSONObject("flashcardImages")) }
                .getOrDefault(emptyMap())
        }

    suspend fun loadTlmImageUrls(): Map<String, String> = withContext(Dispatchers.IO) {
        SyncApiClient.getCatalog(null, token())
            .map { jsonToStringMap(it.optJSONObject("tlmImages")) }
            .getOrDefault(emptyMap())
    }

    private fun planKey(lessonId: String, day: Int) = "${lessonId}_day$day"

    private fun applyPlanRecord(record: JSONObject, planStorage: PlanStorage) {
        val lessonId = record.optString("lessonId").takeIf { it.isNotBlank() } ?: return
        val day = record.optInt("day", 0).takeIf { it > 0 } ?: return
        val plan = record.optJSONObject("plan")
        val selections = jsonToStringMap(record.optJSONObject("selections"))
        val updatedAt = record.optLong("updatedAt", 0L).takeIf { it > 0L } ?: System.currentTimeMillis()

        if (plan != null) {
            planStorage.savePlan(lessonId, day, plan.toString(), selections, updatedAt)
        }

        val statusKey = record.optString("status").takeIf { it.isNotBlank() }
        val status = when {
            statusKey != null -> DayPlanStatus.fromKey(statusKey)
            plan != null -> DayPlanStatus.PLANNED
            else -> DayPlanStatus.NOT_STARTED
        }
        val feedback = PlanFeedback.fromKey(record.optString("feedback").takeIf { it.isNotBlank() })
        val savedAt = record.optLong("savedAt", 0L)
        val completedAt = record.optLong("completedAt", 0L)

        if (statusKey != null || feedback != null || savedAt > 0L || completedAt > 0L) {
            planStorage.applyCloudMeta(lessonId, day, status, feedback, savedAt, completedAt)
        }
    }
}

data class PlanSyncResult(val pulled: Int, val pushed: Int, val totalCloud: Int)

private fun jsonToStringMap(obj: JSONObject?): Map<String, String> {
    if (obj == null) return emptyMap()
    val out = mutableMapOf<String, String>()
    obj.keys().forEach { key ->
        val value = obj.optString(key)
        if (value.isNotBlank()) out[key] = value
    }
    return out
}

/** Profile fields the server accepts. Local-only values (e.g. photo URI) stay on device. */
fun UserPreferences.toProfileJson(): JSONObject {
    val currentLessons = JSONObject()
    getTeacherGrades().forEach { grade ->
        getTeacherSubjects().forEach { subject ->
            val id = getCurrentLesson(grade, subject)
            if (id.isNotBlank()) currentLessons.put("${grade}_$subject", id)
        }
    }
    return JSONObject().apply {
        put("teacherName", teacherName)
        put("phoneNumber", phoneNumber)
        put("language", language)
        put("state", state)
        put("district", district)
        put("adminType", adminType)
        put("zpName", zpName)
        put("corpName", corpName)
        put("medium", medium)
        put("englishComfort", englishComfort)
        put("schoolName", schoolName)
        put("location", location)
        put("pinCode", pinCode)
        put("studentCount", studentCount)
        put("internetAccess", internetAccess)
        put("printingAccess", printingAccess)
        put("teacherGrades", JSONArray(getTeacherGrades()))
        put("teacherSubjects", JSONArray(getTeacherSubjects()))
        put("teacherResources", JSONArray(getTeacherResources()))
        put("currentLessons", currentLessons)
        put("profileComplete", profileComplete)
    }
}

fun UserPreferences.applyProfileJson(data: JSONObject) {
    fun text(key: String, apply: (String) -> Unit) {
        if (data.has(key)) data.optString(key).takeIf { it.isNotBlank() }?.let(apply)
    }

    text("teacherName") { teacherName = it }
    text("phoneNumber") { phoneNumber = it }
    text("language") { language = it }
    text("state") { state = it }
    text("district") { district = it }
    text("adminType") { adminType = it }
    text("zpName") { zpName = it }
    text("corpName") { corpName = it }
    text("medium") { medium = it }
    text("englishComfort") { englishComfort = it }
    text("schoolName") { schoolName = it }
    text("location") { location = it }
    text("pinCode") { pinCode = it }
    text("internetAccess") { internetAccess = it }
    text("printingAccess") { printingAccess = it }

    if (data.has("studentCount")) studentCount = data.optInt("studentCount", studentCount)
    if (data.has("profileComplete")) profileComplete = data.optBoolean("profileComplete", profileComplete)

    data.optJSONArray("teacherGrades")?.let { arr ->
        val grades = (0 until arr.length()).map { arr.optInt(it) }.filter { it > 0 }
        if (grades.isNotEmpty()) setTeacherGrades(grades)
    }
    data.optJSONArray("teacherSubjects")?.let { arr ->
        val subjects = (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        if (subjects.isNotEmpty()) setTeacherSubjects(subjects)
    }
    data.optJSONArray("teacherResources")?.let { arr ->
        val resources = (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        if (resources.isNotEmpty()) setTeacherResources(resources)
    }
    data.optJSONObject("currentLessons")?.let { lessons ->
        lessons.keys().forEach { key ->
            val lessonId = lessons.optString(key).takeIf { it.isNotBlank() } ?: return@forEach
            val parts = key.split("_")
            if (parts.size >= 2) {
                val grade = parts[0].toIntOrNull() ?: return@forEach
                setCurrentLesson(grade, parts.drop(1).joinToString("_"), lessonId)
            }
        }
    }
}
