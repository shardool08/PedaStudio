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

    fun fetchAccount(idToken: String?, cached: TeacherAccount? = null): Result<TeacherAccount> {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) return Result.success(cached ?: TierConfig.defaultAccount())

        val requestBuilder = Request.Builder().url("$base/api/me")
        if (!idToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $idToken")
        }

        return try {
            client.newCall(requestBuilder.get().build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.success(cached ?: TierConfig.defaultAccount())
                }
                val incoming = parseAccount(JSONObject(text))
                Result.success(if (cached != null) mergeAccountUpdate(cached, incoming) else incoming)
            }
        } catch (_: Exception) {
            Result.success(cached ?: TierConfig.defaultAccount())
        }
    }

    fun mergeAccountUpdate(current: TeacherAccount, incoming: TeacherAccount): TeacherAccount {
        val isRealApiAccount = incoming.usage.week.isNotBlank() ||
            incoming.tier != TierConfig.TierId.BASIC ||
            incoming.subscription.status != "none" ||
            incoming.paymentsEnabled
        val tier = when {
            !isRealApiAccount && TierConfig.tierRank(current.tier) > TierConfig.tierRank(incoming.tier) -> current.tier
            TierConfig.tierRank(incoming.tier) >= TierConfig.tierRank(current.tier) -> incoming.tier
            else -> incoming.tier
        }
        val tierFeatures = TierConfig.featuresFor(tier)
        val tierLimits = TierConfig.limitsFor(tier)
        return incoming.copy(
            tier = tier,
            limits = tierLimits,
            features = tierFeatures.copy(
                gradesAvailable = incoming.features.gradesAvailable.ifEmpty { tierFeatures.gradesAvailable },
            ),
            usage = incoming.usage,
            plansRemaining = incoming.plansRemaining ?: current.plansRemaining,
            scansRemaining = incoming.scansRemaining ?: current.scansRemaining,
            subscription = incoming.subscription,
            paymentsEnabled = incoming.paymentsEnabled || current.paymentsEnabled,
            razorpayKeyId = incoming.razorpayKeyId ?: current.razorpayKeyId,
            supportWhatsApp = incoming.supportWhatsApp.ifBlank { current.supportWhatsApp },
            supportEmail = incoming.supportEmail.ifBlank { current.supportEmail },
        )
    }

    fun accountToJson(account: TeacherAccount): JSONObject = JSONObject().apply {
        put("tier", account.tier.key)
        put("plansRemaining", account.plansRemaining ?: JSONObject.NULL)
        put("paymentsEnabled", account.paymentsEnabled)
        account.razorpayKeyId?.let { put("razorpayKeyId", it) }
        put("supportWhatsApp", account.supportWhatsApp)
        put("supportEmail", account.supportEmail)
        put("usage", JSONObject().apply {
            put("week", account.usage.week)
            put("plans", account.usage.plans)
            put("worksheets", account.usage.worksheets)
            put("scans", account.usage.scans)
            put("ocrScans", account.usage.ocrScans)
        })
        put("subscription", JSONObject().apply {
            put("status", account.subscription.status)
            account.subscription.planId?.let { put("planId", it) }
            account.subscription.billingCycle?.let { put("billingCycle", it) }
            account.subscription.expiresAt?.let { put("expiresAt", java.time.Instant.ofEpochMilli(it).toString()) }
        })
    }

    fun parseAccount(json: JSONObject): TeacherAccount {
        val tier = resolveTier(json)
        val hasExplicitTier = json.has("tier") && !json.isNull("tier") &&
            json.optString("tier").isNotBlank()
        val limitsObj = json.optJSONObject("limits")
        val featuresObj = json.optJSONObject("features")
        val usageObj = json.optJSONObject("usage")
        val defaultLimits = TierConfig.limitsFor(tier)
        val defaultFeatures = TierConfig.featuresFor(tier)

        val limits = TierConfig.Limits(
            plansPerWeek = limitsObj?.optWeeklyLimit("plansPerWeek", "plansPerMonth") ?: defaultLimits.plansPerWeek,
            worksheetsPerWeek = limitsObj?.optWeeklyLimit("worksheetsPerWeek", "worksheetsPerMonth") ?: defaultLimits.worksheetsPerWeek,
            scansPerWeek = limitsObj?.optWeeklyLimit("scansPerWeek", "scansPerMonth") ?: defaultLimits.scansPerWeek,
            maxClasses = limitsObj?.optNullableInt("maxClasses") ?: defaultLimits.maxClasses,
            maxStudentsPerClass = limitsObj?.optInt("maxStudentsPerClass", defaultLimits.maxStudentsPerClass)
                ?: defaultLimits.maxStudentsPerClass,
            ocrScansPerWeek = limitsObj?.optWeeklyLimit("ocrScansPerWeek", "ocrScansPerMonth") ?: defaultLimits.ocrScansPerWeek,
        )

        val grades = featuresObj?.optJSONArray("gradesAvailable")?.toIntList()
            ?: defaultFeatures.gradesAvailable

        val parsedFeatures = TierConfig.Features(
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

        val features = if (hasExplicitTier) {
            defaultFeatures.copy(gradesAvailable = grades.ifEmpty { defaultFeatures.gradesAvailable })
        } else {
            parsedFeatures
        }

        val usage = TierConfig.Usage(
            week = usageObj?.optString("week").orEmpty(),
            plans = usageObj?.optInt("plans", 0) ?: 0,
            worksheets = usageObj?.optInt("worksheets", 0) ?: 0,
            scans = usageObj?.optInt("scans", 0) ?: 0,
            ocrScans = usageObj?.optInt("ocrScans", 0) ?: 0,
        )

        val plansRemaining = if (json.has("plansRemaining") && !json.isNull("plansRemaining")) {
            json.optInt("plansRemaining")
        } else {
            limits.plansPerWeek?.let { maxOf(0, it - usage.plans) }
        }

        val scansRemaining = if (json.has("scansRemaining") && !json.isNull("scansRemaining")) {
            json.optInt("scansRemaining")
        } else {
            limits.scansPerWeek?.let { maxOf(0, it - usage.scans) }
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
            scansRemaining = scansRemaining,
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

    private fun resolveTier(json: JSONObject): TierConfig.TierId {
        // Top-level tier from /api/me is authoritative (handles expired subscriptions).
        json.optString("tier").takeIf { it.isNotBlank() }?.let { return TierConfig.parseTier(it) }
        val subTier = json.optJSONObject("subscription")?.optString("tier")?.takeIf { it.isNotBlank() }
        return TierConfig.parseTier(subTier)
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    /** Reads weekly limit keys; falls back to legacy monthly keys during rollout. */
    private fun JSONObject.optWeeklyLimit(weekKey: String, legacyMonthKey: String): Int? {
        optNullableInt(weekKey)?.let { return it }
        return optNullableInt(legacyMonthKey)
    }

    private fun JSONArray.toIntList(): List<Int> {
        val out = mutableListOf<Int>()
        for (i in 0 until length()) out.add(getInt(i))
        return out
    }
}
