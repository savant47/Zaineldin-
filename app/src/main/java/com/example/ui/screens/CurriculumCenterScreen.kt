package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.SubjectType
import com.example.util.AppDiagnosticsLogger
import com.example.util.InAppUpdateManager
import com.example.util.LogSeverity
import com.example.util.UpdateCheckState
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
    totalSyncedLessonsCount: Int,
    onSyncUploadedBooks: () -> Unit,
    onRunSystemDiagnostics: () -> Unit = {},
    onAddGroundedLesson: (
        subject: SubjectType,
        unitTitle: String,
        lessonTitle: String,
        officialContent: String,
        keyTerms: String,
        sourceBookName: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember(context) { InAppUpdateManager(context) }

    val savedRepoSlug by updateManager.githubRepoSlug.collectAsStateWithLifecycle()
    val updateState by updateManager.updateState.collectAsStateWithLifecycle()
    val diagnosticLogs by AppDiagnosticsLogger.logs.collectAsStateWithLifecycle()

    var githubRepoInput by rememberSaveable(savedRepoSlug) { mutableStateOf(savedRepoSlug) }
    var updateActionNotice by remember { mutableStateOf<String?>(null) }

    var manualBugNoteInput by rememberSaveable { mutableStateOf("") }
    var reportCopiedNotice by remember { mutableStateOf<String?>(null) }

    var selectedSubject by remember { mutableStateOf(SubjectType.MATH) }
    var unitTitle by remember { mutableStateOf("Unit 3: Two-Digit Addition") }
    var lessonTitle by remember { mutableStateOf("Lesson 1: Adding with Regrouping") }
    var officialContent by remember { mutableStateOf("") }
    var keyTerms by remember { mutableStateOf("") }
    var sourceBookName by remember { mutableStateOf("Primary 2 Official School PDF 2026/2027") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("curriculum_center_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

                    Button(
                        onClick = {
                            scope.launch {
                                updateManager.checkForUpdates(githubRepoInput)
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
                                Text(
                                    text = "ℹ️ [${state.errorCode}] ${state.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
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

        // 4. Direct Curriculum Ingestion Form (إضافة درس جديد من كتاب المدرسة)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "إضافة درس جديد من كتاب المدرسة (Grounding)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ارفع نص الدرس من ملف الـ PDF مباشرة إلى Firebase Firestore ليكون المرجع الوحيد للمدرس الذكي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "اختر المادة (من المواد الخمس الرسمية ٢٠٢٦/٢٠٢٧):",
                        style = MaterialTheme.typography.labelLarge
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SubjectType.entries) { subject ->
                            FilterChip(
                                selected = subject == selectedSubject,
                                onClick = { selectedSubject = subject },
                                label = { Text(subject.titleAr) },
                                modifier = Modifier.testTag("ingest_subject_chip_${subject.id}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = unitTitle,
                        onValueChange = { unitTitle = it },
                        label = { Text("الباب / الوحدة (مثال: Unit 1 أو المحور الأول)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ingest_unit_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = lessonTitle,
                        onValueChange = { lessonTitle = it },
                        label = { Text("عنوان الدرس (Lesson Title)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ingest_lesson_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = keyTerms,
                        onValueChange = { keyTerms = it },
                        label = { Text("المصطلحات المفتاحية ومعانيها (Key Terms)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ingest_key_terms_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = officialContent,
                        onValueChange = { officialContent = it },
                        label = { Text("النص الرسمي المستخرج من كتاب الـ PDF (المصدر الحصري للمدرس)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("ingest_content_input"),
                        maxLines = 8
                    )

                    Button(
                        onClick = {
                            onAddGroundedLesson(
                                selectedSubject,
                                unitTitle,
                                lessonTitle,
                                officialContent,
                                keyTerms,
                                sourceBookName
                            )
                            officialContent = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_grounded_lesson_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ الدرس في Firebase Firestore")
                    }
                }
            }
        }
    }
}
