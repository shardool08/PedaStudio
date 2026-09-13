package com.tippingpoint.pedastudio.ui.screens



import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.ModalBottomSheet

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.Text

import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.tippingpoint.pedastudio.data.CurriculumRepository

import com.tippingpoint.pedastudio.data.DayPlanStatus

import com.tippingpoint.pedastudio.data.CloudSyncRepository

import com.tippingpoint.pedastudio.data.PlanStorage

import com.tippingpoint.pedastudio.data.TlmResourceCatalog

import com.tippingpoint.pedastudio.i18n.LocalAppStrings

import com.tippingpoint.pedastudio.ui.components.FormSectionCard

import com.tippingpoint.pedastudio.ui.components.InfoBannerCard

import com.tippingpoint.pedastudio.ui.components.PrimaryButton

import com.tippingpoint.pedastudio.ui.components.RegisterScaffold

import com.tippingpoint.pedastudio.ui.theme.AccentTeal

import com.tippingpoint.pedastudio.ui.theme.PrimaryDark

import com.tippingpoint.pedastudio.ui.theme.PrimarySteel

import kotlinx.coroutines.launch

import org.json.JSONArray

import org.json.JSONObject



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun PlanViewScreen(

    lessonId: String,

    day: Int,

    curriculum: CurriculumRepository,

    planStorage: PlanStorage,

    cloud: CloudSyncRepository,

    tlmCatalog: TlmResourceCatalog,

    onBack: () -> Unit,

    onProgressChanged: () -> Unit = {},

    onOpenLessonProgress: (() -> Unit)? = null,

) {

    val s = LocalAppStrings.current

    val scope = rememberCoroutineScope()

    var plan by remember(lessonId, day) { mutableStateOf(planStorage.getPlan(lessonId, day)) }

    var dayMeta by remember(lessonId, day) { mutableStateOf(planStorage.getDayMeta(lessonId, day)) }

    var loading by remember { mutableStateOf(plan == null) }

    var showFeedback by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)



    LaunchedEffect(lessonId, day) {

        if (plan == null) {

            loading = true

            cloud.pullPlan(lessonId, day, planStorage).onSuccess { found ->

                if (found) {

                    plan = planStorage.getPlan(lessonId, day)

                    dayMeta = planStorage.getDayMeta(lessonId, day)

                }

            }

            loading = false

        }

    }



    RegisterScaffold(

        title = s.viewPlan,

        stepLabel = "$lessonId · ${s.dayLabel} $day",

        buttonText = s.continueBtn,

        canContinue = true,

        onBack = onBack,

        onContinue = onBack,

    ) {

        when {

            loading -> Text("Loading plan…", color = PrimaryDark)

            plan == null -> InfoBannerCard(title = s.viewPlan, body = "No plan saved for this day.")

            else -> {

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(

                        when (dayMeta.status) {

                            DayPlanStatus.PLANNED -> s.statusPlanned

                            DayPlanStatus.COMPLETED -> s.statusCompleted

                            DayPlanStatus.NOT_STARTED -> s.statusNotStarted

                        },

                        color = AccentTeal,

                        fontWeight = FontWeight.SemiBold,

                        fontSize = 13.sp,

                    )

                    dayMeta.feedback?.let { fb ->

                        Text(feedbackLabel(s, fb), fontSize = 13.sp, color = PrimarySteel)

                    }

                    planSection("Objective", plan!!.optJSONObject("objective"))

                    planSection("Hook", plan!!.optJSONObject("hook"))

                    planTlmSection(plan!!.optJSONObject("tlm"), tlmCatalog)

                    planSection("Activity", plan!!.optJSONObject("activity"))

                    planSection("Practice", plan!!.optJSONObject("practice"))

                    planSection("Assessment", plan!!.optJSONObject("assessment"))

                    planSection("Closure", plan!!.optJSONObject("closure"))

                    val board = plan!!.optString("board_plan", "")

                    if (board.isNotBlank()) {

                        FormSectionCard(title = "Board plan", subtitle = null) {

                            Text(board, fontSize = 14.sp, color = PrimaryDark)

                        }

                    }

                    if (dayMeta.status == DayPlanStatus.PLANNED) {

                        PrimaryButton(

                            text = s.markCompleted,

                            onClick = { showFeedback = true },

                            modifier = Modifier.fillMaxWidth(),

                        )

                    }

                    onOpenLessonProgress?.let { open ->

                        OutlinedButton(onClick = open, modifier = Modifier.fillMaxWidth()) {

                            Text(s.viewLessonProgress, color = PrimarySteel)

                        }

                    }

                }

            }

        }

    }



    if (showFeedback) {

        ModalBottomSheet(

            onDismissRequest = { showFeedback = false },

            sheetState = sheetState,

            containerColor = Color.White,

        ) {

            FeedbackSheetContent(s) { feedback ->

                showFeedback = false

                planStorage.completePlan(lessonId, day, feedback)

                dayMeta = planStorage.getDayMeta(lessonId, day)

                onProgressChanged()

                scope.launch { cloud.pushPlanMeta(lessonId, day, planStorage) }

            }

        }

    }

}



@Composable

private fun planTlmSection(section: JSONObject?, tlmCatalog: TlmResourceCatalog) {

    if (section == null) return

    val items = section.optJSONArray("items") ?: return

    FormSectionCard(title = "TLM Materials", subtitle = null) {

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            for (i in 0 until items.length()) {

                val item = items.optJSONObject(i) ?: continue

                val name = item.optString("name", "")

                val desc = item.optString("description", "")

                val emoji = tlmCatalog.all().find { name.contains(it.label, ignoreCase = true) }?.emoji

                    ?: tlmCatalog.emojiFor(name.lowercase().replace(' ', '_'))

                Row(modifier = Modifier.fillMaxWidth()) {

                    Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))

                    Column {

                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PrimaryDark)

                        if (desc.isNotBlank()) {

                            Text(desc, fontSize = 13.sp, color = PrimarySteel.copy(0.75f))

                        }

                    }

                }

            }

        }

    }

}



@Composable

private fun planSection(title: String, section: JSONObject?) {

    if (section == null) return

    FormSectionCard(title = title, subtitle = null) {

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

            section.keys().forEach { key ->

                val value = section.get(key)

                when (value) {

                    is JSONArray -> {

                        Text(key.replace('_', ' '), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PrimarySteel)

                        for (i in 0 until value.length()) {

                            val item = value.get(i)

                            Text("• ${formatValue(item)}", fontSize = 14.sp, color = PrimaryDark, modifier = Modifier.padding(start = 8.dp))

                        }

                    }

                    is JSONObject -> {

                        Text(key.replace('_', ' '), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PrimarySteel)

                        Text(formatValue(value), fontSize = 14.sp, color = PrimaryDark, modifier = Modifier.padding(start = 8.dp))

                    }

                    else -> {

                        Text("${key.replace('_', ' ')}: ${formatValue(value)}", fontSize = 14.sp, color = PrimaryDark)

                    }

                }

            }

        }

    }

}



private fun formatValue(value: Any?): String = when (value) {

    null -> ""

    is JSONObject -> value.keys().asSequence().joinToString("\n") { k -> "$k: ${value.get(k)}" }

    is JSONArray -> (0 until value.length()).joinToString("\n") { i -> "• ${value.get(i)}" }

    else -> value.toString()

}


