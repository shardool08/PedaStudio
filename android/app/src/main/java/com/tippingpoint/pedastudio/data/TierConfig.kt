package com.tippingpoint.pedastudio.data

/** Mirrors lib/tier-config.ts for client-side UI gating. */
object TierConfig {
    enum class TierId(val key: String, val label: String) {
        BASIC("basic", "Basic"),
        PRIME("prime", "Prime"),
        MAX("max", "Max"),
    }

    const val BASIC_PLANS_PER_WEEK = 2
    const val PRIME_PLANS_PER_WEEK = 6
    const val MAX_PLANS_PER_WEEK = 6
    const val PRIME_OCR_SCANS_PER_WEEK = 6
    const val MAX_SCANS_PER_WEEK = 2
    const val MAX_OCR_SCANS_PER_WEEK = 12

    data class Limits(
        val plansPerWeek: Int?,
        val worksheetsPerWeek: Int?,
        val scansPerWeek: Int?,
        val maxClasses: Int?,
        val maxStudentsPerClass: Int,
        val ocrScansPerWeek: Int?,
    )

    data class Features(
        val unlimitedPlans: Boolean,
        val planModesAlways: Boolean,
        val planModesAfterUnitTest: Boolean,
        val baselineAssessment: Boolean,
        val endlineAssessment: Boolean,
        val unitTests: Boolean,
        val manualAssessmentEntry: Boolean,
        val fullAssessmentReports: Boolean,
        val shortActionPlan: Boolean,
        val fullActionPlan: Boolean,
        val baselinePlanBand: Boolean,
        val unitTlmKit: Boolean,
        val tlmGapListUnit: Boolean,
        val yearTlmListView: Boolean,
        val yearTlmListFull: Boolean,
        val yearTlmPdfShare: Boolean,
        val bulkPaperScan: Boolean,
        val aiAutoMark: Boolean,
        val reportPdfExport: Boolean,
        val worksheets: Boolean,
        val textbookScan: Boolean,
        val perStudentLongitudinal: Boolean,
        val abilityGroups: Boolean,
        val clusterExport: Boolean,
        val hindiUrduUi: Boolean,
        val gradesAvailable: List<Int>,
    )

    data class Usage(
        val week: String,
        val plans: Int,
        val worksheets: Int,
        val scans: Int,
        val ocrScans: Int,
    )

    fun parseTier(raw: String?): TierId =
        when (raw?.lowercase()) {
            "prime" -> TierId.PRIME
            "max" -> TierId.MAX
            else -> TierId.BASIC
        }

    fun defaultAccount(): TeacherAccount = TeacherAccount(
        tier = TierId.BASIC,
        limits = limitsFor(TierId.BASIC),
        features = featuresFor(TierId.BASIC),
        usage = Usage(week = "", plans = 0, worksheets = 0, scans = 0, ocrScans = 0),
        plansRemaining = BASIC_PLANS_PER_WEEK,
        subscription = SubscriptionInfo("none", null, null, null),
        paymentsEnabled = false,
    )

    fun tierRank(tier: TierId): Int = when (tier) {
        TierId.BASIC -> 0
        TierId.PRIME -> 1
        TierId.MAX -> 2
    }

    fun isAtLeast(current: TierId, required: TierId): Boolean =
        tierRank(current) >= tierRank(required)

    fun limitsFor(tier: TierId): Limits = when (tier) {
        TierId.BASIC -> Limits(BASIC_PLANS_PER_WEEK, 0, 0, 1, 45, 0)
        TierId.PRIME -> Limits(PRIME_PLANS_PER_WEEK, null, 0, 1, 45, PRIME_OCR_SCANS_PER_WEEK)
        TierId.MAX -> Limits(MAX_PLANS_PER_WEEK, null, MAX_SCANS_PER_WEEK, 2, 60, MAX_OCR_SCANS_PER_WEEK)
    }

    fun featuresFor(tier: TierId): Features {
        val fullGrades = listOf(1, 2, 3, 4, 5)
        return when (tier) {
            TierId.BASIC -> Features(
                unlimitedPlans = false,
                planModesAlways = true,
                planModesAfterUnitTest = true,
                baselineAssessment = true,
                endlineAssessment = true,
                unitTests = true,
                manualAssessmentEntry = true,
                fullAssessmentReports = false,
                shortActionPlan = true,
                fullActionPlan = false,
                baselinePlanBand = true,
                unitTlmKit = true,
                tlmGapListUnit = true,
                yearTlmListView = true,
                yearTlmListFull = false,
                yearTlmPdfShare = false,
                bulkPaperScan = false,
                aiAutoMark = false,
                reportPdfExport = false,
                worksheets = false,
                textbookScan = false,
                perStudentLongitudinal = false,
                abilityGroups = false,
                clusterExport = false,
                hindiUrduUi = true,
                gradesAvailable = fullGrades,
            )
            TierId.PRIME -> Features(
                unlimitedPlans = false,
                planModesAlways = true,
                planModesAfterUnitTest = true,
                baselineAssessment = true,
                endlineAssessment = true,
                unitTests = true,
                manualAssessmentEntry = true,
                fullAssessmentReports = true,
                shortActionPlan = true,
                fullActionPlan = true,
                baselinePlanBand = true,
                unitTlmKit = true,
                tlmGapListUnit = true,
                yearTlmListView = true,
                yearTlmListFull = true,
                yearTlmPdfShare = true,
                bulkPaperScan = true,
                aiAutoMark = true,
                reportPdfExport = false,
                worksheets = true,
                textbookScan = false,
                perStudentLongitudinal = true,
                abilityGroups = false,
                clusterExport = false,
                hindiUrduUi = true,
                gradesAvailable = fullGrades,
            )
            TierId.MAX -> Features(
                unlimitedPlans = false,
                planModesAlways = true,
                planModesAfterUnitTest = true,
                baselineAssessment = true,
                endlineAssessment = true,
                unitTests = true,
                manualAssessmentEntry = true,
                fullAssessmentReports = true,
                shortActionPlan = true,
                fullActionPlan = true,
                baselinePlanBand = true,
                unitTlmKit = true,
                tlmGapListUnit = true,
                yearTlmListView = true,
                yearTlmListFull = true,
                yearTlmPdfShare = true,
                bulkPaperScan = true,
                aiAutoMark = true,
                reportPdfExport = true,
                worksheets = true,
                textbookScan = true,
                perStudentLongitudinal = true,
                abilityGroups = true,
                clusterExport = true,
                hindiUrduUi = true,
                gradesAvailable = fullGrades,
            )
        }
    }

    fun canGeneratePlan(account: TeacherAccount): Boolean =
        account.plansRemaining == null || account.plansRemaining > 0

    fun gradeAllowed(account: TeacherAccount, grade: Int): Boolean =
        account.features.gradesAvailable.contains(grade)

    fun upgradeLabel(tier: TierId): String = when (tier) {
        TierId.BASIC -> "Prime"
        TierId.PRIME -> "Max"
        TierId.MAX -> "Max"
    }
}
