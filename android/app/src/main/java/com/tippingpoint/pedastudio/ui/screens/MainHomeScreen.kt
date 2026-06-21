package com.tippingpoint.pedastudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.data.AssessmentRepository
import com.tippingpoint.pedastudio.data.AssessmentUnitGroup
import com.tippingpoint.pedastudio.data.CurriculumRepository
import com.tippingpoint.pedastudio.data.DayPlanStatus
import com.tippingpoint.pedastudio.data.PlanFeedback
import com.tippingpoint.pedastudio.data.IndiaStates
import com.tippingpoint.pedastudio.data.IndianLanguages
import com.tippingpoint.pedastudio.data.LessonItem
import com.tippingpoint.pedastudio.data.LessonProgressStatus
import com.tippingpoint.pedastudio.data.NextPlanAction
import com.tippingpoint.pedastudio.data.PendingHomeAction
import com.tippingpoint.pedastudio.data.PlanProgressHelper
import com.tippingpoint.pedastudio.data.PlanStorage
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.TierConfig
import com.tippingpoint.pedastudio.data.TlmKitRepository
import com.tippingpoint.pedastudio.data.TlmKitSummary
import com.tippingpoint.pedastudio.data.TlmResourceCatalog
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.AppStrings
import com.tippingpoint.pedastudio.i18n.LocalAppLanguage
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.components.InfoBannerCard
import com.tippingpoint.pedastudio.ui.components.MembershipHeroCard
import com.tippingpoint.pedastudio.ui.components.TierBadge
import com.tippingpoint.pedastudio.ui.components.UpgradeBanner
import com.tippingpoint.pedastudio.ui.components.PrimaryButton
import com.tippingpoint.pedastudio.ui.components.ProfileActionRow
import com.tippingpoint.pedastudio.ui.components.ProfileChip
import com.tippingpoint.pedastudio.ui.components.ProfileHeroHeader
import com.tippingpoint.pedastudio.ui.components.ProfileInfoRow
import com.tippingpoint.pedastudio.ui.components.ProfileSectionCard
import com.tippingpoint.pedastudio.ui.components.profileInitials
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.BgTint
import com.tippingpoint.pedastudio.ui.theme.NavBg
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.PrimarySteel
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import com.tippingpoint.pedastudio.ui.theme.WarmPeach
import com.tippingpoint.pedastudio.util.formatSafe

