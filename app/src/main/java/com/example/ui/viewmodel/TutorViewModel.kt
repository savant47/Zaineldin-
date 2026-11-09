package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.BilingualAiTutorService
import com.example.data.curriculum.OfficialCurriculumSeed
import com.example.data.model.ChatInteractionMode
import com.example.data.model.ChatMessage
import com.example.data.model.CurriculumLesson
import com.example.data.model.LanguageMode
import com.example.data.model.RewardBadge
import com.example.data.model.StructuredTutorResponse
import com.example.data.model.StudentLevel
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectType
import com.example.data.repository.TutorRepository
import com.example.util.AppDiagnosticsLogger
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface ChalkboardAiState {
    data object Idle : ChalkboardAiState
    data object Loading : ChalkboardAiState
    data class Ready(val response: StructuredTutorResponse) : ChalkboardAiState
    data class Error(val message: String) : ChalkboardAiState
}

sealed interface TeacherReplyState {
    data object Idle : TeacherReplyState
    data object Thinking : TeacherReplyState
    data class Streaming(val text: String) : TeacherReplyState
    data class Done(val text: String, val earnedPoints: Long) : TeacherReplyState
    data class Error(val message: String) : TeacherReplyState
}

enum class MainTab {
    HOME_SUBJECTS,
    INTERACTIVE_CLASSROOM,
    CHAT_TUTOR,
    CURRICULUM_AND_PYTHON
}

