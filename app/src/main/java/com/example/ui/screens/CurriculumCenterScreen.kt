package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CurriculumLesson
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectType
import com.example.util.AppDiagnosticsLogger
import com.example.util.LogSeverity
import com.example.util.OnDeviceVoiceManager

private data class UploadedBookOverview(
    val titleAr: String,
    val sourceFileNames: String,
    val unitsSummaryAr: String
)

private val UPLOADED_SCHOOL_BOOKS = listOf(
    UploadedBookOverview(
        titleAr = "١. اللغة العربية (وزارة التربية والتعليم + ملحق إجابات الأضواء ٢٠٢٧)",
        sourceFileNames = "Arabic_language_prim2_t1.pdf + 2_إجابات_الاضواء_اللغة_العربية_2ب_ف1_2027.pdf",
        unitsSummaryAr = "٣ وحدات كاملة: المعاملة الطيبة، عادات صحية، الرياضة والتكنولوجيا + بنك أسئلة وإجابات الأضواء."
    ),
    UploadedBookOverview(
        titleAr = "٢. التربية الدينية الإسلامية (كتاب الوزارة والأزهر الشريف ٢٠٢٦/٢٠٢٧)",
        sourceFileNames = "Islamic_religion_prim2_t1.pdf",
        unitsSummaryAr = "٣ وحدات كاملة: التوكل على الله، من نعم الله، الشكر لله (العقيدة، سور القرآن، السيرة والعبادات)."
    ),
    UploadedBookOverview(
        titleAr = "٣. الرياضيات باللغة الإنجليزية (كتاب الوزارة + كتاب المعاصر الأساسي ٢٠٢٧ + ملحق التقييمات والامتحانات)",
        sourceFileNames = "Math_EN_P2_T1.pdf + معاصر ماث 2ب ترم اول 2027.pdf + ملحق معاصر ماث 2ب ترم اول 2027.pdf",
        unitsSummaryAr = "٧ وحدات شاملة تغطي Chapters 1–9، ٢٦ شيت تطبيقي، ١٥ تقييماً أسبوعياً، و١٥ امتحاناً نهائياً شاملاً + القاموس."
    ),
    UploadedBookOverview(
        titleAr = "٤. اللغة الإنجليزية (كتاب الوزارة + كراسة المعاصر التفاعلية ٢٠٢٧ + كشكول التحضير المتقدم)",
        sourceFileNames = "English_language_prim2_t1.pdf + ملحق_المعاصر_انجليزي_2ب_الترم_الاول_2027 + Prim 2_Preparation_T1_Mr Sobhy Borda.pdf",
        unitsSummaryAr = "٩ وحدات شاملة لـ Connect و Connect Plus: مراجعة A Week Is Enough، قواعد متقدمة، و٢٠ امتحاناً شاملاً للمحافظات."
    )
)

