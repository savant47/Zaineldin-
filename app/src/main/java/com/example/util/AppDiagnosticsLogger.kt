package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class LogSeverity(val labelAr: String, val badgeCode: String) {
    INFO("معلومة", "INFO"),
    WARN("تنبيه", "WARN"),
    ERROR("خطأ برمجي", "ERROR"),
    USER_REPORT("بلاغ يدوي", "USER_BUG")
}

data class DiagnosticLogEntry(
    val id: String = UUID.randomUUID().toString().take(8),
    val timestamp: String,
    val severity: LogSeverity,
    val tag: String,
    val errorCode: String,
    val message: String,
    val technicalDetails: String = ""
)

/**
 * مسجل الأخطاء والتقارير التشخيصية الشامل (AppDiagnosticsLogger)
 * يسجل كافة أخطاء الذكاء الاصطناعي، قاعدة البيانات، الصوت، والتحديثات مع كود الخطأ وسبب المشكلة،
 * ويتيح للمستخدم إضافة بلاغ خطأ يدوي ونسخ التقرير الكامل بضغطة واحدة لإرساله للمطور.
 */
object AppDiagnosticsLogger {

    private const val PREFS_NAME = "zein_tutor_diagnostics_prefs"
    private const val KEY_LAST_CRASH = "last_uncaught_crash"

    private val _logs = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
    val logs: StateFlow<List<DiagnosticLogEntry>> = _logs.asStateFlow()

    private var defaultHandlerInstalled = false

    private fun nowFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        return sdf.format(Date())
    }

    fun installCrashCatcher(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCrash = prefs.getString(KEY_LAST_CRASH, null)
        if (!savedCrash.isNullOrBlank()) {
            logError(
                tag = "CrashRecovery",
                errorCode = "UNCAUGHT_EXCEPTION",
                message = "تم رصد إغلاق غير متوقع في الجلسة السابقة",
                throwable = null,
                extraDetails = savedCrash
            )
            prefs.edit().remove(KEY_LAST_CRASH).apply()
        }

        if (!defaultHandlerInstalled) {
            defaultHandlerInstalled = true
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    val stack = stackTraceSummary(throwable)
                    prefs.edit()
                        .putString(
                            KEY_LAST_CRASH,
                            "Thread: ${thread.name}\nError: ${throwable.javaClass.simpleName}: ${throwable.message}\n$stack"
                        )
                        .commit()
                } catch (_: Exception) {
                }
                previousHandler?.uncaughtException(thread, throwable)
            }
        }

        if (_logs.value.isEmpty()) {
            logInfo(
                tag = "AppStartup",
                code = "BOOT_OK",
                message = "تم تشغيل تطبيق مدرس زين الخصوصي بنجاح (v${BuildConfig.VERSION_NAME} - code ${BuildConfig.VERSION_CODE})"
            )
        }
    }

    fun logInfo(tag: String, code: String, message: String, details: String = "") {
        Log.i(tag, "[$code] $message $details")
        appendEntry(
            DiagnosticLogEntry(
                timestamp = nowFormatted(),
                severity = LogSeverity.INFO,
                tag = tag,
                errorCode = code,
                message = message,
                technicalDetails = details
            )
        )
    }

    fun logWarning(tag: String, code: String, message: String, details: String = "") {
        Log.w(tag, "[$code] $message $details")
        appendEntry(
            DiagnosticLogEntry(
                timestamp = nowFormatted(),
                severity = LogSeverity.WARN,
                tag = tag,
                errorCode = code,
                message = message,
                technicalDetails = details
            )
        )
    }

    fun logError(
        tag: String,
        errorCode: String,
        message: String,
        throwable: Throwable? = null,
        extraDetails: String = ""
    ) {
        Log.e(tag, "[$errorCode] $message", throwable)
        val detailsBuilder = StringBuilder()
        if (extraDetails.isNotBlank()) {
            detailsBuilder.append(extraDetails).append("\n")
        }
        if (throwable != null) {
            detailsBuilder.append("Exception: ${throwable.javaClass.name}\n")
            detailsBuilder.append("Cause: ${throwable.localizedMessage ?: throwable.message ?: "Unknown"}\n")
            detailsBuilder.append(stackTraceSummary(throwable))
        }
        appendEntry(
            DiagnosticLogEntry(
                timestamp = nowFormatted(),
                severity = LogSeverity.ERROR,
                tag = tag,
                errorCode = errorCode,
                message = message,
                technicalDetails = detailsBuilder.toString().trim()
            )
        )
    }

    fun addManualUserBugReport(screenName: String, userDescription: String) {
        val cleanDesc = userDescription.trim()
        if (cleanDesc.isBlank()) return
        appendEntry(
            DiagnosticLogEntry(
                timestamp = nowFormatted(),
                severity = LogSeverity.USER_REPORT,
                tag = screenName,
                errorCode = "USER_REPORT_${ (_logs.value.size + 1) }",
                message = cleanDesc,
                technicalDetails = "بلاغ مضاف يدوياً بواسطة المستخدم من داخل التطبيق (Android ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT})"
            )
        )
    }

    fun clearLogs() {
        _logs.value = emptyList()
        logInfo("Diagnostics", "LOGS_CLEARED", "تم مسح سجل الأخطاء وبدء جلسة فحص جديدة.")
    }

    fun buildFullReportText(): String {
        val entries = _logs.value
        val errorCount = entries.count { it.severity == LogSeverity.ERROR || it.severity == LogSeverity.USER_REPORT }
        val sb = StringBuilder()
        sb.appendLine("=== تقرير تشخيص وأخطاء تطبيق مدرس زين الخصوصي ===")
        sb.appendLine("الإصدار: v${BuildConfig.VERSION_NAME} (Code: ${BuildConfig.VERSION_CODE}) | Debug: ${BuildConfig.DEBUG}")
        sb.appendLine("الجهاز: ${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("إجمالي السجلات: ${entries.size} | الأخطاء والبلاغات: $errorCount")
        sb.appendLine("--------------------------------------------------")
        if (entries.isEmpty()) {
            sb.appendLine("لا توجد أخطاء مسجلة حالياً.")
        } else {
            entries.forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. [${item.timestamp}] [${item.severity.badgeCode}] [${item.tag}] Code=${item.errorCode}")
                sb.appendLine("   الرسالة: ${item.message}")
                if (item.technicalDetails.isNotBlank()) {
                    sb.appendLine("   التفاصيل التقنية: ${item.technicalDetails.replace("\n", " | ")}")
                }
            }
        }
        sb.appendLine("==================================================")
        return sb.toString()
    }

    private fun appendEntry(entry: DiagnosticLogEntry) {
        val current = _logs.value
        _logs.value = (listOf(entry) + current).take(80)
    }

    private fun stackTraceSummary(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return sw.toString()
            .lineSequence()
            .take(8)
            .joinToString("\n")
    }
}
