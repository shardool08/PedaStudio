package com.tippingpoint.pedastudio

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tippingpoint.pedastudio.api.AccountApiClient
import com.tippingpoint.pedastudio.auth.PhoneAuthController
import com.tippingpoint.pedastudio.data.AssessmentRepository
import com.tippingpoint.pedastudio.data.CurriculumRepository
import com.tippingpoint.pedastudio.data.FirestoreRepository
import com.tippingpoint.pedastudio.data.FlashcardRepository
import com.tippingpoint.pedastudio.data.MaharashtraRepository
import com.tippingpoint.pedastudio.data.PlanStorage
import com.tippingpoint.pedastudio.data.ScanStorage
import com.tippingpoint.pedastudio.billing.RazorpayPaymentHandler
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.TierConfig
import com.tippingpoint.pedastudio.data.TlmResourceCatalog
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.AppLanguageProvider
import com.tippingpoint.pedastudio.navigation.Routes
import com.tippingpoint.pedastudio.ui.screens.AssessmentEntryScreen
import com.tippingpoint.pedastudio.ui.screens.AssessmentHubScreen
import com.tippingpoint.pedastudio.ui.screens.EditProfileScreen
import com.tippingpoint.pedastudio.ui.screens.LanguageScreen
import com.tippingpoint.pedastudio.ui.screens.LessonDetailScreen
import com.tippingpoint.pedastudio.ui.screens.LoginScreen
import com.tippingpoint.pedastudio.ui.screens.MainHomeScreen
import com.tippingpoint.pedastudio.ui.screens.PlanViewScreen
import com.tippingpoint.pedastudio.ui.screens.QuickPlanScreen
import com.tippingpoint.pedastudio.ui.screens.RegisterStep2Screen
import com.tippingpoint.pedastudio.ui.screens.RegisterStep3Screen
import com.tippingpoint.pedastudio.ui.screens.ScanScreen
import com.tippingpoint.pedastudio.ui.screens.SubscriptionScreen
import com.tippingpoint.pedastudio.ui.screens.TlmKitScreen
import com.tippingpoint.pedastudio.ui.screens.WorksheetScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PedaStudioApp(
    auth: PhoneAuthController,
    paymentHandler: RazorpayPaymentHandler,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences(context.applicationContext) }
    val curriculum = remember { CurriculumRepository(context.applicationContext) }
    val maharashtra = remember { MaharashtraRepository(context.applicationContext) }
    val planStorage = remember { PlanStorage(context.applicationContext) }
    val scanStorage = remember { ScanStorage(context.applicationContext) }
    val flashcards = remember { FlashcardRepository(context.applicationContext) }
    val tlmCatalog = remember { TlmResourceCatalog(context.applicationContext) }
    val assessmentRepo = remember { AssessmentRepository(context.applicationContext) }
    val firestore = remember { FirestoreRepository(tlmCatalog) }
    val nav = rememberNavController()
    var language by remember { mutableStateOf(prefs.language) }
    var plansRevision by remember { mutableIntStateOf(0) }
    var teacherAccount by remember {
        mutableStateOf(prefs.loadCachedTeacherAccount() ?: TierConfig.defaultAccount())
    }

    LaunchedEffect(auth.isLoggedIn) {
        if (!auth.isLoggedIn) return@LaunchedEffect
        try {
            firestore.ensureCatalogSeeded()
            firestore.pullProfile(prefs).onSuccess { pulled ->
                if (pulled) language = prefs.language
            }
            firestore.syncAllPlans(planStorage).onSuccess { plansRevision++ }
            tlmCatalog.applyRemoteImageUrls(firestore.loadTlmImageUrls())
            val cached = prefs.loadCachedTeacherAccount()
            if (cached != null) teacherAccount = cached
            val idToken = auth.getIdToken()
            // AccountApiClient blocks on OkHttp, so it must not run on the Compose main thread.
            val account = withContext(Dispatchers.IO) {
                AccountApiClient.fetchAccount(idToken, cached).getOrNull()
            }
            if (account != null) {
                teacherAccount = account
                prefs.applyTeacherAccount(account)
            }
        } catch (_: Exception) {
            // Keep app usable offline if cloud sync fails on startup.
        }
    }

    val start = when {
        auth.isLoggedIn && prefs.profileComplete -> Routes.HOME
        auth.isLoggedIn -> Routes.REGISTER_STEP2
        else -> Routes.LANGUAGE
    }

    AppLanguageProvider(language = language) {
        NavHost(navController = nav, startDestination = start) {
            composable(Routes.LANGUAGE) {
                LanguageScreen(
                    selected = language,
                    onSelectedChange = {
                        language = it
                        prefs.language = it
                    },
                    onContinue = {
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LANGUAGE) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.LOGIN) {
                val activity = context as MainActivity
                LoginScreen(
                    auth = auth,
                    onSendOtp = { phone -> auth.sendOtp(activity, phone) },
                    onVerifyOtp = { code -> auth.verifyOtp(code) },
                    onSuccess = { phone ->
                        if (phone != "verified") prefs.phoneNumber = phone.filter { it.isDigit() }.takeLast(10)
                        val dest = if (prefs.profileComplete) Routes.HOME else Routes.REGISTER_STEP2
                        nav.navigate(dest) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.REGISTER_STEP2) {
                RegisterStep2Screen(prefs = prefs, maharashtra = maharashtra) {
                    nav.navigate(Routes.REGISTER_STEP3)
                }
            }
            composable(Routes.REGISTER_STEP3) {
                RegisterStep3Screen(
                    prefs = prefs,
                    maharashtra = maharashtra,
                    onBack = { nav.popBackStack() },
                ) {
                    scope.launch { firestore.pushProfile(prefs) }
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER_STEP2) { inclusive = true }
                    }
                }
            }
            composable(Routes.HOME) {
                MainHomeScreen(
                    prefs = prefs,
                    curriculum = curriculum,
                    tlmCatalog = tlmCatalog,
                    assessmentRepo = assessmentRepo,
                    planStorage = planStorage,
                    plansRevision = plansRevision,
                    teacherAccount = teacherAccount,
                    onEditProfile = { nav.navigate(Routes.EDIT_PROFILE) },
                    onQuickPlan = { lessonId, day, mode, reteachNotes ->
                        nav.navigate(Routes.quickPlan(lessonId, day, mode, reteachNotes))
                    },
                    onScan = { lessonId -> nav.navigate(Routes.scan(lessonId)) },
                    onWorksheet = { lessonId, day -> nav.navigate(Routes.worksheet(lessonId, day)) },
                    onTlmKit = { unit -> nav.navigate(Routes.tlmKit(prefs.lastViewedGrade, unit)) },
                    onAssessmentEntry = { type, groupId, groupName ->
                        nav.navigate(
                            Routes.assessmentEntry(
                                prefs.lastViewedGrade,
                                type,
                                groupId ?: "_",
                                groupName ?: "_",
                            ),
                        )
                    },
                    onViewPlan = { lessonId, day -> nav.navigate(Routes.planView(lessonId, day)) },
                    onOpenLesson = { lessonId -> nav.navigate(Routes.lessonDetail(lessonId)) },
                    onChangeLanguage = { nav.navigate(Routes.CHANGE_LANGUAGE) },
                    onManageSubscription = { nav.navigate(Routes.subscription()) },
                    onLessonSelected = { scope.launch { firestore.pushProfile(prefs) } },
                    onDayCompleted = { lessonId, day, feedback ->
                        planStorage.completePlan(lessonId, day, feedback)
                        plansRevision++
                        scope.launch { firestore.pushPlanMeta(lessonId, day, planStorage) }
                    },
                    onPlanStateChanged = { plansRevision++ },
                    onSignOut = {
                        auth.signOut()
                        val savedLang = prefs.language
                        prefs.clearSession()
                        prefs.language = savedLang
                        language = savedLang
                        nav.navigate(Routes.LANGUAGE) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    prefs = prefs,
                    maharashtra = maharashtra,
                    onBack = { nav.popBackStack() },
                    onSaved = {
                        scope.launch { firestore.pushProfile(prefs) }
                        nav.popBackStack()
                    },
                )
            }
            composable(
                route = Routes.SUBSCRIPTION,
                arguments = listOf(
                    navArgument("highlight") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val highlightRaw = entry.arguments?.getString("highlight").orEmpty()
                val highlight = when (highlightRaw) {
                    "prime" -> TierConfig.TierId.PRIME
                    "max" -> TierConfig.TierId.MAX
                    else -> null
                }
                SubscriptionScreen(
                    prefs = prefs,
                    auth = auth,
                    paymentHandler = paymentHandler,
                    account = teacherAccount,
                    highlightTier = highlight,
                    onBack = { nav.popBackStack() },
                    onAccountUpdated = { account ->
                        teacherAccount = AccountApiClient.mergeAccountUpdate(teacherAccount, account)
                        prefs.applyTeacherAccount(teacherAccount)
                    },
                )
            }
            composable(Routes.CHANGE_LANGUAGE) {
                LanguageScreen(
                    selected = language,
                    onSelectedChange = {
                        language = it
                        prefs.language = it
                    },
                    onContinue = {
                        scope.launch { firestore.pushProfile(prefs) }
                        nav.popBackStack()
                    },
                    fromProfile = true,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = Routes.QUICK_PLAN,
                arguments = listOf(
                    navArgument("lessonId") { type = NavType.StringType },
                    navArgument("day") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("mode") { type = NavType.StringType; defaultValue = "" },
                    navArgument("reteachNotes") { type = NavType.StringType; defaultValue = "" },
                    navArgument("afterUnitTest") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                val rawId = entry.arguments?.getString("lessonId")
                val lessonId = rawId?.let { Routes.decodeLessonId(it) }
                if (lessonId.isNullOrBlank()) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                val day = entry.arguments?.getInt("day") ?: 1
                val mode = entry.arguments?.getString("mode").orEmpty()
                val reteachNotes = entry.arguments?.getString("reteachNotes")?.let { Uri.decode(it) }.orEmpty()
                val afterUnitTest = entry.arguments?.getBoolean("afterUnitTest") ?: false
                QuickPlanScreen(
                    lessonId = lessonId,
                    initialDay = day,
                    planningMode = mode,
                    initialReteachNotes = reteachNotes,
                    afterUnitTest = afterUnitTest,
                    prefs = prefs,
                    curriculum = curriculum,
                    planStorage = planStorage,
                    tlmCatalog = tlmCatalog,
                    firestore = firestore,
                    auth = auth,
                    teacherAccount = teacherAccount,
                    onAccountUpdated = { account ->
                        teacherAccount = AccountApiClient.mergeAccountUpdate(teacherAccount, account)
                        prefs.applyTeacherAccount(teacherAccount)
                    },
                    onUpgrade = { nav.navigate(Routes.subscription("prime")) },
                    onBack = { nav.popBackStack() },
                    onPlanReady = { id, readyDay ->
                        prefs.pendingScanLinkId.takeIf { it.isNotBlank() }?.let { scanId ->
                            scanStorage.linkPlan(scanId, id, readyDay)
                            prefs.pendingScanLinkId = ""
                        }
                        prefs.setCurrentLesson(prefs.lastViewedGrade, prefs.lastViewedSubject, id)
                        plansRevision++
                        nav.navigate(Routes.planView(id, readyDay)) {
                            popUpTo(Routes.quickPlan(id, day)) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = Routes.PLAN_VIEW,
                arguments = listOf(
                    navArgument("lessonId") { type = NavType.StringType },
                    navArgument("day") { type = NavType.IntType },
                ),
            ) { entry ->
                val lessonId = entry.arguments?.getString("lessonId")?.let { Routes.decodeLessonId(it) }
                if (lessonId.isNullOrBlank()) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                val day = entry.arguments?.getInt("day") ?: 1
                PlanViewScreen(
                    lessonId = lessonId,
                    day = day,
                    curriculum = curriculum,
                    planStorage = planStorage,
                    firestore = firestore,
                    tlmCatalog = tlmCatalog,
                    onBack = { nav.popBackStack() },
                    onProgressChanged = { plansRevision++ },
                    onOpenLessonProgress = { nav.navigate(Routes.lessonDetail(lessonId)) },
                )
            }
            composable(
                route = Routes.LESSON_DETAIL,
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { entry ->
                val lessonId = entry.arguments?.getString("lessonId")?.let { Routes.decodeLessonId(it) }
                if (lessonId.isNullOrBlank()) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                LessonDetailScreen(
                    lessonId = lessonId,
                    prefs = prefs,
                    curriculum = curriculum,
                    planStorage = planStorage,
                    firestore = firestore,
                    teacherAccount = teacherAccount,
                    plansRevision = plansRevision,
                    onBack = { nav.popBackStack() },
                    onQuickPlan = { id, day, mode, reteachNotes ->
                        nav.navigate(Routes.quickPlan(id, day, mode, reteachNotes))
                    },
                    onViewPlan = { id, d -> nav.navigate(Routes.planView(id, d)) },
                    onWorksheet = { id, day -> nav.navigate(Routes.worksheet(id, day)) },
                    onUpgrade = { nav.navigate(Routes.subscription("prime")) },
                    onProgressChanged = { plansRevision++ },
                )
            }
            composable(
                route = Routes.FLASHCARDS,
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { entry ->
                val lessonId = entry.arguments?.getString("lessonId")?.let { Routes.decodeLessonId(it) }
                if (lessonId.isNullOrBlank()) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                val lesson = curriculum.findLessonAnywhere(lessonId)
                LaunchedEffect(lessonId) {
                    nav.navigate(Routes.tlmKit(lesson?.gradeNumber() ?: prefs.lastViewedGrade, lesson?.unit ?: 1)) {
                        popUpTo(Routes.flashcards(lessonId)) { inclusive = true }
                    }
                }
            }
            composable(
                route = Routes.SCAN,
                arguments = listOf(
                    navArgument("lessonId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val lessonId = entry.arguments?.getString("lessonId")?.let { Routes.decodeLessonId(it) }
                ScanScreen(
                    lessonId = lessonId,
                    prefs = prefs,
                    curriculum = curriculum,
                    assessmentRepo = assessmentRepo,
                    tlmCatalog = tlmCatalog,
                    planStorage = planStorage,
                    scanStorage = scanStorage,
                    auth = auth,
                    teacherAccount = teacherAccount,
                    onBack = { nav.popBackStack() },
                    onUpgrade = { nav.navigate(Routes.subscription("max")) },
                    onAccountUpdated = { account ->
                        teacherAccount = AccountApiClient.mergeAccountUpdate(teacherAccount, account)
                        prefs.applyTeacherAccount(teacherAccount)
                    },
                    onOpenWorksheet = { id, day -> nav.navigate(Routes.worksheet(id, day)) },
                    onOpenQuickPlan = { id, day, scanId ->
                        prefs.pendingScanLinkId = scanId
                        nav.navigate(Routes.quickPlan(id, day, "", ""))
                    },
                    onViewPlan = { id, day -> nav.navigate(Routes.planView(id, day)) },
                    onOpenTlmKit = { unit -> nav.navigate(Routes.tlmKit(prefs.lastViewedGrade, unit)) },
                    onOpenAssessment = { type, groupId, groupName ->
                        nav.navigate(
                            Routes.assessmentEntry(
                                prefs.lastViewedGrade,
                                type,
                                groupId ?: "_",
                                groupName ?: "_",
                            ),
                        )
                    },
                )
            }
            composable(
                route = Routes.WORKSHEET,
                arguments = listOf(
                    navArgument("lessonId") { type = NavType.StringType },
                    navArgument("day") { type = NavType.IntType; defaultValue = 1 },
                ),
            ) { entry ->
                val lessonId = entry.arguments?.getString("lessonId")?.let { Routes.decodeLessonId(it) }
                if (lessonId.isNullOrBlank()) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                val day = entry.arguments?.getInt("day") ?: 1
                WorksheetScreen(
                    lessonId = lessonId,
                    day = day,
                    prefs = prefs,
                    curriculum = curriculum,
                    auth = auth,
                    teacherAccount = teacherAccount,
                    onBack = { nav.popBackStack() },
                    onUpgrade = { nav.navigate(Routes.subscription("prime")) },
                    onAccountUpdated = { account ->
                        teacherAccount = AccountApiClient.mergeAccountUpdate(teacherAccount, account)
                        prefs.applyTeacherAccount(teacherAccount)
                    },
                )
            }
            composable(
                route = Routes.TLM_KIT,
                arguments = listOf(
                    navArgument("grade") { type = NavType.IntType },
                    navArgument("unit") { type = NavType.IntType; defaultValue = 1 },
                ),
            ) { entry ->
                val grade = entry.arguments?.getInt("grade") ?: prefs.lastViewedGrade
                val unit = entry.arguments?.getInt("unit") ?: 1
                TlmKitScreen(
                    grade = grade,
                    initialUnit = unit,
                    prefs = prefs,
                    curriculum = curriculum,
                    tlmCatalog = tlmCatalog,
                    teacherAccount = teacherAccount,
                    onBack = { nav.popBackStack() },
                    onUpgrade = { nav.navigate(Routes.subscription("prime")) },
                )
            }
            composable(
                route = Routes.ASSESSMENT,
                arguments = listOf(navArgument("grade") { type = NavType.IntType }),
            ) { entry ->
                val grade = entry.arguments?.getInt("grade") ?: prefs.lastViewedGrade
                AssessmentHubScreen(
                    grade = grade,
                    subject = prefs.lastViewedSubject,
                    prefs = prefs,
                    curriculum = curriculum,
                    assessmentRepo = assessmentRepo,
                    auth = auth,
                    teacherAccount = teacherAccount,
                    onBack = { nav.popBackStack() },
                    onOpenEntry = { type, groupId, groupName ->
                        nav.navigate(Routes.assessmentEntry(grade, type, groupId ?: "_", groupName ?: "_"))
                    },
                    onAccountUpdated = { account ->
                        teacherAccount = AccountApiClient.mergeAccountUpdate(teacherAccount, account)
                        prefs.applyTeacherAccount(teacherAccount)
                    },
                )
            }
            composable(
                route = Routes.ASSESSMENT_ENTRY,
                arguments = listOf(
                    navArgument("grade") { type = NavType.IntType },
                    navArgument("type") { type = NavType.StringType },
                    navArgument("groupId") { type = NavType.StringType; defaultValue = "_" },
                    navArgument("groupName") { type = NavType.StringType; defaultValue = "_" },
                ),
            ) { entry ->
                val grade = entry.arguments?.getInt("grade") ?: prefs.lastViewedGrade
                val type = entry.arguments?.getString("type") ?: "unit"
                val groupId = Routes.decodeGroupId(entry.arguments?.getString("groupId"))
                val groupName = Routes.decodeGroupId(entry.arguments?.getString("groupName"))
                AssessmentEntryScreen(
                    type = type,
                    groupId = groupId,
                    groupName = groupName,
                    grade = grade,
                    subject = prefs.lastViewedSubject,
                    prefs = prefs,
                    auth = auth,
                    teacherAccount = teacherAccount,
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                    onPlanReteach = { lessonId, notes, afterUnitTest ->
                        nav.navigate(Routes.quickPlan(lessonId, 1, "reteach", notes, afterUnitTest))
                    },
                    onUpgrade = { nav.navigate(Routes.subscription()) },
                    onAccountUpdated = { account ->
                        teacherAccount = AccountApiClient.mergeAccountUpdate(teacherAccount, account)
                        prefs.applyTeacherAccount(teacherAccount)
                    },
                )
            }
        }
    }
}
