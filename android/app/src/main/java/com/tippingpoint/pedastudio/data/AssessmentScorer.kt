package com.tippingpoint.pedastudio.data

import com.tippingpoint.pedastudio.api.AssessmentToolData
import com.tippingpoint.pedastudio.api.AssessmentToolItem
import com.tippingpoint.pedastudio.api.ItemTallyPayload
import kotlin.math.round

data class McqTally(
    val countA: Int = 0,
    val countB: Int = 0,
    val countC: Int = 0,
    val countD: Int = 0,
    val notAssessed: Int = 0,
)

data class SubjectiveTally(
    val correctCount: Int = 0,
    val notAssessed: Int = 0,
)

sealed class ItemTallyState {
    abstract val notAssessed: Int

    data class Mcq(
        val counts: McqTally = McqTally(),
    ) : ItemTallyState() {
        override val notAssessed: Int get() = counts.notAssessed
    }

    data class Subjective(
        val correctCount: Int = 0,
        override val notAssessed: Int = 0,
    ) : ItemTallyState()
}

data class StrandPreview(
    val strand: String,
    val label: String,
    val percent: Double,
)

data class AssessmentScorePreview(
    val scorePercent: Double,
    val strands: List<StrandPreview>,
    val weakItemIds: List<String>,
)

object AssessmentScorer {

    fun initialTallies(tool: AssessmentToolData): Map<String, ItemTallyState> =
        tool.items.associate { item ->
            item.id to if (item.format == "mcq") {
                ItemTallyState.Mcq()
            } else {
                ItemTallyState.Subjective()
            }
        }

    fun mergeSavedTallies(
        base: Map<String, ItemTallyState>,
        payloads: List<ItemTallyPayload>,
    ): Map<String, ItemTallyState> {
        val out = base.toMutableMap()
        for (p in payloads) {
            out[p.itemId] = when (p.format) {
                "mcq" -> ItemTallyState.Mcq(
                    McqTally(
                        countA = p.countA,
                        countB = p.countB,
                        countC = p.countC,
                        countD = p.countD,
                        notAssessed = p.notAssessed,
                    ),
                )
                else -> ItemTallyState.Subjective(
                    correctCount = p.correctCount,
                    notAssessed = p.notAssessed,
                )
            }
        }
        return out
    }

    fun toPayloads(tallies: Map<String, ItemTallyState>): List<ItemTallyPayload> =
        tallies.map { (id, state) ->
            when (state) {
                is ItemTallyState.Mcq -> ItemTallyPayload(
                    itemId = id,
                    format = "mcq",
                    countA = state.counts.countA,
                    countB = state.counts.countB,
                    countC = state.counts.countC,
                    countD = state.counts.countD,
                    notAssessed = state.counts.notAssessed,
                )
                is ItemTallyState.Subjective -> ItemTallyPayload(
                    itemId = id,
                    format = "subjective",
                    correctCount = state.correctCount,
                    notAssessed = state.notAssessed,
                )
            }
        }

    fun preview(
        tool: AssessmentToolData,
        studentsAssessed: Int,
        tallies: Map<String, ItemTallyState>,
    ): AssessmentScorePreview {
        if (studentsAssessed < 1) {
            return AssessmentScorePreview(0.0, emptyList(), emptyList())
        }

        val itemById = tool.items.associateBy { it.id }
        var totalPossible = 0.0
        var totalObtained = 0.0
        val weak = mutableListOf<String>()
        val strandAgg = mutableMapOf<String, Pair<Double, Double>>() // obtained, possible

        for (item in tool.items) {
            totalPossible += item.marks
            val state = tallies[item.id] ?: continue
            val denom = (studentsAssessed - state.notAssessed).coerceAtLeast(1)
            val pValue = when (state) {
                is ItemTallyState.Mcq -> {
                    val correct = when (item.correctOption?.uppercase()) {
                        "A" -> state.counts.countA
                        "B" -> state.counts.countB
                        "C" -> state.counts.countC
                        "D" -> state.counts.countD
                        else -> 0
                    }
                    correct.toDouble() / denom
                }
                is ItemTallyState.Subjective -> state.correctCount.toDouble() / denom
            }
            val obtained = pValue * item.marks
            totalObtained += obtained
            if (pValue < 0.4) weak += item.id

            val prev = strandAgg[item.flnStrand] ?: (0.0 to 0.0)
            strandAgg[item.flnStrand] = (prev.first + obtained) to (prev.second + item.marks)
        }

        val scorePercent = if (totalPossible > 0) {
            round((totalObtained / totalPossible) * 1000) / 10.0
        } else 0.0

        val strands = strandAgg.map { (strand, pair) ->
            val (obtained, possible) = pair
            StrandPreview(
                strand = strand,
                label = strandLabel(strand),
                percent = if (possible > 0) round((obtained / possible) * 1000) / 10.0 else 0.0,
            )
        }.sortedBy { it.strand }

        return AssessmentScorePreview(scorePercent, strands, weak)
    }

    private fun strandLabel(code: String): String = when (code) {
        "OL" -> "Oral Language"
        "PA" -> "Phonological Awareness"
        "DEC" -> "Decoding"
        "RF" -> "Reading Fluency"
        "RC" -> "Reading Comprehension"
        "WR" -> "Writing"
        "VOC" -> "Vocabulary"
        else -> code
    }
}
