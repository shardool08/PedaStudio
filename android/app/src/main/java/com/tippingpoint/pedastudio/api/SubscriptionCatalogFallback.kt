package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.data.TierConfig

/**
 * Bundled subscription catalog when the API is unreachable or returns partial data.
 * Keep in sync with lib/subscription-config.ts (PAID_PLANS, TIER_MARKETING, TIER_COMPARISON).
 */
object SubscriptionCatalogFallback {
    fun catalog(): SubscriptionCatalog = SubscriptionCatalog(
        paymentsEnabled = false,
        razorpayKeyId = null,
        razorpayTestMode = false,
        supportWhatsApp = "919876543210",
        supportEmail = "support@pedastudio.in",
        marketing = marketing,
        comparison = comparison,
        plans = plans,
    )

    fun merge(remote: SubscriptionCatalog?): SubscriptionCatalog {
        val base = catalog()
        if (remote == null) return base
        return remote.copy(
            marketing = remote.marketing.ifEmpty { base.marketing },
            comparison = remote.comparison.ifEmpty { base.comparison },
            plans = remote.plans.ifEmpty { base.plans },
            supportWhatsApp = remote.supportWhatsApp.ifBlank { base.supportWhatsApp },
            supportEmail = remote.supportEmail.ifBlank { base.supportEmail },
        )
    }

    private val plans = listOf(
        PaidPlanOffer("prime_monthly", TierConfig.TierId.PRIME, "monthly", 129, 12900, "per month", null),
        PaidPlanOffer("prime_yearly", TierConfig.TierId.PRIME, "yearly", 999, 99900, "per year", 549, 83),
        PaidPlanOffer("max_monthly", TierConfig.TierId.MAX, "monthly", 279, 27900, "per month", null),
        PaidPlanOffer("max_yearly", TierConfig.TierId.MAX, "yearly", 2299, 229900, "per year", 1049, 192),
    )

    private val marketing = listOf(
        TierMarketing(
            tier = TierConfig.TierId.BASIC,
            label = "Basic",
            tagline = "Free — all grades, two plans a week",
            highlights = listOf(
                "2 lesson plans per week",
                "All Grades 1–5 English",
                "Unit tests + annual baseline & endline",
                "Re-teach when students need more help",
            ),
            badge = null,
        ),
        TierMarketing(
            tier = TierConfig.TierId.PRIME,
            label = "Prime",
            tagline = "All Grades 1–5 · ₹2.75/day on annual plan",
            highlights = listOf(
                "6 lesson plans per week",
                "Worksheet with every plan you make",
                "Learning-based plans from your test scores",
                "Bulk answer-sheet marking + skill reports",
            ),
            badge = "Best value",
        ),
        TierMarketing(
            tier = TierConfig.TierId.MAX,
            label = "Max",
            tagline = "Scan-to-plan & cluster reports for SRG",
            highlights = listOf(
                "Everything in Prime",
                "2 textbook scans per week → instant lesson plan",
                "School cluster report pack for SRG / Head Master",
                "Unlimited worksheets + PDF skill reports",
            ),
            badge = null,
        ),
    )

    val comparison: List<TierComparisonRow> = listOf(
        TierComparisonRow("Grades (English)", "All Grades 1–5", "All Grades 1–5", "All Grades 1–5"),
        TierComparisonRow("Lesson plans", "2 per week", "6 per week", "6 per week"),
        TierComparisonRow("Re-teach, practice & continue", "✓", "✓", "✓"),
        TierComparisonRow("Worksheets", "—", "1 with each lesson plan", "Unlimited"),
        TierComparisonRow("Unit tests", "✓", "✓", "✓"),
        TierComparisonRow("Annual assessment (baseline & endline)", "✓", "✓", "✓"),
        TierComparisonRow("Student skill report", "Class summary", "Full class skill map", "Full + PDF export"),
        TierComparisonRow("Learning-based lesson plans", "—", "From your test scores", "Priority tailoring"),
        TierComparisonRow("Bulk answer-sheet marking", "—", "✓", "✓ + faster queue"),
        TierComparisonRow("Scan & plan (textbook photo)", "—", "—", "2 per week"),
        TierComparisonRow("School cluster report pack", "—", "—", "✓"),
    )
}
