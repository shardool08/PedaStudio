package com.tippingpoint.pedastudio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.data.TlmKitSummary
import com.tippingpoint.pedastudio.data.TlmResourceCatalog
import com.tippingpoint.pedastudio.i18n.AppStrings
import com.tippingpoint.pedastudio.ui.theme.AccentLight
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.PrimarySteel
import com.tippingpoint.pedastudio.ui.theme.WarmPeach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TlmKitHighlightCard(
    s: AppStrings,
    kit: TlmKitSummary,
    unit: Int,
    tlmCatalog: TlmResourceCatalog,
    onOpenFullKit: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val missing = kit.items.filter { !it.owned }
    val essentialMissing = missing.filter { it.essential }
    val needsAttention = kit.gapCount > 0
    val bg = if (needsAttention) WarmPeach else AccentLight.copy(alpha = 0.35f)

    if (compact) {
        Card(
            onClick = onOpenFullKit,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = if (needsAttention) PrimaryDark else AccentTeal,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        s.homeTlmKitHeadline.format(unit),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = PrimaryDark,
                        maxLines = 1,
                    )
                    Text(
                        if (needsAttention) {
                            s.homeTlmGapAlert.format(kit.gapCount, kit.items.size)
                        } else {
                            s.tlmKitSummary.format(kit.ownedCount, kit.items.size)
                        },
                        fontSize = 11.sp,
                        color = if (needsAttention) PrimaryDark.copy(0.85f) else PrimarySteel.copy(0.75f),
                        maxLines = 1,
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (needsAttention) PrimaryDark else AccentTeal,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = if (needsAttention) PrimaryDark else AccentTeal,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        s.homeTlmKitHeadline.format(unit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryDark,
                    )
                    Text(
                        if (needsAttention) {
                            s.homeTlmGapAlert.format(kit.gapCount, kit.items.size)
                        } else {
                            s.tlmKitSummary.format(kit.ownedCount, kit.items.size)
                        },
                        fontSize = 13.sp,
                        color = if (needsAttention) PrimaryDark.copy(0.85f) else PrimarySteel.copy(0.8f),
                        fontWeight = if (needsAttention) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }

            if (needsAttention && !compact) {
                val preview = (essentialMissing.ifEmpty { missing }).take(3)
                preview.forEach { item ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tlmCatalog.emojiFor(item.id), fontSize = 18.sp)
                        Text(
                            item.label,
                            fontSize = 13.sp,
                            color = PrimaryDark,
                            fontWeight = if (item.essential) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        if (item.essential) {
                            Text(s.tlmKitEssential, fontSize = 10.sp, color = AccentTeal)
                        }
                    }
                }
            } else if (!needsAttention && !compact) {
                Text(s.homeTlmReady, fontSize = 12.sp, color = AccentTeal, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = onOpenFullKit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (needsAttention) PrimaryDark else AccentTeal,
                ),
            ) {
                Text(s.homeTlmViewAll, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.padding(start = 4.dp).size(18.dp))
            }
        }
    }
}
