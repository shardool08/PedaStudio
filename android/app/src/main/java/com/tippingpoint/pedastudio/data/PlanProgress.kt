package com.tippingpoint.pedastudio.data

enum class DayPlanStatus(val key: String) {
    NOT_STARTED("not_started"),
    PLANNED("planned"),
    COMPLETED("completed"),
    ;

    companion object {
        fun fromKey(key: String?): DayPlanStatus =
            entries.find { it.key == key } ?: NOT_STARTED
    }
}

enum class LessonProgressStatus {
    NOT_STARTED,
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
}

enum class PlanFeedback(val key: String) {
    WENT_WELL("went_well"),
    SOME_STRUGGLED("some_struggled"),
    MOST_DIDNT_UNDERSTAND("most_didnt_understand"),
    COULDNT_FINISH("couldnt_finish"),
    READY_FOR_MORE("ready_for_more"),
    ;

    companion object {
        fun fromKey(key: String?): PlanFeedback? =
            key?.let { k -> entries.find { it.key == k } }
    }
}

data class DayPlanMeta(
    val status: DayPlanStatus,
    val feedback: PlanFeedback? = null,
    val savedAt: Long = 0L,
    val completedAt: Long = 0L,
)

data class NextPlanAction(
    val action: String,
    val label: String,
    val description: String,
    val lessonId: String,
    val day: Int,
)

data class PendingHomeAction(
    val completedDay: Int,
    val feedback: PlanFeedback,
    val nextAction: NextPlanAction,
)

object PlanProgressHelper {
    fun getLessonStatus(lessonId: String, totalDays: Int, planStorage: PlanStorage): LessonProgressStatus {
        if (totalDays <= 0) return LessonProgressStatus.NOT_STARTED
        var hasPlanned = false
        var hasCompleted = false
        var allCompleted = totalDays > 0
        for (d in 1..totalDays) {
            when (planStorage.getDayMeta(lessonId, d).status) {
                DayPlanStatus.PLANNED -> {
                    hasPlanned = true
                    allCompleted = false
                }
                DayPlanStatus.COMPLETED -> hasCompleted = true
                DayPlanStatus.NOT_STARTED -> allCompleted = false
            }
        }
        if (allCompleted) return LessonProgressStatus.COMPLETED
        if (hasCompleted) return LessonProgressStatus.IN_PROGRESS
        if (hasPlanned) return LessonProgressStatus.PLANNED
        return LessonProgressStatus.NOT_STARTED
    }

    fun getFirstUnplannedDay(lessonId: String, totalDays: Int, planStorage: PlanStorage): Int? {
        for (d in 1..totalDays) {
            if (planStorage.getDayMeta(lessonId, d).status == DayPlanStatus.NOT_STARTED) return d
        }
        return null
    }

    fun getFirstPlannedDay(lessonId: String, totalDays: Int, planStorage: PlanStorage): Int? {
        for (d in 1..totalDays) {
            val status = planStorage.getDayMeta(lessonId, d).status
            if (status == DayPlanStatus.PLANNED || status == DayPlanStatus.COMPLETED) return d
        }
        return null
    }

    /** First day marked planned (not completed) with a saved plan — the live classroom session. */
    fun getOngoingPlanDay(lessonId: String, totalDays: Int, planStorage: PlanStorage): Int? {
        for (d in 1..totalDays) {
            when (planStorage.getDayMeta(lessonId, d).status) {
                DayPlanStatus.PLANNED -> if (planStorage.hasPlan(lessonId, d)) return d
                DayPlanStatus.COMPLETED -> continue
                DayPlanStatus.NOT_STARTED -> return null
            }
        }
        return null
    }

    /** Day to open from home "Current plan" — ongoing session, else last useful saved plan. */
    fun getCurrentPlanDay(lessonId: String, totalDays: Int, planStorage: PlanStorage): Int? {
        getOngoingPlanDay(lessonId, totalDays, planStorage)?.let { return it }
        return getActivePlanDay(lessonId, totalDays, planStorage)
            ?.takeIf { planStorage.hasPlan(lessonId, it) }
    }

    /**
     * Day whose saved plan the teacher most likely wants to open now — not always Day 1.
     * Uses last edited plan, else the latest planned day before the next unplanned day.
     */
    fun getActivePlanDay(lessonId: String, totalDays: Int, planStorage: PlanStorage): Int? {
        planStorage.getLastPlan()?.let { (id, day) ->
            if (id == lessonId && day in 1..totalDays && planStorage.hasPlan(lessonId, day)) {
                return day
            }
        }
        val nextUnplanned = getFirstUnplannedDay(lessonId, totalDays, planStorage)
        val lastPlannedBeforeNext = (nextUnplanned ?: (totalDays + 1)) - 1
        if (lastPlannedBeforeNext >= 1) {
            for (d in lastPlannedBeforeNext downTo 1) {
                if (planStorage.hasPlan(lessonId, d)) return d
            }
        }
        for (d in 1..totalDays) {
            if (planStorage.getDayMeta(lessonId, d).status == DayPlanStatus.PLANNED) return d
        }
        return null
    }

