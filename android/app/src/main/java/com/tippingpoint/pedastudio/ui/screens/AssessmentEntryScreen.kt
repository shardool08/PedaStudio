package com.tippingpoint.pedastudio.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.api.AssessmentApiClient
import com.tippingpoint.pedastudio.api.AssessmentToolData
import com.tippingpoint.pedastudio.api.AssessmentToolItem
import com.tippingpoint.pedastudio.auth.PhoneAuthController
import com.tippingpoint.pedastudio.data.AssessmentReteachHelper
import com.tippingpoint.pedastudio.data.AssessmentScorer
import com.tippingpoint.pedastudio.data.ItemTallyState
import com.tippingpoint.pedastudio.data.McqTally
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.components.PrimaryButton
import com.tippingpoint.pedastudio.ui.components.RegisterScaffold
import com.tippingpoint.pedastudio.ui.components.UpgradeBanner
import com.tippingpoint.pedastudio.util.ImageEncoding
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AssessmentEntryScreen(
    type: String,
    groupId: String?,
    groupName: String?,
    grade: Int,
    subject: String,
    prefs: UserPreferences,
    auth: PhoneAuthController,
    teacherAccount: TeacherAccount,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onPlanReteach: (lessonId: String, reteachNotes: String, afterUnitTest: Boolean) -> Unit,
    onUpgrade: () -> Unit,
    onAccountUpdated: (TeacherAccount) -> Unit,
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var tool by remember { mutableStateOf<AssessmentToolData?>(null) }
    var useLegacy by remember { mutableStateOf(false) }

    var score by remember { mutableFloatStateOf(70f) }
    var students by remember { mutableStateOf(prefs.studentCount.coerceAtLeast(1).toString()) }
    var notes by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }
    var savedWeakItems by remember { mutableStateOf<List<String>>(emptyList()) }
    val tallies = remember { mutableStateMapOf<String, ItemTallyState>() }

    val title = when (type) {
        "baseline" -> s.assessmentBaseline
        "endline" -> s.assessmentEndline
        else -> groupName ?: s.assessmentUnitTests
    }

    LaunchedEffect(type, groupId, grade) {
        loading = true
        error = ""
        val result = withContext(Dispatchers.IO) {
            AssessmentApiClient.fetchTool(
                grade = grade,
                subject = subject,
                medium = prefs.medium,
                type = type,
                groupId = groupId,
                idToken = auth.getIdToken(),
            )
        }
        result.onSuccess { resp ->
            resp.account?.let { onAccountUpdated(it) }
            if (resp.available && resp.tool != null) {
                tool = resp.tool
                useLegacy = false
                val base = AssessmentScorer.initialTallies(resp.tool)
                tallies.clear()
                if (resp.saved != null) {
                    tallies.putAll(AssessmentScorer.mergeSavedTallies(base, resp.saved.tallies))
                    students = resp.saved.studentsAssessed.coerceAtLeast(1).toString()
                    notes = resp.saved.notes
                    savedWeakItems = resp.saved.weakItems
                } else {
                    tallies.putAll(base)
                }
            } else {
                useLegacy = true
                tool = null
            }
        }.onFailure {
            useLegacy = true
            error = it.message ?: s.assessmentLoadError
        }
        loading = false
    }

    val currentTool = tool
    val afterUnitTest = type == "unit"

    suspend fun performSave(closeAfter: Boolean): Boolean {
        val count = students.toIntOrNull() ?: prefs.studentCount.coerceAtLeast(1)
        val structuredScore = if (currentTool != null && !useLegacy) {
            AssessmentScorer.preview(currentTool, count, tallies).scorePercent.toInt()
        } else {
            score.toInt()
        }
        val result = withContext(Dispatchers.IO) {
            if (currentTool != null && !useLegacy) {
                AssessmentApiClient.saveScore(
                    type = type,
                    grade = grade,
                    subject = subject,
                    medium = prefs.medium,
                    groupId = groupId,
                    groupName = groupName,
                    scorePercent = structuredScore,
                    studentsAssessed = count,
                    notes = notes,
                    tallies = AssessmentScorer.toPayloads(tallies),
                    idToken = auth.getIdToken(),
                )
            } else {
                AssessmentApiClient.saveScore(
                    type = type,
                    grade = grade,
                    subject = subject,
                    medium = prefs.medium,
                    groupId = groupId,
                    groupName = groupName,
                    scorePercent = score.toInt(),
                    studentsAssessed = count,
                    notes = notes,
                    tallies = null,
                    idToken = auth.getIdToken(),
                )
            }
        }
        return result.fold(
            onSuccess = { saved ->
                saved.account?.let { onAccountUpdated(it) }
                if (saved.weakItems.isNotEmpty()) {
                    savedWeakItems = saved.weakItems
                } else if (currentTool != null) {
                    savedWeakItems = AssessmentScorer.preview(currentTool, count, tallies).weakItemIds
                }
                saveMessage = s.assessmentSavedOk
                if (closeAfter) onSaved()
                true
            },
            onFailure = {
                error = it.message ?: s.assessmentSaveError
                false
            },
        )
    }

    fun runScan(scanMode: String, encoded: List<Pair<String, String>>) {
        val activeTool = tool ?: return
        scanning = true
        scanMessage = ""
        error = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                AssessmentApiClient.scanMarkPapers(
                    grade = grade,
                    subject = subject,
                    medium = prefs.medium,
                    type = type,
                    groupId = groupId,
                    scanMode = scanMode,
                    images = encoded,
                    idToken = auth.getIdToken(),
                )
            }
            scanning = false
            result.onSuccess { r ->
                r.account?.let { onAccountUpdated(it) }
                if (r.studentsProcessed > 0) {
                    students = r.studentsProcessed.toString()
                }
                val base = AssessmentScorer.initialTallies(activeTool)
                tallies.clear()
                tallies.putAll(AssessmentScorer.mergeSavedTallies(base, r.tallies))
                scanMessage = s.assessmentBulkScanDone.format(r.studentsProcessed)
                if (r.warnings.isNotEmpty()) {
                    error = r.warnings.joinToString("\n")
                }
                saving = true
                performSave(closeAfter = false)
                saving = false
            }.onFailure {
                error = it.message ?: s.assessmentBulkScanError
            }
        }
    }

    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(15),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val encoded = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching { ImageEncoding.encodeUri(context, uri) }.getOrNull()
                }
            }
            if (encoded.isEmpty()) {
                error = s.assessmentBulkScanError
                return@launch
            }
            runScan("papers", encoded)
        }
    }

    val tallyPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val encoded = withContext(Dispatchers.IO) {
                runCatching { ImageEncoding.encodeUri(context, uri) }.getOrNull()
            }
            if (encoded == null) {
                error = s.assessmentBulkScanError
                return@launch
            }
            runScan("tally_sheet", listOf(encoded))
        }
    }

    fun shareMarkdown(copy: String) {
        scope.launch {
            saving = true
            val result = withContext(Dispatchers.IO) {
                AssessmentApiClient.downloadToolMarkdown(
                    grade = grade,
                    subject = subject,
                    medium = prefs.medium,
                    type = type,
                    groupId = groupId,
                    copy = copy,
                    format = "html",
                    idToken = auth.getIdToken(),
                )
            }
            saving = false
            result.onSuccess { html ->
                context.startActivity(
                    Intent(Intent.ACTION_SEND).apply {
                        this.type = "text/html"
                        putExtra(Intent.EXTRA_TEXT, html)
                        putExtra(Intent.EXTRA_SUBJECT, "${currentTool?.title ?: title} — $copy (print to PDF)")
                    },
                )
            }.onFailure { error = it.message ?: s.assessmentDownloadError }
        }
    }

    fun exportReport() {
        scope.launch {
            saving = true
            val result = withContext(Dispatchers.IO) {
                AssessmentApiClient.downloadReportHtml(
                    grade = grade,
                    subject = subject,
                    medium = prefs.medium,
                    type = type,
                    groupId = groupId,
                    idToken = auth.getIdToken(),
                )
            }
            saving = false
            result.onSuccess { html ->
                context.startActivity(
                    Intent(Intent.ACTION_SEND).apply {
                        this.type = "text/html"
                        putExtra(Intent.EXTRA_TEXT, html)
                        putExtra(Intent.EXTRA_SUBJECT, "$title — Class Report")
                    },
                )
            }.onFailure { error = it.message ?: s.assessmentExportError }
        }
    }

    RegisterScaffold(
        title = if (useLegacy) s.assessmentEntryTitle else s.assessmentToolTitle,
        stepLabel = title,
        buttonText = s.saveProfile,
        canContinue = !saving && !loading && !scanning,
        onBack = if (saving || scanning) null else onBack,
        onContinue = {
            saving = true
            error = ""
            saveMessage = ""
            scope.launch {
                val count = students.toIntOrNull() ?: prefs.studentCount.coerceAtLeast(1)
                val weak = if (currentTool != null && !useLegacy) {
                    AssessmentScorer.preview(currentTool, count, tallies).weakItemIds
                } else {
                    emptyList()
                }
                performSave(closeAfter = weak.isEmpty())
                saving = false
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = AccentTeal)
                }
            }

            if (useLegacy && !loading) {
                Text(s.assessmentLegacyHint, fontSize = 12.sp, color = PrimaryDark.copy(0.65f))
            }

            if (currentTool != null && !useLegacy) {
                StructuredAssessmentContent(
                    tool = currentTool,
                    teacherAccount = teacherAccount,
                    students = students,
                    onStudentsChange = { students = it.filter { ch -> ch.isDigit() }.take(3) },
                    notes = notes,
                    onNotesChange = { notes = it.take(300) },
                    tallies = tallies,
                    weakItemIds = savedWeakItems.ifEmpty {
                        AssessmentScorer.preview(
                            currentTool,
                            students.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            tallies,
                        ).weakItemIds
                    },
                    saveMessage = saveMessage,
                    onDownloadStudent = { shareMarkdown("student") },
                    onDownloadAssessor = { shareMarkdown("assessor") },
                    onScanPapers = {
                        multiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onScanTallySheet = {
                        tallyPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onExportReport = { exportReport() },
                    onPlanReteach = {
                        val weak = savedWeakItems.ifEmpty {
                            AssessmentScorer.preview(
                                currentTool,
                                students.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                                tallies,
                            ).weakItemIds
                        }
                        val lessonId = AssessmentReteachHelper.pickLessonId(currentTool, weak)
                        if (lessonId != null) {
                            val reteachNotes = AssessmentReteachHelper.buildReteachNotes(currentTool, weak)
                            onPlanReteach(lessonId, reteachNotes, afterUnitTest)
                        }
                    },
                    onUpgrade = onUpgrade,
                    scanning = scanning,
                    scanMessage = scanMessage,
                    saving = saving,
                )
            } else if (!loading) {
                LegacyAssessmentContent(
                    score = score,
                    onScoreChange = { score = it },
                    students = students,
                    onStudentsChange = { students = it.filter { ch -> ch.isDigit() }.take(3) },
                    notes = notes,
                    onNotesChange = { notes = it.take(300) },
                )
            }

            if (saving) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = AccentTeal)
                }
            }
            if (error.isNotBlank()) {
                Text(error, color = Color(0xFFC62828), fontSize = 13.sp)
                PrimaryButton(text = s.assessmentRetry, onClick = { error = "" })
            }
        }
    }
}

