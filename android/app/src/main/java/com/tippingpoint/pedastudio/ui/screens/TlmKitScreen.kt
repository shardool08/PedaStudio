package com.tippingpoint.pedastudio.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.data.CurriculumRepository
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.TlmKitRepository
import com.tippingpoint.pedastudio.data.TlmKitSummary
import com.tippingpoint.pedastudio.data.TlmResourceCatalog
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.components.InfoBannerCard
import com.tippingpoint.pedastudio.ui.components.RegisterScaffold
import com.tippingpoint.pedastudio.ui.components.UpgradeBanner
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.BgTint
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import com.tippingpoint.pedastudio.ui.theme.WarmPeach

@Composable
fun TlmKitScreen(
    grade: Int,
    initialUnit: Int,
    prefs: UserPreferences,
    curriculum: CurriculumRepository,
    tlmCatalog: TlmResourceCatalog,
    teacherAccount: TeacherAccount,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val repo = remember { TlmKitRepository(curriculum, tlmCatalog) }
    var yearView by remember { mutableStateOf(false) }
    var unit by remember { mutableIntStateOf(initialUnit.coerceAtLeast(1)) }
    val resources = remember(prefs) { prefs.getTeacherResources() }
    val fullYear = teacherAccount.hasFeature { it.yearTlmListFull }

    val kit: TlmKitSummary = remember(grade, unit, yearView, resources, fullYear) {
        if (yearView) {
            repo.buildYearKit(grade, "english", prefs.medium, resources, fullYear)
        } else {
            repo.buildUnitKit(grade, unit, "english", prefs.medium, resources)
        }
    }

    RegisterScaffold(
        title = s.tlmKitTitle,
        stepLabel = if (yearView) s.tlmKitYear else "${s.unitLabel} $unit",
        buttonText = s.closeBtn,
        canContinue = true,
        onBack = onBack,
        onContinue = onBack,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!teacherAccount.hasFeature { it.unitTlmKit }) {
                UpgradeBanner(s = s, message = s.tlmKitLocked, onUpgrade = onUpgrade)
                return@Column
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !yearView, onClick = { yearView = false }, label = { Text(s.tlmKitUnit) })
                FilterChip(selected = yearView, onClick = { yearView = true }, label = { Text(s.tlmKitYear) })
            }

            if (!yearView) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..4).forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text("${s.unitLabel} $u", fontSize = 12.sp) },
                        )
                    }
                }
            }

            InfoBannerCard(
                title = s.tlmKitSummary.format(kit.ownedCount, kit.items.size),
                body = kit.procurementNote,
            )

            if (yearView && !fullYear) {
                UpgradeBanner(s = s, message = s.tlmKitYearLocked, onUpgrade = onUpgrade)
            }

            kit.items.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.owned) BgTint else WarmPeach.copy(0.25f),
                    ),
                    border = BorderStroke(1.dp, SeasideBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(tlmCatalog.emojiFor(item.id), fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.label, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
                                if (item.essential) {
                                    Text(s.tlmKitEssential, fontSize = 10.sp, color = AccentTeal)
                                }
                            }
                            Text(item.reason, fontSize = 11.sp, color = PrimaryDark.copy(0.65f))
                        }
                        Icon(
                            if (item.owned) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (item.owned) AccentTeal else Color(0xFFC62828),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (teacherAccount.hasFeature { it.yearTlmPdfShare }) {
                Button(
                    onClick = {
                        val text = buildTlmShareText(kit, s)
                        context.startActivity(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                ) {
                    Text(s.tlmKitShare)
                }
            }
        }
    }
}

private fun buildTlmShareText(kit: TlmKitSummary, s: com.tippingpoint.pedastudio.i18n.AppStrings): String =
    buildString {
        appendLine("PedaStudio TLM Procurement — Grade ${kit.grade}")
        appendLine(kit.procurementNote)
        appendLine()
        kit.items.filter { !it.owned }.forEach {
            appendLine("• ${it.label}${if (it.essential) " (essential)" else ""}")
        }
    }
