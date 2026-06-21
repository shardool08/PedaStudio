package com.tippingpoint.pedastudio.i18n

data class CommonStrings(
    val chooseLanguage: String,
    val appLanguage: String,
    val selectLanguage: String,
    val translationsComingSoon: String,
    val search: String,
    val noMatches: String,
    val tapToSelect: String,
    val chooseOne: String,
    val continueBtn: String,
    val goToHome: String,
    val closeBtn: String,
    val dismiss: String,
    val comingSoon: String,
    val comingSoonBody: String,
    val lessonNotFound: String,
)

data class AuthStrings(
    val enterMobile: String,
    val sendOtp: String,
    val verifyOtp: String,
    val otpPlaceholder: String,
)

data class RegistrationStrings(
    val registration: String,
    val step2: String,
    val step3: String,
    val yourName: String,
    val yourNameSub: String,
    val fullName: String,
    val whatYouTeach: String,
    val whatYouTeachSub: String,
    val grades: String,
    val subjects: String,
    val whereYouWork: String,
    val whereYouWorkSub: String,
    val state: String,
    val selectState: String,
    val district: String,
    val selectDistrict: String,
    val districtName: String,
    val adminType: String,
    val zp: String,
    val selectZp: String,
    val corp: String,
    val selectCorp: String,
    val langComfort: String,
    val langComfortSub: String,
    val schoolMedium: String,
    val selectMedium: String,
    val englishComfort: String,
    val comfortLow: String,
    val comfortMed: String,
    val comfortHigh: String,
    val schoolDetails: String,
    val schoolDetailsSub: String,
    val schoolName: String,
    val locationType: String,
    val urban: String,
    val semiUrban: String,
    val rural: String,
    val pinCode: String,
    val classSize: String,
    val classSizeSub: String,
    val students: String,
    val classroomResources: String,
    val classroomResourcesSub: String,
    val teachingMaterials: String,
    val technology: String,
    val technologySub: String,
    val internetAccess: String,
    val selectInternet: String,
    val printingAccess: String,
    val selectPrinting: String,
)

data class NavStrings(
    val navHome: String,
    val navRoadmap: String,
    val navProfile: String,
)

data class HomeStrings(
    val helloTeacher: String,
    val setCurrentLesson: String,
    val setCurrentLessonBody: String,
    val goToRoadmap: String,
    val current: String,
    val upNext: String,
    val planLessonSoon: String,
    val tapLessonNow: String,
    val unitLabel: String,
    val homePlanSub: String,
    val homePlanSubProgress: String,
    val homeScanSub: String,
    val homeOpenRoadmap: String,
    val homeUnitProgress: String,
    val homeCurrentPlan: String,
    val homeCurrentPlanDay: String,
    val homeCurrentPlanFocus: String,
    val homeCurrentPlanOpen: String,
    val homeCurrentPlanEmpty: String,
    val homeCurrentLesson: String,
    val homeNextUp: String,
    val homeNextPlanSub: String,
    val homeNextPlanAction: String,
    val homeAllDaysPlanned: String,
    val homeTlmKitHeadline: String,
    val homeTlmGapAlert: String,
    val homeTlmReady: String,
    val homeTlmViewAll: String,
    val homeTlmScanHint: String,
)

data class ProfileStrings(
    val profileTitle: String,
    val signOut: String,
    val labelName: String,
    val labelState: String,
    val labelDistrict: String,
    val labelSchool: String,
    val labelMedium: String,
    val labelPhone: String,
    val labelGrades: String,
    val editProfile: String,
    val changeLanguage: String,
    val accountSettings: String,
    val saveProfile: String,
)

