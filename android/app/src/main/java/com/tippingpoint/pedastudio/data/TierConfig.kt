package com.tippingpoint.pedastudio.data

/** Mirrors lib/tier-config.ts for client-side UI gating. */
object TierConfig {
    enum class TierId(val key: String, val label: String) {
        BASIC("basic", "Basic"),
        PRIME("prime", "Prime"),
        MAX("max", "Max"),
    }

    data class Limits(
        val plansPerMonth: Int?,
        val worksheetsPerMonth: Int?,
        val scansPerMonth: Int?,
        val maxClasses: Int?,
        val maxStudentsPerClass: Int,
        val ocrScansPerMonth: Int?,
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
        val month: String,
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
        usage = Usage(month = "", plans = 0, worksheets = 0, scans = 0, ocrScans = 0),
        plansRemaining = 20,
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
        TierId.BASIC -> Limits(20, 0, 0, 1, 45, 0)
        TierId.PRIME -> Limits(null, 10, 15, 2, 45, 120)
        TierId.MAX -> Limits(null, null, 60, null, 60, null)
    }

    fun featuresFor(tier: TierId): Features {
        val basicGrades = listOf(1, 2, 3)
        val primeGrades = listOf(1, 2, 3, 4, 5)
        val maxGrades = listOf(1, 2, 3, 4, 5, 6, 7, 8)
        return when (tier) {
            TierId.BASIC -> Features(
                unlimitedPlans = false,
                planModesAlways = false,
                planModesAfterUnitTest = true,
                baselineAssessment = true,
                endlineAssessment = true,
                unitTests = true,
                manualAssessmentEntry = true,
                fullAssessmentReports = true,
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
                hindiUrduUi = false,
                gradesAvailable = basicGrades,
            )
            TierId.PRIME -> Features(
                unlimitedPlans = true,
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
                abilityGroups = false,
                clusterExport = false,
                hindiUrduUi = true,
                gradesAvailable = primeGrades,
            )
            TierId.MAX -> Features(
                unlimitedPlans = true,
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
                gradesAvailable = maxGrades,
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

data class SubscriptionInfo(
    val status: String,
    val planId: String?,
    val billingCycle: String?,
    val expiresAt: Long?,
) {
    val isActive: Boolean get() = status == "active" && (expiresAt == null || expiresAt > System.currentTimeMillis())
}

data class TeacherAccount(
    val tier: TierConfig.TierId,
    val limits: TierConfig.Limits,
    val features: TierConfig.Features,
    val usage: TierConfig.Usage,
    val plansRemaining: Int?,
    val subscription: SubscriptionInfo = SubscriptionInfo("none", null, null, null),
    val paymentsEnabled: Boolean = false,
    val razorpayKeyId: String? = null,
    val supportWhatsApp: String = "919876543210",
    val supportEmail: String = "support@pedastudio.in",
) {
    fun hasFeature(feature: (TierConfig.Features) -> Boolean): Boolean = feature(features)

    fun needsUpgradeForPrime(): Boolean = tier == TierConfig.TierId.BASIC

    fun needsUpgradeForMax(): Boolean = tier != TierConfig.TierId.MAX
}