@Composable
private fun StructuredAssessmentContent(
    tool: AssessmentToolData,
    teacherAccount: TeacherAccount,
    students: String,
    onStudentsChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    tallies: MutableMap<String, ItemTallyState>,
    weakItemIds: List<String>,
    saveMessage: String,
    onDownloadStudent: () -> Unit,
    onDownloadAssessor: () -> Unit,
    onScanPapers: () -> Unit,
    onScanTallySheet: () -> Unit,
    onExportReport: () -> Unit,
    onPlanReteach: () -> Unit,
    onUpgrade: () -> Unit,
    scanning: Boolean,
    scanMessage: String,
    saving: Boolean,
) {
    val s = LocalAppStrings.current
    val preview = AssessmentScorer.preview(
        tool,
        students.toIntOrNull()?.coerceAtLeast(1) ?: 1,
        tallies,
    )

    Text(
        "${tool.totalMarks} marks · ~${tool.recommendedMinutes} min",
        fontSize = 12.sp,
        color = PrimaryDark.copy(0.65f),
    )

    if (tool.administrationNotes.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FAF8)),
            border = BorderStroke(1.dp, SeasideBorder),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(s.assessmentAdminNotes, fontWeight = FontWeight.SemiBold, color = PrimaryDark, fontSize = 13.sp)
                tool.administrationNotes.forEach { note ->
                    Text("• $note", fontSize = 12.sp, color = PrimaryDark.copy(0.75f))
                }
            }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDownloadStudent, enabled = !saving, modifier = Modifier.weight(1f)) {
            Text(s.assessmentDownloadStudent, fontSize = 12.sp)
        }
        Button(
            onClick = onDownloadAssessor,
            enabled = !saving,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
        ) {
            Text(s.assessmentDownloadAssessor, fontSize = 12.sp)
        }
    }

    val canScan = teacherAccount.hasFeature { it.bulkPaperScan && it.aiAutoMark }
    val canExport = teacherAccount.hasFeature { it.reportPdfExport }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.assessmentBulkScanTitle, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
            Text(s.assessmentBulkScanHint, fontSize = 12.sp, color = PrimaryDark.copy(0.65f))
            if (!canScan) {
                UpgradeBanner(s = s, message = s.assessmentScanLocked, onUpgrade = onUpgrade)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onScanPapers,
                        enabled = !saving && !scanning,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(s.assessmentBulkScanPapers, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onScanTallySheet,
                        enabled = !saving && !scanning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                    ) {
                        Text(s.assessmentBulkScanTally, fontSize = 12.sp)
                    }
                }
                if (scanning) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = AccentTeal)
                    }
                    Text(s.assessmentBulkScanning, fontSize = 12.sp, color = PrimaryDark.copy(0.7f))
                }
                if (scanMessage.isNotBlank()) {
                    Text(scanMessage, fontSize = 12.sp, color = AccentTeal, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FAF8)),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(s.assessmentComputedScore, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
            Text("${preview.scorePercent.toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
            if (preview.strands.isNotEmpty()) {
                preview.strands.forEach { strand ->
                    Text("${strand.label}: ${strand.percent.toInt()}%", fontSize = 12.sp, color = PrimaryDark.copy(0.75f))
                }
            }
        }
    }

    if (weakItemIds.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
            border = BorderStroke(1.dp, SeasideBorder),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.assessmentWeakItemsTitle, fontWeight = FontWeight.SemiBold, color = PrimaryDark, fontSize = 13.sp)
                weakItemIds.take(6).forEach { id ->
                    val item = tool.items.find { it.id == id }
                    Text(
                        "• ${item?.competency ?: id}",
                        fontSize = 12.sp,
                        color = PrimaryDark.copy(0.8f),
                    )
                }
                Button(
                    onClick = onPlanReteach,
                    enabled = !saving && !scanning,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                ) {
                    Text(s.assessmentPlanReteach)
                }
            }
        }
    }

    if (saveMessage.isNotBlank()) {
        Text(saveMessage, fontSize = 13.sp, color = AccentTeal, fontWeight = FontWeight.Medium)
    }

    OutlinedTextField(
        value = students,
        onValueChange = onStudentsChange,
        label = { Text(s.students) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )

    Text(s.assessmentTallyHint, fontSize = 12.sp, color = PrimaryDark.copy(0.65f))

    tool.items.forEachIndexed { index, item ->
        ItemTallyCard(
            index = index + 1,
            item = item,
            state = tallies[item.id],
            onStateChange = { tallies[item.id] = it },
        )
    }

    if (canExport) {
        Text(s.assessmentExportReportHint, fontSize = 12.sp, color = PrimaryDark.copy(0.65f))
        OutlinedButton(
            onClick = onExportReport,
            enabled = !saving && !scanning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(s.assessmentExportReport)
        }
    }

    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text(s.notesLabel) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
}

