package com.tippingpoint.pedastudio.navigation

import android.net.Uri

object Routes {
    const val LANGUAGE = "language"
    const val LOGIN = "login"
    const val REGISTER_STEP2 = "register_step2"
    const val REGISTER_STEP3 = "register_step3"
    const val HOME = "home"
    const val EDIT_PROFILE = "edit_profile"
    const val QUICK_PLAN = "quick_plan/{lessonId}/{day}?mode={mode}&reteachNotes={reteachNotes}&afterUnitTest={afterUnitTest}"
    const val PLAN_VIEW = "plan_view/{lessonId}/{day}"
    const val FLASHCARDS = "flashcards/{lessonId}"
    const val SCAN = "scan?lessonId={lessonId}"
    const val WORKSHEET = "worksheet/{lessonId}/{day}"
    const val TLM_KIT = "tlm_kit/{grade}/{unit}"
    const val ASSESSMENT = "assessment/{grade}"
    const val ASSESSMENT_ENTRY = "assessment_entry/{grade}/{type}/{groupId}/{groupName}"
    const val LESSON_DETAIL = "lesson/{lessonId}"
    const val CHANGE_LANGUAGE = "change_language"
    const val SUBSCRIPTION = "subscription?highlight={highlight}"

    fun subscription(highlight: String? = null): String =
        if (highlight.isNullOrBlank()) "subscription?highlight="
        else "subscription?highlight=$highlight"

    fun quickPlan(
        lessonId: String,
        day: Int = 1,
        mode: String = "",
        reteachNotes: String = "",
        afterUnitTest: Boolean = false,
    ): String {
        val base = "quick_plan/${encode(lessonId)}/$day"
        val params = mutableListOf<String>()
        if (mode.isNotBlank()) params.add("mode=${encode(mode)}")
        if (reteachNotes.isNotBlank()) params.add("reteachNotes=${encode(reteachNotes)}")
        if (afterUnitTest) params.add("afterUnitTest=true")
        if (params.isEmpty()) return base
        return "$base?${params.joinToString("&")}"
    }
    fun planView(lessonId: String, day: Int) = "plan_view/${encode(lessonId)}/$day"
    fun flashcards(lessonId: String) = "flashcards/${encode(lessonId)}"
    fun scan(lessonId: String? = null) =
        if (lessonId.isNullOrBlank()) "scan?lessonId="
        else "scan?lessonId=${encode(lessonId)}"
    fun worksheet(lessonId: String, day: Int = 1) = "worksheet/${encode(lessonId)}/$day"
    fun tlmKit(grade: Int, unit: Int = 1) = "tlm_kit/$grade/$unit"
    fun assessment(grade: Int) = "assessment/$grade"
    fun assessmentEntry(grade: Int, type: String, groupId: String = "_", groupName: String = "_") =
        "assessment_entry/$grade/$type/${encode(if (groupId.isBlank()) "_" else groupId)}/${encode(if (groupName.isBlank()) "_" else groupName)}"

    fun lessonDetail(lessonId: String) = "lesson/${encode(lessonId)}"

    fun decodeLessonId(raw: String?): String? = raw?.let { Uri.decode(it) }

    fun decodeGroupId(raw: String?): String? {
        val decoded = raw?.let { Uri.decode(it) } ?: return null
        return if (decoded == "_") null else decoded
    }

    private fun encode(value: String): String = Uri.encode(value)
}
