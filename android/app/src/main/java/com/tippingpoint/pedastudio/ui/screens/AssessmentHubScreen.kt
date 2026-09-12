package com.tippingpoint.pedastudio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.api.AssessmentApiClient
import com.tippingpoint.pedastudio.api.AssessmentHubData
import com.tippingpoint.pedastudio.auth.PhoneAuthController
import com.tippingpoint.pedastudio.data.AssessmentRepository
import com.tippingpoint.pedastudio.data.CurriculumRepository
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.components.InfoBannerCard
import com.tippingpoint.pedastudio.ui.components.RegisterScaffold
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AssessmentHubScreen(
    grade: Int,
    subject: String,
    prefs: UserPreferences,
    curriculum: CurriculumRepository,
    assessmentRepo: AssessmentRepository,
    auth: PhoneAuthController,
    teacherAccount: TeacherAccount,
    onBack: () -> Unit,
    onOpenEntry: (type: String, groupId: String?, groupName: String?) -> Unit,
    onAccountUpdated: (TeacherAccount) -> Unit,
) {
    val s = LocalAppStrings.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var hub by remember { mutableStateOf<AssessmentHubData?>(null) }
    val localGroups = remember(grade, subject, prefs.medium) {
        assessmentRepo.getUnitTests(grade, prefs.medium, curriculum.getLessons(grade, subject, prefs.medium))
    }

    LaunchedEffect(grade, subject, prefs.medium) {
        loading = true
        error = ""
        val result = withContext(Dispatchers.IO) {
            AssessmentApiClient.fetchCatalog(grade, subject, prefs.medium, auth.getIdToken())
        }
        result.onSuccess {
            hub = it
            it.account?.let { acc -> onAccountUpdated(acc) }
        }.onFailure {
            error = it.message ?: s.assessmentLoadError
        }
        loading = false
    }

    fun scoreFor(type: String, groupId: String? = null): Int? =
        hub?.scores?.find { it.type == type && (groupId == null || it.groupId == groupId) }?.scorePercent

    RegisterScaffold(
        title = s.assessmentHubTitle,
        stepLabel = s.assessmentHubSub.format(grade),
        buttonText = s.closeBtn,
        canContinue = !loading,
        onBack = onBack,
        onContinue = onBack,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = AccentTeal)
                }
            }
            if (error.isNotBlank() && hub == null) {
                InfoBannerCard(title = s.assessmentHubTitle, body = error)
            }

            AssessmentTypeCard(
                title = hub?.catalog?.baselineTitle ?: s.assessmentBaseline,
                body = hub?.catalog?.baselineDesc ?: s.assessmentBaselineDesc,
                score = scoreFor("baseline"),
                structuredTool = hub?.catalog?.baselineHasTool == true,
                onOpen = { onOpenEntry("baseline", null, s.assessmentBaseline) },
            )
            AssessmentTypeCard(
                title = s.assessmentUnitTests,
                body = s.assessmentUnitTestsDesc,
                score = null,
                onOpen = null,
            )

            val groups = hub?.catalog?.unitTests?.takeIf { it.isNotEmpty() }?.map {
                com.tippingpoint.pedastudio.data.AssessmentUnitGroup(
                    id = it.id,
                    name = it.name,
                    focus = it.focus,
                    lessons = it.lessons,
                    unit = it.unit,
                )
            } ?: localGroups

            groups.forEach { group ->
                val apiGroup = hub?.catalog?.unitTests?.find { it.id == group.id }
                AssessmentTypeCard(
                    title = "${s.unitLabel} ${group.unit} · ${group.name}",
                    body = group.focus,
                    score = scoreFor("unit", group.id),
                    structuredTool = apiGroup?.hasTool == true,
                    onOpen = { onOpenEntry("unit", group.id, group.name) },
                )
            }

            AssessmentTypeCard(
                title = hub?.catalog?.endlineTitle ?: s.assessmentEndline,
                body = hub?.catalog?.endlineDesc ?: s.assessmentEndlineDesc,
                score = scoreFor("endline"),
                structuredTool = hub?.catalog?.endlineHasTool == true,
                onOpen = { onOpenEntry("endline", null, s.assessmentEndline) },
            )
        }
    }
}

@Composable
private fun AssessmentTypeCard(
    title: String,
    body: String,
    score: Int?,
    structuredTool: Boolean = false,
    onOpen: (() -> Unit)?,
) {
    val s = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = PrimaryDark, fontSize = 15.sp)
                    if (structuredTool) {
                        Text(s.assessmentStructuredBadge, fontSize = 10.sp, color = AccentTeal, fontWeight = FontWeight.Medium)
                    }
                }
                score?.let {
                    Text("$it%", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Text(body, fontSize = 12.sp, color = PrimaryDark.copy(0.7f))
            if (onOpen != null) {
                TextButton(onClick = onOpen) {
                    Text(
                        if (score == null) s.assessmentOpenTool else s.assessmentUpdateScores,
                        color = AccentTeal,
                    )
                }
            }
        }
    }
}
