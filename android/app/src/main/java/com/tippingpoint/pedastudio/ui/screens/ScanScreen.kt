package com.tippingpoint.pedastudio.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import com.tippingpoint.pedastudio.api.ScanAnalysis
import com.tippingpoint.pedastudio.api.ScanApiClient
import com.tippingpoint.pedastudio.auth.PhoneAuthController
import com.tippingpoint.pedastudio.data.AssessmentRepository
import com.tippingpoint.pedastudio.data.CurriculumRepository
import com.tippingpoint.pedastudio.data.PlanProgressHelper
import com.tippingpoint.pedastudio.data.PlanStorage
import com.tippingpoint.pedastudio.data.SavedScan
import com.tippingpoint.pedastudio.data.ScanStorage
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.TlmKitRepository
import com.tippingpoint.pedastudio.data.TlmResourceCatalog
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import androidx.core.content.ContextCompat
import com.tippingpoint.pedastudio.ui.encodeBitmapForScan
import com.tippingpoint.pedastudio.ui.encodeImageUriForScan
import com.tippingpoint.pedastudio.ui.scanCaptureFile
import com.tippingpoint.pedastudio.ui.scanCaptureUri
import com.tippingpoint.pedastudio.ui.components.InfoBannerCard
import com.tippingpoint.pedastudio.ui.components.PrimaryButton
import com.tippingpoint.pedastudio.ui.components.RegisterScaffold
import com.tippingpoint.pedastudio.ui.components.TlmKitHighlightCard
import com.tippingpoint.pedastudio.ui.components.UpgradeBanner
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.BgTint
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.PrimarySteel
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import com.tippingpoint.pedastudio.ui.theme.WarmPeach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_SCAN_BYTES = 4_500_000

