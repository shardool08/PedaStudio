package com.tippingpoint.pedastudio.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.api.SubscriptionApiClient
import com.tippingpoint.pedastudio.api.SubscriptionCatalog
import com.tippingpoint.pedastudio.api.SubscriptionCatalogFallback
import com.tippingpoint.pedastudio.api.TierMarketing
import com.tippingpoint.pedastudio.auth.PhoneAuthController
import com.tippingpoint.pedastudio.billing.RazorpayPaymentHandler
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.TierConfig
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.components.BillingCycleToggle
import com.tippingpoint.pedastudio.ui.components.PlanFeatureComparisonTable
import com.tippingpoint.pedastudio.ui.components.MembershipHeroCard
import com.tippingpoint.pedastudio.ui.components.PlanOfferCard
import com.tippingpoint.pedastudio.ui.components.RegisterScaffold
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.PrimarySteel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SubscriptionScreen(
    prefs: UserPreferences,
    auth: PhoneAuthController,
    paymentHandler: RazorpayPaymentHandler,
    account: TeacherAccount,
    highlightTier: TierConfig.TierId?,
    onBack: () -> Unit,
    onAccountUpdated: (TeacherAccount) -> Unit,
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var catalog by remember { mutableStateOf(SubscriptionCatalogFallback.catalog()) }
    var loading by remember { mutableStateOf(true) }
    var paying by remember { mutableStateOf(false) }
    var yearly by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }

    val paymentsLive = remember(catalog, account) {
        val key = catalog?.razorpayKeyId?.takeIf { it.isNotBlank() }
            ?: account.razorpayKeyId?.takeIf { it.isNotBlank() }
        val enabled = catalog?.paymentsEnabled == true || account.paymentsEnabled
        enabled && !key.isNullOrBlank()
    }
    val razorpayKey = catalog?.razorpayKeyId?.takeIf { it.isNotBlank() } ?: account.razorpayKeyId

    fun reloadCatalog() {
        scope.launch {
            loading = true
            error = ""
            val result = withContext(Dispatchers.IO) { SubscriptionApiClient.fetchCatalog() }
            catalog = SubscriptionCatalogFallback.merge(result.getOrNull())
            if (result.isFailure) {
                error = if (result.getOrNull() == null) {
                    s.subCatalogOffline
                } else {
                    result.exceptionOrNull()?.message ?: s.subLoadError
                }
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reloadCatalog() }

    fun contactWhatsApp() {
        val phone = account.supportWhatsApp.ifBlank { catalog?.supportWhatsApp ?: "919876543210" }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=${Uri.encode(s.subWhatsAppPrefill)}"))
        context.startActivity(intent)
    }

    fun startPurchase(tier: TierConfig.TierId) {
        if (tier == TierConfig.TierId.BASIC) return
        if (TierConfig.tierRank(account.tier) >= TierConfig.tierRank(tier)) return

        if (!paymentsLive) {
            error = s.subPaymentsOffline
            return
        }

        val cat = catalog
        val cycle = if (yearly) "yearly" else "monthly"
        val plan = cat?.plans?.firstOrNull { it.tier == tier && it.billingCycle == cycle }
        if (plan == null) {
            error = s.subPlanUnavailable
            return
        }
        paying = true
        error = ""
        scope.launch {
            val orderResult = withContext(Dispatchers.IO) {
                SubscriptionApiClient.createOrder(plan.id, prefs.teacherName, prefs.phoneNumber, auth.getIdToken())
            }
            orderResult.fold(
                onSuccess = { order ->
                    paymentHandler.startCheckout(order, razorpayKey) { payResult ->
                        payResult.fold(
                            onSuccess = { payment ->
                                scope.launch {
                                    val verifyResult = withContext(Dispatchers.IO) {
                                        SubscriptionApiClient.verifyPayment(
                                            payment.orderId,
                                            payment.paymentId,
                                            payment.signature,
                                            payment.planId,
                                            auth.getIdToken(),
                                        )
                                    }
                                    paying = false
                                    verifyResult.fold(
                                        onSuccess = { ok ->
                                            success = s.subPaymentSuccess.format(ok.account.tier.label)
                                            onAccountUpdated(ok.account)
                                            prefs.applyTeacherAccount(ok.account)
                                        },
                                        onFailure = { e -> error = e.message ?: s.subPaymentFailed },
                                    )
                                }
                            },
                            onFailure = { e ->
                                paying = false
                                if (!e.message.orEmpty().contains("cancel", ignoreCase = true)) {
                                    error = e.message ?: s.subPaymentFailed
                                }
                            },
                        )
                    }
                },
                onFailure = { e ->
                    paying = false
                    error = e.message ?: s.subPaymentFailed
                },
            )
        }
    }

    RegisterScaffold(
        title = s.subTitle,
        stepLabel = s.subSubtitle,
        buttonText = s.closeBtn,
        canContinue = !paying,
        onBack = if (paying) null else onBack,
        onContinue = onBack,
    ) {
        if (loading) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = AccentTeal, modifier = Modifier.height(24.dp))
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MembershipHeroCard(s = s, account = account)

            if (success.isNotBlank()) {
                Text(success, color = AccentTeal, fontSize = 14.sp)
            }
            if (error.isNotBlank()) {
                Text(error, color = Color(0xFFC62828), fontSize = 13.sp)
                TextButton(onClick = { reloadCatalog() }, enabled = !loading) {
                    Text("Retry", color = AccentTeal)
                }
            }

            BillingCycleToggle(s = s, yearly = yearly, onYearlyChange = { yearly = it })

            if (catalog?.razorpayTestMode == true) {
                Text(
                    "Test payment mode — Razorpay card 4111 1111 1111 1111, any future expiry & CVV",
                    fontSize = 11.sp,
                    color = PrimarySteel.copy(0.65f),
                )
            }

            PlanFeatureComparisonTable(
                s = s,
                rows = catalog?.comparison?.takeIf { it.isNotEmpty() }
                    ?: SubscriptionCatalogFallback.comparison,
            )

            val marketing = catalog?.marketing.orEmpty()
            fun m(tier: TierConfig.TierId): TierMarketing? = marketing.find { it.tier == tier }

            val accountRank = TierConfig.tierRank(account.tier)
            fun isCurrentTier(card: TierConfig.TierId) = account.tier == card
            fun isIncludedTier(card: TierConfig.TierId) =
                accountRank > TierConfig.tierRank(card)
            fun canPurchase(card: TierConfig.TierId) =
                accountRank < TierConfig.tierRank(card) && paymentsLive && !paying

            PlanOfferCard(
                s = s,
                tier = TierConfig.TierId.BASIC,
                tagline = m(TierConfig.TierId.BASIC)?.tagline ?: s.subBasicTagline,
                highlights = m(TierConfig.TierId.BASIC)?.highlights ?: emptyList(),
                badge = null,
                priceLabel = null,
                periodLabel = null,
                savingsLabel = null,
                isCurrent = isCurrentTier(TierConfig.TierId.BASIC),
                isIncluded = isIncludedTier(TierConfig.TierId.BASIC),
                isRecommended = false,
                buttonText = s.subFreeForever,
                enabled = false,
                onSelect = {},
            )

            val primePlan = catalog?.plans?.firstOrNull {
                it.tier == TierConfig.TierId.PRIME && it.billingCycle == if (yearly) "yearly" else "monthly"
            }
            PlanOfferCard(
                s = s,
                tier = TierConfig.TierId.PRIME,
                tagline = m(TierConfig.TierId.PRIME)?.tagline ?: "",
                highlights = m(TierConfig.TierId.PRIME)?.highlights ?: emptyList(),
                badge = m(TierConfig.TierId.PRIME)?.badge ?: s.subMostPopular,
                priceLabel = primePlan?.amountInr?.toString(),
                periodLabel = primePlan?.periodLabel,
                savingsLabel = if (yearly) primePlan?.savingsInr?.let { s.subSaveInr.format(it) } else null,
                monthlyEquivalentInr = if (yearly) primePlan?.monthlyEquivalentInr else null,
                isCurrent = isCurrentTier(TierConfig.TierId.PRIME),
                isIncluded = isIncludedTier(TierConfig.TierId.PRIME),
                isRecommended = (highlightTier == TierConfig.TierId.PRIME ||
                    (highlightTier == null && accountRank == TierConfig.tierRank(TierConfig.TierId.BASIC))) &&
                    !isCurrentTier(TierConfig.TierId.PRIME),
                buttonText = s.subSubscribePrime,
                enabled = canPurchase(TierConfig.TierId.PRIME),
                onSelect = { startPurchase(TierConfig.TierId.PRIME) },
            )

            val maxPlan = catalog?.plans?.firstOrNull {
                it.tier == TierConfig.TierId.MAX && it.billingCycle == if (yearly) "yearly" else "monthly"
            }
            PlanOfferCard(
                s = s,
                tier = TierConfig.TierId.MAX,
                tagline = m(TierConfig.TierId.MAX)?.tagline ?: "",
                highlights = m(TierConfig.TierId.MAX)?.highlights ?: emptyList(),
                badge = null,
                priceLabel = maxPlan?.amountInr?.toString(),
                periodLabel = maxPlan?.periodLabel,
                savingsLabel = if (yearly) maxPlan?.savingsInr?.let { s.subSaveInr.format(it) } else null,
                monthlyEquivalentInr = if (yearly) maxPlan?.monthlyEquivalentInr else null,
                isCurrent = isCurrentTier(TierConfig.TierId.MAX),
                isIncluded = false,
                isRecommended = (highlightTier == TierConfig.TierId.MAX ||
                    (highlightTier == null && accountRank == TierConfig.tierRank(TierConfig.TierId.PRIME))) &&
                    !isCurrentTier(TierConfig.TierId.MAX),
                buttonText = s.subSubscribeMax,
                enabled = canPurchase(TierConfig.TierId.MAX),
                onSelect = { startPurchase(TierConfig.TierId.MAX) },
            )

            if (!paymentsLive) {
                Text(s.subPaymentsOffline, fontSize = 12.sp, color = PrimarySteel.copy(0.75f))
                TextButton(onClick = { contactWhatsApp() }) {
                    Text(s.subWhatsAppSupport, color = AccentTeal)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
