package com.tippingpoint.pedastudio.data

import com.tippingpoint.pedastudio.api.AssessmentToolData

object AssessmentReteachHelper {

    fun buildReteachNotes(tool: AssessmentToolData, weakItemIds: List<String>): String {
        if (weakItemIds.isEmpty()) return ""
        val lines = mutableListOf<String>()
        lines.add("Unit/assessment: ${tool.title}")
        lines.add("Weak learning outcomes (below 40% class correct):")
        for (id in weakItemIds.take(8)) {
            val item = tool.items.find { it.id == id } ?: continue
            lines.add("• ${item.id}: ${item.competency}")
        }
        return lines.joinToString("\n")
    }

    fun pickLessonId(tool: AssessmentToolData, weakItemIds: List<String>): String? {
        for (id in weakItemIds) {
            val lessonId = tool.items.find { it.id == id }?.lessonIds?.firstOrNull()
            if (!lessonId.isNullOrBlank()) return lessonId
        }
        return tool.items.firstOrNull { it.lessonIds.isNotEmpty() }?.lessonIds?.firstOrNull()
    }
}
