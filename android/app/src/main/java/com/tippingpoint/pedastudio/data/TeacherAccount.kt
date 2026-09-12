package com.tippingpoint.pedastudio.data

data class SubscriptionInfo(
    val status: String,
    val planId: String?,
    val billingCycle: String?,
    val expiresAt: Long?,
) {
    val isActive: Boolean get() = status == "active" && (expiresAt == null || expiresAt > System.currentTimeMillis())
    val isTrial: Boolean get() = planId == "max_trial" && isActive
}

data class TeacherAccount(
    val tier: TierConfig.TierId,
    val limits: TierConfig.Limits,
    val features: TierConfig.Features,
    val usage: TierConfig.Usage,
    val plansRemaining: Int?,
    val scansRemaining: Int? = null,
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