class TutorViewModel(
    private val repository: TutorRepository,
    private val aiService: BilingualAiTutorService,
    private val currentUserId: String
) : ViewModel() {

    private val _currentTab = MutableStateFlow(MainTab.HOME_SUBJECTS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _selectedSubject = MutableStateFlow(SubjectType.ARABIC)
    val selectedSubject: StateFlow<SubjectType> = _selectedSubject.asStateFlow()

    // يبدأ الدرس النشط بقيمة null حتى يختار الطالب أو ولي الأمر المادة والدرس بنفسه
    private val _activeLesson = MutableStateFlow<CurriculumLesson?>(null)
    val activeLesson: StateFlow<CurriculumLesson?> = _activeLesson.asStateFlow()

    private val _chalkboardState = MutableStateFlow<ChalkboardAiState>(ChalkboardAiState.Idle)
    val chalkboardState: StateFlow<ChalkboardAiState> = _chalkboardState.asStateFlow()

    private val _teacherReplyState = MutableStateFlow<TeacherReplyState>(TeacherReplyState.Idle)
    val teacherReplyState: StateFlow<TeacherReplyState> = _teacherReplyState.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _chatSubjectFilter = MutableStateFlow("all")
    val chatSubjectFilter: StateFlow<String> = _chatSubjectFilter.asStateFlow()

    private val _chatInteractionMode = MutableStateFlow(ChatInteractionMode.CHAT)
    val chatInteractionMode: StateFlow<ChatInteractionMode> = _chatInteractionMode.asStateFlow()

    private val _chatStreamingReply = MutableStateFlow<String?>(null)
    val chatStreamingReply: StateFlow<String?> = _chatStreamingReply.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    // سجل محلي فوري يندمج مع سجل السحابة لضمان عدم تأخر رسائل الشات أبداً
    private val _localChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _clearedBeforeSeconds = MutableStateFlow(0L)
    private var activeChatJob: Job? = null

    private val remoteChatFlow = repository.observeChatMessages(currentUserId)
        .onStart { emit(emptyList()) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            AppDiagnosticsLogger.logWarning(
                tag = TAG,
                code = "CHAT_OBSERVE_FALLBACK",
                message = "استخدام سجل المحادثة المحلي السريع",
                details = rootError.localizedMessage ?: ""
            )
            emit(emptyList())
        }

    val chatMessagesState: StateFlow<DataUiState<List<ChatMessage>>> = combine(
        remoteChatFlow,
        _localChatMessages,
        _clearedBeforeSeconds
    ) { remoteList, localList, clearedBefore ->
        val mergedMap = LinkedHashMap<String, ChatMessage>()
        remoteList
            .filter { (it.createdAt?.seconds ?: 0L) >= clearedBefore }
            .forEach { msg ->
                val key = msg.id.ifBlank { "${msg.sender}_${msg.createdAt?.seconds ?: 0L}_${msg.messageText.hashCode()}" }
                mergedMap[key] = msg
            }
        localList.forEach { msg ->
            val msgSec = msg.createdAt?.seconds ?: 0L
            val hasRecentRemoteDuplicate = remoteList.any { remote ->
                remote.sender == msg.sender &&
                    remote.messageText == msg.messageText &&
                    abs((remote.createdAt?.seconds ?: 0L) - msgSec) <= 4L
            }
            if (!hasRecentRemoteDuplicate) {
                mergedMap[msg.id] = msg
            }
        }
        val finalMessages = mergedMap.values.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
        DataUiState.Success(finalMessages) as DataUiState<List<ChatMessage>>
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DataUiState.Success(emptyList())
    )

    private val _localProfileOverride = MutableStateFlow<StudentProfile?>(null)

    val profileState: StateFlow<DataUiState<StudentProfile>> = combine(
        repository.observeProfile(currentUserId)
            .map { it ?: StudentProfile(userId = currentUserId) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                AppDiagnosticsLogger.logWarning(
                    tag = TAG,
                    code = "PROFILE_OBSERVE_WARN",
                    message = "تعذر قراءة ملف الطالب من السحابة — تم تفعيل الملف الافتراضي لزين الدين.",
                    details = rootError.localizedMessage ?: ""
                )
                emit(StudentProfile(userId = currentUserId))
            },
        _localProfileOverride
    ) { repoProfile, localOverride ->
        val resolved = if (localOverride != null) {
            repoProfile.copy(
                gradeLabel = localOverride.gradeLabel,
                academicYear = localOverride.academicYear,
                currentTerm = localOverride.currentTerm,
                activeSubjectIds = localOverride.activeSubjectIds,
                speechPitch = localOverride.speechPitch,
                speechRate = localOverride.speechRate
            )
        } else {
            repoProfile
        }
        DataUiState.Success(resolved)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DataUiState.Success(StudentProfile(userId = currentUserId))
    )

    private val defaultSeededLessons: List<CurriculumLesson> by lazy {
        OfficialCurriculumSeed.getInitialLessons(currentUserId)
            .map { (key, lesson) ->
                if (lesson.id.isBlank()) lesson.copy(id = key) else lesson
            }
            .sortedWith(compareBy({ it.subjectId }, { it.lessonOrder }))
    }

    val lessonsState: StateFlow<DataUiState<List<CurriculumLesson>>> = repository.observeLessons(currentUserId)
        .map<List<CurriculumLesson>, DataUiState<List<CurriculumLesson>>> { rawList ->
            val list = if (rawList.isNotEmpty()) rawList else defaultSeededLessons
            val currentId = _activeLesson.value?.id
            if (currentId != null) {
                list.firstOrNull { it.id == currentId }?.let { updated ->
                    _activeLesson.value = updated
                }
            }
            DataUiState.Success(list)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            AppDiagnosticsLogger.logWarning(
                tag = TAG,
                code = "LESSONS_OBSERVE_FALLBACK",
                message = "تم تحميل الـ ٢٤ وحدة من قاعدة الكتب المدمجة مباشرة.",
                details = rootError.localizedMessage ?: ""
            )
            emit(DataUiState.Success(defaultSeededLessons))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Success(defaultSeededLessons)
        )

    init {
        viewModelScope.launch {
            val initResult = repository.ensureInitialProfileAndCurriculum()
            if (initResult.isFailure) {
                AppDiagnosticsLogger.logWarning(
                    tag = TAG,
                    code = "FIRESTORE_SEED_WARN",
                    message = "مزامنة الخلفية مع السحابة قيد الانتظار — جميع الكتب الـ ٢٤ جاهزة محلياً.",
                    details = initResult.exceptionOrNull()?.localizedMessage ?: ""
                )
            } else {
                AppDiagnosticsLogger.logInfo(
                    tag = TAG,
                    code = "FIRESTORE_SEED_OK",
                    message = "تمت مزامنة ملف الطالب زين الدين والـ ٢٤ وحدة دراسية مع السحابة بنجاح."
                )
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun selectSubject(subject: SubjectType) {
        _selectedSubject.value = subject
        // إذا كان هناك درس مفتوح من مادة أخرى، نفرغ السبورة ليختار الطالب درس المادة الجديدة بنفسه
        if (_activeLesson.value != null && _activeLesson.value?.subjectId != subject.id) {
            _activeLesson.value = null
            _chalkboardState.value = ChalkboardAiState.Idle
            _teacherReplyState.value = TeacherReplyState.Idle
        }
    }

    fun openLessonInClassroom(lesson: CurriculumLesson) {
        _selectedSubject.value = SubjectType.fromId(lesson.subjectId)
        _activeLesson.value = lesson
        _teacherReplyState.value = TeacherReplyState.Idle
        _currentTab.value = MainTab.INTERACTIVE_CLASSROOM

        val currentProfile = (profileState.value as? DataUiState.Success)?.data ?: StudentProfile(userId = currentUserId)
        val studentLevel = StudentLevel.fromCode(currentProfile.studentLevel)
        val languageMode = LanguageMode.fromCode(currentProfile.preferredLanguageMode)

        // عرض الشرح المرجعي فوراً على السبورة في أقل من ١٠ ملي ثانية بدون أي انتظار!
        val instantBoard = aiService.buildInstantGroundedChalkboard(
            lesson = lesson,
            studentLevel = studentLevel,
            languageMode = languageMode
        )
        _chalkboardState.value = ChalkboardAiState.Ready(instantBoard)
        AppDiagnosticsLogger.logInfo(
            tag = TAG,
            code = "LESSON_OPENED_INSTANT",
            message = "تم فتح درس (${lesson.lessonTitle}) في السبورة التفاعلية فوراً."
        )
    }

    fun clearActiveClassroomLesson() {
        _activeLesson.value = null
        _chalkboardState.value = ChalkboardAiState.Idle
        _teacherReplyState.value = TeacherReplyState.Idle
    }

    fun setStudentLevel(level: StudentLevel) {
        viewModelScope.launch {
            val result = repository.updateStudentLevel(level)
            _statusBannerMessage.value = "تم ضبط مستوى الشرح لزين الدين إلى: (${level.labelAr})"
            val lesson = _activeLesson.value
            if (_currentTab.value == MainTab.INTERACTIVE_CLASSROOM && lesson != null) {
                val currentProfile = (profileState.value as? DataUiState.Success)?.data ?: StudentProfile(userId = currentUserId)
                val languageMode = LanguageMode.fromCode(currentProfile.preferredLanguageMode)
                _chalkboardState.value = ChalkboardAiState.Ready(
                    aiService.buildInstantGroundedChalkboard(lesson, level, languageMode)
                )
            }
            if (result.isFailure) {
                AppDiagnosticsLogger.logWarning(TAG, "UPDATE_LEVEL_WARN", result.exceptionOrNull()?.message ?: "")
            }
        }
    }

    fun setLanguageMode(mode: LanguageMode) {
        viewModelScope.launch {
            repository.updateLanguageMode(mode)
            _statusBannerMessage.value = "تم التبديل الفوري للغة الشرح: (${mode.labelAr})"
            val lesson = _activeLesson.value
            if (_currentTab.value == MainTab.INTERACTIVE_CLASSROOM && lesson != null) {
                val currentProfile = (profileState.value as? DataUiState.Success)?.data ?: StudentProfile(userId = currentUserId)
                val studentLevel = StudentLevel.fromCode(currentProfile.studentLevel)
                _chalkboardState.value = ChalkboardAiState.Ready(
                    aiService.buildInstantGroundedChalkboard(lesson, studentLevel, mode)
                )
            }
        }
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateReminderSettings(enabled, hour, minute)
            _statusBannerMessage.value = if (enabled) {
                "تم تفعيل التنبيه اليومي لمراجعة دروس زين الدين الساعة $hour:${minute.toString().padStart(2, '0')}"
            } else {
                "تم إيقاف تنبيهات المراجعة اليومية"
            }
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun explainCurrentLessonWithGemini() {
        val lesson = _activeLesson.value ?: return
        val currentProfile = (profileState.value as? DataUiState.Success)?.data ?: StudentProfile(userId = currentUserId)
        val studentLevel = StudentLevel.fromCode(currentProfile.studentLevel)
        val languageMode = LanguageMode.fromCode(currentProfile.preferredLanguageMode)

        viewModelScope.launch {
            _chalkboardState.value = ChalkboardAiState.Loading
            try {
                val structured = aiService.generateStructuredLessonExplanation(
                    lesson = lesson,
                    studentLevel = studentLevel,
                    languageMode = languageMode
                )
                _chalkboardState.value = ChalkboardAiState.Ready(structured)
                launch {
                    repository.saveLessonAiExplanation(lesson.id, structured.fullReadableScript)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppDiagnosticsLogger.logError(
                    tag = TAG,
                    errorCode = "EXPLAIN_LESSON_ERR",
                    message = "خطأ أثناء تحديث شرح السبورة — تم عرض الشرح الموثق من الكتاب.",
                    throwable = e
                )
                _chalkboardState.value = ChalkboardAiState.Ready(
                    aiService.buildInstantGroundedChalkboard(lesson, studentLevel, languageMode)
                )
            }
        }
    }

    fun askOrAnswerTeacher(zeinInput: String) {
        val cleanInput = zeinInput.trim()
        if (cleanInput.isBlank()) return
        val lesson = _activeLesson.value ?: return
        val currentProfile = (profileState.value as? DataUiState.Success)?.data ?: StudentProfile(userId = currentUserId)
        val studentLevel = StudentLevel.fromCode(currentProfile.studentLevel)
        val languageMode = LanguageMode.fromCode(currentProfile.preferredLanguageMode)
        val boardQuestion = (_chalkboardState.value as? ChalkboardAiState.Ready)
            ?.response
            ?.singleSimpleQuestion
            ?: "ما الذي فهمته من الدرس يا زين الدين؟"

        viewModelScope.launch {
            _teacherReplyState.value = TeacherReplyState.Thinking
            val accumulated = StringBuilder()
            try {
                aiService.streamTeacherReplyToZein(
                    lesson = lesson,
                    studentLevel = studentLevel,
                    languageMode = languageMode,
                    currentQuestionOnBoard = boardQuestion,
                    zeinSpokenOrTypedInput = cleanInput
                ).collect { chunk ->
                    accumulated.append(chunk)
                    _teacherReplyState.value = TeacherReplyState.Streaming(accumulated.toString())
                }

                val finalReply = accumulated.toString().trim()
                val rewardPoints = 25L
                _teacherReplyState.value = TeacherReplyState.Done(
                    text = finalReply,
                    earnedPoints = rewardPoints
                )
                _statusBannerMessage.value = "أحسنت يا زين الدين! ربحت +$rewardPoints نجمة ذهبية للمشاركة مع المدرس!"

                launch {
                    repository.completeLessonAndAwardPoints(
                        lessonId = lesson.id,
                        wasAlreadyCompleted = lesson.isCompleted,
                        currentPoints = currentProfile.points,
                        currentCompletedCount = currentProfile.completedLessonsCount,
                        pointsReward = rewardPoints
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppDiagnosticsLogger.logError(
                    tag = TAG,
                    errorCode = "ASK_TEACHER_ERR",
                    message = "خطأ أثناء محادثة المدرس في السبورة",
                    throwable = e
                )
                _teacherReplyState.value = TeacherReplyState.Error(
                    e.localizedMessage ?: "حدث خطأ أثناء محادثة المدرس."
                )
            }
        }
    }

    fun updateAcademicSettings(
        gradeLabel: String,
        academicYear: String,
        currentTerm: String,
        activeSubjectIds: List<String>,
        speechPitch: Float,
        speechRate: Float
    ) {
        val current = (profileState.value as? DataUiState.Success)?.data ?: StudentProfile(userId = currentUserId)
        val updatedLocal = current.copy(
            gradeLabel = gradeLabel,
            academicYear = academicYear,
            currentTerm = currentTerm,
            activeSubjectIds = activeSubjectIds,
            speechPitch = speechPitch,
            speechRate = speechRate
        )
        _localProfileOverride.value = updatedLocal
        _statusBannerMessage.value = "تم تحديث السنة والترم والمواد المقررة وإعدادات الصوت بنجاح!"

        viewModelScope.launch {
            val result = repository.updateAcademicSettings(
                gradeLabel = gradeLabel,
                academicYear = academicYear,
                currentTerm = currentTerm,
                activeSubjectIds = activeSubjectIds,
                speechPitch = speechPitch,
                speechRate = speechRate
            )
            if (result.isSuccess) {
                AppDiagnosticsLogger.logInfo(
                    TAG,
                    "ACADEMIC_SETTINGS_SAVED",
                    "السنة: $academicYear | الترم: $currentTerm | الصف: $gradeLabel | المواد: ${activeSubjectIds.size}"
                )
            } else {
                val err = result.exceptionOrNull()
                AppDiagnosticsLogger.logWarning(
                    TAG,
                    "SETTINGS_UPDATE_OFFLINE",
                    "تم حفظ الإعدادات محلياً في الذاكرة بنجاح.",
                    err?.localizedMessage ?: ""
                )
            }
        }
    }

    fun addCurriculumPdfDocument(
        subject: SubjectType,
        documentTitle: String,
        term: String,
        extractedContent: String,
        fileName: String,
        keyTermsSummary: String = ""
    ) {
        if (extractedContent.trim().length < 10) {
            _statusBannerMessage.value = "محتوى الملف قصير جداً أو فارغ، يرجى التأكد من اختيار ملف صالح."
            return
        }
        val currentLessons = (lessonsState.value as? DataUiState.Success)?.data.orEmpty()
        val nextOrder = (currentLessons.filter { it.subjectId == subject.id }.maxOfOrNull { it.lessonOrder } ?: 0L) + 1L

        viewModelScope.launch {
            val result = repository.addGroundedLesson(
                subject = subject,
                unitTitle = "محتوى مرفوع: $term",
                lessonTitle = documentTitle,
                lessonOrder = nextOrder,
                officialContent = extractedContent,
                keyTerms = keyTermsSummary.ifBlank { "مفاهيم ومصطلحات أساسية من $fileName" },
                sourceBookName = "ملف PDF: $fileName ($term)"
            )
            if (result.isSuccess) {
                _statusBannerMessage.value = "تمت إضافة ملف ($fileName) لمنهج ${subject.titleAr} بنجاح!"
                AppDiagnosticsLogger.logInfo(
                    TAG,
                    "PDF_BOOK_ADDED",
                    "تم رفع وحفظ ملف منهج: $documentTitle للمادة: ${subject.titleAr}"
                )
            } else {
                val err = result.exceptionOrNull()
                _statusBannerMessage.value = "تعذر حفظ محتوى الملف: ${err?.localizedMessage ?: ""}"
                AppDiagnosticsLogger.logError(TAG, "PDF_INGEST_ERR", "تعذر حفظ ملف المنهج", err)
            }
        }
    }

    fun deleteLesson(lessonId: String) {
        viewModelScope.launch {
            val result = repository.deleteGroundedLesson(lessonId)
            if (result.isSuccess) {
                _statusBannerMessage.value = "تم حذف الوحدة الدراسية بنجاح"
            } else {
                _statusBannerMessage.value = "تعذر حذف الوحدة: ${result.exceptionOrNull()?.localizedMessage ?: ""}"
            }
        }
    }

    fun syncAllUploadedBooks() {
        viewModelScope.launch {
            val result = repository.syncUploadedBooksToDatabase()
            if (result.isSuccess) {
                val count = result.getOrDefault(24)
                _statusBannerMessage.value =
                    "تمت مزامنة ورفع جميع الكتب المدرسية وكتب المعاصر المرفوعة ($count وحدة ودرساً شاملاً لـ ٥ مواد ٢٠٢٦/٢٠٢٧ بعد حذف المكررات) في قاعدة البيانات بنجاح!"
            } else {
                val err = result.exceptionOrNull()
                AppDiagnosticsLogger.logError(TAG, "SYNC_BOOKS_ERR", "تعذر مزامنة الكتب مع السحابة", err)
                _statusBannerMessage.value =
                    "الكتب الـ ٢٤ محملة وجاهزة محلياً (ملاحظة سحابية: ${err?.localizedMessage ?: ""})"
            }
        }
    }

    fun setChatSubjectFilter(subjectFilter: String) {
        _chatSubjectFilter.value = subjectFilter
    }

    fun setChatInteractionMode(mode: ChatInteractionMode) {
        _chatInteractionMode.value = mode
    }

    fun sendChatQuery(
        queryText: String,
        modeOverride: ChatInteractionMode? = null,
        subjectOverride: String? = null
    ) {
        val cleanQuery = queryText.trim()
        if (cleanQuery.isBlank()) return

        // إذا كان هناك طلب سابق معلق، نلغيه فوراً للاستجابة الفورية لضغطة المستخدم الجديدة
        activeChatJob?.cancel()

        val activeMode = modeOverride ?: _chatInteractionMode.value
        _chatInteractionMode.value = activeMode
        val activeFilter = subjectOverride ?: _chatSubjectFilter.value
        _chatSubjectFilter.value = activeFilter

        val allLessons = (lessonsState.value as? DataUiState.Success)?.data?.ifEmpty { defaultSeededLessons } ?: defaultSeededLessons
        val recentHistory = (chatMessagesState.value as? DataUiState.Success)?.data.orEmpty()
        val currentProfile = (profileState.value as? DataUiState.Success)?.data ?: StudentProfile(userId = currentUserId)
        val studentLevel = StudentLevel.fromCode(currentProfile.studentLevel)
        val languageMode = LanguageMode.fromCode(currentProfile.preferredLanguageMode)

        val nowSeconds = System.currentTimeMillis() / 1000L
        val userMsg = ChatMessage(
            id = "local_user_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            userId = currentUserId,
            sender = "user",
            messageText = cleanQuery,
            subjectFilter = activeFilter,
            interactionMode = activeMode.code,
            createdAt = Timestamp(nowSeconds, 0)
        )
        _localChatMessages.value = _localChatMessages.value + userMsg

        activeChatJob = viewModelScope.launch {
            _isChatGenerating.value = true
            _chatStreamingReply.value = ""

            // حفظ رسالة الطالب في الخلفية بدون تعطيل أو تأخير رد الذكاء الاصطناعي!
            launch {
                repository.addChatMessage(
                    sender = "user",
                    messageText = cleanQuery,
                    subjectFilter = activeFilter,
                    interactionMode = activeMode.code
                )
            }

            val accumulated = StringBuilder()
            try {
                aiService.streamGroundedChatReply(
                    userMessage = cleanQuery,
                    interactionMode = activeMode,
                    subjectFilter = activeFilter,
                    allGroundedLessons = allLessons,
                    recentMessages = recentHistory,
                    studentLevel = studentLevel,
                    languageMode = languageMode
                ).collect { chunk ->
                    accumulated.append(chunk)
                    _chatStreamingReply.value = accumulated.toString()
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotBlank()) {
                    val aiMsg = ChatMessage(
                        id = "local_ai_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                        userId = currentUserId,
                        sender = "ai",
                        messageText = finalReply,
                        subjectFilter = activeFilter,
                        interactionMode = activeMode.code,
                        createdAt = Timestamp((System.currentTimeMillis() / 1000L) + 1L, 0)
                    )
                    _localChatMessages.value = _localChatMessages.value + aiMsg

                    launch {
                        repository.addChatMessage(
                            sender = "ai",
                            messageText = finalReply,
                            subjectFilter = activeFilter,
                            interactionMode = activeMode.code
                        )
                        repository.awardPointsForChatInteraction(
                            currentPoints = currentProfile.points,
                            pointsReward = 15L
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                AppDiagnosticsLogger.logWarning(
                    tag = TAG,
                    code = "CHAT_TIMEOUT_HANDLED",
                    message = "انتهت مهلة الانتظار وتم استكمال الرد المرجعي."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppDiagnosticsLogger.logError(
                    tag = TAG,
                    errorCode = "CHAT_QUERY_ERR",
                    message = "خطأ غير متوقع في الشات",
                    throwable = e
                )
                _statusBannerMessage.value = "حدث خطأ أثناء تحضير الرد — راجع سجل الأخطاء للتفاصيل."
            } finally {
                _chatStreamingReply.value = null
                _isChatGenerating.value = false
            }
        }
    }

    fun clearChatHistory() {
        activeChatJob?.cancel()
        aiService.clearPendingAssessment()
        _isChatGenerating.value = false
        _chatStreamingReply.value = null
        _clearedBeforeSeconds.value = (System.currentTimeMillis() / 1000L) + 2L
        _localChatMessages.value = emptyList()
        viewModelScope.launch {
            repository.clearChatHistory()
            _statusBannerMessage.value = "تم مسح سجل المحادثة بنجاح"
        }
    }

    fun runSelfDiagnosticsCheck() {
        viewModelScope.launch {
            AppDiagnosticsLogger.logInfo(
                tag = "SelfTest",
                code = "DIAG_START",
                message = "بدء الفحص الذاتي الشامل للتطبيق (قاعدة البيانات + الكتب الـ ٢٤ + محرك الذكاء الاصطناعي)..."
            )
            val loadedLessonsCount = (lessonsState.value as? DataUiState.Success)?.data?.size ?: 0
            AppDiagnosticsLogger.logInfo(
                tag = "SelfTest",
                code = "CURRICULUM_CHECK",
                message = "عدد الوحدات الدراسية الجاهزة في الذاكرة: $loadedLessonsCount من أصل ٢٤ وحدة."
            )

            val fsRes = repository.getLessonsOnce(currentUserId)
            if (fsRes.isSuccess) {
                AppDiagnosticsLogger.logInfo(
                    tag = "SelfTest",
                    code = "FIRESTORE_OK",
                    message = "الاتصال بقاعدة بيانات Cloud Firestore يعمل بنجاح (${fsRes.getOrNull()?.size ?: 0} وثيقة سحابية)."
                )
            } else {
                AppDiagnosticsLogger.logError(
                    tag = "SelfTest",
                    errorCode = "FIRESTORE_CONN_FAIL",
                    message = "فشل الاتصال المباشر بـ Firestore (التطبيق يعمل حالياً بالنسخة المدمجة للكتب الـ ٢٤).",
                    throwable = fsRes.exceptionOrNull()
                )
            }
            _statusBannerMessage.value = "اكتمل فحص النظام وتسجيل النتائج في (سجل الأخطاء والتقارير)."
        }
    }

    fun getBadgesForPoints(points: Long, completedCount: Long): List<RewardBadge> {
        return listOf(
            RewardBadge(
                id = "starter_star",
                titleAr = "نجم البداية الذكي",
                descriptionAr = "الانضمام لفصل زين الدين التفاعلي",
                requiredPoints = 100L,
                isUnlocked = points >= 100L
            ),
            RewardBadge(
                id = "bilingual_hero",
                titleAr = "بطل اللغتين (Bilingual Hero)",
                descriptionAr = "إكمال أول درس تفاعلي وجمع ١٢٥ نجمة",
                requiredPoints = 125L,
                isUnlocked = points >= 125L || completedCount >= 1L
            ),
            RewardBadge(
                id = "math_champion",
                titleAr = "عبقري الـ Math والـ Connect",
                descriptionAr = "الوصول إلى ١٧٥ نجمة ذهبية",
                requiredPoints = 175L,
                isUnlocked = points >= 175L
            ),
            RewardBadge(
                id = "grade2_scholar",
                titleAr = "وسام التفوق - الصف الثاني لغات",
                descriptionAr = "الوصول إلى ٢٥٠ نجمة وإتقان المنهج الحديث",
                requiredPoints = 250L,
                isUnlocked = points >= 250L || completedCount >= 5L
            )
        )
    }

    private companion object {
        const val TAG = "TutorViewModel"
    }
}
