package com.tippingpoint.pedastudio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.api.TierComparisonRow
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.TierConfig
import com.tippingpoint.pedastudio.i18n.AppStrings
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.BgTint
import com.tippingpoint.pedastudio.ui.theme.NavBg
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.PrimarySteel
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import com.tippingpoint.pedastudio.ui.theme.WarmPeach
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TierBadge(tier: TierConfig.TierId, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tier) {
        TierConfig.TierId.BASIC -> Color(0xFFE8EEF2) to PrimarySteel
        TierConfig.TierId.PRIME -> AccentTeal to Color.White
        TierConfig.TierId.MAX -> Color(0xFF496580) to Color.White
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
    ) {
        Text(
            tier.label.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun MembershipHeroCard(
    s: AppStrings,
    account: TeacherAccount,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (account.tier == TierConfig.TierId.BASIC) BgTint else WarmPeach.copy(0.35f),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TierBadge(account.tier)
                if (account.subscription.isTrial) {
                    Text(s.subTrialActive.format(trialDaysLeft(account)), color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                } else if (account.subscription.isActive) {
                    Text(s.subActive, color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                when (account.tier) {
                    TierConfig.TierId.BASIC -> s.subBasicTagline
                    TierConfig.TierId.PRIME -> s.subPrimeTagline
                    TierConfig.TierId.MAX -> s.subMaxTagline
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryDark,
            )
            val usageText = if (account.plansRemaining == null) {
                s.tierPlansUnlimited
            } else {
                s.tierPlansRemaining.format(account.plansRemaining)
            }
            Text(usageText, fontSize = 13.sp, color = PrimarySteel.copy(0.8f))
            account.subscription.expiresAt?.let { exp ->
                if (account.subscription.isActive) {
                    val fmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    Text(
                        s.subRenewsOn.format(fmt.format(Date(exp))),
                        fontSize = 12.sp,
                        color = PrimarySteel.copy(0.65f),
                    )
                }
            }
        }
    }
}

@Composable
fun BillingCycleToggle(
    s: AppStrings,
    yearly: Boolean,
    onYearlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(selected = !yearly, onClick = { onYearlyChange(false) }, label = { Text(s.subMonthly) })
        Spacer(Modifier.width(8.dp))
        FilterChip(selected = yearly, onClick = { onYearlyChange(true) }, label = { Text(s.subYearlySave) })
    }
}

@Composable
fun PlanOfferCard(
    s: AppStrings,
    tier: TierConfig.TierId,
    tagline: String,
    highlights: List<String>,
    badge: String?,
    priceLabel: String?,
    periodLabel: String?,
    savingsLabel: String?,
    monthlyEquivalentInr: Int? = null,
    isCurrent: Boolean,
    isIncluded: Boolean = false,
    isRecommended: Boolean,
    buttonText: String,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = when {
        isCurrent -> PrimarySteel
        isRecommended -> AccentTeal
        else -> SeasideBorder
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (isRecommended) 2.dp else 1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRecommended) 3.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tier.label, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryDark)
                    if (isRecommended) {
                        Icon(Icons.Default.Star, null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                    }
                }
                badge?.let {
                    Surface(shape = RoundedCornerShape(8.dp), color = WarmPeach) {
                        Text(it, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(tagline, fontSize = 13.sp, color = PrimarySteel.copy(0.85f))
            highlights.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Check, null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                    Text(line, fontSize = 13.sp, color = PrimaryDark.copy(0.85f))
                }
            }
            if (priceLabel != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("₹$priceLabel", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    periodLabel?.let {
                        Text(" / $it", fontSize = 13.sp, color = PrimarySteel.copy(0.7f), modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                savingsLabel?.let {
                    Text(it, fontSize = 12.sp, color = AccentTeal, fontWeight = FontWeight.Medium)
                }
                monthlyEquivalentInr?.let { equiv ->
                    Text(
                        s.subYearlyEquivalent.format(equiv),
                        fontSize = 12.sp,
                        color = PrimarySteel.copy(0.7f),
                    )
                }
            } else {
                Text(s.subFreeForever, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
            }
            when {
                isCurrent -> {
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text(s.subCurrentPlan)
                    }
                }
                isIncluded -> {
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text(s.subIncludedInPlan)
                    }
                }
                tier == TierConfig.TierId.BASIC && !isIncluded -> {
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text(s.subFreeForever)
                    }
                }
                else -> {
                    Button(
                        onClick = onSelect,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRecommended) AccentTeal else PrimaryDark),
                    ) {
                        Text(buttonText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun PlanFeatureComparisonTable(
    s: AppStrings,
    rows: List<TierComparisonRow> = defaultComparisonRows(),
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(s.subCompareTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryDark)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.weight(1.4f), fontSize = 11.sp)
                Text("Basic", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimarySteel)
                Text("Prime", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
                Text("Max", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
            }
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.feature, modifier = Modifier.weight(1.4f), fontSize = 12.sp, color = PrimaryDark)
                    Text(row.basic, modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = PrimarySteel.copy(0.85f))
                    Text(row.prime, modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = PrimarySteel.copy(0.85f))
                    Text(row.max, modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = PrimarySteel.copy(0.85f))
                }
            }
        }
    }
}

private fun defaultComparisonRows(): List<TierComparisonRow> = listOf(
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

private fun trialDaysLeft(account: TeacherAccount): Int {
    val expires = account.subscription.expiresAt ?: return MAX_TRIAL_DAYS
    val msLeft = expires - System.currentTimeMillis()
    return maxOf(1, ((msLeft + 86_399_999) / 86_400_000).toInt())
}

private const val MAX_TRIAL_DAYS = 7

@Composable
fun UpgradeBanner(
    s: AppStrings,
    message: String,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavBg),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.Lock, null, tint = AccentTeal)
            Column(modifier = Modifier.weight(1f)) {
                Text(message, fontSize = 13.sp, color = PrimaryDark, fontWeight = FontWeight.Medium)
                Text(s.tierUpgradeHint, fontSize = 11.sp, color = PrimarySteel.copy(0.7f))
            }
            Button(
                onClick = onUpgrade,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
            ) {
                Text(s.subUpgrade, fontSize = 12.sp)
            }
        }
    }
}
