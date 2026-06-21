package com.tippingpoint.pedastudio.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.api.WorksheetApiClient
import com.tippingpoint.pedastudio.api.WorksheetResult
import com.tippingpoint.pedastudio.auth.PhoneAuthController
import com.tippingpoint.pedastudio.data.CurriculumRepository
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.components.PrimaryButton
import com.tippingpoint.pedastudio.ui.components.RegisterScaffold
import com.tippingpoint.pedastudio.ui.components.UpgradeBanner
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke

@Composable
fun WorksheetScreen(
    lessonId: String,
    day: Int,
    prefs: UserPreferences,
    curriculum: CurriculumRepository,
    auth: PhoneAuthController,
    teacherAccount: TeacherAccount,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onAccountUpdated: (TeacherAccount) -> Unit,
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lesson = remember(lessonId) {
        curriculum.findLessonAnywhere(lessonId)
    }

    var loading by remember { mutableStateOf(false) }
    var worksheet by remember { mutableStateOf<WorksheetResult?>(null) }
    var error by remember { mutableStateOf("") }

    fun generate() {
        loading = true
        error = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                WorksheetApiClient.generate(lessonId, day, auth.getIdToken())
            }
            loading = false
            result.onSuccess {
                worksheet = it
                it.account?.let { acc -> onAccountUpdated(acc) }
            }.onFailure { error = it.message ?: s.worksheetError }
        }
    }

    LaunchedEffect(lessonId, day) {
        if (teacherAccount.hasFeature { it.worksheets } && worksheet == null && !loading) {
            generate()
        }
    }

    RegisterScaffold(
        title = s.worksheetTitle,
        stepLabel = lesson?.curriculumTitle ?: lessonId,
        buttonText = s.closeBtn,
        canContinue = !loading,
        onBack = if (loading) null else onBack,
        onContinue = onBack,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!teacherAccount.hasFeature { it.worksheets }) {
                UpgradeBanner(s = s, message = s.worksheetLocked, onUpgrade = onUpgrade)
                return@Column
            }

            if (loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = AccentTeal)
                }
            }
            if (error.isNotBlank()) {
                Text(error, color = Color(0xFFC62828), fontSize = 13.sp)
                PrimaryButton(text = s.worksheetRetry, onClick = { generate() })
            }

            worksheet?.let { ws ->
                Text(ws.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDark)
                Text(ws.instructions, fontSize = 13.sp, color = PrimaryDark.copy(0.85f))
                ws.items.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SeasideBorder),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${index + 1}. ${item.question}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            if (item.options.isNotEmpty()) {
                                item.options.forEach { opt ->
                                    Text("• $opt", fontSize = 12.sp, color = PrimaryDark.copy(0.75f))
                                }
                            }
                        }
                    }
                }
                if (ws.teacherNotes.isNotBlank()) {
                    Text(s.worksheetTeacherNotes, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(ws.teacherNotes, fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        val share = buildShareText(ws, lesson?.curriculumTitle ?: lessonId, day)
                        context.startActivity(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, share)
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                ) {
                    Text(s.worksheetShare)
                }
            }
        }
    }
}

private fun buildShareText(ws: WorksheetResult, lessonTitle: String, day: Int): String = buildString {
    appendLine("PedaStudio Worksheet — $lessonTitle (Day $day)")
    appendLine(ws.title)
    appendLine()
    appendLine(ws.instructions)
    appendLine()
    ws.items.forEachIndexed { i, item ->
        appendLine("${i + 1}. ${item.question}")
        item.options.forEach { appendLine("   • $it") }
        appendLine()
    }
    if (ws.teacherNotes.isNotBlank()) {
        appendLine("Teacher notes: ${ws.teacherNotes}")
    }
}
