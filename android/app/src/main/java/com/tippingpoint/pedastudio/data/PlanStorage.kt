package com.tippingpoint.pedastudio.data

import android.content.Context
import org.json.JSONObject

data class PlanRef(val lessonId: String, val day: Int)

class PlanStorage(context: Context) {
    private val prefs = context.getSharedPreferences("pedastudio_plans", Context.MODE_PRIVATE)

    fun savePlan(
        lessonId: String,
        day: Int,
        planJson: String,
        selections: Map<String, String> = emptyMap(),
        updatedAt: Long = System.currentTimeMillis(),
    ) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(key(lessonId, day), planJson)
            .putString(selectionsKey(lessonId, day), JSONObject(selections).toString())
            .putLong(updatedKey(lessonId, day), updatedAt)
            .putString(statusKey(lessonId, day), DayPlanStatus.PLANNED.key)
            .putLong(savedAtKey(lessonId, day), now)
            .remove(feedbackKey(lessonId, day))
            .remove(completedAtKey(lessonId, day))
            .putString("last_plan_key", "$lessonId|$day")
            .apply()
    }

    fun completePlan(lessonId: String, day: Int, feedback: PlanFeedback) {
        prefs.edit()
            .putString(statusKey(lessonId, day), DayPlanStatus.COMPLETED.key)
            .putString(feedbackKey(lessonId, day), feedback.key)
            .putLong(completedAtKey(lessonId, day), System.currentTimeMillis())
            .remove(homeActionDismissKey(lessonId, day))
            .apply()
    }

    fun applyCloudMeta(
        lessonId: String,
        day: Int,
        status: DayPlanStatus,
        feedback: PlanFeedback?,
        savedAt: Long,
        completedAt: Long,
    ) {
        prefs.edit()
            .putString(statusKey(lessonId, day), status.key)
            .apply {
                if (feedback != null) putString(feedbackKey(lessonId, day), feedback.key)
                else remove(feedbackKey(lessonId, day))
                if (savedAt > 0L) putLong(savedAtKey(lessonId, day), savedAt)
                if (completedAt > 0L) putLong(completedAtKey(lessonId, day), completedAt)
            }
            .apply()
    }

    fun getDayMeta(lessonId: String, day: Int): DayPlanMeta {
        val storedStatus = prefs.getString(statusKey(lessonId, day), null)
        val status = when {
            storedStatus != null -> DayPlanStatus.fromKey(storedStatus)
            hasPlan(lessonId, day) -> DayPlanStatus.PLANNED
            else -> DayPlanStatus.NOT_STARTED
        }
        return DayPlanMeta(
            status = status,
            feedback = PlanFeedback.fromKey(prefs.getString(feedbackKey(lessonId, day), null)),
            savedAt = prefs.getLong(savedAtKey(lessonId, day), 0L),
            completedAt = prefs.getLong(completedAtKey(lessonId, day), 0L),
        )
    }

    fun getPlanSelections(lessonId: String, day: Int): Map<String, String> {
        val raw = prefs.getString(selectionsKey(lessonId, day), null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun getPlan(lessonId: String, day: Int): JSONObject? {
        val raw = prefs.getString(key(lessonId, day), null) ?: return null
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun getPlanUpdatedAt(lessonId: String, day: Int): Long =
        prefs.getLong(updatedKey(lessonId, day), 0L)

    fun hasPlan(lessonId: String, day: Int): Boolean = getPlan(lessonId, day) != null

    fun firstSavedDay(lessonId: String, maxDays: Int = 10): Int? {
        for (d in 1..maxDays) {
            val meta = getDayMeta(lessonId, d)
            if (meta.status == DayPlanStatus.PLANNED || meta.status == DayPlanStatus.COMPLETED) return d
        }
        return null
    }

    fun listLocalPlans(): List<PlanRef> {
        return prefs.all.keys
            .asSequence()
            .filter { it.startsWith("plan_") }
            .mapNotNull { prefKey ->
                val withoutPrefix = prefKey.removePrefix("plan_")
                val lastUnderscore = withoutPrefix.lastIndexOf('_')
                if (lastUnderscore <= 0) return@mapNotNull null
                val lessonId = withoutPrefix.substring(0, lastUnderscore)
                val day = withoutPrefix.substring(lastUnderscore + 1).toIntOrNull() ?: return@mapNotNull null
                PlanRef(lessonId, day)
            }
            .distinct()
            .sortedByDescending { getPlanUpdatedAt(it.lessonId, it.day) }
            .toList()
    }

    fun getLastPlan(): Pair<String, Int>? {
        val raw = prefs.getString("last_plan_key", null) ?: return null
        val parts = raw.split("|")
        if (parts.size != 2) return null
        val day = parts[1].toIntOrNull() ?: return null
        return parts[0] to day
    }

    fun dismissHomeNextAction(lessonId: String, completedDay: Int) {
        prefs.edit().putBoolean(homeActionDismissKey(lessonId, completedDay), true).apply()
    }

    fun isHomeNextActionDismissed(lessonId: String, completedDay: Int): Boolean =
        prefs.getBoolean(homeActionDismissKey(lessonId, completedDay), false)

    private fun homeActionDismissKey(lessonId: String, day: Int) = "home_dismiss_${lessonId}_$day"

    private fun key(lessonId: String, day: Int) = "plan_${lessonId}_$day"

    private fun selectionsKey(lessonId: String, day: Int) = "selections_${lessonId}_$day"

    private fun updatedKey(lessonId: String, day: Int) = "updated_${lessonId}_$day"

    private fun statusKey(lessonId: String, day: Int) = "status_${lessonId}_$day"

    private fun feedbackKey(lessonId: String, day: Int) = "feedback_${lessonId}_$day"

    private fun savedAtKey(lessonId: String, day: Int) = "savedAt_${lessonId}_$day"

    private fun completedAtKey(lessonId: String, day: Int) = "completedAt_${lessonId}_$day"
}
