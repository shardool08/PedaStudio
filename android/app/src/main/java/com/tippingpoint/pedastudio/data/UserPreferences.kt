package com.tippingpoint.pedastudio.data

import android.content.Context
import org.json.JSONArray

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("pedastudio", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("lang", "mr") ?: "mr"
        set(value) = prefs.edit().putString("lang", value).apply()

    var phoneNumber: String
        get() = prefs.getString("phoneNumber", "") ?: ""
        set(value) = prefs.edit().putString("phoneNumber", value).apply()

    var teacherName: String
        get() = prefs.getString("teacherName", "") ?: ""
        set(value) = prefs.edit().putString("teacherName", value).apply()

    var state: String
        get() = prefs.getString("state", IndiaStates.MAHARASHTRA) ?: IndiaStates.MAHARASHTRA
        set(value) = prefs.edit().putString("state", value).apply()

    var district: String
        get() = prefs.getString("district", "") ?: ""
        set(value) = prefs.edit().putString("district", value).apply()

    var adminType: String
        get() = prefs.getString("adminType", "") ?: ""
        set(value) = prefs.edit().putString("adminType", value).apply()

    var zpName: String
        get() = prefs.getString("zpName", "") ?: ""
        set(value) = prefs.edit().putString("zpName", value).apply()

    var corpName: String
        get() = prefs.getString("corpName", "") ?: ""
        set(value) = prefs.edit().putString("corpName", value).apply()

    var medium: String
        get() = prefs.getString("medium", "") ?: ""
        set(value) = prefs.edit().putString("medium", value).apply()

    var englishComfort: String
        get() = prefs.getString("englishComfort", "") ?: ""
        set(value) = prefs.edit().putString("englishComfort", value).apply()

    var schoolName: String
        get() = prefs.getString("schoolName", "") ?: ""
        set(value) = prefs.edit().putString("schoolName", value).apply()

    var location: String
        get() = prefs.getString("location", "") ?: ""
        set(value) = prefs.edit().putString("location", value).apply()

    var pinCode: String
        get() = prefs.getString("pinCode", "") ?: ""
        set(value) = prefs.edit().putString("pinCode", value).apply()

    var studentCount: Int
        get() = prefs.getInt("studentCount", 30)
        set(value) = prefs.edit().putInt("studentCount", value).apply()

    var internetAccess: String
        get() = prefs.getString("internetAccess", "") ?: ""
        set(value) = prefs.edit().putString("internetAccess", value).apply()

    var printingAccess: String
        get() = prefs.getString("printingAccess", "") ?: ""
        set(value) = prefs.edit().putString("printingAccess", value).apply()

    var currentLesson: String
        get() = prefs.getString("currentLesson", "") ?: ""
        set(value) = prefs.edit().putString("currentLesson", value).apply()

    var profileComplete: Boolean
        get() = prefs.getBoolean("profileComplete", false)
        set(value) = prefs.edit().putBoolean("profileComplete", value).apply()

    var cachedTier: String
        get() = prefs.getString("cachedTier", "basic") ?: "basic"
        set(value) = prefs.edit().putString("cachedTier", value).apply()

    var cachedPlansRemaining: Int
        get() = prefs.getInt("cachedPlansRemaining", 20)
        set(value) = prefs.edit().putInt("cachedPlansRemaining", value).apply()

    fun applyTeacherAccount(account: TeacherAccount) {
        cachedTier = account.tier.key
        cachedPlansRemaining = account.plansRemaining ?: 999
    }

    fun getTeacherGrades(): List<Int> = readIntList("teacherGrades", listOf(1))

    fun setTeacherGrades(grades: List<Int>) = writeIntList("teacherGrades", grades)

    fun getTeacherSubjects(): List<String> = readStringList("teacherSubjects", listOf("english"))

    fun setTeacherSubjects(subjects: List<String>) = writeStringList("teacherSubjects", subjects)

    fun getTeacherResources(): List<String> = readStringList("teacherResources", emptyList())

    fun setTeacherResources(resources: List<String>) = writeStringList("teacherResources", resources)

    fun getCurrentLesson(grade: Int, subject: String): String {
        val key = "currentLesson_${grade}_$subject"
        return prefs.getString(key, null) ?: currentLesson
    }

    fun setCurrentLesson(grade: Int, subject: String, lessonId: String) {
        prefs.edit()
            .putString("currentLesson_${grade}_$subject", lessonId)
            .putString("currentLesson", lessonId)
            .apply()
    }

    var lastViewedGrade: Int
        get() = prefs.getInt("lastViewedGrade", getTeacherGrades().firstOrNull() ?: 1)
        set(value) = prefs.edit().putInt("lastViewedGrade", value).apply()

    var lastViewedSubject: String
        get() = prefs.getString("lastViewedSubject", getTeacherSubjects().firstOrNull() ?: "english") ?: "english"
        set(value) = prefs.edit().putString("lastViewedSubject", value).apply()

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun readIntList(key: String, default: List<Int>): List<Int> {
        val raw = prefs.getString(key, null) ?: return default
        return try {
            JSONArray(raw).let { arr ->
                (0 until arr.length()).map { arr.getInt(it) }
            }
        } catch (_: Exception) {
            default
        }
    }

    private fun writeIntList(key: String, values: List<Int>) {
        prefs.edit().putString(key, JSONArray(values).toString()).apply()
    }

    private fun readStringList(key: String, default: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return default
        return try {
            JSONArray(raw).let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
        } catch (_: Exception) {
            default
        }
    }

    private fun writeStringList(key: String, values: List<String>) {
        prefs.edit().putString(key, JSONArray(values).toString()).apply()
    }
}