data class PlanStrings(
    val planLesson: String,
    val flashcardsBtn: String,
    val quickPlanTitle: String,
    val generatePlan: String,
    val generatingPlan: String,
    val planError: String,
    val apiNotConfigured: String,
    val dayLabel: String,
    val goalLabel: String,
    val hookLabel: String,
    val teachingLabel: String,
    val practiceLabel: String,
    val assessmentLabel: String,
    val notesLabel: String,
    val viewPlan: String,
    val flashcardsTitle: String,
    val pagesLabel: String,
    val daysLabel: String,
    val lessonProgressTitle: String,
    val dayWisePlan: String,
    val planDayBtn: String,
    val markCompleted: String,
    val rePlan: String,
    val feedbackTitle: String,
    val feedbackSub: String,
    val fbWentWell: String,
    val fbSomeStruggled: String,
    val fbMostDidntUnderstand: String,
    val fbCouldntFinish: String,
    val fbReadyForMore: String,
    val recommendedNext: String,
    val statusNotStarted: String,
    val statusPlanned: String,
    val statusInProgress: String,
    val statusCompleted: String,
    val viewLessonProgress: String,
    val nextDayAction: String,
    val nextLessonAction: String,
    val nextDayDesc: String,
    val nextLessonDesc: String,
    val practiceDesc: String,
    val reteachDesc: String,
    val continueDesc: String,
    val addPracticeDay: String,
    val reteachDay: String,
    val continueSamePlan: String,
    val planNextDayBtn: String,
    val reteachNotesTitle: String,
    val reteachNotesSub: String,
    val reteachNotesHint: String,
    val modeReteachBanner: String,
    val modePracticeBanner: String,
    val modeContinueBanner: String,
    val generatingStatusReading: String,
    val generatingStatusBuilding: String,
    val generatingStatusTlm: String,
    val generatingStatusAlmost: String,
    val generatingStatusDone: String,
)

data class TierStrings(
    val tierPlanTitle: String,
    val tierCurrentPlan: String,
    val tierPlansRemaining: String,
    val tierPlansUnlimited: String,
    val tierUpgradeHint: String,
    val tierGradeLocked: String,
)

data class SubscriptionStrings(
    val subTitle: String,
    val subSubtitle: String,
    val subActive: String,
    val subBasicTagline: String,
    val subPrimeTagline: String,
    val subMaxTagline: String,
    val subRenewsOn: String,
    val subMonthly: String,
    val subYearlySave: String,
    val subFreeForever: String,
    val subCurrentPlan: String,
    val subMostPopular: String,
    val subSubscribePrime: String,
    val subSubscribeMax: String,
    val subContactUpgrade: String,
    val subUpgrade: String,
    val subManagePlan: String,
    val subLoadError: String,
    val subPlanUnavailable: String,
    val subPaymentSuccess: String,
    val subPaymentFailed: String,
    val subPaymentsOffline: String,
    val subWhatsAppSupport: String,
    val subWhatsAppPrefill: String,
    val subSaveInr: String,
)

data class ScanStrings(
    val scanTitle: String,
    val scanBtn: String,
    val scanSubtitle: String,
    val scanPickPhoto: String,
    val scanAnalyze: String,
    val scanAnalyzing: String,
    val scanError: String,
    val scanImageError: String,
    val scanLocked: String,
    val scanResultTitle: String,
    val scanResultsTitle: String,
    val scanOpenCamera: String,
    val scanCameraHint: String,
    val scanCameraCancelled: String,
    val scanCaptureAgain: String,
    val scanCreatePlan: String,
    val scanNextSteps: String,
    val scanVocabulary: String,
    val scanSuggested: String,
)

data class WorksheetStrings(
    val worksheetBtn: String,
    val worksheetTitle: String,
    val worksheetLocked: String,
    val worksheetError: String,
    val worksheetRetry: String,
    val worksheetShare: String,
    val worksheetTeacherNotes: String,
)

data class TlmStrings(
    val tlmKitBtn: String,
    val tlmKitTitle: String,
    val tlmKitUnit: String,
    val tlmKitYear: String,
    val tlmKitLocked: String,
    val tlmKitYearLocked: String,
    val tlmKitEssential: String,
    val tlmKitSummary: String,
    val tlmKitShare: String,
)

