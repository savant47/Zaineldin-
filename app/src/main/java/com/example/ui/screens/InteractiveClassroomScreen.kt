package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ChalkboardStep
import com.example.data.model.CurriculumLesson
import com.example.data.model.LanguageMode
import com.example.data.model.StudentLevel
import com.example.data.model.SubjectType
import com.example.ui.theme.ChalkCyanAccent
import com.example.ui.theme.ChalkWhiteText
import com.example.ui.theme.ChalkYellowAccent
import com.example.ui.theme.ChalkboardSurfaceBottom
import com.example.ui.theme.ChalkboardSurfaceTop
import com.example.ui.theme.ChalkboardWoodFrame
import com.example.ui.viewmodel.ChalkboardAiState
import com.example.ui.viewmodel.TeacherReplyState
import com.example.util.OnDeviceVoiceManager

@Composable
fun InteractiveClassroomScreen(
    activeLesson: CurriculumLesson?,
    selectedSubject: SubjectType,
    allLessons: List<CurriculumLesson>,
    allLessonsForSubject: List<CurriculumLesson>,
    studentLevel: StudentLevel,
    languageMode: LanguageMode,
    chalkboardState: ChalkboardAiState,
    teacherReplyState: TeacherReplyState,
    onSelectSubject: (SubjectType) -> Unit,
    onSelectLesson: (CurriculumLesson) -> Unit,
    onSwitchLanguageMode: (LanguageMode) -> Unit,
    onRegenerateExplanation: () -> Unit,
    onSendStudentQuestionOrAnswer: (String) -> Unit
) {
    val context = LocalContext.current
    val voiceManager = remember(context) { OnDeviceVoiceManager(context) }

    val isSpeaking by voiceManager.isSpeaking.collectAsStateWithLifecycle()
    val isListening by voiceManager.isListening.collectAsStateWithLifecycle()
    val partialTranscript by voiceManager.partialTranscript.collectAsStateWithLifecycle()
    val voiceStatusMessage by voiceManager.voiceStatusMessage.collectAsStateWithLifecycle()

    var studentInputText by remember { mutableStateOf("") }
    var showGroundingSource by remember { mutableStateOf(false) }
    var sttRecognizeEnglish by remember { mutableStateOf(false) }

    DisposableEffect(voiceManager) {
        onDispose {
            voiceManager.release()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening(
                recognizeEnglish = sttRecognizeEnglish,
                onResult = { recognizedText ->
                    studentInputText = recognizedText
                    onSendStudentQuestionOrAnswer(recognizedText)
                }
            )
        }
    }

    // إذا لم يختر الطالب المادة أو الدرس بعد، نعرض شاشة اختيار المادة والدرس داخل السبورة بدلاً من فرض درس Math تلقائياً
    if (activeLesson == null) {
        val lessonsForSelectedSubject = remember(allLessons, selectedSubject) {
            allLessons.filter { it.subjectId == selectedSubject.id }.sortedBy { it.lessonOrder }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("classroom_lesson_picker_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(4.dp, ChalkboardWoodFrame, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(ChalkboardSurfaceTop, ChalkboardSurfaceBottom)
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_teacher_avatar_1794083054459),
                            contentDescription = "شخصية المدرس الودود",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, ChalkYellowAccent, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "مرحباً بك يا زين الدين في السبورة التفاعلية! 👨‍🏫",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ChalkYellowAccent,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "اختر المادة أولاً ثم اضغط على الدرس الذي تريد أن يشرحه لك المدرس على السبورة الخضراء الآن (يظهر الشرح فوراً بدون انتظار):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChalkWhiteText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Text(
                    text = "١. اختر المادة الدراسية:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SubjectType.entries) { subj ->
                        FilterChip(
                            selected = subj == selectedSubject,
                            onClick = { onSelectSubject(subj) },
                            label = {
                                Text(
                                    text = subj.titleAr,
                                    fontWeight = if (subj == selectedSubject) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("chalkboard_pick_subject_${subj.id}")
                        )
                    }
                }
            }

            item {
                Text(
                    text = "٢. اختر درساً من (${selectedSubject.titleAr}) لكتابته على السبورة فوراً:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(
                items = lessonsForSelectedSubject,
                key = { item -> item.id.ifBlank { "${item.subjectId}_${item.lessonOrder}_${item.lessonTitle}" } }
            ) { item ->
                Card(
                    onClick = { onSelectLesson(item) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chalkboard_pick_lesson_${item.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.unitTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.lessonTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "المرجع: ${item.sourceBookName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSelectLesson(item) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("اشرح الآن")
                        }
                    }
                }
            }
        }
        return
    }

    val subjectType = SubjectType.fromId(activeLesson.subjectId)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("interactive_classroom_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Subject + Lesson Switcher Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SubjectType.entries) { subj ->
                        FilterChip(
                            selected = subj == subjectType,
                            onClick = { onSelectSubject(subj) },
                            label = { Text(subj.titleAr, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = allLessonsForSubject,
                        key = { item -> "quick_${item.id.ifBlank { "${item.subjectId}_${item.lessonOrder}_${item.lessonTitle}" }}" }
                    ) { item ->
                        val selected = item.id == activeLesson.id
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectLesson(item) },
                            label = {
                                Text(
                                    text = item.lessonTitle,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag("classroom_lesson_chip_${item.id}")
                        )
                    }
                }
            }
        }

        // ==================== TOP HALF: TEACHER + GREEN CHALKBOARD ====================
        item {
            ChalkboardTopSection(
                lesson = activeLesson,
                subjectType = subjectType,
                studentLevel = studentLevel,
                languageMode = languageMode,
                chalkboardState = chalkboardState,
                showGroundingSource = showGroundingSource,
                onToggleGroundingSource = { showGroundingSource = !showGroundingSource },
                onRegenerate = onRegenerateExplanation
            )
        }

        // ==================== INSTANT LANGUAGE SWITCHER BAR ====================
        item {
            InstantLanguageSwitcherCard(
                currentMode = languageMode,
                isBilingualSubject = subjectType.isBilingualSubject,
                sttRecognizeEnglish = sttRecognizeEnglish,
                onToggleSttLang = { sttRecognizeEnglish = !sttRecognizeEnglish },
                onSelectLanguageMode = onSwitchLanguageMode
            )
        }

        // ==================== BOTTOM HALF: FREE ON-DEVICE VOICE (SPEAKER + MIC) & INTERACTION ====================
        item {
            VoiceAndInteractionBottomSection(
                isSpeaking = isSpeaking,
                isListening = isListening,
                voiceStatusMessage = voiceStatusMessage,
                partialTranscript = partialTranscript,
                studentInputText = studentInputText,
                onStudentInputChange = { studentInputText = it },
                sttRecognizeEnglish = sttRecognizeEnglish,
                teacherReplyState = teacherReplyState,
                onSpeakerClick = {
                    if (isSpeaking) {
                        voiceManager.stopSpeaking()
                    } else {
                        when (val state = chalkboardState) {
                            is ChalkboardAiState.Ready -> {
                                voiceManager.speakChalkboardExplanation(
                                    greeting = state.response.warmGreetingToZein,
                                    steps = state.response.steps,
                                    question = state.response.singleSimpleQuestion,
                                    isBilingualSubject = subjectType.isBilingualSubject,
                                    languageMode = languageMode
                                )
                            }
                            else -> {
                                voiceManager.speakSingleText(
                                    text = activeLesson.officialContent,
                                    preferEnglish = subjectType.isBilingualSubject && languageMode == LanguageMode.ENGLISH
                                )
                            }
                        }
                    }
                },
                onSpeakTeacherReply = { replyText ->
                    voiceManager.speakSingleText(
                        text = replyText,
                        preferEnglish = false
                    )
                },
                onMicrophoneClick = {
                    if (isListening) {
                        voiceManager.stopListening()
                    } else {
                        val hasAudioPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasAudioPerm) {
                            voiceManager.startListening(
                                recognizeEnglish = sttRecognizeEnglish,
                                onResult = { recognizedText ->
                                    studentInputText = recognizedText
                                    onSendStudentQuestionOrAnswer(recognizedText)
                                }
                            )
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                onSubmitTypedAnswer = {
                    if (studentInputText.isNotBlank()) {
                        onSendStudentQuestionOrAnswer(studentInputText)
                    }
                }
            )
        }
    }
}

@Composable
private fun ChalkboardTopSection(
    lesson: CurriculumLesson,
    subjectType: SubjectType,
    studentLevel: StudentLevel,
    languageMode: LanguageMode,
    chalkboardState: ChalkboardAiState,
    showGroundingSource: Boolean,
    onToggleGroundingSource: () -> Unit,
    onRegenerate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 6.dp,
                color = ChalkboardWoodFrame,
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("green_chalkboard_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ChalkboardSurfaceTop, ChalkboardSurfaceBottom)
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Teacher Avatar Header inside the Chalkboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_teacher_avatar_1794083054459),
                    contentDescription = "شخصية المدرس الودود",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, ChalkYellowAccent, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = ChalkYellowAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${lesson.subjectNameAr} • مستوى ${studentLevel.labelAr}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChalkYellowAccent
                        )
                    }
                    Text(
                        text = lesson.lessonTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ChalkWhiteText
                    )
                    Text(
                        text = lesson.unitTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0)
                    )
                }
                IconButton(
                    onClick = onRegenerate,
                    modifier = Modifier.testTag("regenerate_chalkboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة شرح الدرس على السبورة",
                        tint = ChalkYellowAccent
                    )
                }
            }

            // Grounding Source Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "المرجع: ${lesson.sourceBookName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChalkWhiteText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                OutlinedButton(
                    onClick = onToggleGroundingSource,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChalkYellowAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("toggle_grounding_source_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showGroundingSource) "إخفاء نص الكتاب" else "عرض نص الكتاب المرجعي",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (showGroundingSource) {
                Surface(
                    color = Color(0xFF092618),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "النص المرجعي الحصري من كتاب المدرسة (Grounding Source):",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChalkYellowAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lesson.officialContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = ChalkWhiteText
                        )
                    }
                }
            }

            // Chalkboard Explanation Body
            when (chalkboardState) {
                is ChalkboardAiState.Idle, is ChalkboardAiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = ChalkYellowAccent)
                        Text(
                            text = "يقوم المدرس الخصوصي الآن بكتابة شرح الدرس لزين الدين على السبورة...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChalkWhiteText,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is ChalkboardAiState.Error -> {
                    Surface(
                        color = Color(0xFF3F1D1D),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = chalkboardState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFECACA)
                            )
                            Text(
                                text = "الشرح المرجعي المباشر من الكتاب المدرسي:",
                                style = MaterialTheme.typography.labelLarge,
                                color = ChalkYellowAccent
                            )
                            Text(
                                text = lesson.officialContent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChalkWhiteText
                            )
                            Button(
                                onClick = onRegenerate,
                                colors = ButtonDefaults.buttonColors(containerColor = ChalkYellowAccent)
                            ) {
                                Text("إعادة المحاولة مع المدرس الذكي", color = Color.Black)
                            }
                        }
                    }
                }

                is ChalkboardAiState.Ready -> {
                    val data = chalkboardState.response
                    // Warm Greeting
                    Surface(
                        color = Color(0x26FFFFFF),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "👋 ${data.warmGreetingToZein}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ChalkYellowAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Bilingual Steps on the Chalkboard
                    data.steps.forEachIndexed { idx, step ->
                        ChalkboardStepItem(
                            index = idx + 1,
                            step = step,
                            isBilingualSubject = subjectType.isBilingualSubject,
                            languageMode = languageMode
                        )
                    }

                    // Key Vocabulary Chips
                    if (data.keyTermsSummary.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "📌 أهم مصطلحات الدرس لزين الدين:",
                                style = MaterialTheme.typography.labelLarge,
                                color = ChalkYellowAccent
                            )
                            Text(
                                text = data.keyTermsSummary.joinToString("  •  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD1FAE5)
                            )
                        }
                    }

                    // Single Simple End-of-Lesson Question
                    Surface(
                        color = Color(0xFF1E563B),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, ChalkYellowAccent, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🌟 سؤال المدرس البسيط لزين الدين (+٢٥ نجمة):",
                                style = MaterialTheme.typography.labelLarge,
                                color = ChalkYellowAccent
                            )
                            Text(
                                text = data.singleSimpleQuestion,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ChalkWhiteText
                            )
                            Text(
                                text = data.encouragementMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChalkboardStepItem(
    index: Int,
    step: ChalkboardStep,
    isBilingualSubject: Boolean,
    languageMode: LanguageMode
) {
    Surface(
        color = Color(0x1FFFFFFF),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$index. ${step.stepTitle}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ChalkYellowAccent
            )
            if (step.englishLine.isNotBlank() && (!isBilingualSubject || languageMode != LanguageMode.ARABIC)) {
                Text(
                    text = step.englishLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ChalkCyanAccent,
                    fontWeight = FontWeight.Medium
                )
            }
            if (step.arabicExplanation.isNotBlank() && languageMode != LanguageMode.ENGLISH) {
                Text(
                    text = "💡 الشرح بالعربية: ${step.arabicExplanation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChalkWhiteText
                )
            }
        }
    }
}