    fun nextLessonInCurriculum(lessons: List<LessonItem>, lessonId: String): LessonItem? {
        val index = lessons.indexOfFirst { it.id == lessonId }
        if (index < 0 || index + 1 >= lessons.size) return null
        return lessons[index + 1]
    }

    /**
     * Picks the active lesson for home/roadmap: keeps manual selection until that lesson is
     * fully completed, then advances to the next textbook lesson.
     */
    fun resolveCurrentLessonId(
        lessons: List<LessonItem>,
        storedLessonId: String,
        planStorage: PlanStorage,
    ): String {
        if (lessons.isEmpty()) return ""

        val stored = lessons.find { it.id == storedLessonId }
        if (storedLessonId.isNotBlank() && stored != null) {
            val status = getLessonStatus(storedLessonId, stored.days, planStorage)
            if (status != LessonProgressStatus.COMPLETED) return storedLessonId
            nextLessonInCurriculum(lessons, storedLessonId)?.let { return it.id }
            return storedLessonId
        }

        lessons.firstOrNull { lesson ->
            getLessonStatus(lesson.id, lesson.days, planStorage) != LessonProgressStatus.COMPLETED
        }?.let { return it.id }

        return lessons.first().id
    }

    fun getNextAction(
        lessonId: String,
        day: Int,
        feedback: PlanFeedback,
        totalDays: Int,
    ): NextPlanAction = when (feedback) {
        PlanFeedback.WENT_WELL, PlanFeedback.READY_FOR_MORE -> {
            if (day < totalDays) {
                NextPlanAction(
                    action = "next_day",
                    label = "Continue to Day ${day + 1}",
                    description = "Students understood well. Move ahead.",
                    lessonId = lessonId,
                    day = day + 1,
                )
            } else {
                NextPlanAction(
                    action = "next_lesson",
                    label = "Move to next lesson",
                    description = "All days completed!",
                    lessonId = lessonId,
                    day = day,
                )
            }
        }
        PlanFeedback.SOME_STRUGGLED -> NextPlanAction(
            action = "practice",
            label = "Add a practice day",
            description = "Repeat with a different activity.",
            lessonId = lessonId,
            day = day,
        )
        PlanFeedback.MOST_DIDNT_UNDERSTAND -> NextPlanAction(
            action = "reteach",
            label = "Re-teach this day",
            description = "Try a simpler approach.",
            lessonId = lessonId,
            day = day,
        )
        PlanFeedback.COULDNT_FINISH -> NextPlanAction(
            action = "continue",
            label = "Continue same plan",
            description = "Pick up where you left off.",
            lessonId = lessonId,
            day = day,
        )
    }

    /** Day to send to quick-plan when the teacher chooses Re-plan from home feedback. */
    fun replanDayFor(completedDay: Int, action: NextPlanAction): Int = when (action.action) {
        "practice", "reteach", "continue" -> action.day
        else -> completedDay
    }

    /**
     * After marking a day complete, surfaces the recommended follow-up on home until the teacher
     * plans ahead, re-plans, continues, or dismisses.
     */
    fun getPendingHomeAction(lesson: LessonItem, planStorage: PlanStorage): PendingHomeAction? {
        if (getOngoingPlanDay(lesson.id, lesson.days, planStorage) != null) return null

        var completedDay: Int? = null
        var completedAt = 0L
        for (d in 1..lesson.days) {
            val meta = planStorage.getDayMeta(lesson.id, d)
            if (meta.status == DayPlanStatus.COMPLETED && meta.feedback != null && meta.completedAt >= completedAt) {
                completedDay = d
                completedAt = meta.completedAt
            }
        }
        val day = completedDay ?: return null
        if (planStorage.isHomeNextActionDismissed(lesson.id, day)) return null

        val feedback = planStorage.getDayMeta(lesson.id, day).feedback ?: return null
        val action = getNextAction(lesson.id, day, feedback, lesson.days)
        if (!isHomeActionPending(lesson, action, planStorage)) return null
        return PendingHomeAction(day, feedback, action)
    }

    fun isHomeActionPending(lesson: LessonItem, action: NextPlanAction, planStorage: PlanStorage): Boolean {
        when (action.action) {
            "next_day" -> {
                if (action.day !in 1..lesson.days) return false
                return planStorage.getDayMeta(lesson.id, action.day).status == DayPlanStatus.NOT_STARTED
            }
            "next_lesson" -> {
                return getLessonStatus(lesson.id, lesson.days, planStorage) == LessonProgressStatus.COMPLETED
            }
            "practice", "reteach", "continue" -> {
                return planStorage.getDayMeta(lesson.id, action.day).status == DayPlanStatus.COMPLETED
            }
        }
        return false
    }

    fun statusIcon(status: DayPlanStatus): String = when (status) {
        DayPlanStatus.NOT_STARTED -> "○"
        DayPlanStatus.PLANNED -> "📝"
        DayPlanStatus.COMPLETED -> "✅"
    }

    fun lessonStatusIcon(status: LessonProgressStatus): String = when (status) {
        LessonProgressStatus.NOT_STARTED -> "○"
        LessonProgressStatus.PLANNED -> "📝"
        LessonProgressStatus.IN_PROGRESS -> "▶"
        LessonProgressStatus.COMPLETED -> "✅"
    }
}
