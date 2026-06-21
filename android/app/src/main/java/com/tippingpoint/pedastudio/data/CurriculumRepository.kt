package com.tippingpoint.pedastudio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DayInfo(val day: Int, val level: String, val focus: String)

data class LessonItem(
    val id: String,
    val unit: Int,
    val en: String,
    val mr: String,
    val hi: String,
    val ur: String,
    val type: String,
    val pages: String,
    val days: Int,
    val vocabulary: List<String> = emptyList(),
    val bloomsProgression: List<DayInfo> = emptyList(),
) {
    fun title(lang: String): String = when (lang) {
        "mr" -> mr.ifBlank { en }
        "hi" -> hi.ifBlank { en }
        "ur" -> ur.ifBlank { en }
        else -> en
    }

    /** Textbook lesson title — always English; does not follow app UI language. */
    val curriculumTitle: String get() = en

    fun gradeNumber(): Int = id.substringBefore('.').filter { it.isDigit() }.toIntOrNull() ?: 1

    fun dayFocus(day: Int): DayInfo? = bloomsProgression.find { it.day == day }
}

class CurriculumRepository(context: Context) {
    private val root: JSONObject

    init {
        val json = context.assets.open("curriculum.json").bufferedReader().use { it.readText() }
        root = JSONObject(json)
    }

    fun isEnglishMedium(medium: String): Boolean {
        val m = medium.lowercase()
        return m == "english" || m == "semi_english" || m == "semi-english"
    }

    private fun keyFor(grade: Int, medium: String): String? = when {
        grade == 1 && isEnglishMedium(medium) -> "1_l1"
        grade == 1 -> "1_l2"
        grade == 2 && isEnglishMedium(medium) -> "2_l1"
        grade == 2 -> "2_l2"
        grade == 3 -> "3_l2"
        grade == 4 -> "4_l2"
        grade == 5 -> "5_l2"
        else -> null
    }

    fun getLessons(grade: Int, subject: String, medium: String): List<LessonItem> {
        if (subject != "english") return emptyList()
        val key = keyFor(grade, medium) ?: return emptyList()
        val arr = root.optJSONArray(key) ?: return emptyList()
        return parseArray(arr)
    }

    fun findLesson(lessonId: String, grade: Int, subject: String, medium: String): LessonItem? =
        getLessons(grade, subject, medium).find { it.id == lessonId }

    fun findLessonAnywhere(lessonId: String): LessonItem? {
        val keys = root.keys()
        while (keys.hasNext()) {
            val arr = root.optJSONArray(keys.next()) ?: continue
            parseArray(arr).firstOrNull { it.id == lessonId }?.let { return it }
        }
        return null
    }

    fun isAvailable(grade: Int, subject: String): Boolean {
        return subject == "english" && grade in 1..5 && getLessons(grade, subject, "marathi").isNotEmpty()
    }

    private fun parseArray(arr: JSONArray): List<LessonItem> {
        val out = mutableListOf<LessonItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val vocab = mutableListOf<String>()
            o.optJSONArray("vocabulary")?.let { va ->
                for (j in 0 until va.length()) vocab.add(va.getString(j))
            }
            val blooms = mutableListOf<DayInfo>()
            o.optJSONArray("bloomsProgression")?.let { ba ->
                for (j in 0 until ba.length()) {
                    val b = ba.getJSONObject(j)
                    blooms.add(DayInfo(b.getInt("day"), b.getString("level"), b.getString("focus")))
                }
            }
            out.add(
                LessonItem(
                    id = o.getString("id"),
                    unit = o.getInt("unit"),
                    en = o.getString("en"),
                    mr = o.optString("mr", o.getString("en")),
                    hi = o.optString("hi", o.getString("en")),
                    ur = o.optString("ur", o.getString("en")),
                    type = o.getString("type"),
                    pages = o.getString("pages"),
                    days = o.getInt("days"),
                    vocabulary = vocab,
                    bloomsProgression = blooms,
                )
            )
        }
        return out
    }
}
