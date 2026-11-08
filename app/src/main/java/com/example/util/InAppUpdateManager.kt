package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class UpdateAvailable(
        val latestVersionName: String,
        val latestVersionCode: Int,
        val releaseTitle: String,
        val changelog: String,
        val apkDownloadUrl: String,
        val releasePageUrl: String
    ) : UpdateCheckState
    data class UpToDate(
        val currentVersionName: String,
        val currentVersionCode: Int,
        val message: String
    ) : UpdateCheckState
    data class Error(val errorCode: String, val message: String) : UpdateCheckState
}

/**
 * مدير التحديث الداخلي الهوائي عبر GitHub (In-App GitHub OTA Updater)
 * يفحص مستودع GitHub بعد المزامنة (عبر GitHub Releases API أو ملف version.json في الفرع الرئيسي)
 * وينبه المستخدم فوراً بوجود تحديث جديد مع إمكانية تحميل وتثبيت ملف الـ APK مباشرة.
 */
class InAppUpdateManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _githubRepoSlug = MutableStateFlow(
        prefs.getString(KEY_GITHUB_REPO, DEFAULT_REPO_SLUG).orEmpty().ifBlank { DEFAULT_REPO_SLUG }
    )
    val githubRepoSlug: StateFlow<String> = _githubRepoSlug.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    fun saveGithubRepository(rawInput: String) {
        val cleaned = normalizeRepoSlug(rawInput)
        prefs.edit().putString(KEY_GITHUB_REPO, cleaned).apply()
        _githubRepoSlug.value = cleaned
        AppDiagnosticsLogger.logInfo(
            tag = "InAppUpdater",
            code = "REPO_CONFIGURED",
            message = "تم حفظ مستودع GitHub للتحديث التلقائي: $cleaned"
        )
    }

    suspend fun checkForUpdates(customRepoInput: String? = null) {
        val slug = normalizeRepoSlug(customRepoInput ?: _githubRepoSlug.value)
        if (slug.isBlank() || !slug.contains("/")) {
            _updateState.value = UpdateCheckState.Error(
                errorCode = "INVALID_REPO_SLUG",
                message = "يرجى إدخال اسم مستودع GitHub بالصيغة الصحيحة: username/repository"
            )
            AppDiagnosticsLogger.logWarning(
                tag = "InAppUpdater",
                code = "INVALID_REPO_SLUG",
                message = "صيغة مستودع GitHub غير مكتملة: '$slug'"
            )
            return
        }

        saveGithubRepository(slug)
        _updateState.value = UpdateCheckState.Checking

        withContext(Dispatchers.IO) {
            try {
                // 1. فحص أحدث إصدار مبني عبر GitHub Actions Releases API
                val releaseApiUrl = "https://api.github.com/repos/$slug/releases/latest"
                val releaseResult = fetchJsonFromUrl(releaseApiUrl)

                if (releaseResult != null) {
                    val tagName = releaseResult.optString("tag_name", "v1.1")
                    val releaseName = releaseResult.optString("name", "تحديث جديد من GitHub ($tagName)")
                    val body = releaseResult.optString("body", "تم بناء نسخة جديدة تلقائياً بعد المزامنة مع GitHub.")
                    val htmlUrl = releaseResult.optString("html_url", "https://github.com/$slug/releases")

                    var apkUrl = ""
                    val assets = releaseResult.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            val downloadUrl = asset.optString("browser_download_url", "")
                            if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                                apkUrl = downloadUrl
                                break
                            }
                        }
                    }

                    val remoteCode = extractVersionCodeFromTag(tagName, releaseResult.optInt("id", 2))
                    val lastDismissedOrInstalledTag = prefs.getString(KEY_INSTALLED_RELEASE_TAG, "") ?: ""

                    if (remoteCode > BuildConfig.VERSION_CODE || (tagName.isNotBlank() && tagName != lastDismissedOrInstalledTag)) {
                        val finalDownloadUrl = apkUrl.ifBlank { htmlUrl }
                        _updateState.value = UpdateCheckState.UpdateAvailable(
                            latestVersionName = tagName,
                            latestVersionCode = remoteCode,
                            releaseTitle = releaseName,
                            changelog = body.take(500),
                            apkDownloadUrl = finalDownloadUrl,
                            releasePageUrl = htmlUrl
                        )
                        AppDiagnosticsLogger.logInfo(
                            tag = "InAppUpdater",
                            code = "UPDATE_FOUND",
                            message = "تم العثور على تحديث جديد على GitHub: $tagName",
                            details = "APK URL: $finalDownloadUrl"
                        )
                        return@withContext
                    }
                }

                // 2. فحص ملف version.json من الفرع الرئيسي (main أو master) مباشرة بعد المزامنة
                val rawVersionJson = fetchJsonFromUrl("https://raw.githubusercontent.com/$slug/main/version.json")
                    ?: fetchJsonFromUrl("https://raw.githubusercontent.com/$slug/master/version.json")

                if (rawVersionJson != null) {
                    val remoteVersionCode = rawVersionJson.optInt("versionCode", BuildConfig.VERSION_CODE)
                    val remoteVersionName = rawVersionJson.optString("versionName", BuildConfig.VERSION_NAME)
                    val changelogAr = rawVersionJson.optString(
                        "changelogAr",
                        "تحديث جديد متاح بعد المزامنة مع GitHub."
                    )
                    val apkDirectUrl = rawVersionJson.optString(
                        "apkDownloadUrl",
                        "https://github.com/$slug/releases/latest/download/zein-tutor-debug.apk"
                    )
                    val releaseUrl = "https://github.com/$slug/releases"

                    if (remoteVersionCode > BuildConfig.VERSION_CODE || remoteVersionName != BuildConfig.VERSION_NAME) {
                        _updateState.value = UpdateCheckState.UpdateAvailable(
                            latestVersionName = remoteVersionName,
                            latestVersionCode = remoteVersionCode,
                            releaseTitle = "تحديث متاح (الإصدار $remoteVersionName)",
                            changelog = changelogAr,
                            apkDownloadUrl = apkDirectUrl,
                            releasePageUrl = releaseUrl
                        )
                        AppDiagnosticsLogger.logInfo(
                            tag = "InAppUpdater",
                            code = "VERSION_JSON_UPDATE",
                            message = "تحديث جديد مكتشف عبر version.json: v$remoteVersionName ($remoteVersionCode)"
                        )
                    } else {
                        _updateState.value = UpdateCheckState.UpToDate(
                            currentVersionName = BuildConfig.VERSION_NAME,
                            currentVersionCode = BuildConfig.VERSION_CODE,
                            message = "أنت تستخدم أحدث إصدار متزامن مع GitHub (v${BuildConfig.VERSION_NAME} - الكود ${BuildConfig.VERSION_CODE})."
                        )
                        AppDiagnosticsLogger.logInfo(
                            tag = "InAppUpdater",
                            code = "UP_TO_DATE",
                            message = "التطبيق محدث لآخر إصدار على المستودع $slug"
                        )
                    }
                } else {
                    _updateState.value = UpdateCheckState.Error(
                        errorCode = "REPO_OR_RELEASE_NOT_FOUND",
                        message = "لم يتم العثور على إصدار منشور بعد في المستودع ($slug). بمجرد مزامنة الكود مع GitHub سيقوم الـ Workflow ببناء ملف الـ APK تلقائياً."
                    )
                    AppDiagnosticsLogger.logWarning(
                        tag = "InAppUpdater",
                        code = "REPO_NOT_FOUND_404",
                        message = "المستودع $slug غير عام أو لم يتم تشغيل GitHub Actions عليه بعد."
                    )
                }
            } catch (e: Exception) {
                _updateState.value = UpdateCheckState.Error(
                    errorCode = "UPDATE_NET_ERR",
                    message = "تعذر الاتصال بـ GitHub لفحص التحديثات: ${e.localizedMessage ?: "خطأ في الشبكة"}"
                )
                AppDiagnosticsLogger.logError(
                    tag = "InAppUpdater",
                    errorCode = "UPDATE_CHECK_EXCEPTION",
                    message = "فشل فحص التحديث الداخلي من GitHub",
                    throwable = e
                )
            }
        }
    }

    fun downloadAndInstallUpdate(
        apkUrl: String,
        versionLabel: String
    ): String {
        return try {
            if (apkUrl.endsWith(".apk", ignoreCase = true)) {
                val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val request = DownloadManager.Request(Uri.parse(apkUrl))
                    .setTitle("تحديث مدرس زين الخصوصي ($versionLabel)")
                    .setDescription("جاري تحميل النسخة الجديدة من GitHub...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "zein-tutor-$versionLabel.apk"
                    )
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                dm.enqueue(request)
            }

            // فتح رابط التحميل المباشر أيضاً في المتصفح لضمان التثبيت الفوري على جميع إصدارات أندرويد
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(browserIntent)

            prefs.edit().putString(KEY_INSTALLED_RELEASE_TAG, versionLabel).apply()
            AppDiagnosticsLogger.logInfo(
                tag = "InAppUpdater",
                code = "APK_DOWNLOAD_STARTED",
                message = "بدأ تحميل وتثبيت التحديث $versionLabel",
                details = apkUrl
            )
            "بدأ تحميل ملف التحديث ($versionLabel) في الإشعارات والمتصفح — اضغط على الملف بعد التحميل للتثبيت."
        } catch (e: Exception) {
            AppDiagnosticsLogger.logError(
                tag = "InAppUpdater",
                errorCode = "APK_DOWNLOAD_ERR",
                message = "تعذر بدء تحميل ملف الـ APK",
                throwable = e
            )
            "تعذر التحميل التلقائي: ${e.localizedMessage ?: ""}"
        }
    }

    private fun fetchJsonFromUrl(urlStr: String): JSONObject? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "ZeinPrivateTutorAndroidApp/${BuildConfig.VERSION_NAME}")
        }
        return try {
            val status = conn.responseCode
            if (status in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun extractVersionCodeFromTag(tag: String, fallback: Int): Int {
        val digits = Regex("(\\d+)$").find(tag)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return digits ?: fallback
    }

    private fun normalizeRepoSlug(input: String): String {
        return input.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removeSuffix(".git")
            .trim('/')
    }

    companion object {
        private const val PREFS_NAME = "zein_ota_updater_prefs"
        private const val KEY_GITHUB_REPO = "github_repo_slug"
        private const val KEY_INSTALLED_RELEASE_TAG = "installed_release_tag"
        const val DEFAULT_REPO_SLUG = "Savant47/zein-private-tutor"
    }
}