@Composable
fun CurriculumCenterScreen(
    currentUserId: String,
    profile: StudentProfile,
    allLessons: List<CurriculumLesson>,
    totalSyncedLessonsCount: Int,
    onSyncUploadedBooks: () -> Unit,
    onRunSystemDiagnostics: () -> Unit = {},
    onUpdateAcademicSettings: (
        gradeLabel: String,
        academicYear: String,
        currentTerm: String,
        activeSubjectIds: List<String>,
        speechPitch: Float,
        speechRate: Float
    ) -> Unit,
    onAddCurriculumPdfDocument: (
        subject: SubjectType,
        documentTitle: String,
        term: String,
        extractedContent: String,
        fileName: String,
        keyTermsSummary: String
    ) -> Unit,
    onDeleteLesson: (lessonId: String) -> Unit
) {
    val context = LocalContext.current
    val voiceManager = remember(context) { OnDeviceVoiceManager(context) }
    val diagnosticLogs by AppDiagnosticsLogger.logs.collectAsStateWithLifecycle()

    // Academic Settings State
    var selectedGrade by rememberSaveable(profile.gradeLabel) { mutableStateOf(profile.gradeLabel) }
    var selectedYear by rememberSaveable(profile.academicYear) { mutableStateOf(profile.academicYear) }
    var selectedTerm by rememberSaveable(profile.currentTerm) { mutableStateOf(profile.currentTerm) }
    var activeSubjects by rememberSaveable(profile.activeSubjectIds) { mutableStateOf(profile.activeSubjectIds) }
    var pitchVal by rememberSaveable(profile.speechPitch) { mutableFloatStateOf(profile.speechPitch) }
    var rateVal by rememberSaveable(profile.speechRate) { mutableFloatStateOf(profile.speechRate) }
    var settingsSavedNotice by remember { mutableStateOf<String?>(null) }

    // Diagnostics / Error Log States
    var manualBugNoteInput by rememberSaveable { mutableStateOf("") }
    var reportCopiedNotice by remember { mutableStateOf<String?>(null) }

    // PDF Ingestion states
    var selectedPdfSubject by remember { mutableStateOf(SubjectType.MATH) }
    var selectedPdfTerm by rememberSaveable(profile.currentTerm) { mutableStateOf(profile.currentTerm) }
    var pdfDocumentTitle by rememberSaveable { mutableStateOf("") }
    var pdfKeyTerms by rememberSaveable { mutableStateOf("") }
    var pdfContentText by rememberSaveable { mutableStateOf("") }
    var selectedPdfFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var pdfStatusMessage by remember { mutableStateOf<String?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var displayName = "curriculum_document.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
                selectedPdfFileName = displayName
                if (pdfDocumentTitle.isBlank()) {
                    pdfDocumentTitle = displayName.removeSuffix(".pdf").replace("_", " ")
                }

                val stream = context.contentResolver.openInputStream(uri)
                val rawBytes = stream?.readBytes() ?: ByteArray(0)
                stream?.close()

                val extracted = try {
                    val rawStr = String(rawBytes, Charsets.UTF_8)
                    val cleanLines = rawStr.lines()
                        .filter { it.isNotBlank() && it.any { ch -> ch.isLetterOrDigit() } }
                        .joinToString("\n")
                    if (cleanLines.length > 50) cleanLines.take(3000) else "محتوى ملف PDF: $displayName جاهز للمذاكرة والشرح والمراجعة."
                } catch (_: Exception) {
                    "ملف دراسي موثق ($displayName) يحتوي على شروحات وتدريبات المنهج المقرر لـ ${selectedPdfSubject.titleAr}."
                }

                pdfContentText = extracted
                pdfStatusMessage = "تم اختيار ملف ($displayName) بنجاح - انقر على حفظ لاعتماده في الشرح والسبورة."
                AppDiagnosticsLogger.logInfo("PdfPicker", "PDF_SELECTED", "تم اختيار الملف: $displayName")
            } catch (e: Exception) {
                pdfStatusMessage = "تعذر قراءة الملف: ${e.localizedMessage ?: ""}"
                AppDiagnosticsLogger.logError("PdfPicker", "PDF_READ_FAIL", "فشل قراءة ملف PDF", e)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("curriculum_center_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ١. الرأس الأنيق: إعدادات التطبيق والمناهج ⚙️
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الإعدادات وإدارة المناهج",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${profile.gradeLabel} • ${profile.currentTerm} (${profile.academicYear})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // ٢. بطاقة إعدادات المرحلة الدراسية والترم والمواد المقررة (Academic Settings Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academic_settings_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "السنة والترم والمواد المقررة لزين الدين",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // أ. اختيار الصف الدراسي (الصفوف الابتدائية ١-٦)
                    Text(
                        text = "١. الصف الدراسي (الصفوف الابتدائية ١ إلى ٦ لغات):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الأول الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الأول الابتدائي لغات" },
                                label = { Text("١ب (الصف الأول)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الثاني الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الثاني الابتدائي لغات" },
                                label = { Text("٢ب (الصف الثاني)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الثالث الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الثالث الابتدائي لغات" },
                                label = { Text("٣ب (الصف الثالث)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الرابع الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الرابع الابتدائي لغات" },
                                label = { Text("٤ب (الصف الرابع)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الخامس الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الخامس الابتدائي لغات" },
                                label = { Text("٥ب (الصف الخامس)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف السادس الابتدائي لغات",
                                onClick = { selectedGrade = "الصف السادس الابتدائي لغات" },
                                label = { Text("٦ب (الصف السادس)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    // ب. اختيار الترم (الترم الأول الحالي / الترم الثاني)
                    Text(
                        text = "٢. الفصل الدراسي (الترم):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedTerm == "الترم الأول",
                            onClick = { selectedTerm = "الترم الأول" },
                            label = { Text("الترم الأول (الحالي)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedTerm == "الترم الثاني",
                            onClick = { selectedTerm = "الترم الثاني" },
                            label = { Text("الترم الثاني") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    // ج. العام الدراسي
                    Text(
                        text = "٣. العام الدراسي:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("٢٠٢٥ / ٢٠٢٦", "٢٠٢٦ / ٢٠٢٧", "٢٠٢٧ / ٢٠٢٨").forEach { yr ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedYear == yr,
                                onClick = { selectedYear = yr },
                                label = { Text(yr) }
                            )
                        }
                    }

                    // د. المواد المقررة للتدريس
                    Text(
                        text = "٤. المواد المقررة للتدريس (${activeSubjects.size} مواد مفعّلة):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SubjectType.entries.forEach { subj ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(subj.accentHex),
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = subj.titleAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Switch(
                                    checked = activeSubjects.contains(subj.id),
                                    onCheckedChange = { isChecked ->
                                        activeSubjects = if (isChecked) {
                                            activeSubjects + subj.id
                                        } else {
                                            if (activeSubjects.size > 1) activeSubjects - subj.id else activeSubjects
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // زر حفظ الإعدادات
                    Button(
                        onClick = {
                            onUpdateAcademicSettings(
                                selectedGrade,
                                selectedYear,
                                selectedTerm,
                                activeSubjects,
                                pitchVal,
                                rateVal
                            )
                            settingsSavedNotice = "تم حفظ الصف ($selectedGrade) و($selectedTerm) والعام ($selectedYear) بنجاح!"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_academic_settings_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ وتطبيق الإعدادات الآن")
                    }

                    if (settingsSavedNotice != null) {
                        Text(
                            text = settingsSavedNotice.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ٣. بطاقة نبرة وسرعة صوت المدرس الطبيعي (Voice Tuning Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_voice_tuning_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "نبرة وسرعة صوت المدرس العربي الطبيعي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "سرعة الإلقاء والشرح: ${"%.2f".format(rateVal)}x (هادئ وواضح لطفل المرحلة الابتدائية)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = rateVal,
                        onValueChange = {
                            rateVal = it
                            voiceManager.updateVoiceTuning(pitchVal, it)
                        },
                        valueRange = 0.7f..1.15f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                voiceManager.speakSingleText(
                                    "أهلاً بك يا زين الدين! نحن الآن في $selectedGrade $selectedTerm، هل أنت مستعد للدرس؟",
                                    preferEnglish = false
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تجربة صوت المدرس")
                        }

                        OutlinedButton(
                            onClick = { voiceManager.openAndroidTtsSettings() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعدادات محرك Google TTS")
                        }
                    }
                }
            }
        }

        // ٤. مركز رفع وإدارة كتب وملفات المنهج بصيغة PDF (PDF Curriculum Documents Center)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdf_curriculum_upload_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مركز رفع وتعديل كتب وملفات المنهج (PDF)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ارفع ملفات المناهج والمذكرات ليعتمدها الذكاء الاصطناعي كمرجع أساسي للشرح",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // اختيار المادة
                    Text(
                        text = "١. حدد المادة الدراسية للملف المرفوع:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SubjectType.entries) { subject ->
                            FilterChip(
                                selected = subject == selectedPdfSubject,
                                onClick = { selectedPdfSubject = subject },
                                label = { Text(subject.titleAr) },
                                modifier = Modifier.testTag("pdf_subject_chip_${subject.id}")
                            )
                        }
                    }

                    // اختيار الترم
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "٢. الفصل الدراسي:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FilterChip(
                            selected = selectedPdfTerm == "الترم الأول",
                            onClick = { selectedPdfTerm = "الترم الأول" },
                            label = { Text("الترم الأول") }
                        )
                        FilterChip(
                            selected = selectedPdfTerm == "الترم الثاني",
                            onClick = { selectedPdfTerm = "الترم الثاني" },
                            label = { Text("الترم الثاني") }
                        )
                    }

                    // زر اختيار ملف PDF من ذاكرة الجهاز
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("pick_pdf_file_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedPdfFileName != null) "تغيير ملف الـ PDF المختار" else "اختر ملف PDF من الهاتف / التابلت")
                            }

                            if (selectedPdfFileName != null) {
                                Text(
                                    text = "الملف المختار: $selectedPdfFileName",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pdfDocumentTitle,
                        onValueChange = { pdfDocumentTitle = it },
                        label = { Text("عنوان الكتاب أو المذكرة (مثال: كتاب المعاصر ماث الوحدة الثالثة)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_doc_title_input")
                    )

                    OutlinedTextField(
                        value = pdfKeyTerms,
                        onValueChange = { pdfKeyTerms = it },
                        label = { Text("كلمات مفتاحية ومصطلحات رئيسية (اختياري)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_key_terms_input")
                    )

                    if (pdfStatusMessage != null) {
                        Text(
                            text = pdfStatusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            val finalTitle = pdfDocumentTitle.ifBlank {
                                selectedPdfFileName?.removeSuffix(".pdf") ?: "كتاب دراسي جديد"
                            }
                            val finalFileName = selectedPdfFileName ?: "uploaded_manual_curriculum.pdf"
                            val finalContent = pdfContentText.ifBlank {
                                "ملف منهج مقرر لـ ${selectedPdfSubject.titleAr} ($selectedPdfTerm) - يحتوي على الشروحات والأنشطة والتقييمات للعام الدراسي ${profile.academicYear}."
                            }
                            onAddCurriculumPdfDocument(
                                selectedPdfSubject,
                                finalTitle,
                                selectedPdfTerm,
                                finalContent,
                                finalFileName,
                                pdfKeyTerms
                            )
                            pdfStatusMessage = "🎉 تم حفظ ملف المنهج ($finalTitle) بنجاح وأصبح جاهزاً للاستخدام في الشات والسبورة!"
                            selectedPdfFileName = null
                            pdfDocumentTitle = ""
                            pdfKeyTerms = ""
                            pdfContentText = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_pdf_curriculum_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ الكتاب في قاعدة بيانات الذكاء الاصطناعي")
                    }

                    // قائمة الملفات المضافة يدوياً
                    val customUploadedLessons = allLessons.filter { it.unitTitle.startsWith("محتوى مرفوع") || it.sourceBookName.startsWith("ملف PDF") }
                    if (customUploadedLessons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ملفات ومذكرات الـ PDF المضافة يدوياً (${customUploadedLessons.size}):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        customUploadedLessons.forEach { doc ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.lessonTitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${doc.subjectNameAr} • ${doc.sourceBookName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteLesson(doc.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف الملف",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ٥. بطاقة الكتب المدرسية المعتمدة في النظام (School Books Catalog)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الكتب المدرسية المدمجة في قاعدة البيانات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إجمالي الوحدات والدروس المرجعية: $totalSyncedLessonsCount وحدة ودرساً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    UPLOADED_SCHOOL_BOOKS.forEach { book ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = book.titleAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "الملفات: ${book.sourceFileNames}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = book.unitsSummaryAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onSyncUploadedBooks,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sync_uploaded_books_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مزامنة وتحديث جميع الكتب المدمجة")
                    }
                }
            }
        }

        // ٦. بطاقة فحص النظام وسجل الأخطاء والتقارير (Diagnostics & Error Report)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("error_log_and_report_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "سجل الفحص والتشخيص (Diagnostics Log)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "انسخ تقرير الفحص بنقرة واحدة عند مواجهة أي مشكلة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { AppDiagnosticsLogger.clearLogs() },
                            modifier = Modifier.testTag("clear_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح اللوج"
                            )
                        }
                    }

                    // إضافة ملاحظة يدوية
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualBugNoteInput,
                            onValueChange = { manualBugNoteInput = it },
                            label = { Text("سجّل ملاحظة أو مشكلة واجهتك...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_bug_note_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = {
                                if (manualBugNoteInput.isNotBlank()) {
                                    AppDiagnosticsLogger.addManualUserBugReport(
                                        screenName = "UserReport",
                                        userDescription = manualBugNoteInput
                                    )
                                    manualBugNoteInput = ""
                                    reportCopiedNotice = "تمت إضافة ملاحظتك بنجاح!"
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("add_bug_note_button")
                        ) {
                            Text("إضافة")
                        }
                    }

                    // أزرار الفحص والنسخ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRunSystemDiagnostics,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("run_self_diagnostics_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فحص النظام")
                        }

                        Button(
                            onClick = {
                                val fullReport = AppDiagnosticsLogger.buildFullReportText()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("ZeinTutor_Report", fullReport)
                                )
                                reportCopiedNotice = "تم نسخ تقرير الأخطاء واللوج بنجاح!"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("copy_full_error_report_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ التقرير")
                        }
                    }

                    if (reportCopiedNotice != null) {
                        Text(
                            text = reportCopiedNotice.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // نافذة عرض السجلات
                    Surface(
                        color = Color(0xFF0F1E17),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "سجل الأحداث البرمجية (${diagnosticLogs.size} سجل):",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFCD34D),
                                fontWeight = FontWeight.Bold
                            )
                            diagnosticLogs.take(10).forEach { entry ->
                                val badgeColor = when (entry.severity) {
                                    LogSeverity.ERROR, LogSeverity.USER_REPORT -> Color(0xFFFCA5A5)
                                    LogSeverity.WARN -> Color(0xFFFDE68A)
                                    LogSeverity.INFO -> Color(0xFFA7F3D0)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "[${entry.timestamp}] ${entry.tag} • ${entry.errorCode}",
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = entry.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
