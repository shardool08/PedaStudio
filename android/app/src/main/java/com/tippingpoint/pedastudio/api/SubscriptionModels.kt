package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.TierConfig

data class PaidPlanOffer(
    val id: String,
    val tier: TierConfig.TierId,
    val billingCycle: String,
    val amountInr: Int,
    val amountPaise: Int,
    val periodLabel: String,
    val savingsInr: Int?,
    val monthlyEquivalentInr: Int? = null,
)

data class TierMarketing(
    val tier: TierConfig.TierId,
    val label: String,
    val tagline: String,
    val highlights: List<String>,
    val badge: String?,
)

data class TierComparisonRow(
    val feature: String,
    val basic: String,
    val prime: String,
    val max: String,
)

data class SubscriptionCatalog(
    val paymentsEnabled: Boolean,
    val razorpayKeyId: String?,
    val razorpayTestMode: Boolean = false,
    val supportWhatsApp: String,
    val supportEmail: String,
    val marketing: List<TierMarketing>,
    val comparison: List<TierComparisonRow> = emptyList(),
    val plans: List<PaidPlanOffer>,
)

data class CheckoutOrder(
    val orderId: String,
    val amountPaise: Int,
    val amountInr: Int,
    val currency: String,
    val keyId: String?,
    val planId: String,
    val planLabel: String,
    val prefillName: String,
    val prefillContact: String = "",
)

data class PaymentSuccess(
    val paymentId: String,
    val orderId: String,
    val signature: String,
    val planId: String,
    val account: TeacherAccount,
)
