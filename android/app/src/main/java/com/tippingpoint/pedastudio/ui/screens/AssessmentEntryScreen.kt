package com.tippingpoint.pedastudio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.api.AssessmentApiClient
import com.tippingpoint.pedastudio.auth.PhoneAuthController
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.UserPreferences
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.components.PrimaryButton
import com.tippingpoint.pedastudio.ui.components.RegisterScaffold
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AssessmentEntryScreen(
    type: String,
    groupId: String?,
    groupName: String?,
    grade: Int,
    prefs: UserPreferences,
    auth: PhoneAuthController,
    teacherAccount: TeacherAccount,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onAccountUpdated: (TeacherAccount) -> Unit,
) {
    val s = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var score by remember { mutableFloatStateOf(70f) }
    var students by remember { mutableStateOf(prefs.studentCount.coerceAtLeast(1).toString()) }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val title = when (type) {
        "baseline" -> s.assessmentBaseline
        "endline" -> s.assessmentEndline
        else -> groupName ?: s.assessmentUnitTests
    }

    RegisterScaffold(
        title = s.assessmentEntryTitle,
        stepLabel = title,
        buttonText = s.saveProfile,
        canContinue = !saving,
        onBack = if (saving) null else onBack,
        onContinue = {
            saving = true
            error = ""
            scope.launch {
                val count = students.toIntOrNull() ?: prefs.studentCount
                val result = withContext(Dispatchers.IO) {
                    AssessmentApiClient.saveScore(
                        type = type,
                        grade = grade,
                        subject = "english",
                        groupId = groupId,
                        groupName = groupName,
                        scorePercent = score.toInt(),
                        studentsAssessed = count,
                        notes = notes,
                        idToken = auth.getIdToken(),
                    )
                }
                saving = false
                result.onSuccess { (record, account) ->
                    account?.let { onAccountUpdated(it) }
                    onSaved()
                }.onFailure { error = it.message ?: s.assessmentSaveError }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(s.assessmentScoreLabel, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
            Text("${score.toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
            Slider(
                value = score,
                onValueChange = { score = it },
                valueRange = 0f..100f,
                steps = 20,
            )
            Text(s.assessmentScoreHint, fontSize = 12.sp, color = PrimaryDark.copy(0.6f))

            OutlinedTextField(
                value = students,
                onValueChange = { students = it.filter { ch -> ch.isDigit() }.take(3) },
                label = { Text(s.students) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(300) },
                label = { Text(s.notesLabel) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

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