@Composable
fun ScanScreen(
    lessonId: String?,
    prefs: UserPreferences,
    curriculum: CurriculumRepository,
    assessmentRepo: AssessmentRepository,
    tlmCatalog: TlmResourceCatalog,
    planStorage: PlanStorage,
    scanStorage: ScanStorage,
    auth: PhoneAuthController,
    teacherAccount: TeacherAccount,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onAccountUpdated: (TeacherAccount) -> Unit,
    onOpenWorksheet: (String, Int) -> Unit,
    onOpenQuickPlan: (String, Int, String) -> Unit,
    onViewPlan: (String, Int) -> Unit,
    onOpenTlmKit: (Int) -> Unit,
    onOpenAssessment: (String, String?, String?) -> Unit,
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tlmRepo = remember { TlmKitRepository(curriculum, tlmCatalog) }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf<ScanAnalysis?>(null) }
    var savedScans by remember { mutableStateOf(scanStorage.listScans()) }
    var showSaved by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf("") }
    var activeScanId by remember { mutableStateOf("") }

    val resolved = remember(analysis, lessonId, prefs) {
        resolveScanLesson(curriculum, prefs, lessonId, analysis)
    }
    val planDay = remember(resolved.first, planStorage) {
        val lesson = curriculum.findLessonAnywhere(resolved.first)
        if (lesson != null) {
            PlanProgressHelper.getFirstUnplannedDay(lesson.id, lesson.days, planStorage) ?: 1
        } else 1
    }
    val unitKit = remember(resolved.second, prefs) {
        tlmRepo.buildUnitKit(
            prefs.lastViewedGrade,
            resolved.second,
            "english",
            prefs.medium,
            prefs.getTeacherResources(),
        )
    }

    fun refreshSaved() {
        savedScans = scanStorage.listScans()
    }

    fun loadSavedScan(saved: SavedScan) {
        activeScanId = saved.id
        analysis = ScanAnalysis(
            detectedLesson = saved.detectedLesson,
            detectedPage = "",
            summary = saved.summary,
            vocabulary = saved.vocabulary,
            suggestedActions = listOf("quick_plan"),
            actionHints = emptyMap(),
            account = null,
        )
        showSaved = false
        savedMessage = ""
        previewBitmap = null
        imageUri = null
    }

    fun saveCurrentScan(): String {
        val a = analysis ?: return activeScanId
        val id = scanStorage.saveScan(
            lessonId = resolved.first,
            lessonTitle = resolved.third,
            detectedLesson = a.detectedLesson,
            summary = a.summary,
            vocabulary = a.vocabulary,
        )
        activeScanId = id
        refreshSaved()
        savedMessage = s.scanSaved
        return id
    }

    fun runAnalysis(encoded: Pair<String, String>) {
        if (encoded.first.length > MAX_SCAN_BYTES) {
            analyzing = false
            error = s.scanImageTooLarge
            return
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                ScanApiClient.analyzePage(encoded.first, encoded.second, lessonId, auth.getIdToken())
            }
            result.onSuccess { scanResult ->
                analysis = scanResult
                scanResult.account?.let { onAccountUpdated(it) }
            }.onFailure { e ->
                error = e.message ?: s.scanError
            }
            analyzing = false
        }
    }

    fun analyzeEncoded(encoded: Pair<String, String>) {
        error = ""
        runAnalysis(encoded)
    }

    fun analyzeBitmap(bitmap: Bitmap) {
        previewBitmap = bitmap
        imageUri = null
        analysis = null
        analyzing = true
        error = ""
        scope.launch {
            val encoded = withContext(Dispatchers.IO) {
                encodeBitmapForScan(bitmap)
            }
            if (encoded == null) {
                analyzing = false
                error = s.scanImageError
                return@launch
            }
            runAnalysis(encoded)
        }
    }

    fun analyzeUri(uri: Uri) {
        imageUri = uri
        previewBitmap = null
        analysis = null
        analyzing = true
        error = ""
        scope.launch {
            val encoded = withContext(Dispatchers.IO) {
                encodeImageUriForScan(context, uri)
            }
            if (encoded == null) {
                analyzing = false
                error = s.scanImageError
                return@launch
            }
            runAnalysis(encoded)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val captureFile = scanCaptureFile(context)
        when {
            success && captureFile.exists() && captureFile.length() > 0L -> {
                analyzeUri(scanCaptureUri(context))
            }
            success -> {
                error = s.scanImageError
            }
            else -> {
                error = s.scanCameraCancelled
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            takePictureLauncher.launch(scanCaptureUri(context))
        } else {
            error = s.scanCameraPermission
        }
    }

    fun openCamera() {
        error = ""
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                takePictureLauncher.launch(scanCaptureUri(context))
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) analyzeUri(uri)
    }

    RegisterScaffold(
        title = s.scanTitle,
        stepLabel = when {
            analyzing -> s.scanAnalyzing
            analysis != null -> s.scanResultsTitle
            else -> s.scanSubtitle
        },
        buttonText = s.closeBtn,
        canContinue = !analyzing,
        onBack = if (analyzing) null else onBack,
        onContinue = onBack,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!teacherAccount.hasFeature { it.textbookScan }) {
                UpgradeBanner(s = s, message = s.scanLocked, onUpgrade = onUpgrade)
                return@Column
            }

            if (teacherAccount.scansRemaining == 0) {
                InfoBannerCard(title = s.scanLimitReached, body = s.tierScansRemaining.format(0))
                return@Column
            }

            teacherAccount.scansRemaining?.let { remaining ->
                Text(
                    s.tierScansRemaining.format(remaining),
                    fontSize = 13.sp,
                    color = PrimarySteel.copy(0.75f),
                )
            }

            if (savedScans.isNotEmpty()) {
                TextButton(onClick = { showSaved = !showSaved }, modifier = Modifier.fillMaxWidth()) {
                    Text("${s.scanViewSaved} (${savedScans.size})", color = AccentTeal)
                }
            }

            if (showSaved && savedScans.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SeasideBorder),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s.scanSavedScans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryDark)
                        val dateFmt = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
                        savedScans.take(8).forEach { saved ->
                            OutlinedButton(
                                onClick = { loadSavedScan(saved) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        saved.detectedLesson.ifBlank { saved.lessonTitle }.ifBlank { s.scanResultTitle },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                    )
                                    Text(dateFmt.format(Date(saved.savedAt)), fontSize = 11.sp, color = PrimarySteel)
                                    if (saved.planLessonId != null && saved.planDay != null) {
                                        Text(s.scanHasPlan, fontSize = 11.sp, color = AccentTeal)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (savedMessage.isNotBlank()) {
                Text(savedMessage, color = AccentTeal, fontSize = 13.sp)
            }

            if (previewBitmap == null && imageUri == null && analysis == null && !analyzing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BgTint),
                    border = BorderStroke(1.dp, SeasideBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, tint = AccentTeal, modifier = Modifier.size(48.dp))
                        Text(s.scanCameraHint, fontSize = 14.sp, color = PrimaryDark, fontWeight = FontWeight.Medium)
                        PrimaryButton(text = s.scanOpenCamera, onClick = { openCamera() })
                        OutlinedButton(onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text(s.scanPickPhoto)
                        }
                    }
                }
            }

            previewBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop,
                )
            }

            if (analyzing) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = AccentTeal)
                        Text(s.scanAnalyzing, fontSize = 13.sp, color = PrimarySteel)
                    }
                }
            }

            if (error.isNotBlank()) {
                Text(error, color = Color(0xFFC62828), fontSize = 13.sp)
                OutlinedButton(onClick = { openCamera() }, modifier = Modifier.fillMaxWidth()) {
                    Text(s.scanCaptureAgain)
                }
            }

            analysis?.let { a ->
                InfoBannerCard(
                    title = a.detectedLesson.ifBlank { resolved.third }.ifBlank { s.scanResultTitle },
                    body = a.summary,
                )
                if (a.vocabulary.isNotEmpty()) {
                    Text(s.scanVocabulary, fontWeight = FontWeight.SemiBold, color = PrimaryDark, fontSize = 13.sp)
                    Text(a.vocabulary.take(8).joinToString(", "), fontSize = 13.sp, color = PrimarySteel)
                }

                Text(s.scanNextSteps, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryDark)
                PrimaryButton(
                    text = "${s.scanCreatePlan} · ${s.dayLabel} $planDay",
                    onClick = {
                        val scanId = activeScanId.ifBlank { saveCurrentScan() }
                        onOpenQuickPlan(resolved.first, planDay, scanId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = { saveCurrentScan() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(s.scanSave)
                }
                val linked = activeScanId.takeIf { it.isNotBlank() }?.let { scanStorage.loadScan(it) }
                    ?: savedScans.firstOrNull { it.lessonId == resolved.first && it.planLessonId != null }
                if (linked?.planLessonId != null && linked.planDay != null) {
                    OutlinedButton(
                        onClick = { onViewPlan(linked.planLessonId, linked.planDay) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(s.scanViewPlan)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            if (teacherAccount.hasFeature { it.worksheets }) {
                                onOpenWorksheet(resolved.first, planDay)
                            } else onUpgrade()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(s.worksheetBtn, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onOpenTlmKit(resolved.second) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(s.tlmKitBtn, fontSize = 12.sp)
                    }
                }

                if (a.suggestedActions.any { it.equals("assessment", ignoreCase = true) }) {
                    OutlinedButton(
                        onClick = {
                            val grade = prefs.lastViewedGrade
                            val lessons = curriculum.getLessons(grade, prefs.lastViewedSubject, prefs.medium)
                            val group = assessmentRepo
                                .getUnitTests(grade, prefs.medium, lessons)
                                .firstOrNull { it.unit == resolved.second }
                            if (group != null) {
                                onOpenAssessment("unit", group.id, group.name)
                            } else {
                                onOpenAssessment("baseline", null, s.assessmentBaseline)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(s.assessmentBtn, fontSize = 12.sp)
                    }
                }

                TlmKitHighlightCard(
                    s = s,
                    kit = unitKit,
                    unit = resolved.second,
                    tlmCatalog = tlmCatalog,
                    onOpenFullKit = { onOpenTlmKit(resolved.second) },
                    compact = false,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { openCamera() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(s.scanCaptureAgain, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** Returns lessonId, unit, display title */
private fun resolveScanLesson(
    curriculum: CurriculumRepository,
    prefs: UserPreferences,
    hintLessonId: String?,
    analysis: ScanAnalysis?,
): Triple<String, Int, String> {
    if (!hintLessonId.isNullOrBlank()) {
        val lesson = curriculum.findLessonAnywhere(hintLessonId)
        if (lesson != null) return Triple(lesson.id, lesson.unit, lesson.curriculumTitle)
    }
    val lessons = curriculum.getLessons(prefs.lastViewedGrade, prefs.lastViewedSubject, prefs.medium)
    val detected = analysis?.detectedLesson.orEmpty().lowercase()
    if (detected.isNotBlank()) {
        lessons.firstOrNull { l ->
            l.en.lowercase().contains(detected) || detected.contains(l.en.lowercase().take(12))
        }?.let { return Triple(it.id, it.unit, it.curriculumTitle) }
        lessons.firstOrNull { l ->
            analysis?.vocabulary?.any { v -> l.vocabulary.any { lv -> lv.equals(v, ignoreCase = true) } } == true
        }?.let { return Triple(it.id, it.unit, it.curriculumTitle) }
    }
    val fallback = lessons.firstOrNull()
    return Triple(fallback?.id ?: "1.1", fallback?.unit ?: 1, fallback?.curriculumTitle ?: "")
}
