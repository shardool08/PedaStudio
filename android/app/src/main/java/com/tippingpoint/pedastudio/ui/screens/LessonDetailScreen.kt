package com.tippingpoint.pedastudio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.tippingpoint.pedastudio.data.DayInfo
import com.tippingpoint.pedastudio.data.DayPlanMeta
import com.tippingpoint.pedastudio.data.DayPlanStatus
import com.tippingpoint.pedastudio.data.FirestoreRepository
import com.tippingpoint.pedastudio.data.LessonItem
import com.tippingpoint.pedastudio.data.LessonProgressStatus
import com.tippingpoint.pedastudio.data.NextPlanAction
import com.tippingpoint.pedastudio.data.PlanFeedback
import com.tippingpoint.pedastudio.data.PlanProgressHelper
import com.tippingpoint.pedastudio.data.PlanStorage
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.AppStrings
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.components.InfoBannerCard
import com.tippingpoint.pedastudio.ui.components.PrimaryButton
import com.tippingpoint.pedastudio.ui.components.RegisterScaffold
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.BgTint
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.PrimarySteel
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import com.tippingpoint.pedastudio.ui.theme.WarmPeach
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    lessonId: String,
    prefs: UserPreferences,
    curriculum: CurriculumRepository,
    planStorage: PlanStorage,
    firestore: FirestoreRepository,
    teacherAccount: TeacherAccount,
    plansRevision: Int,
    onBack: () -> Unit,
    onQuickPlan: (String, Int, String, String) -> Unit,
    onViewPlan: (String, Int) -> Unit,
    onWorksheet: (String, Int) -> Unit,
    onUpgrade: () -> Unit,
    onProgressChanged: () -> Unit,
) {
    val s = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val lesson = remember(lessonId) { curriculum.findLessonAnywhere(lessonId) }
    val roadmapLessons = remember(prefs.lastViewedGrade, prefs.lastViewedSubject, prefs.medium) {
        curriculum.getLessons(prefs.lastViewedGrade, prefs.lastViewedSubject, prefs.medium)
    }

    fun advanceToNextLesson() {
        PlanProgressHelper.nextLessonInCurriculum(roadmapLessons, lessonId)?.let { next ->
            prefs.setCurrentLesson(prefs.lastViewedGrade, prefs.lastViewedSubject, next.id)
            scope.launch { firestore.pushProfile(prefs) }
        }
    }
    var refreshKey by remember { mutableStateOf(0) }
    var feedbackDay by remember { mutableStateOf<Int?>(null) }
    var reteachNotesDay by remember { mutableStateOf<Int?>(null) }
    var nextAction by remember { mutableStateOf<NextPlanAction?>(null) }

    val lessonStatus = remember(lessonId, plansRevision, refreshKey) {
        lesson?.let { PlanProgressHelper.getLessonStatus(lessonId, it.days, planStorage) }
            ?: LessonProgressStatus.NOT_STARTED
    }

    if (lesson == null) {
        RegisterScaffold(
            title = s.lessonProgressTitle,
            stepLabel = lessonId,
            buttonText = s.continueBtn,
            canContinue = true,
            onBack = onBack,
            onContinue = onBack,
        ) {
            InfoBannerCard(title = s.lessonProgressTitle, body = "Lesson not found.")
        }
        return
    }

    val dayRows = remember(lesson, plansRevision, refreshKey) {
        lesson.bloomsProgression.ifEmpty {
            (1..lesson.days).map { d ->
                DayInfo(d, "Day $d", lesson.dayFocus(d)?.focus ?: "")
            }
        }
    }

    fun submitFeedback(day: Int, feedback: PlanFeedback) {
        planStorage.completePlan(lessonId, day, feedback)
        nextAction = PlanProgressHelper.getNextAction(lessonId, day, feedback, lesson.days)
        refreshKey++
        onProgressChanged()
        scope.launch { firestore.pushPlanMeta(lessonId, day, planStorage) }
    }

    val dayMetas = remember(lessonId, plansRevision, refreshKey, dayRows) {
        dayRows.associate { dayInfo ->
            dayInfo.day to planStorage.getDayMeta(lessonId, dayInfo.day)
        }
    }

    RegisterScaffold(
        title = s.lessonProgressTitle,
        stepLabel = "${lesson.id} · ${lesson.curriculumTitle}",
        buttonText = s.continueBtn,
        canContinue = true,
        onBack = onBack,
        onContinue = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                StatusPill(text = lesson.type)
                StatusPill(text = "${s.pagesLabel} ${lesson.pages}")
                StatusPill(text = "${lesson.days} ${s.daysLabel}")
                StatusPill(
                    text = lessonStatusLabel(s, lessonStatus),
                    highlight = true,
                )
            }

            nextAction?.let { action ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimarySteel),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s.recommendedNext, fontSize = 11.sp, color = Color.White.copy(0.7f))
                        Text(localizedNextLabel(s, action), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text(localizedNextDescription(s, action), fontSize = 13.sp, color = Color.White.copy(0.8f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    nextAction = null
                                    when (action.action) {
                                        "next_lesson" -> {
                                            advanceToNextLesson()
                                            onProgressChanged()
                                            onBack()
                                        }
                                        "continue" -> onViewPlan(action.lessonId, action.day)
                                        "practice" -> onQuickPlan(action.lessonId, action.day, "practice", "")
                                        "reteach" -> reteachNotesDay = action.day
                                        else -> onQuickPlan(action.lessonId, action.day, "", "")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            ) {
                                Text(localizedNextLabel(s, action), color = PrimarySteel, fontSize = 13.sp)
                            }
                            OutlinedButton(onClick = { nextAction = null }) {
                                Text(s.dismiss, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Text(s.dayWisePlan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = PrimarySteel.copy(0.55f))

            for (dayInfo in dayRows) {
                DayProgressCard(
                    s = s,
                    dayInfo = dayInfo,
                    meta = dayMetas[dayInfo.day] ?: DayPlanMeta(DayPlanStatus.NOT_STARTED),
                    worksheetsEnabled = teacherAccount.hasFeature { it.worksheets },
                    onPlanDay = { onQuickPlan(lessonId, dayInfo.day, "", "") },
                    onViewPlan = { onViewPlan(lessonId, dayInfo.day) },
                    onWorksheet = {
                        if (teacherAccount.hasFeature { it.worksheets }) onWorksheet(lessonId, dayInfo.day)
                        else onUpgrade()
                    },
                    onMarkCompleted = { feedbackDay = dayInfo.day },
                    onRePlan = { onQuickPlan(lessonId, dayInfo.day, "", "") },
                )
            }
        }
    }

    if (feedbackDay != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { feedbackDay = null },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            FeedbackSheetContent(
                s = s,
                onSelect = { feedback ->
                    val day = feedbackDay ?: return@FeedbackSheetContent
                    feedbackDay = null
                    submitFeedback(day, feedback)
                },
            )
        }
    }

    if (reteachNotesDay != null) {
        val reteachSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { reteachNotesDay = null },
            sheetState = reteachSheetState,
            containerColor = Color.White,
        ) {
            ReteachNotesSheetContent(
                s = s,
                onSubmit = { notes ->
                    val day = reteachNotesDay ?: return@ReteachNotesSheetContent
                    reteachNotesDay = null
                    nextAction = null
                    onQuickPlan(lessonId, day, "reteach", notes)
                },
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, highlight: Boolean = false) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlight) WarmPeach else Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            color = PrimarySteel,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun DayProgressCard(
    s: AppStrings,
    dayInfo: DayInfo,
    meta: com.tippingpoint.pedastudio.data.DayPlanMeta,
    worksheetsEnabled: Boolean,
    onPlanDay: () -> Unit,
    onViewPlan: () -> Unit,
    onWorksheet: () -> Unit,
    onMarkCompleted: () -> Unit,
    onRePlan: () -> Unit,
) {
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    PlanProgressHelper.statusIcon(meta.status),
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${s.dayLabel} ${dayInfo.day}", fontWeight = FontWeight.Bold, color = PrimaryDark, fontSize = 15.sp)
                        Text(dayInfo.level, fontSize = 11.sp, color = AccentTeal, modifier = Modifier.padding(top = 2.dp))
                    }
                    when (meta.status) {
                        DayPlanStatus.PLANNED -> if (meta.savedAt > 0L) {
                            Text("${s.statusPlanned} · ${dateFormat.format(Date(meta.savedAt))}", fontSize = 11.sp, color = PrimarySteel.copy(0.55f))
                        }
                        DayPlanStatus.COMPLETED -> if (meta.completedAt > 0L) {
                            Text("${s.statusCompleted} · ${dateFormat.format(Date(meta.completedAt))}", fontSize = 11.sp, color = PrimarySteel.copy(0.55f))
                        }
                        else -> Unit
                    }
                }
            }
            if (dayInfo.focus.isNotBlank()) {
                Text(dayInfo.focus, fontSize = 14.sp, color = PrimaryDark.copy(0.85f))
            }
            meta.feedback?.let { fb ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgTint),
                    border = BorderStroke(1.dp, SeasideBorder),
                ) {
                    Text(
                        feedbackLabel(s, fb),
                        modifier = Modifier.padding(10.dp),
                        fontSize = 12.sp,
                        color = AccentTeal,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            when (meta.status) {
                DayPlanStatus.NOT_STARTED -> {
                    PrimaryButton(
                        text = "${s.planDayBtn} ${dayInfo.day}",
                        onClick = onPlanDay,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DayPlanStatus.PLANNED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onViewPlan, modifier = Modifier.weight(1f)) {
                            Text(s.viewPlan, fontSize = 13.sp, color = PrimarySteel)
                        }
                        PrimaryButton(
                            text = s.markCompleted,
                            onClick = onMarkCompleted,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedButton(onClick = onWorksheet, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (worksheetsEnabled) s.worksheetBtn else "${s.worksheetBtn} · Prime",
                            fontSize = 13.sp,
                            color = if (worksheetsEnabled) AccentTeal else PrimarySteel.copy(0.6f),
                        )
                    }
                }
                DayPlanStatus.COMPLETED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onViewPlan, modifier = Modifier.weight(1f)) {
                            Text(s.viewPlan, fontSize = 13.sp, color = PrimarySteel)
                        }
                        OutlinedButton(onClick = onRePlan, modifier = Modifier.weight(1f)) {
                            Text(s.rePlan, fontSize = 13.sp, color = PrimarySteel)
                        }
                    }
                    OutlinedButton(onClick = onWorksheet, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (worksheetsEnabled) s.worksheetBtn else "${s.worksheetBtn} · Prime",
                            fontSize = 13.sp,
                            color = if (worksheetsEnabled) AccentTeal else PrimarySteel.copy(0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReteachNotesSheetContent(
    s: AppStrings,
    onSubmit: (String) -> Unit,
) {
    var notes by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(s.reteachNotesTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDark)
        Text(s.reteachNotesSub, fontSize = 13.sp, color = PrimarySteel.copy(0.7f))
        com.tippingpoint.pedastudio.ui.components.OutlinedFormField(
            value = notes,
            onValueChange = { notes = it },
            label = s.reteachNotesHint,
            singleLine = false,
        )
        PrimaryButton(
            text = s.continueBtn,
            onClick = {
                if (notes.isNotBlank()) onSubmit(notes.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = notes.isNotBlank(),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
fun FeedbackSheetContent(
    s: AppStrings,
    onSelect: (PlanFeedback) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(s.feedbackTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDark)
        Text(s.feedbackSub, fontSize = 13.sp, color = PrimarySteel.copy(0.7f))
        FeedbackOption(s.fbWentWell) { onSelect(PlanFeedback.WENT_WELL) }
        FeedbackOption(s.fbSomeStruggled) { onSelect(PlanFeedback.SOME_STRUGGLED) }
        FeedbackOption(s.fbMostDidntUnderstand) { onSelect(PlanFeedback.MOST_DIDNT_UNDERSTAND) }
        FeedbackOption(s.fbCouldntFinish) { onSelect(PlanFeedback.COULDNT_FINISH) }
        FeedbackOption(s.fbReadyForMore) { onSelect(PlanFeedback.READY_FOR_MORE) }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun FeedbackOption(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 14.sp, color = PrimaryDark)
    }
}

private fun lessonStatusLabel(s: AppStrings, status: LessonProgressStatus): String = when (status) {
    LessonProgressStatus.NOT_STARTED -> s.statusNotStarted
    LessonProgressStatus.PLANNED -> s.statusPlanned
    LessonProgressStatus.IN_PROGRESS -> s.statusInProgress
    LessonProgressStatus.COMPLETED -> s.statusCompleted
}

fun feedbackLabel(s: AppStrings, feedback: PlanFeedback): String = when (feedback) {
    PlanFeedback.WENT_WELL -> s.fbWentWell
    PlanFeedback.SOME_STRUGGLED -> s.fbSomeStruggled
    PlanFeedback.MOST_DIDNT_UNDERSTAND -> s.fbMostDidntUnderstand
    PlanFeedback.COULDNT_FINISH -> s.fbCouldntFinish
    PlanFeedback.READY_FOR_MORE -> s.fbReadyForMore
}

fun localizedNextLabel(s: AppStrings, action: NextPlanAction): String = when (action.action) {
    "next_day" -> s.nextDayAction.format(action.day)
    "next_lesson" -> s.nextLessonAction
    "practice" -> s.addPracticeDay
    "reteach" -> s.reteachDay
    "continue" -> s.continueSamePlan
    else -> action.label
}

fun localizedNextDescription(s: AppStrings, action: NextPlanAction): String = when (action.action) {
    "next_day" -> s.nextDayDesc
    "next_lesson" -> s.nextLessonDesc
    "practice" -> s.practiceDesc
    "reteach" -> s.reteachDesc
    "continue" -> s.continueDesc
    else -> action.description
}