@Composable
private fun ItemTallyCard(
    index: Int,
    item: AssessmentToolItem,
    state: ItemTallyState?,
    onStateChange: (ItemTallyState) -> Unit,
) {
    val s = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Q$index · ${item.id} (${item.marks}m)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = AccentTeal,
            )
            Text(item.stem, fontSize = 14.sp, color = PrimaryDark, fontWeight = FontWeight.Medium)
            Text(item.competency, fontSize = 11.sp, color = PrimaryDark.copy(0.6f))

            when (state) {
                is ItemTallyState.Mcq -> {
                    item.options.forEach { (key, text) ->
                        Text("$key) $text", fontSize = 12.sp, color = PrimaryDark.copy(0.8f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TallyField("A", state.counts.countA) {
                            onStateChange(ItemTallyState.Mcq(state.counts.copy(countA = it)))
                        }
                        TallyField("B", state.counts.countB) {
                            onStateChange(ItemTallyState.Mcq(state.counts.copy(countB = it)))
                        }
                        TallyField("C", state.counts.countC) {
                            onStateChange(ItemTallyState.Mcq(state.counts.copy(countC = it)))
                        }
                        TallyField("D", state.counts.countD) {
                            onStateChange(ItemTallyState.Mcq(state.counts.copy(countD = it)))
                        }
                    }
                    TallyField(s.assessmentNotAssessed, state.counts.notAssessed, modifier = Modifier.fillMaxWidth(0.4f)) {
                        onStateChange(ItemTallyState.Mcq(state.counts.copy(notAssessed = it)))
                    }
                }
                is ItemTallyState.Subjective -> {
                    TallyField(s.assessmentCorrectCount, state.correctCount, modifier = Modifier.fillMaxWidth(0.5f)) {
                        onStateChange(state.copy(correctCount = it))
                    }
                    TallyField(s.assessmentNotAssessed, state.notAssessed, modifier = Modifier.fillMaxWidth(0.4f)) {
                        onStateChange(state.copy(notAssessed = it))
                    }
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun TallyField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { raw -> onChange(raw.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0) },
        label = { Text(label, fontSize = 11.sp) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun LegacyAssessmentContent(
    score: Float,
    onScoreChange: (Float) -> Unit,
    students: String,
    onStudentsChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
) {
    val s = LocalAppStrings.current
    Text(s.assessmentScoreLabel, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
    Text("${score.toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
    Slider(
        value = score,
        onValueChange = onScoreChange,
        valueRange = 0f..100f,
        steps = 20,
    )
    Text(s.assessmentScoreHint, fontSize = 12.sp, color = PrimaryDark.copy(0.6f))
    OutlinedTextField(
        value = students,
        onValueChange = onStudentsChange,
        label = { Text(s.students) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text(s.notesLabel) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
}
