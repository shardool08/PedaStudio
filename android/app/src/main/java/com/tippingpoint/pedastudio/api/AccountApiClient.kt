package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.BuildConfig
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.TierConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AccountApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun fetchAccount(idToken: String?): Result<TeacherAccount> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.success(TierConfig.defaultAccount())

        val requestBuilder = Request.Builder().url("$base/api/me")
        if (!idToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $idToken")
        }

        return try {
            client.newCall(requestBuilder.get().build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.success(TierConfig.defaultAccount())
                }
                Result.success(parseAccount(JSONObject(text)))
            }
        } catch (_: Exception) {
            Result.success(TierConfig.defaultAccount())
        }
    }

    fun parseAccount(json: JSONObject): TeacherAccount {
        val tier = TierConfig.parseTier(json.optString("tier", "basic"))
        val limitsObj = json.optJSONObject("limits")
        val featuresObj = json.optJSONObject("features")
        val usageObj = json.optJSONObject("usage")
        val defaultLimits = TierConfig.limitsFor(tier)
        val defaultFeatures = TierConfig.featuresFor(tier)

        val limits = TierConfig.Limits(
            plansPerMonth = limitsObj?.optNullableInt("plansPerMonth") ?: defaultLimits.plansPerMonth,
            worksheetsPerMonth = limitsObj?.optNullableInt("worksheetsPerMonth") ?: defaultLimits.worksheetsPerMonth,
            scansPerMonth = limitsObj?.optNullableInt("scansPerMonth") ?: defaultLimits.scansPerMonth,
            maxClasses = limitsObj?.optNullableInt("maxClasses") ?: defaultLimits.maxClasses,
            maxStudentsPerClass = limitsObj?.optInt("maxStudentsPerClass", defaultLimits.maxStudentsPerClass)
                ?: defaultLimits.maxStudentsPerClass,
            ocrScansPerMonth = limitsObj?.optNullableInt("ocrScansPerMonth") ?: defaultLimits.ocrScansPerMonth,
        )

        val grades = featuresObj?.optJSONArray("gradesAvailable")?.toIntList()
            ?: defaultFeatures.gradesAvailable

        val features = TierConfig.Features(
            unlimitedPlans = featuresObj?.optBoolean("unlimitedPlans", defaultFeatures.unlimitedPlans) == true,
            planModesAlways = featuresObj?.optBoolean("planModesAlways", defaultFeatures.planModesAlways) == true,
            planModesAfterUnitTest = featuresObj?.optBoolean("planModesAfterUnitTest", defaultFeatures.planModesAfterUnitTest) == true,
            baselineAssessment = featuresObj?.optBoolean("baselineAssessment", defaultFeatures.baselineAssessment) == true,
            endlineAssessment = featuresObj?.optBoolean("endlineAssessment", defaultFeatures.endlineAssessment) == true,
            unitTests = featuresObj?.optBoolean("unitTests", defaultFeatures.unitTests) == true,
            manualAssessmentEntry = featuresObj?.optBoolean("manualAssessmentEntry", defaultFeatures.manualAssessmentEntry) == true,
            fullAssessmentReports = featuresObj?.optBoolean("fullAssessmentReports", defaultFeatures.fullAssessmentReports) == true,
            shortActionPlan = featuresObj?.optBoolean("shortActionPlan", defaultFeatures.shortActionPlan) == true,
            fullActionPlan = featuresObj?.optBoolean("fullActionPlan", defaultFeatures.fullActionPlan) == true,
            baselinePlanBand = featuresObj?.optBoolean("baselinePlanBand", defaultFeatures.baselinePlanBand) == true,
            unitTlmKit = featuresObj?.optBoolean("unitTlmKit", defaultFeatures.unitTlmKit) == true,
            tlmGapListUnit = featuresObj?.optBoolean("tlmGapListUnit", defaultFeatures.tlmGapListUnit) == true,
            yearTlmListView = featuresObj?.optBoolean("yearTlmListView", defaultFeatures.yearTlmListView) == true,
            yearTlmListFull = featuresObj?.optBoolean("yearTlmListFull", defaultFeatures.yearTlmListFull) == true,
            yearTlmPdfShare = featuresObj?.optBoolean("yearTlmPdfShare", defaultFeatures.yearTlmPdfShare) == true,
            bulkPaperScan = featuresObj?.optBoolean("bulkPaperScan", defaultFeatures.bulkPaperScan) == true,
            aiAutoMark = featuresObj?.optBoolean("aiAutoMark", defaultFeatures.aiAutoMark) == true,
            reportPdfExport = featuresObj?.optBoolean("reportPdfExport", defaultFeatures.reportPdfExport) == true,
            worksheets = featuresObj?.optBoolean("worksheets", defaultFeatures.worksheets) == true,
            textbookScan = featuresObj?.optBoolean("textbookScan", defaultFeatures.textbookScan) == true,
            perStudentLongitudinal = featuresObj?.optBoolean("perStudentLongitudinal", defaultFeatures.perStudentLongitudinal) == true,
            abilityGroups = featuresObj?.optBoolean("abilityGroups", defaultFeatures.abilityGroups) == true,
            clusterExport = featuresObj?.optBoolean("clusterExport", defaultFeatures.clusterExport) == true,
            hindiUrduUi = featuresObj?.optBoolean("hindiUrduUi", defaultFeatures.hindiUrduUi) == true,
            gradesAvailable = grades,
        )

        val usage = TierConfig.Usage(
            month = usageObj?.optString("month").orEmpty(),
            plans = usageObj?.optInt("plans", 0) ?: 0,
            worksheets = usageObj?.optInt("worksheets", 0) ?: 0,
            scans = usageObj?.optInt("scans", 0) ?: 0,
            ocrScans = usageObj?.optInt("ocrScans", 0) ?: 0,
        )

        val plansRemaining = if (json.has("plansRemaining") && !json.isNull("plansRemaining")) {
            json.optInt("plansRemaining")
        } else {
            limits.plansPerMonth?.let { maxOf(0, it - usage.plans) }
        }

        val subObj = json.optJSONObject("subscription")
        val expiresRaw = subObj?.optString("expiresAt").orEmpty()
        val expiresAt = runCatching {
            if (expiresRaw.isBlank()) null else java.time.Instant.parse(expiresRaw).toEpochMilli()
        }.getOrNull()

        return TeacherAccount(
            tier = tier,
            limits = limits,
            features = features,
            usage = usage,
            plansRemaining = plansRemaining,
            subscription = com.tippingpoint.pedastudio.data.SubscriptionInfo(
                status = subObj?.optString("status", "none") ?: "none",
                planId = subObj?.optString("planId"),
                billingCycle = subObj?.optString("billingCycle"),
                expiresAt = expiresAt,
            ),
            paymentsEnabled = json.optBoolean("paymentsEnabled", false),
            razorpayKeyId = json.optString("razorpayKeyId").takeIf { it.isNotBlank() },
            supportWhatsApp = json.optString("supportWhatsApp", "919876543210"),
            supportEmail = json.optString("supportEmail", "support@pedastudio.in"),
        )
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    private fun JSONArray.toIntList(): List<Int> {
        val out = mutableListOf<Int>()
        for (i in 0 until length()) out.add(getInt(i))
        return out
    }
}