private enum class HomeTab { HOME, ROADMAP, PROFILE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    prefs: UserPreferences,
    curriculum: CurriculumRepository,
    tlmCatalog: TlmResourceCatalog,
    assessmentRepo: AssessmentRepository,
    planStorage: PlanStorage,
    plansRevision: Int = 0,
    teacherAccount: TeacherAccount,
    onEditProfile: () -> Unit,
    onQuickPlan: (String, Int, String, String) -> Unit,
    onScan: (String) -> Unit,
    onWorksheet: (String, Int) -> Unit,
    onTlmKit: (Int) -> Unit,
    onAssessmentEntry: (type: String, groupId: String?, groupName: String?) -> Unit,
    onViewPlan: (String, Int) -> Unit,
    onOpenLesson: (String) -> Unit,
    onChangeLanguage: () -> Unit,
    onManageSubscription: () -> Unit,
    onLessonSelected: () -> Unit,
    onDayCompleted: (lessonId: String, day: Int, feedback: PlanFeedback) -> Unit,
    onPlanStateChanged: () -> Unit,
    onSignOut: () -> Unit,
) {
    var tab by remember { mutableStateOf(HomeTab.HOME) }
    var selectedGrade by remember { mutableIntStateOf(prefs.getTeacherGrades().firstOrNull() ?: 1) }
    var selectedSubject by remember { mutableStateOf(prefs.getTeacherSubjects().firstOrNull() ?: "english") }
    var pickCurrent by remember { mutableStateOf(false) }
    var currentLessonId by remember { mutableStateOf("") }

    val lessons = remember(selectedGrade, selectedSubject, prefs.medium) {
        curriculum.getLessons(selectedGrade, selectedSubject, prefs.medium)
    }
    val available = curriculum.isAvailable(selectedGrade, selectedSubject)

    LaunchedEffect(selectedGrade, selectedSubject) {
        prefs.lastViewedGrade = selectedGrade
        prefs.lastViewedSubject = selectedSubject
    }

    LaunchedEffect(selectedGrade, selectedSubject, lessons, plansRevision) {
        val stored = prefs.getCurrentLesson(selectedGrade, selectedSubject)
        val resolved = PlanProgressHelper.resolveCurrentLessonId(lessons, stored, planStorage)
        currentLessonId = resolved
        if (resolved.isNotBlank() && resolved != stored) {
            prefs.setCurrentLesson(selectedGrade, selectedSubject, resolved)
            if (stored.isNotBlank()) onLessonSelected()
        }
    }

    val currentLesson = lessons.find { it.id == currentLessonId }
    val gradeAllowed = TierConfig.gradeAllowed(teacherAccount, selectedGrade)
    val assessmentGroups = remember(selectedGrade, prefs.medium, lessons) {
        assessmentRepo.getUnitTests(selectedGrade, prefs.medium, lessons)
    }
    val nextPlanDay = currentLesson?.let {
        PlanProgressHelper.getFirstUnplannedDay(it.id, it.days, planStorage)
    }
    val currentPlanDay = currentLesson?.let {
        PlanProgressHelper.getCurrentPlanDay(it.id, it.days, planStorage)
    }
    val ongoingPlanDay = currentLesson?.let {
        PlanProgressHelper.getOngoingPlanDay(it.id, it.days, planStorage)
    }
    val lang = LocalAppLanguage.current
    val s = LocalAppStrings.current
    val teacherFallback = when (lang) {
        "mr" -> "शिक्षक"
        "hi" -> "शिक्षक"
        "ur" -> "استاد"
        else -> "Teacher"
    }

    Scaffold(
        containerColor = NavBg,
        topBar = {
            TopAppBar(
                title = {
                    if (tab == HomeTab.PROFILE) {
                        Text(s.profileTitle, fontWeight = FontWeight.Bold)
                    } else {
                        Column {
                            Text("PedaStudio", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    s.helloTeacher.format(prefs.teacherName.ifBlank { teacherFallback }),
                                    fontSize = 12.sp,
                                    color = PrimaryDark.copy(alpha = 0.6f),
                                )
                                TierBadge(teacherAccount.tier)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (tab == HomeTab.PROFILE) NavBg else Color.White,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = Color.White,
            ) {
                NavigationBarItem(selected = tab == HomeTab.HOME, onClick = { pickCurrent = false; tab = HomeTab.HOME }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(s.navHome, fontSize = 11.sp) })
                NavigationBarItem(selected = tab == HomeTab.ROADMAP, onClick = { tab = HomeTab.ROADMAP }, icon = { Icon(Icons.Default.List, null) }, label = { Text(s.navRoadmap, fontSize = 11.sp) })
                NavigationBarItem(selected = tab == HomeTab.PROFILE, onClick = { pickCurrent = false; tab = HomeTab.PROFILE }, icon = { Icon(Icons.Default.Person, null) }, label = { Text(s.navProfile, fontSize = 11.sp) })
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (tab != HomeTab.PROFILE) {
                GradeSubjectBar(
                    s = s,
                    grades = prefs.getTeacherGrades(),
                    subjects = prefs.getTeacherSubjects(),
                    selectedGrade = selectedGrade,
                    selectedSubject = selectedSubject,
                    teacherAccount = teacherAccount,
                    onGrade = { grade ->
                        if (TierConfig.gradeAllowed(teacherAccount, grade)) {
                            selectedGrade = grade
                            currentLessonId = ""
                        } else {
                            onManageSubscription()
                        }
                    },
                    onSubject = {
                        selectedSubject = it
                        currentLessonId = ""
                    },
                )
            }

            when (tab) {
                HomeTab.HOME -> HomeTabContent(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    s = s,
                    prefs = prefs,
                    curriculum = curriculum,
                    tlmCatalog = tlmCatalog,
                    lessons = lessons,
                    available = available,
                    gradeAllowed = gradeAllowed,
                    selectedGrade = selectedGrade,
                    teacherAccount = teacherAccount,
                    currentLesson = currentLesson,
                    nextPlanDay = nextPlanDay,
                    currentPlanDay = currentPlanDay,
                    ongoingPlanDay = ongoingPlanDay,
                    planStorage = planStorage,
                    plansRevision = plansRevision,
                    onPickLesson = { tab = HomeTab.ROADMAP; pickCurrent = true },
                    onQuickPlan = { id, day, mode, notes -> onQuickPlan(id, day, mode, notes) },
                    onScan = { id -> onScan(id) },
                    onViewPlan = { id, day -> onViewPlan(id, day) },
                    onOpenLesson = onOpenLesson,
                    onOpenRoadmap = { tab = HomeTab.ROADMAP },
                    onTlmKit = onTlmKit,
                    onManageSubscription = onManageSubscription,
                    onDayCompleted = onDayCompleted,
                    onPlanStateChanged = onPlanStateChanged,
                    onAdvanceToNextLesson = {
                        PlanProgressHelper.nextLessonInCurriculum(lessons, currentLessonId)?.let { next ->
                            currentLessonId = next.id
                            prefs.setCurrentLesson(selectedGrade, selectedSubject, next.id)
                            onLessonSelected()
                        }
                    },
                )
                HomeTab.ROADMAP -> RoadmapTabContent(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    s = s,
                    lessons = lessons,
                    assessmentGroups = assessmentGroups,
                    available = available,
                    currentId = currentLesson?.id ?: currentLessonId,
                    pickCurrent = pickCurrent,
                    planStorage = planStorage,
                    plansRevision = plansRevision,
                    onSelectCurrent = { id ->
                        currentLessonId = id
                        prefs.setCurrentLesson(selectedGrade, selectedSubject, id)
                        pickCurrent = false
                        tab = HomeTab.HOME
                        onLessonSelected()
                    },
                    onOpenLesson = onOpenLesson,
                    onTlmKit = onTlmKit,
                    onAssessmentEntry = onAssessmentEntry,
                )
                HomeTab.PROFILE -> ProfileTabContent(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    prefs = prefs,
                    teacherAccount = teacherAccount,
                    onEditProfile = onEditProfile,
                    onChangeLanguage = onChangeLanguage,
                    onManageSubscription = onManageSubscription,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

@Composable
private fun GradeSubjectBar(
    s: AppStrings,
    grades: List<Int>,
    subjects: List<String>,
    selectedGrade: Int,
    selectedSubject: String,
    teacherAccount: TeacherAccount,
    onGrade: (Int) -> Unit,
    onSubject: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                grades.forEach { g ->
                    val allowed = TierConfig.gradeAllowed(teacherAccount, g)
                    FilterChip(
                        selected = g == selectedGrade,
                        onClick = { onGrade(g) },
                        label = {
                            Text(
                                buildString {
                                    if (!allowed) append("🔒 ")
                                    append(if (g == 0) "KG" else g.toString())
                                },
                                fontSize = 13.sp,
                            )
                        },
                    )
                }
            }
            if (selectedGrade != 0 && subjects.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subjects.forEach { sub ->
                        FilterChip(
                            selected = sub == selectedSubject,
                            onClick = { onSubject(sub) },
                            label = { Text(sub.replaceFirstChar { it.uppercase() }, fontSize = 13.sp) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTabContent(
    modifier: Modifier = Modifier,
    s: AppStrings,
    prefs: UserPreferences,
    curriculum: CurriculumRepository,
    tlmCatalog: TlmResourceCatalog,
    lessons: List<LessonItem>,
    available: Boolean,
    gradeAllowed: Boolean,
    selectedGrade: Int,
    teacherAccount: TeacherAccount,
    currentLesson: LessonItem?,
    nextPlanDay: Int?,
    currentPlanDay: Int?,
    ongoingPlanDay: Int?,
    planStorage: PlanStorage,
    plansRevision: Int,
    onPickLesson: () -> Unit,
    onQuickPlan: (String, Int, String, String) -> Unit,
    onScan: (String) -> Unit,
    onViewPlan: (String, Int) -> Unit,
    onOpenLesson: (String) -> Unit,
    onOpenRoadmap: () -> Unit,
    onTlmKit: (Int) -> Unit,
    onManageSubscription: () -> Unit,
    onDayCompleted: (lessonId: String, day: Int, feedback: PlanFeedback) -> Unit,
    onPlanStateChanged: () -> Unit,
    onAdvanceToNextLesson: () -> Unit,
) {
    val tlmRepo = remember { TlmKitRepository(curriculum, tlmCatalog) }
    var feedbackDay by remember { mutableStateOf<Int?>(null) }
    var reteachNotesDay by remember { mutableStateOf<Int?>(null) }
    val feedbackSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reteachSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!gradeAllowed) {
                UpgradeBanner(
                    s = s,
                    message = s.tierGradeLocked.format(
                        if (selectedGrade == 0) 0 else selectedGrade,
                        TierConfig.upgradeLabel(teacherAccount.tier),
                    ),
                    onUpgrade = onManageSubscription,
                )
            } else if (teacherAccount.tier == TierConfig.TierId.BASIC && (teacherAccount.plansRemaining ?: 1) <= 5) {
                UpgradeBanner(
                    s = s,
                    message = s.tierPlansRemaining.format(teacherAccount.plansRemaining ?: 0),
                    onUpgrade = onManageSubscription,
                )
            }
            if (!available) {
                InfoBannerCard(s.comingSoon, s.comingSoonBody)
            } else if (currentLesson == null) {
                InfoBannerCard(s.setCurrentLesson, s.setCurrentLessonBody)
                PrimaryButton(text = s.goToRoadmap, onClick = onPickLesson)
            } else {
                val unitLessons = remember(lessons, currentLesson.unit) {
                    lessons.filter { it.unit == currentLesson.unit }
                }
                val unitPlannedCount = remember(unitLessons, plansRevision) {
                    unitLessons.count { lesson ->
                        PlanProgressHelper.getLessonStatus(lesson.id, lesson.days, planStorage) !=
                            LessonProgressStatus.NOT_STARTED
                    }
                }
                val unitKit = remember(currentLesson.unit, prefs, plansRevision) {
                    tlmRepo.buildUnitKit(
                        selectedGrade,
                        currentLesson.unit,
                        "english",
                        prefs.medium,
                        prefs.getTeacherResources(),
                    )
                }

                val pendingAction = remember(currentLesson.id, plansRevision) {
                    PlanProgressHelper.getPendingHomeAction(currentLesson, planStorage)
                }

                HomePlanSection(
                    s = s,
                    lesson = currentLesson,
                    unitPlannedCount = unitPlannedCount,
                    unitTotalCount = unitLessons.size,
                    unitKit = unitKit,
                    tlmCatalog = tlmCatalog,
                    planDay = ongoingPlanDay ?: currentPlanDay,
                    ongoingDay = ongoingPlanDay,
                    nextPlanDay = nextPlanDay,
                    pendingAction = pendingAction,
                    planStorage = planStorage,
                    onOpenRoadmap = onOpenRoadmap,
                    onOpenPlan = {
                        val day = ongoingPlanDay ?: currentPlanDay
                        if (day != null) onViewPlan(currentLesson.id, day)
                        else onOpenLesson(currentLesson.id)
                    },
                    onPlanDay = { day -> onQuickPlan(currentLesson.id, day, "", "") },
                    onMarkCompleted = { feedbackDay = ongoingPlanDay },
                    onExecuteNextAction = { action ->
                        when (action.action) {
                            "next_lesson" -> {
                                val next = PlanProgressHelper.nextLessonInCurriculum(lessons, currentLesson.id)
                                onAdvanceToNextLesson()
                                next?.let { onQuickPlan(it.id, 1, "", "") }
                            }
                            "continue" -> {
                                onViewPlan(action.lessonId, action.day)
                                pendingAction?.let { pending ->
                                    planStorage.dismissHomeNextAction(currentLesson.id, pending.completedDay)
                                    onPlanStateChanged()
                                }
                            }
                            "practice" -> onQuickPlan(action.lessonId, action.day, "practice", "")
                            else -> onQuickPlan(action.lessonId, action.day, "", "")
                        }
                    },
                    onReteach = { reteachNotesDay = it },
                    onPlanNextDay = { completedDay ->
                        val nextDay = completedDay + 1
                        if (nextDay <= currentLesson.days) {
                            onQuickPlan(currentLesson.id, nextDay, "", "")
                            planStorage.dismissHomeNextAction(currentLesson.id, completedDay)
                            onPlanStateChanged()
                        }
                    },
                    onReplan = { completedDay, action ->
                        val day = PlanProgressHelper.replanDayFor(completedDay, action)
                        val mode = when (action.action) {
                            "continue" -> "continue"
                            "practice" -> "practice"
                            else -> ""
                        }
                        onQuickPlan(currentLesson.id, day, mode, "")
                    },
                    onDismissNextAction = { completedDay ->
                        planStorage.dismissHomeNextAction(currentLesson.id, completedDay)
                        onPlanStateChanged()
                    },
                    onOpenTlmKit = { onTlmKit(currentLesson.unit) },
                )
                TextButton(
                    onClick = { onOpenLesson(currentLesson.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "${s.viewLessonProgress} · ${s.worksheetBtn}",
                        color = AccentTeal,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (available && currentLesson != null) {
            HomeQuickActionCard(
                title = s.scanBtn,
                subtitle = if (teacherAccount.hasFeature { it.textbookScan }) s.homeScanSub else s.scanLocked,
                icon = Icons.Default.PhotoCamera,
                filled = false,
                onClick = {
                    if (teacherAccount.hasFeature { it.textbookScan }) onScan(currentLesson.id)
                    else onManageSubscription()
                },
            )
        }
    }

    if (feedbackDay != null && currentLesson != null) {
        ModalBottomSheet(
            onDismissRequest = { feedbackDay = null },
            sheetState = feedbackSheetState,
            containerColor = Color.White,
        ) {
            FeedbackSheetContent(
                s = s,
                onSelect = { feedback ->
                    val day = feedbackDay ?: return@FeedbackSheetContent
                    feedbackDay = null
                    onDayCompleted(currentLesson.id, day, feedback)
                },
            )
        }
    }

    if (reteachNotesDay != null && currentLesson != null) {
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
                    onQuickPlan(currentLesson.id, day, "reteach", notes)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomePlanSection(
    s: AppStrings,
    lesson: LessonItem,
    unitPlannedCount: Int,
    unitTotalCount: Int,
    unitKit: TlmKitSummary,
    tlmCatalog: TlmResourceCatalog,
    planDay: Int?,
    ongoingDay: Int?,
    nextPlanDay: Int?,
    pendingAction: PendingHomeAction?,
    planStorage: PlanStorage,
    onOpenRoadmap: () -> Unit,
    onOpenPlan: () -> Unit,
    onPlanDay: (Int) -> Unit,
    onMarkCompleted: () -> Unit,
    onExecuteNextAction: (NextPlanAction) -> Unit,
    onReteach: (completedDay: Int) -> Unit,
    onPlanNextDay: (completedDay: Int) -> Unit,
    onReplan: (completedDay: Int, action: NextPlanAction) -> Unit,
    onDismissNextAction: (completedDay: Int) -> Unit,
    onOpenTlmKit: () -> Unit,
) {
    val dayInfo = planDay?.let { lesson.dayFocus(it) }
    val hasSavedPlan = planDay != null && planStorage.hasPlan(lesson.id, planDay)
    val isTeaching = ongoingDay != null
    val needsPlan = !isTeaching &&
        pendingAction == null &&
        nextPlanDay != null &&
        planStorage.getDayMeta(lesson.id, nextPlanDay).status == DayPlanStatus.NOT_STARTED
    val headerLabel = when {
        pendingAction != null -> s.recommendedNext
        isTeaching -> s.homeCurrentPlan
        needsPlan -> s.homeNextUp
        hasSavedPlan -> s.homeCurrentPlan
        else -> s.homeCurrentLesson
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmPeach),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                onClick = onOpenRoadmap,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.55f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        s.homeUnitProgress.format(lesson.unit, unitPlannedCount, unitTotalCount),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDark,
                    )
                    Icon(Icons.Default.ChevronRight, null, tint = PrimaryDark.copy(0.5f), modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    headerLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark.copy(0.55f),
                    letterSpacing = 0.8.sp,
                )
                if (planDay != null && isTeaching) {
                    Surface(shape = RoundedCornerShape(20.dp), color = PrimaryDark) {
                        Text(
                            s.homeCurrentPlanDay.format(planDay, lesson.days),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }

            Text(
                lesson.curriculumTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = PrimaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${s.unitLabel} ${lesson.unit} · ${lesson.id} · ${s.pagesLabel} ${lesson.pages}",
                fontSize = 12.sp,
                color = PrimaryDark.copy(0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (isTeaching && dayInfo != null) {
                Text(
                    s.homeCurrentPlanFocus.formatSafe(dayInfo.level, dayInfo.focus),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryDark.copy(0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            when {
                pendingAction != null -> {
                    HomeFeedbackActionBlock(
                        s = s,
                        pending = pendingAction,
                        lessonDays = lesson.days,
                        onPrimary = { onExecuteNextAction(pendingAction.nextAction) },
                        onReteach = { onReteach(pendingAction.completedDay) },
                        onPlanNextDay = { onPlanNextDay(pendingAction.completedDay) },
                        onReplan = { onReplan(pendingAction.completedDay, pendingAction.nextAction) },
                        onDismiss = { onDismissNextAction(pendingAction.completedDay) },
                    )
                }
                isTeaching -> {
                    TextButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Default.MenuBook, null, tint = PrimaryDark, modifier = Modifier.size(18.dp))
                            Text(
                                if (hasSavedPlan) s.homeCurrentPlanOpen else s.homeCurrentPlanEmpty,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryDark,
                            )
                            Icon(Icons.Default.ChevronRight, null, tint = PrimaryDark, modifier = Modifier.size(18.dp))
                        }
                    }
                    PrimaryButton(
                        text = s.markCompleted,
                        onClick = onMarkCompleted,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                needsPlan -> {
                    Text(
                        if (nextPlanDay == 1) s.homeNextPlanSub else s.nextDayDesc,
                        fontSize = 13.sp,
                        color = PrimaryDark.copy(0.75f),
                    )
                    PrimaryButton(
                        text = if (nextPlanDay == 1) s.planLesson else "${s.planDayBtn} $nextPlanDay",
                        onClick = { nextPlanDay?.let(onPlanDay) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                nextPlanDay == null && planDay != null -> {
                    Text(
                        s.homeAllDaysPlanned,
                        fontSize = 12.sp,
                        color = PrimaryDark.copy(0.75f),
                    )
                    TextButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
                        Text(s.homeCurrentPlanOpen, color = PrimaryDark, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> {
                    Text(
                        s.homeCurrentPlanEmpty,
                        fontSize = 13.sp,
                        color = PrimaryDark.copy(0.75f),
                    )
                }
            }

            HorizontalDivider(color = PrimaryDark.copy(alpha = 0.12f))

            HomeEmbeddedTlmList(
                s = s,
                unit = lesson.unit,
                kit = unitKit,
                tlmCatalog = tlmCatalog,
                onOpenFullKit = onOpenTlmKit,
            )
        }
    }
}

@Composable
private fun HomeFeedbackActionBlock(
    s: AppStrings,
    pending: PendingHomeAction,
    lessonDays: Int,
    onPrimary: () -> Unit,
    onReteach: () -> Unit,
    onPlanNextDay: () -> Unit,
    onReplan: () -> Unit,
    onDismiss: () -> Unit,
) {
    val action = pending.nextAction
    val hasNextDay = pending.completedDay < lessonDays
    val primaryLabel = when (action.action) {
        "reteach" -> s.reteachDay
        else -> localizedNextLabel(s, action)
    }
    val primaryHandler = when (action.action) {
        "reteach" -> onReteach
        else -> onPrimary
    }
    val secondary = when (action.action) {
        "reteach", "practice" -> if (hasNextDay) s.planNextDayBtn to onPlanNextDay else null
        "continue" -> s.rePlan to onReplan
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PrimarySteel),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(feedbackLabel(s, pending.feedback), fontSize = 12.sp, color = Color.White.copy(0.85f))
            Text(
                localizedNextLabel(s, action),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp,
            )
            Text(
                localizedNextDescription(s, action),
                fontSize = 13.sp,
                color = Color.White.copy(0.8f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = primaryHandler,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                ) {
                    Text(
                        primaryLabel,
                        color = PrimarySteel,
                        fontSize = 12.sp,
                        maxLines = 2,
                    )
                }
                secondary?.let { (label, handler) ->
                    OutlinedButton(onClick = handler, modifier = Modifier.weight(1f)) {
                        Text(label, color = Color.White, fontSize = 12.sp, maxLines = 2)
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(s.dismiss, color = Color.White.copy(0.85f), fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeEmbeddedTlmList(
    s: AppStrings,
    unit: Int,
    kit: TlmKitSummary,
    tlmCatalog: TlmResourceCatalog,
    onOpenFullKit: () -> Unit,
) {
    val missing = kit.items.filter { !it.owned }
    val essentialMissing = missing.filter { it.essential }
    val preview = (essentialMissing.ifEmpty { missing }).take(4)
    val needsAttention = kit.gapCount > 0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                tint = if (needsAttention) PrimaryDark else AccentTeal,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    s.homeTlmKitHeadline.format(unit),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = PrimaryDark,
                )
                Text(
                    if (needsAttention) {
                        s.homeTlmGapAlert.format(kit.gapCount, kit.items.size)
                    } else {
                        s.homeTlmReady
                    },
                    fontSize = 12.sp,
                    color = PrimaryDark.copy(0.7f),
                )
            }
        }

        if (needsAttention && preview.isNotEmpty()) {
            preview.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tlmCatalog.emojiFor(item.id), fontSize = 16.sp)
                    Text(
                        item.label,
                        fontSize = 13.sp,
                        color = PrimaryDark,
                        fontWeight = if (item.essential) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.essential) {
                        Text(s.tlmKitEssential, fontSize = 10.sp, color = AccentTeal)
                    }
                }
            }
        }

        TextButton(onClick = onOpenFullKit, modifier = Modifier.fillMaxWidth()) {
            Text(s.homeTlmViewAll, color = AccentTeal, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Icon(Icons.Default.ChevronRight, null, tint = AccentTeal, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (filled) AccentTeal else Color.White,
        ),
        border = BorderStroke(1.dp, if (filled) AccentTeal else SeasideBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (filled) 2.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (filled) Color.White else AccentTeal,
                modifier = Modifier.size(26.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (filled) Color.White else PrimaryDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = if (filled) Color.White.copy(0.85f) else PrimarySteel.copy(0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RoadmapTabContent(
    modifier: Modifier = Modifier,
    s: AppStrings,
    lessons: List<LessonItem>,
    assessmentGroups: List<AssessmentUnitGroup>,
    available: Boolean,
    currentId: String,
    pickCurrent: Boolean,
    planStorage: PlanStorage,
    plansRevision: Int,
    onSelectCurrent: (String) -> Unit,
    onOpenLesson: (String) -> Unit,
    onTlmKit: (Int) -> Unit,
    onAssessmentEntry: (type: String, groupId: String?, groupName: String?) -> Unit,
) {
    if (!available) {
        InfoBannerCard(s.comingSoon, s.comingSoonBody, modifier = Modifier.padding(16.dp))
        return
    }
    if (pickCurrent) {
        InfoBannerCard(s.tapLessonNow, "", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(NavBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            RoadmapMilestoneCard(
                s = s,
                title = s.assessmentBaseline,
                subtitle = s.assessmentBaselineDesc,
                icon = Icons.Default.Assessment,
                accent = true,
                onClick = { onAssessmentEntry("baseline", null, s.assessmentBaseline) },
            )
        }

        val units = lessons.map { it.unit }.distinct().sorted()
        units.forEach { unit ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${s.unitLabel} $unit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryDark,
                    )
                    TextButton(onClick = { onTlmKit(unit) }) {
                        Text(s.tlmKitBtn, fontSize = 12.sp, color = AccentTeal)
                    }
                }
            }
            items(lessons.filter { it.unit == unit }) { lesson ->
                val selected = lesson.id == currentId
                val lessonStatus = remember(lesson.id, plansRevision) {
                    PlanProgressHelper.getLessonStatus(lesson.id, lesson.days, planStorage)
                }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (pickCurrent) onSelectCurrent(lesson.id) else onOpenLesson(lesson.id)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (selected) AccentTeal else Color.White),
                    border = BorderStroke(1.dp, if (selected) AccentTeal else SeasideBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(lesson.curriculumTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (selected) androidx.compose.ui.graphics.Color.White else PrimaryDark, modifier = Modifier.weight(1f))
                            Text(
                                PlanProgressHelper.lessonStatusIcon(lessonStatus),
                                fontSize = 16.sp,
                                color = if (selected) androidx.compose.ui.graphics.Color.White else PrimaryDark,
                            )
                        }
                        Text(lesson.id, fontSize = 12.sp, color = if (selected) androidx.compose.ui.graphics.Color.White.copy(0.85f) else PrimaryDark.copy(0.55f))
                        Text("${s.unitLabel} ${lesson.unit} · ${s.pagesLabel} ${lesson.pages} · ${lesson.days} ${s.daysLabel}", fontSize = 11.sp, color = if (selected) androidx.compose.ui.graphics.Color.White.copy(0.7f) else PrimaryDark.copy(0.4f))
                    }
                }
            }
            assessmentGroups.filter { it.unit == unit }.forEach { group ->
                item {
                    RoadmapMilestoneCard(
                        s = s,
                        title = group.name,
                        subtitle = s.roadmapUnitTestSub.format(unit, group.focus),
                        icon = Icons.Default.Assessment,
                        accent = false,
                        onClick = { onAssessmentEntry("unit", group.id, group.name) },
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            RoadmapMilestoneCard(
                s = s,
                title = s.assessmentEndline,
                subtitle = s.assessmentEndlineDesc,
                icon = Icons.Default.Assessment,
                accent = true,
                onClick = { onAssessmentEntry("endline", null, s.assessmentEndline) },
            )
        }
    }
}

@Composable
private fun RoadmapMilestoneCard(
    s: AppStrings,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (accent) WarmPeach.copy(0.35f) else Color.White,
        ),
        border = BorderStroke(1.dp, if (accent) WarmPeach else SeasideBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PrimaryDark)
                Text(subtitle, fontSize = 12.sp, color = PrimarySteel.copy(0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimarySteel.copy(0.45f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileTabContent(
    modifier: Modifier = Modifier,
    prefs: UserPreferences,
    teacherAccount: TeacherAccount,
    onEditProfile: () -> Unit,
    onChangeLanguage: () -> Unit,
    onManageSubscription: () -> Unit,
    onSignOut: () -> Unit,
) {
    val s = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val languageLabel = IndianLanguages.options.find { it.value == lang }?.label ?: lang
    val teacherFallback = when (lang) {
        "mr" -> "शिक्षक"
        "hi" -> "शिक्षक"
        "ur" -> "استاد"
        else -> "Teacher"
    }
    val displayName = prefs.teacherName.ifBlank { teacherFallback }
    val initials = profileInitials(displayName)
    val stateLabel = IndiaStates.all.find { it.value == prefs.state }?.label ?: prefs.state
    val locationLabel = when (prefs.location) {
        "urban" -> s.urban
        "semi_urban" -> s.semiUrban
        "rural" -> s.rural
        else -> prefs.location.ifBlank { "—" }
    }
    val comfortLabel = when (prefs.englishComfort) {
        "difficult" -> s.comfortLow
        "stumbling" -> s.comfortMed
        "comfortable" -> s.comfortHigh
        else -> prefs.englishComfort.ifBlank { "—" }
    }
    val mediumLabel = prefs.medium.replace('_', ' ').replaceFirstChar { it.uppercase() }
    val grades = prefs.getTeacherGrades()
    val subjects = prefs.getTeacherSubjects()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
    ) {
        item {
            ProfileHeroHeader(
                initials = initials,
                name = displayName,
                phone = prefs.phoneNumber,
                school = prefs.schoolName,
            )
        }

        item {
            MembershipHeroCard(s = s, account = teacherAccount, modifier = Modifier.padding(horizontal = 16.dp))
        }

        item {
            ProfileSectionCard(title = s.tierPlanTitle) {
                ProfileActionRow(
                    label = s.subManagePlan,
                    value = teacherAccount.tier.label,
                    icon = Icons.Default.Star,
                    onClick = onManageSubscription,
                    showDivider = teacherAccount.tier != TierConfig.TierId.MAX,
                )
                if (teacherAccount.tier != TierConfig.TierId.MAX) {
                    ProfileActionRow(
                        label = s.subUpgrade,
                        value = TierConfig.upgradeLabel(teacherAccount.tier),
                        icon = Icons.Default.ChevronRight,
                        onClick = onManageSubscription,
                        showDivider = false,
                    )
                }
            }
        }

        item {
            ProfileSectionCard(title = s.whatYouTeach) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.grades, fontSize = 12.sp, color = PrimarySteel.copy(0.7f), fontWeight = FontWeight.Medium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        grades.forEach { g ->
                            ProfileChip(text = if (g == 0) "KG" else g.toString())
                        }
                        if (grades.isEmpty()) ProfileChip(text = "—")
                    }
                    if (subjects.isNotEmpty()) {
                        Text(s.subjects, fontSize = 12.sp, color = PrimarySteel.copy(0.7f), fontWeight = FontWeight.Medium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            subjects.forEach { sub ->
                                ProfileChip(text = sub.replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                    ProfileInfoRow(label = s.englishComfort, value = comfortLabel, showDivider = false)
                }
            }
        }

        item {
            ProfileSectionCard(title = s.whereYouWork) {
                ProfileInfoRow(label = s.labelState, value = stateLabel)
                ProfileInfoRow(label = s.labelDistrict, value = prefs.district.ifBlank { "—" })
                ProfileInfoRow(label = s.schoolMedium, value = mediumLabel, showDivider = false)
            }
        }

        item {
            ProfileSectionCard(title = s.schoolDetails) {
                ProfileInfoRow(label = s.labelSchool, value = prefs.schoolName.ifBlank { "—" })
                ProfileInfoRow(label = s.locationType, value = locationLabel)
                ProfileInfoRow(
                    label = s.pinCode,
                    value = prefs.pinCode.ifBlank { "—" },
                    showDivider = false,
                )
            }
        }

        item {
            ProfileSectionCard(title = s.accountSettings) {
                ProfileActionRow(
                    label = s.changeLanguage,
                    value = languageLabel,
                    icon = Icons.Default.Language,
                    onClick = onChangeLanguage,
                    showDivider = false,
                )
                ProfileActionRow(
                    label = s.editProfile,
                    icon = Icons.Default.Edit,
                    onClick = onEditProfile,
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(s.signOut, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