data class AssessmentStrings(
    val assessmentBtn: String,
    val assessmentHubTitle: String,
    val assessmentHubSub: String,
    val assessmentBaseline: String,
    val assessmentBaselineDesc: String,
    val assessmentEndline: String,
    val assessmentEndlineDesc: String,
    val assessmentUnitTests: String,
    val assessmentUnitTestsDesc: String,
    val assessmentEnterScores: String,
    val assessmentUpdateScores: String,
    val assessmentEntryTitle: String,
    val assessmentScoreLabel: String,
    val assessmentScoreHint: String,
    val assessmentSaveError: String,
    val assessmentLoadError: String,
    val assessmentRetry: String,
)

data class RoadmapStrings(
    val roadmapUnitTestSub: String,
)

/** Root i18n bundle — nested groups keep constructors under Dalvik register limits. */
data class AppStrings(
    val common: CommonStrings,
    val auth: AuthStrings,
    val reg: RegistrationStrings,
    val nav: NavStrings,
    val home: HomeStrings,
    val profile: ProfileStrings,
    val plan: PlanStrings,
    val tier: TierStrings,
    val subscription: SubscriptionStrings,
    val scan: ScanStrings,
    val worksheet: WorksheetStrings,
    val tlm: TlmStrings,
    val assessment: AssessmentStrings,
    val roadmap: RoadmapStrings,
) {
    val chooseLanguage get() = common.chooseLanguage
    val appLanguage get() = common.appLanguage
    val selectLanguage get() = common.selectLanguage
    val translationsComingSoon get() = common.translationsComingSoon
    val search get() = common.search
    val noMatches get() = common.noMatches
    val tapToSelect get() = common.tapToSelect
    val chooseOne get() = common.chooseOne
    val continueBtn get() = common.continueBtn
    val goToHome get() = common.goToHome
    val closeBtn get() = common.closeBtn
    val dismiss get() = common.dismiss
    val comingSoon get() = common.comingSoon
    val comingSoonBody get() = common.comingSoonBody
    val lessonNotFound get() = common.lessonNotFound

    val enterMobile get() = auth.enterMobile
    val sendOtp get() = auth.sendOtp
    val verifyOtp get() = auth.verifyOtp
    val otpPlaceholder get() = auth.otpPlaceholder

    val registration get() = reg.registration
    val step2 get() = reg.step2
    val step3 get() = reg.step3
    val yourName get() = reg.yourName
    val yourNameSub get() = reg.yourNameSub
    val fullName get() = reg.fullName
    val whatYouTeach get() = reg.whatYouTeach
    val whatYouTeachSub get() = reg.whatYouTeachSub
    val grades get() = reg.grades
    val subjects get() = reg.subjects
    val whereYouWork get() = reg.whereYouWork
    val whereYouWorkSub get() = reg.whereYouWorkSub
    val state get() = reg.state
    val selectState get() = reg.selectState
    val district get() = reg.district
    val selectDistrict get() = reg.selectDistrict
    val districtName get() = reg.districtName
    val adminType get() = reg.adminType
    val zp get() = reg.zp
    val selectZp get() = reg.selectZp
    val corp get() = reg.corp
    val selectCorp get() = reg.selectCorp
    val langComfort get() = reg.langComfort
    val langComfortSub get() = reg.langComfortSub
    val schoolMedium get() = reg.schoolMedium
    val selectMedium get() = reg.selectMedium
    val englishComfort get() = reg.englishComfort
    val comfortLow get() = reg.comfortLow
    val comfortMed get() = reg.comfortMed
    val comfortHigh get() = reg.comfortHigh
    val schoolDetails get() = reg.schoolDetails
    val schoolDetailsSub get() = reg.schoolDetailsSub
    val schoolName get() = reg.schoolName
    val locationType get() = reg.locationType
    val urban get() = reg.urban
    val semiUrban get() = reg.semiUrban
    val rural get() = reg.rural
    val pinCode get() = reg.pinCode
    val classSize get() = reg.classSize
    val classSizeSub get() = reg.classSizeSub
    val students get() = reg.students
    val classroomResources get() = reg.classroomResources
    val classroomResourcesSub get() = reg.classroomResourcesSub
    val teachingMaterials get() = reg.teachingMaterials
    val technology get() = reg.technology
    val technologySub get() = reg.technologySub
    val internetAccess get() = reg.internetAccess
    val selectInternet get() = reg.selectInternet
    val printingAccess get() = reg.printingAccess
    val selectPrinting get() = reg.selectPrinting

    val navHome get() = nav.navHome
    val navRoadmap get() = nav.navRoadmap
    val navProfile get() = nav.navProfile

    val helloTeacher get() = home.helloTeacher
    val setCurrentLesson get() = home.setCurrentLesson
    val setCurrentLessonBody get() = home.setCurrentLessonBody
    val goToRoadmap get() = home.goToRoadmap
    val current get() = home.current
    val upNext get() = home.upNext
    val planLessonSoon get() = home.planLessonSoon
    val tapLessonNow get() = home.tapLessonNow
    val unitLabel get() = home.unitLabel
    val homePlanSub get() = home.homePlanSub
    val homePlanSubProgress get() = home.homePlanSubProgress
    val homeScanSub get() = home.homeScanSub
    val homeOpenRoadmap get() = home.homeOpenRoadmap
    val homeUnitProgress get() = home.homeUnitProgress
    val homeCurrentPlan get() = home.homeCurrentPlan
    val homeCurrentPlanDay get() = home.homeCurrentPlanDay
    val homeCurrentPlanFocus get() = home.homeCurrentPlanFocus
    val homeCurrentPlanOpen get() = home.homeCurrentPlanOpen
    val homeCurrentPlanEmpty get() = home.homeCurrentPlanEmpty
    val homeCurrentLesson get() = home.homeCurrentLesson
    val homeNextUp get() = home.homeNextUp
    val homeNextPlanSub get() = home.homeNextPlanSub
    val homeNextPlanAction get() = home.homeNextPlanAction
    val homeAllDaysPlanned get() = home.homeAllDaysPlanned
    val homeTlmKitHeadline get() = home.homeTlmKitHeadline
    val homeTlmGapAlert get() = home.homeTlmGapAlert
    val homeTlmReady get() = home.homeTlmReady
    val homeTlmViewAll get() = home.homeTlmViewAll
    val homeTlmScanHint get() = home.homeTlmScanHint

    val profileTitle get() = profile.profileTitle
    val signOut get() = profile.signOut
    val labelName get() = profile.labelName
    val labelState get() = profile.labelState
    val labelDistrict get() = profile.labelDistrict
    val labelSchool get() = profile.labelSchool
    val labelMedium get() = profile.labelMedium
    val labelPhone get() = profile.labelPhone
    val labelGrades get() = profile.labelGrades
    val editProfile get() = profile.editProfile
    val changeLanguage get() = profile.changeLanguage
    val accountSettings get() = profile.accountSettings
    val saveProfile get() = profile.saveProfile

    val planLesson get() = plan.planLesson
    val flashcardsBtn get() = plan.flashcardsBtn
    val quickPlanTitle get() = plan.quickPlanTitle
    val generatePlan get() = plan.generatePlan
    val generatingPlan get() = plan.generatingPlan
    val planError get() = plan.planError
    val apiNotConfigured get() = plan.apiNotConfigured
    val dayLabel get() = plan.dayLabel
    val goalLabel get() = plan.goalLabel
    val hookLabel get() = plan.hookLabel
    val teachingLabel get() = plan.teachingLabel
    val practiceLabel get() = plan.practiceLabel
    val assessmentLabel get() = plan.assessmentLabel
    val notesLabel get() = plan.notesLabel
    val viewPlan get() = plan.viewPlan
    val flashcardsTitle get() = plan.flashcardsTitle
    val pagesLabel get() = plan.pagesLabel
    val daysLabel get() = plan.daysLabel
    val lessonProgressTitle get() = plan.lessonProgressTitle
    val dayWisePlan get() = plan.dayWisePlan
    val planDayBtn get() = plan.planDayBtn
    val markCompleted get() = plan.markCompleted
    val rePlan get() = plan.rePlan
    val feedbackTitle get() = plan.feedbackTitle
    val feedbackSub get() = plan.feedbackSub
    val fbWentWell get() = plan.fbWentWell
    val fbSomeStruggled get() = plan.fbSomeStruggled
    val fbMostDidntUnderstand get() = plan.fbMostDidntUnderstand
    val fbCouldntFinish get() = plan.fbCouldntFinish
    val fbReadyForMore get() = plan.fbReadyForMore
    val recommendedNext get() = plan.recommendedNext
    val statusNotStarted get() = plan.statusNotStarted
    val statusPlanned get() = plan.statusPlanned
    val statusInProgress get() = plan.statusInProgress
    val statusCompleted get() = plan.statusCompleted
    val viewLessonProgress get() = plan.viewLessonProgress
    val nextDayAction get() = plan.nextDayAction
    val nextLessonAction get() = plan.nextLessonAction
    val nextDayDesc get() = plan.nextDayDesc
    val nextLessonDesc get() = plan.nextLessonDesc
    val practiceDesc get() = plan.practiceDesc
    val reteachDesc get() = plan.reteachDesc
    val continueDesc get() = plan.continueDesc
    val addPracticeDay get() = plan.addPracticeDay
    val reteachDay get() = plan.reteachDay
    val continueSamePlan get() = plan.continueSamePlan
    val planNextDayBtn get() = plan.planNextDayBtn
    val reteachNotesTitle get() = plan.reteachNotesTitle
    val reteachNotesSub get() = plan.reteachNotesSub
    val reteachNotesHint get() = plan.reteachNotesHint
    val modeReteachBanner get() = plan.modeReteachBanner
    val modePracticeBanner get() = plan.modePracticeBanner
    val modeContinueBanner get() = plan.modeContinueBanner
    val generatingStatusReading get() = plan.generatingStatusReading
    val generatingStatusBuilding get() = plan.generatingStatusBuilding
    val generatingStatusTlm get() = plan.generatingStatusTlm
    val generatingStatusAlmost get() = plan.generatingStatusAlmost
    val generatingStatusDone get() = plan.generatingStatusDone

    val tierPlanTitle get() = tier.tierPlanTitle
    val tierCurrentPlan get() = tier.tierCurrentPlan
    val tierPlansRemaining get() = tier.tierPlansRemaining
    val tierPlansUnlimited get() = tier.tierPlansUnlimited
    val tierUpgradeHint get() = tier.tierUpgradeHint
    val tierGradeLocked get() = tier.tierGradeLocked

    val subTitle get() = subscription.subTitle
    val subSubtitle get() = subscription.subSubtitle
    val subActive get() = subscription.subActive
    val subBasicTagline get() = subscription.subBasicTagline
    val subPrimeTagline get() = subscription.subPrimeTagline
    val subMaxTagline get() = subscription.subMaxTagline
    val subRenewsOn get() = subscription.subRenewsOn
    val subMonthly get() = subscription.subMonthly
    val subYearlySave get() = subscription.subYearlySave
    val subFreeForever get() = subscription.subFreeForever
    val subCurrentPlan get() = subscription.subCurrentPlan
    val subMostPopular get() = subscription.subMostPopular
    val subSubscribePrime get() = subscription.subSubscribePrime
    val subSubscribeMax get() = subscription.subSubscribeMax
    val subContactUpgrade get() = subscription.subContactUpgrade
    val subUpgrade get() = subscription.subUpgrade
    val subManagePlan get() = subscription.subManagePlan
    val subLoadError get() = subscription.subLoadError
    val subPlanUnavailable get() = subscription.subPlanUnavailable
    val subPaymentSuccess get() = subscription.subPaymentSuccess
    val subPaymentFailed get() = subscription.subPaymentFailed
    val subPaymentsOffline get() = subscription.subPaymentsOffline
    val subWhatsAppSupport get() = subscription.subWhatsAppSupport
    val subWhatsAppPrefill get() = subscription.subWhatsAppPrefill
    val subSaveInr get() = subscription.subSaveInr

    val scanTitle get() = scan.scanTitle
    val scanBtn get() = scan.scanBtn
    val scanSubtitle get() = scan.scanSubtitle
    val scanPickPhoto get() = scan.scanPickPhoto
    val scanAnalyze get() = scan.scanAnalyze
    val scanAnalyzing get() = scan.scanAnalyzing
    val scanError get() = scan.scanError
    val scanImageError get() = scan.scanImageError
    val scanLocked get() = scan.scanLocked
    val scanResultTitle get() = scan.scanResultTitle
    val scanResultsTitle get() = scan.scanResultsTitle
    val scanOpenCamera get() = scan.scanOpenCamera
    val scanCameraHint get() = scan.scanCameraHint
    val scanCameraCancelled get() = scan.scanCameraCancelled
    val scanCaptureAgain get() = scan.scanCaptureAgain
    val scanCreatePlan get() = scan.scanCreatePlan
    val scanNextSteps get() = scan.scanNextSteps
    val scanVocabulary get() = scan.scanVocabulary
    val scanSuggested get() = scan.scanSuggested

    val worksheetBtn get() = worksheet.worksheetBtn
    val worksheetTitle get() = worksheet.worksheetTitle
    val worksheetLocked get() = worksheet.worksheetLocked
    val worksheetError get() = worksheet.worksheetError
    val worksheetRetry get() = worksheet.worksheetRetry
    val worksheetShare get() = worksheet.worksheetShare
    val worksheetTeacherNotes get() = worksheet.worksheetTeacherNotes

    val tlmKitBtn get() = tlm.tlmKitBtn
    val tlmKitTitle get() = tlm.tlmKitTitle
    val tlmKitUnit get() = tlm.tlmKitUnit
    val tlmKitYear get() = tlm.tlmKitYear
    val tlmKitLocked get() = tlm.tlmKitLocked
    val tlmKitYearLocked get() = tlm.tlmKitYearLocked
    val tlmKitEssential get() = tlm.tlmKitEssential
    val tlmKitSummary get() = tlm.tlmKitSummary
    val tlmKitShare get() = tlm.tlmKitShare

    val assessmentBtn get() = assessment.assessmentBtn
    val assessmentHubTitle get() = assessment.assessmentHubTitle
    val assessmentHubSub get() = assessment.assessmentHubSub
    val assessmentBaseline get() = assessment.assessmentBaseline
    val assessmentBaselineDesc get() = assessment.assessmentBaselineDesc
    val assessmentEndline get() = assessment.assessmentEndline
    val assessmentEndlineDesc get() = assessment.assessmentEndlineDesc
    val assessmentUnitTests get() = assessment.assessmentUnitTests
    val assessmentUnitTestsDesc get() = assessment.assessmentUnitTestsDesc
    val assessmentEnterScores get() = assessment.assessmentEnterScores
    val assessmentUpdateScores get() = assessment.assessmentUpdateScores
    val assessmentEntryTitle get() = assessment.assessmentEntryTitle
    val assessmentScoreLabel get() = assessment.assessmentScoreLabel
    val assessmentScoreHint get() = assessment.assessmentScoreHint
    val assessmentSaveError get() = assessment.assessmentSaveError
    val assessmentLoadError get() = assessment.assessmentLoadError
    val assessmentRetry get() = assessment.assessmentRetry

    val roadmapUnitTestSub get() = roadmap.roadmapUnitTestSub
}