@Composable
private fun InstantLanguageSwitcherCard(
    currentMode: LanguageMode,
    isBilingualSubject: Boolean,
    sttRecognizeEnglish: Boolean,
    onToggleSttLang: () -> Unit,
    onSelectLanguageMode: (LanguageMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("instant_language_switcher_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "التبديل الفوري للغات أثناء الدرس",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilterChip(
                    selected = sttRecognizeEnglish,
                    onClick = onToggleSttLang,
                    label = {
                        Text(
                            text = if (sttRecognizeEnglish) "المايكروفون: English" else "المايكروفون: عربي",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.testTag("mic_lang_toggle_chip")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageMode.entries.forEach { mode ->
                    val selected = mode == currentMode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectLanguageMode(mode) },
                        label = {
                            Text(
                                text = mode.badgeAr,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lang_mode_chip_${mode.code}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceAndInteractionBottomSection(
    isSpeaking: Boolean,
    isListening: Boolean,
    voiceStatusMessage: String,
    partialTranscript: String,
    studentInputText: String,
    onStudentInputChange: (String) -> Unit,
    sttRecognizeEnglish: Boolean,
    teacherReplyState: TeacherReplyState,
    onSpeakerClick: () -> Unit,
    onSpeakTeacherReply: (String) -> Unit,
    onMicrophoneClick: () -> Unit,
    onSubmitTypedAnswer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_controls_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Two Big Prominent Buttons: Speaker (TTS) & Microphone (STT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSpeakerClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .testTag("speaker_tts_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeaking) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "زر السماعة لقراءة السبورة"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpeaking) "إيقاف القراءة" else "سماعة السبورة",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Button(
                    onClick = onMicrophoneClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .testTag("microphone_stt_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) {
                            Color(0xFFDC2626)
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "زر المايكروفون للتحدث مع المدرس"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "جاري الاستماع..." else "تحدث للمدرس",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Voice Status Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isListening && partialTranscript.isNotBlank()) {
                        "🎙️ ما يقوله زين الدين الآن: $partialTranscript"
                    } else {
                        "🔊 $voiceStatusMessage"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Text / Voice Input Field so Zein can also type or edit his spoken answer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = studentInputText,
                    onValueChange = onStudentInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("student_answer_input"),
                    placeholder = {
                        Text("اكتب إجابة زين الدين أو سؤاله للمدرس هنا...")
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = false,
                    maxLines = 3
                )

                Button(
                    onClick = onSubmitTypedAnswer,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("send_answer_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال للمدرس"
                    )
                }
            }

            // Teacher Live Evaluation / Conversational Reply Box
            when (teacherReplyState) {
                is TeacherReplyState.Idle -> {}
                is TeacherReplyState.Thinking -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("المدرس يستمع ويحضّر الرد لزين الدين...")
                    }
                }
                is TeacherReplyState.Streaming -> {
                    TeacherFeedbackBox(
                        replyText = teacherReplyState.text,
                        earnedPoints = null,
                        onReadAloud = { onSpeakTeacherReply(teacherReplyState.text) }
                    )
                }
                is TeacherReplyState.Done -> {
                    TeacherFeedbackBox(
                        replyText = teacherReplyState.text,
                        earnedPoints = teacherReplyState.earnedPoints,
                        onReadAloud = { onSpeakTeacherReply(teacherReplyState.text) }
                    )
                }
                is TeacherReplyState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = teacherReplyState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherFeedbackBox(
    replyText: String,
    earnedPoints: Long?,
    onReadAloud: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_feedback_box")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (earnedPoints != null) {
                            "رد المدرس الخصوصي (+$earnedPoints نجمة لزين!)"
                        } else {
                            "المدرس يكتب الرد الآن..."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(onClick = onReadAloud) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "نطق رد المدرس",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = replyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
