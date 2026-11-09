package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Book
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
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.CurriculumLesson
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectType
import com.example.util.AppDiagnosticsLogger
import com.example.util.InAppUpdateManager
import com.example.util.LogSeverity
import com.example.util.OnDeviceVoiceManager
import com.example.util.UpdateCheckState
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val updateManager = remember(context) { InAppUpdateManager(context) }
    val voiceManager = remember(context) { OnDeviceVoiceManager(context) }

    val savedRepoSlug by updateManager.githubRepoSlug.collectAsStateWithLifecycle()
    val savedGithubToken by updateManager.githubToken.collectAsStateWithLifecycle()
    val updateState by updateManager.updateState.collectAsStateWithLifecycle()
    val diagnosticLogs by AppDiagnosticsLogger.logs.collectAsStateWithLifecycle()

    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    var githubRepoInput by rememberSaveable(savedRepoSlug) { mutableStateOf(savedRepoSlug) }
    var githubTokenInput by rememberSaveable(savedGithubToken) { mutableStateOf(savedGithubToken) }
    var updateActionNotice by remember { mutableStateOf<String?>(null) }

    var manualBugNoteInput by rememberSaveable { mutableStateOf("") }
    var reportCopiedNotice by remember { mutableStateOf<String?>(null) }

    // PDF Ingestion states
    var selectedPdfSubject by remember { mutableStateOf(SubjectType.MATH) }
    var selectedPdfTerm by rememberSaveable { mutableStateOf(profile.currentTerm) }
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
                // استخراج اسم الملف
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

                // قراءة محتوى الملف كنص (إذا كان text أو تدعيم استخراج المحتوى)
                val stream = context.contentResolver.openInputStream(uri)
                val rawBytes = stream?.readBytes() ?: ByteArray(0)
                stream?.close()

                val extracted = try {
                    val rawStr = String(rawBytes, Charsets.UTF_8)
                    // تنظيف الأحرف غير المقروءة في حال ملف ثنائي
                    val cleanLines = rawStr.lines()
                        .filter { it.isNotBlank() && it.any { ch -> ch.isLetterOrDigit() } }
                        .joinToString("\n")
                    if (cleanLines.length > 50) cleanLines.take(3000) else "محتوى ملف PDF: $displayName جاهز للمذاكرة والشرح والمراجعة."
                } catch (_: Exception) {
                    "ملف دراسي موثق ($displayName) يحتوي على شروحات وتدريبات المنهج المقرر لـ ${selectedPdfSubject.titleAr}."
                }

                pdfContentText = extracted
                pdfStatusMessage = "تم اختيار ملف ($displayName) بنجاح - يمكنك مراجعة العنوان والنقر على حفظ لإضافته فوراً."
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
        // 0. Header with Academic Settings Gear (ترس إعدادات السنة والترم والمواد)
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${profile.gradeLabel} • ${profile.currentTerm}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "العام الدراسي: ${profile.academicYear} | ${profile.activeSubjectIds.size} مواد مقررة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .testTag("academic_settings_gear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "ترس إعدادات السنة والترم والمواد",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 1. In-App GitHub OTA Update Card (التحديث الداخلي التلقائي بعد المزامنة مع GitHub)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("in_app_github_update_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التحديث الداخلي التلقائي عبر GitHub 🚀",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الإصدار الحالي المثبت: v${BuildConfig.VERSION_NAME} (كود البناء: ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚙️ تم إعداد ملف الـ Workflow التلقائي في المشروع:\n(.github/workflows/android-release-ota.yml + version.json)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "بمجرد عمل مزامنة (Push/Sync) مع GitHub، يقوم GitHub Actions تلقائياً ببناء ملف الـ APK الجديد ونشره في Releases ليظهر لك إشعار التحديث هنا فوراً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = githubRepoInput,
                        onValueChange = { githubRepoInput = it },
                        label = { Text("اسم مستودع GitHub (مثال: username/repo)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_repo_input")
                    )

                    // شرح وتوجيه واضح جداً بخصوص المستودع الخاص (Private)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🔒 هام: هل مستودع GitHub مضبوط كـ Private (خاص)؟",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "إذا كان المستودع Private، فإن GitHub يمنع هواتف الأندرويد من جلب الـ APK ويعطي خطأ 404.\n" +
                                        "• للحل بضغطة واحدة: اجعل المستودع Public (عام) في GitHub عبر الزر أدناه.\n" +
                                        "• أو: اكتب رمز GitHub Token في الخانة التالية إذا رغبت بإبقائه خاصاً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedButton(
                                onClick = {
                                    val targetSlug = githubRepoInput.trim().ifBlank { InAppUpdateManager.DEFAULT_REPO_SLUG }
                                    val settingsUrl = "https://github.com/$targetSlug/settings"
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(settingsUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فتح إعدادات المستودع في GitHub (لتحويله إلى Public)")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = githubTokenInput,
                        onValueChange = { githubTokenInput = it },
                        label = { Text("رمز GitHub Token (اختياري - فقط في حال إبقاء المستودع Private)") },
                        placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_token_input")
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                updateManager.checkForUpdates(githubRepoInput, githubTokenInput)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("check_github_update_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فحص وجود تحديث جديد على GitHub الآن")
                    }

                    when (val state = updateState) {
                        is UpdateCheckState.Idle -> {}
                        is UpdateCheckState.Checking -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري الاتصال بـ GitHub وفحص أحدث نسخة APK...")
                            }
                        }
                        is UpdateCheckState.UpdateAvailable -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "🎉 يتوفر تحديث جديد: ${state.releaseTitle} (${state.latestVersionName})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = state.changelog,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Button(
                                        onClick = {
                                            updateActionNotice = updateManager.downloadAndInstallUpdate(
                                                apkUrl = state.apkDownloadUrl,
                                                versionLabel = state.latestVersionName
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("download_install_apk_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تحميل وتثبيت التحديث الآن (APK)")
                                    }
                                }
                            }
                        }
                        is UpdateCheckState.UpToDate -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✅ ${state.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        is UpdateCheckState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ℹ️ [${state.errorCode}] ${state.message}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val targetSlug = githubRepoInput.trim().ifBlank { InAppUpdateManager.DEFAULT_REPO_SLUG }
                                            val releasesUrl = "https://github.com/$targetSlug/releases"
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releasesUrl)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.Launch, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فتح صفحة Releases على GitHub للتحميل المباشر")
                                    }
                                }
                            }
                        }
                    }

                    if (updateActionNotice != null) {
                        Text(
                            text = updateActionNotice.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Error Log & Bug Report Card (زرار إضافة أخطاء + سجل اللوج + نسخ الريبورت لإرساله للمطور)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("error_log_and_report_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
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
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "سجل الأخطاء وتقارير الفحص (Log & Error Report)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اعرف كود الخطأ بالضبط بعد تسطيب الـ APK وانسخه بضغطة واحدة للإصلاح",
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

                    // إضافة ملاحظة أو بلاغ خطأ يدوي
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualBugNoteInput,
                            onValueChange = { manualBugNoteInput = it },
                            label = { Text("سجّل ملاحظة أو مشكلة واجهتك في الـ APK...") },
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
                                        screenName = "UserAPKReport",
                                        userDescription = manualBugNoteInput
                                    )
                                    manualBugNoteInput = ""
                                    reportCopiedNotice = "تمت إضافة ملاحظتك إلى سجل الأخطاء بنجاح!"
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("add_bug_note_button")
                        ) {
                            Text("إضافة خطأ")
                        }
                    }

                    // أزرار الفحص الذاتي ونسخ التقرير الكامل
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRunSystemDiagnostics,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("run_self_diagnostics_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فحص النظام الآن")
                        }

                        Button(
                            onClick = {
                                val fullReport = AppDiagnosticsLogger.buildFullReportText()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("ZeinTutor_Error_Report", fullReport)
                                )
                                reportCopiedNotice = "تم نسخ تقرير الأخطاء واللوج بالكامل! الصقه في الشات لإصلاحه فوراً."
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("copy_full_error_report_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ ريبورت الأخطاء")
                        }
                    }

                    if (reportCopiedNotice != null) {
                        Text(
                            text = reportCopiedNotice.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // عرض قائمة اللوج والأكواد
                    Surface(
                        color = Color(0xFF0D1914),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "سجل الأحداث والأكواد البرمجية (${diagnosticLogs.size} سجل):",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFCD34D),
                                fontWeight = FontWeight.Bold
                            )
                            diagnosticLogs.take(15).forEach { entry ->
                                val badgeColor = when (entry.severity) {
                                    LogSeverity.ERROR, LogSeverity.USER_REPORT -> Color(0xFFFCA5A5)
                                    LogSeverity.WARN -> Color(0xFFFDE68A)
                                    LogSeverity.INFO -> Color(0xFFA7F3D0)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "[${entry.timestamp}] [${entry.severity.badgeCode}] ${entry.tag} • Code: ${entry.errorCode}",
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
                                    if (entry.technicalDetails.isNotBlank()) {
                                        Text(
                                            text = entry.technicalDetails,
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFD1D5DB)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Uploaded School PDF Books Overview & One-Tap Sync Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "الكتب المدرسية المرفوعة في قاعدة البيانات (٢٠٢٦/٢٠٢٧)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إجمالي الوحدات والدروس المحفوظة حالياً: $totalSyncedLessonsCount وحدة ودرساً مرجعياً",
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
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = book.titleAr,
                                        style = MaterialTheme.typography.labelLarge,
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
                            .height(50.dp)
                            .testTag("sync_uploaded_books_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مزامنة وتحديث جميع الكتب المرفوعة في قاعدة البيانات الآن")
                    }
                }
            }
        }

        // 4. مركز رفع وإدارة ملفات المنهج والكتب الدراسية بصيغة PDF (PDF Curriculum Documents Center)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdf_curriculum_upload_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مركز رفع وتعديل كتب وملفات المنهج (PDF)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ارفع ملفات الـ PDF للترم الحالي والترم الثاني والعام القادم ليقوم الذكاء الاصطناعي بدراستها واعتمادها كمرجع حصري للتدريس",
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الملف المختار: $selectedPdfFileName",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pdfDocumentTitle,
                        onValueChange = { pdfDocumentTitle = it },
                        label = { Text("اسم الكتاب أو المذكرة (مثال: كتاب المدرسة ترم ثاني أو ملحق مراجعة نهائية)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pdfKeyTerms,
                        onValueChange = { pdfKeyTerms = it },
                        label = { Text("ملاحظات ومصطلحات رئيسية في الملف (اختياري)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_terms_input"),
                        singleLine = true
                    )

                    if (pdfContentText.isNotBlank()) {
                        OutlinedTextField(
                            value = pdfContentText,
                            onValueChange = { pdfContentText = it },
                            label = { Text("محتوى المنهج المستخرج والمدروس من الملف") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 6
                        )
                    }

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
                                "ملف منهج مقرر لـ ${selectedPdfSubject.titleAr} (${selectedPdfTerm}) - يحتوي على الشروحات والأنشطة والتقييمات للعام الدراسي ${profile.academicYear}."
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

                    // قائمة الملفات والدروس المضافة مؤخراً
                    val customUploadedLessons = allLessons.filter { it.unitTitle.startsWith("محتوى مرفوع") || it.sourceBookName.startsWith("ملف PDF") }
                    if (customUploadedLessons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
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
                                        .padding(10.dp),
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
    }

    // =========================================================================
    // نافذة ترس الإعدادات: السنة الدراسية والترم والمواد المقررة والصوت العربي
    // =========================================================================
    if (showSettingsDialog) {
        var selectedGrade by rememberSaveable { mutableStateOf(profile.gradeLabel) }
        var selectedYear by rememberSaveable { mutableStateOf(profile.academicYear) }
        var selectedTerm by rememberSaveable { mutableStateOf(profile.currentTerm) }
        var activeSubjects by rememberSaveable { mutableStateOf(profile.activeSubjectIds) }
        var pitchVal by rememberSaveable { mutableFloatStateOf(profile.speechPitch) }
        var rateVal by rememberSaveable { mutableFloatStateOf(profile.speechRate) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إعدادات السنة والترم والمواد المقررة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ١. اختيار الصف الدراسي (الصفوف الابتدائية كاملة من الأول إلى السادس)
                    Text(
                        text = "الصف الدراسي (الصفوف الابتدائية ١-٦):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الأول الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الأول الابتدائي لغات" },
                                label = { Text("١ب (الأول)") }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الثاني الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الثاني الابتدائي لغات" },
                                label = { Text("٢ب (الثاني)") }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الثالث الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الثالث الابتدائي لغات" },
                                label = { Text("٣ب (الثالث)") }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الرابع الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الرابع الابتدائي لغات" },
                                label = { Text("٤ب (الرابع)") }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف الخامس الابتدائي لغات",
                                onClick = { selectedGrade = "الصف الخامس الابتدائي لغات" },
                                label = { Text("٥ب (الخامس)") }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedGrade == "الصف السادس الابتدائي لغات",
                                onClick = { selectedGrade = "الصف السادس الابتدائي لغات" },
                                label = { Text("٦ب (السادس)") }
                            )
                        }
                    }

                    // ٢. الترم الدراسي (متاح التبديل والاختيار في أي وقت)
                    Text(
                        text = "الفصل الدراسي (الترم):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedTerm == "الترم الأول",
                            onClick = { selectedTerm = "الترم الأول" },
                            label = { Text("الترم الأول (الحالي)") }
                        )
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedTerm == "الترم الثاني",
                            onClick = { selectedTerm = "الترم الثاني" },
                            label = { Text("الترم الثاني") }
                        )
                    }

                    // ٣. العام الدراسي
                    Text(
                        text = "العام الدراسي:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedYear == "٢٠٢٥ / ٢٠٢٦",
                            onClick = { selectedYear = "٢٠٢٥ / ٢٠٢٦" },
                            label = { Text("٢٠٢٥/٢٦") }
                        )
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedYear == "٢٠٢٦ / ٢٠٢٧",
                            onClick = { selectedYear = "٢٠٢٦ / ٢٠٢٧" },
                            label = { Text("٢٠٢٦/٢٧") }
                        )
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedYear == "٢٠٢٧ / ٢٠٢٨",
                            onClick = { selectedYear = "٢٠٢٧ / ٢٠٢٨" },
                            label = { Text("٢٠٢٧/٢٨") }
                        )
                    }

                    // ٤. المواد المقررة النشطة
                    Text(
                        text = "المواد المقررة للتدريس:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    SubjectType.entries.forEach { subj ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = subj.titleAr,
                                style = MaterialTheme.typography.bodyMedium
                            )
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

                    // ٥. إعدادات الصوت العربي الطبيعي
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نبرة وسرعة صوت المدرس (صوت طبيعي فصيح):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "سرعة الإلقاء: ${"%.2f".format(rateVal)}x (هادئ وواضح لطفل لغات)",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = rateVal,
                                onValueChange = {
                                    rateVal = it
                                    voiceManager.updateVoiceTuning(pitchVal, it)
                                },
                                valueRange = 0.7f..1.1f
                            )
                            OutlinedButton(
                                onClick = { voiceManager.openAndroidTtsSettings() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فتح إعدادات تحويل النص لكلام في الهاتف (Google TTS)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
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
                        showSettingsDialog = false
                    }
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

