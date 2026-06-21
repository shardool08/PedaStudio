package com.tippingpoint.pedastudio.data

import android.content.Context
import org.json.JSONObject

data class TlmKitItem(
    val id: String,
    val label: String,
    val essential: Boolean,
    val reason: String,
    val owned: Boolean,
)

data class TlmKitSummary(
    val grade: Int,
    val unit: Int?,
    val scope: String,
    val items: List<TlmKitItem>,
    val ownedCount: Int,
    val gapCount: Int,
    val procurementNote: String,
)

class TlmKitRepository(
    private val curriculum: CurriculumRepository,
    private val tlmCatalog: TlmResourceCatalog,
) {
    private val typeTlm = mapOf(
        "song" to listOf("phone", "blackboard", "ball"),
        "conversation" to listOf("blackboard", "notebook", "picture_cards"),
        "phonics" to listOf("chart", "flashcards", "blackboard", "notebook", "printer"),
        "story" to listOf("picture_cards", "blackboard", "puppets"),
        "poem" to listOf("phone", "blackboard", "chart"),
        "picture-talk" to listOf("chart", "picture_cards", "real_objects", "blackboard"),
    )
    private val essentials = listOf("blackboard", "textbook", "notebook")

    fun buildUnitKit(
        grade: Int,
        unit: Int,
        subject: String,
        medium: String,
        teacherResources: List<String>,
    ): TlmKitSummary {
        val lessons = curriculum.getLessons(grade, subject, medium).filter { it.unit == unit }
        return buildKit(lessons, grade, unit, "unit", teacherResources)
    }

    fun buildYearKit(
        grade: Int,
        subject: String,
        medium: String,
        teacherResources: List<String>,
        fullList: Boolean,
    ): TlmKitSummary {
        val lessons = curriculum.getLessons(grade, subject, medium)
        val kit = buildKit(lessons, grade, null, "year", teacherResources)
        if (fullList) return kit
        return kit.copy(
            items = kit.items.filter { it.essential || it.owned }.take(8),
            procurementNote = "Upgrade to Prime for the full year procurement list.",
        )
    }

    private fun buildKit(
        lessons: List<LessonItem>,
        grade: Int,
        unit: Int?,
        scope: String,
        teacherResources: List<String>,
    ): TlmKitSummary {
        val owned = teacherResources.toSet()
        val reasons = linkedMapOf<String, MutableSet<String>>()

        for (lesson in lessons) {
            for (id in recommendedFor(lesson)) {
                reasons.getOrPut(id) { mutableSetOf() }.add(
                    if (scope == "year") "Unit ${lesson.unit}" else lesson.en.take(40),
                )
            }
        }

        val items = reasons.map { (id, refs) ->
            TlmKitItem(
                id = id,
                label = tlmCatalog.labelFor(id),
                essential = essentials.contains(id),
                reason = if (scope == "year") {
                    "Needed across ${refs.take(4).joinToString(", ")}"
                } else {
                    "Used in: ${refs.take(3).joinToString("; ")}"
                },
                owned = owned.contains(id),
            )
        }.sortedWith(compareByDescending<TlmKitItem> { it.essential }.thenBy { it.label })

        val gap = items.count { !it.owned }
        return TlmKitSummary(
            grade = grade,
            unit = unit,
            scope = scope,
            items = items,
            ownedCount = items.count { it.owned },
            gapCount = gap,
            procurementNote = when {
                gap == 0 -> "Your classroom has the core materials."
                scope == "year" -> "Year procurement list: $gap item(s) still needed."
                else -> "$gap item(s) to arrange before Unit $unit teaching."
            },
        )
    }

    private fun recommendedFor(lesson: LessonItem): Set<String> {
        val fromType = typeTlm[lesson.type] ?: listOf("blackboard", "notebook")
        val out = linkedSetOf<String>()
        out.addAll(essentials)
        out.addAll(fromType)
        if (lesson.vocabulary.size >= 8) {
            out.add("flashcards")
            out.add("chart")
        }
        if (lesson.type == "phonics") out.add("printer")
        return out
    }
}

class AssessmentRepository(context: Context) {
    private val root: JSONObject

    init {
        val json = context.assets.open("assessments.json").bufferedReader().use { it.readText() }
        root = JSONObject(json)
    }

    fun keyFor(grade: Int, medium: String): String? = when {
        grade == 1 && isEnglishMedium(medium) -> "1_l1"
        grade == 1 -> "1_l2"
        grade == 2 && isEnglishMedium(medium) -> "2_l1"
        grade == 2 -> "2_l2"
        grade == 3 -> "3_l2"
        grade == 4 -> "4_l2"
        grade == 5 -> "5_l2"
        else -> null
    }

    fun getUnitTests(grade: Int, medium: String, lessons: List<LessonItem>): List<AssessmentUnitGroup> {
        val key = keyFor(grade, medium) ?: return emptyList()
        val obj = root.optJSONObject(key) ?: return emptyList()
        val out = mutableListOf<AssessmentUnitGroup>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val g = obj.optJSONObject(id) ?: continue
            val lessonIds = g.optJSONArray("lessons")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it) }
            } ?: emptyList()
            val unit = lessons.find { lessonIds.contains(it.id) }?.unit ?: 1
            out += AssessmentUnitGroup(
                id = id,
                name = g.optString("name"),
                focus = g.optString("focus"),
                lessons = lessonIds,
                unit = unit,
            )
        }
        return out.sortedWith(compareBy({ it.unit }, { it.id }))
    }

    private fun isEnglishMedium(medium: String): Boolean {
        val m = medium.lowercase()
        return m == "english" || m == "semi_english" || m == "semi-english"
    }
}

data class AssessmentUnitGroup(
    val id: String,
    val name: String,
    val focus: String,
    val lessons: List<String>,
    val unit: Int,
)
