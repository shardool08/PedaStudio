package com.tippingpoint.pedastudio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedScan(
    val id: String,
    val savedAt: Long,
    val lessonId: String,
    val lessonTitle: String,
    val detectedLesson: String,
    val summary: String,
    val vocabulary: List<String>,
    val planLessonId: String?,
    val planDay: Int?,
)

class ScanStorage(context: Context) {
    private val prefs = context.getSharedPreferences("pedastudio_scans", Context.MODE_PRIVATE)

    fun saveScan(
        lessonId: String,
        lessonTitle: String,
        detectedLesson: String,
        summary: String,
        vocabulary: List<String>,
    ): String {
        val id = System.currentTimeMillis().toString()
        val now = System.currentTimeMillis()
        val obj = JSONObject().apply {
            put("id", id)
            put("savedAt", now)
            put("lessonId", lessonId)
            put("lessonTitle", lessonTitle)
            put("detectedLesson", detectedLesson)
            put("summary", summary)
            put("vocabulary", JSONArray(vocabulary))
        }
        val ids = loadIds().toMutableList()
        ids.add(0, id)
        prefs.edit()
            .putString(key(id), obj.toString())
            .putString("scan_ids", JSONArray(ids).toString())
            .apply()
        return id
    }

    fun linkPlan(scanId: String, lessonId: String, day: Int) {
        val raw = prefs.getString(key(scanId), null) ?: return
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return
        obj.put("planLessonId", lessonId)
        obj.put("planDay", day)
        prefs.edit().putString(key(scanId), obj.toString()).apply()
    }

    fun listScans(): List<SavedScan> =
        loadIds().mapNotNull { loadScan(it) }

    fun loadScan(id: String): SavedScan? {
        val raw = prefs.getString(key(id), null) ?: return null
        return runCatching { parseScan(JSONObject(raw)) }.getOrNull()
    }

    fun deleteScan(id: String) {
        val ids = loadIds().filterNot { it == id }
        prefs.edit()
            .remove(key(id))
            .putString("scan_ids", JSONArray(ids).toString())
            .apply()
    }

    private fun loadIds(): List<String> {
        val raw = prefs.getString("scan_ids", null) ?: return emptyList()
        return runCatching {
            JSONArray(raw).let { arr -> (0 until arr.length()).map { arr.getString(it) } }
        }.getOrDefault(emptyList())
    }

    private fun parseScan(obj: JSONObject): SavedScan {
        val vocab = obj.optJSONArray("vocabulary")?.let { arr ->
            (0 until arr.length()).map { arr.optString(it) }
        } ?: emptyList()
        val planDay = if (obj.has("planDay") && !obj.isNull("planDay")) obj.optInt("planDay") else null
        return SavedScan(
            id = obj.optString("id"),
            savedAt = obj.optLong("savedAt"),
            lessonId = obj.optString("lessonId"),
            lessonTitle = obj.optString("lessonTitle"),
            detectedLesson = obj.optString("detectedLesson"),
            summary = obj.optString("summary"),
            vocabulary = vocab,
            planLessonId = obj.optString("planLessonId").takeIf { it.isNotBlank() },
            planDay = planDay,
        )
    }

    private fun key(id: String) = "scan_$id"
}
