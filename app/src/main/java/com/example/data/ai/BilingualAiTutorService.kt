package com.example.data.ai

import com.example.data.model.ChalkboardStep
import com.example.data.model.ChatInteractionMode
import com.example.data.model.ChatMessage
import com.example.data.model.CurriculumLesson
import com.example.data.model.LanguageMode
import com.example.data.model.StructuredTutorResponse
import com.example.data.model.StudentLevel
import com.example.data.model.SubjectType
import com.example.util.AppDiagnosticsLogger
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

data class AssessmentQuestionItem(
    val number: Int,
    val questionTypeLabel: String,
    val questionPrompt: String,
    val options: List<String> = emptyList(),
    val correctOptionLetterAr: String = "",
    val correctOptionLetterEn: String = "",
    val correctAnswerText: String,
    val acceptedKeywords: List<String>,
    val explanationAr: String,
    val points: Int
)

data class PendingAssessment(
    val id: String,
    val mode: ChatInteractionMode,
    val subjectId: String,
    val subjectNameAr: String,
    val topicTitle: String,
    val totalPoints: Int,
    val questions: List<AssessmentQuestionItem>
)

/**
 * محرك الذكاء الاصطناعي ومنطق التدريس الثنائي (Bilingual AI Logic)
 * - يعرض الأسئلة فقط عند طلب كويز أو امتحان (بدون كشف الإجابات أو النتيجة في نفس الرسالة).
 * - يحفظ الاختبار النشط في الذاكرة، وبمجرد إرسال الطالب لإجاباته يقوم بتصحيحها وإعطاء الدرجة والحل الصحيح بالأسفل.
 * - لا يذكر أسماء الكتب الخارجية أو المصادر (مثل المعاصر أو الأضواء) داخل الأسئلة أو الردود.
 */
class BilingualAiTutorService {

    private val variationCounter = AtomicInteger(0)

    @Volatile
    private var pendingAssessment: PendingAssessment? = null

    fun hasPendingAssessment(): Boolean = pendingAssessment != null

    fun clearPendingAssessment() {
        pendingAssessment = null
    }

    private val chalkboardSchema = Schema.obj(
        properties = mapOf(
            "chalkboardHeader" to Schema.string(
                description = "عنوان جذاب للدرس يظهر أعلى السبورة الخضراء"
            ),
            "warmGreetingToZein" to Schema.string(
                description = "تحية مشجعة وودودة بالاسم للطفل زين الدين (8 سنوات)"
            ),
            "steps" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "stepTitle" to Schema.string(description = "عنوان الفقرة أو الفكرة على السبورة"),
                        "englishLine" to Schema.string(
                            description = "الشرح بالإنجليزية لمادتي Math و English (أو جملة الدرس الرئيسية بالعربية لمادتي العربي والدين)"
                        ),
                        "arabicExplanation" to Schema.string(
                            description = "الشرح والتوضيح الفوري باللغة العربية المبسطة لضمان فهم زين الدين للمصطلحات والمعنى"
                        )
                    )
                ),
                description = "خطوات الشرح المنظمة على السبورة (من 3 إلى 4 خطوات واضحة)"
            ),
            "keyTermsSummary" to Schema.array(
                items = Schema.string(),
                description = "أهم المصطلحات والكلمات المفتاحية من النص المرجعي مع معناها"
            ),
            "singleSimpleQuestion" to Schema.string(
                description = "سؤال واحد بسيط ومشجع في نهاية الشرح لقياس فهم زين الدين"
            ),
            "expectedAnswerKeywords" to Schema.array(
                items = Schema.string(),
                description = "كلمات مفتاحية متوقعة في إجابة زين الدين الصحيحة"
            ),
            "encouragementMessage" to Schema.string(
                description = "رسالة تحفيزية قصيرة لزين الدين"
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = chalkboardSchema
            }
        )
    }

    private val conversationalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    fun cleanTopicTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("(?i)El-Moasser\\s*(Math|English)?\\s*2027:?"), "")
            .replace(Regex("ملحق تقييمات المعاصر\\s*(ماث|إنجليزي)?\\s*٢٠٢٧:?"), "")
            .replace(Regex("مراجعة وأساليب الأضواء:?"), "")
            .replace(Regex("كراسة المعاصر التفاعلية:?"), "")
            .replace(Regex("تحضير مستر صبحي بردة:?"), "")
            .replace("المعاصر", "")
            .replace("الأضواء", "")
            .trim()
            .removePrefix("-")
            .removePrefix(":")
            .trim()
            .ifBlank { rawTitle }
    }

    /**
     * يبني شرحاً فورياً منظماً على السبورة من النص الرسمي للدرس في أقل من ١٠ ملي ثانية
     */
    fun buildInstantGroundedChalkboard(
        lesson: CurriculumLesson,
        studentLevel: StudentLevel,
        languageMode: LanguageMode
    ): StructuredTutorResponse {
        val subjectType = SubjectType.fromId(lesson.subjectId)
        val cleanTitle = cleanTopicTitle(lesson.lessonTitle)
        val rawSections = lesson.officialContent
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val chunkSize = (rawSections.size / 3).coerceAtLeast(2)
        val stepGroups = rawSections.chunked(chunkSize).take(4)

        val steps = stepGroups.mapIndexed { idx, lines ->
            val firstLine = lines.firstOrNull().orEmpty().removePrefix("-").removePrefix("*").trim()
            val bodyText = lines.joinToString("\n")
            if (subjectType.isBilingualSubject) {
                ChalkboardStep(
                    stepTitle = "الفكرة (${idx + 1}): ${firstLine.take(55)}",
                    englishLine = bodyText,
                    arabicExplanation = when (subjectType) {
                        SubjectType.MATH ->
                            "شرح المدرس لزين: ركّز يا بطل في الأرقام والمصطلحات الإنجليزية هنا (${lesson.keyTerms.take(120)})، وطبق الخطوات بالترتيب."
                        else ->
                            "شرح المدرس لزين: نقرأ الجمل والكلمات الإنجليزية بوضوح، ومعناها الأساسي يدور حول (${lesson.keyTerms.take(120)})."
                    }
                )
            } else {
                ChalkboardStep(
                    stepTitle = "الفقرة (${idx + 1}): ${firstLine.take(55)}",
                    englishLine = "",
                    arabicExplanation = bodyText
                )
            }
        }.ifEmpty {
            listOf(
                ChalkboardStep(
                    stepTitle = cleanTitle,
                    englishLine = if (subjectType.isBilingualSubject) lesson.officialContent else "",
                    arabicExplanation = lesson.officialContent
                )
            )
        }

        val termsList = lesson.keyTerms
            .split(",", "،", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(8)

        val question = when (subjectType) {
            SubjectType.MATH ->
                "سؤال التحدي لزين الدين: من درس ($cleanTitle)، اذكر مثالاً أو احسب مسألة واحدة مما شرحناه على السبورة!"
            SubjectType.CONNECT, SubjectType.CONNECT_PLUS ->
                "سؤال المدرس لزين الدين: هات كلمة أو جملة إنجليزية واحدة تعلمتها اليوم من درس ($cleanTitle) وقل معناها بالعربية!"
            SubjectType.RELIGION ->
                "سؤال المدرس لزين الدين: ما أهم خلق أو درس جميل تعلمته اليوم من ($cleanTitle)؟"
            SubjectType.ARABIC ->
                "سؤال المدرس لزين الدين: استخرج كلمة أو قاعدة جميلة تعلمتها اليوم من درس ($cleanTitle)!"
        }

        val greeting = "أهلاً بك يا بطلنا زين الدين! جاهز لشرح درس ($cleanTitle) بمستوى (${studentLevel.labelAr}) ووضع (${languageMode.badgeAr})."

        val readableBuilder = StringBuilder()
        readableBuilder.append(greeting).append("\n\n")
        steps.forEachIndexed { idx, step ->
            readableBuilder.append("${idx + 1}. ${step.stepTitle}:\n")
            if (step.englishLine.isNotBlank()) {
                readableBuilder.append(step.englishLine).append("\n")
            }
            if (step.arabicExplanation.isNotBlank()) {
                readableBuilder.append("التوضيح بالعربية: ").append(step.arabicExplanation).append("\n\n")
            }
        }
        readableBuilder.append("سؤال الدرس لزين الدين: ").append(question)

        return StructuredTutorResponse(
            chalkboardHeader = cleanTitle,
            warmGreetingToZein = greeting,
            steps = steps,
            keyTermsSummary = termsList,
            singleSimpleQuestion = question,
            expectedAnswerKeywords = termsList.take(4),
            encouragementMessage = "أنت عبقري يا زين الدين! أجب في المربع بالأسفل أو بالمايكروفون لتربح +٢٥ نجمة ذهبية.",
            fullReadableScript = readableBuilder.toString().trim()
        )
    }

    fun buildCorePedagogicalPrompt(
        lesson: CurriculumLesson,
        studentLevel: StudentLevel,
        languageMode: LanguageMode
    ): String {
        val subjectType = SubjectType.fromId(lesson.subjectId)
        val cleanTitle = cleanTopicTitle(lesson.lessonTitle)
        val levelInstruction = when (studentLevel) {
            StudentLevel.BEGINNER ->
                "مستوى الطالب الحالي: (مبتدئ) - استخدم جملاً قصيرة جداً، أمثلة حسية قريبة من طفل 8 سنوات، وتدرجاً هادئاً."
            StudentLevel.ADVANCED ->
                "مستوى الطالب الحالي: (متقدم) - قدم شرحاً ثرياً مع أمثلة تطبيقية ذكية وسؤال ختامي ينمي التفكير."
        }

        val languageOverrideInstruction = when {
            !subjectType.isBilingualSubject ->
                "هذه المادة هي (${lesson.subjectNameAr}): التزم بشرحها باللغة العربية الفصحى المبسطة المحببة للأطفال."
            languageMode == LanguageMode.ARABIC ->
                "طلب الطالب التبديل الفوري إلى الوضع العربي المبسّط: ركّز على الشرح بالعربية الفصحى المبسطة مع ذكر المصطلح الإنجليزي الأساسي فقط."
            languageMode == LanguageMode.ENGLISH ->
                "طلب الطالب التبديل الفوري إلى الوضع الإنجليزي المبسّط: اشرح بجمل إنجليزية بسيطة جداً ومناسبة لسن 8 سنوات، مع ترجمة عربية موجزة للمصطلحات الجديدة."
            else ->
                "وضع التدريس الثنائي للغات (Bilingual Mode): اشرح الفكرة باللغة الإنجليزية أولاً في حقل englishLine، ثم قم بشرح المعنى وتوضيحه باللغة العربية فوراً في حقل arabicExplanation لضمان فهم زين الدين للاصطلاحات."
        }

        return """
            أنت مدرس خصوصي ودود لطفل في الصف الثاني الابتدائي اسمه 'زين الدين' (8 سنوات).
            - لا تذكر أسماء أي كتب خارجية (مثل المعاصر أو الأضواء) في كلامك.
            - $levelInstruction
            - $languageOverrideInstruction

            بيانات الدرس:
            - المادة: ${lesson.subjectNameAr}
            - عنوان الدرس: $cleanTitle
            - المصطلحات الأساسية: ${lesson.keyTerms}
            - المحتوى العلمي للدرس:
            \"\"\"
            ${lesson.officialContent.take(2500)}
            \"\"\"
        """.trimIndent()
    }

    suspend fun generateStructuredLessonExplanation(
        lesson: CurriculumLesson,
        studentLevel: StudentLevel,
        languageMode: LanguageMode
    ): StructuredTutorResponse {
        val prompt = buildCorePedagogicalPrompt(lesson, studentLevel, languageMode)
        return try {
            val response = withTimeout(4_500L) {
                structuredModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("EMPTY_AI_RESPONSE")

            val json = JSONObject(jsonText)
            val header = cleanTopicTitle(json.optString("chalkboardHeader", lesson.lessonTitle))
            val greeting = json.optString(
                "warmGreetingToZein",
                "أهلاً بك يا بطلنا زين الدين! هيا نتعلم معاً درس اليوم."
            )

            val stepsArray = json.optJSONArray("steps")
            val steps = mutableListOf<ChalkboardStep>()
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    val item = stepsArray.getJSONObject(i)
                    steps.add(
                        ChalkboardStep(
                            stepTitle = item.optString("stepTitle", "الفكرة ${i + 1}"),
                            englishLine = item.optString("englishLine", ""),
                            arabicExplanation = item.optString("arabicExplanation", "")
                        )
                    )
                }
            }

            val termsArray = json.optJSONArray("keyTermsSummary")
            val terms = mutableListOf<String>()
            if (termsArray != null) {
                for (i in 0 until termsArray.length()) {
                    terms.add(termsArray.getString(i))
                }
            }

            val question = json.optString("singleSimpleQuestion", "ما أهم فكرة تعلمتها اليوم يا زين الدين؟")
            val keywordsArray = json.optJSONArray("expectedAnswerKeywords")
            val keywords = mutableListOf<String>()
            if (keywordsArray != null) {
                for (i in 0 until keywordsArray.length()) {
                    keywords.add(keywordsArray.getString(i))
                }
            }

            val encouragement = json.optString(
                "encouragementMessage",
                "أنت طالب عبقري يا زين الدين! أجب عن السؤال لتحصل على النجوم الذهبية."
            )

            val readableBuilder = StringBuilder()
            readableBuilder.append(greeting).append("\n\n")
            steps.forEachIndexed { idx, step ->
                readableBuilder.append("${idx + 1}. ${step.stepTitle}:\n")
                if (step.englishLine.isNotBlank()) {
                    readableBuilder.append(step.englishLine).append("\n")
                }
                if (step.arabicExplanation.isNotBlank()) {
                    readableBuilder.append("التوضيح بالعربية: ").append(step.arabicExplanation).append("\n\n")
                }
            }
            readableBuilder.append("سؤال الدرس لزين الدين: ").append(question)

            StructuredTutorResponse(
                chalkboardHeader = header,
                warmGreetingToZein = greeting,
                steps = steps.ifEmpty { buildInstantGroundedChalkboard(lesson, studentLevel, languageMode).steps },
                keyTermsSummary = terms,
                singleSimpleQuestion = question,
                expectedAnswerKeywords = keywords,
                encouragementMessage = encouragement,
                fullReadableScript = readableBuilder.toString().trim()
            )
        } catch (_: TimeoutCancellationException) {
            buildInstantGroundedChalkboard(lesson, studentLevel, languageMode)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            buildInstantGroundedChalkboard(lesson, studentLevel, languageMode)
        }
    }

    fun streamTeacherReplyToZein(
        lesson: CurriculumLesson,
        studentLevel: StudentLevel,
        languageMode: LanguageMode,
        currentQuestionOnBoard: String,
        zeinSpokenOrTypedInput: String
    ): Flow<String> = flow {
        val terms = lesson.keyTerms.split(",", "،").map { it.trim() }.filter { it.isNotBlank() }
        val matchedTerm = terms.firstOrNull { zeinSpokenOrTypedInput.contains(it, ignoreCase = true) }
        val cleanTitle = cleanTopicTitle(lesson.lessonTitle)
        val praiseLine = if (matchedTerm != null) {
            "إجابة ممتازة وصحيحة يا بطلنا زين الدين! 🌟 لقد ذكرت ($matchedTerm) ببراعة!"
        } else {
            "أحسنت المحاولة والمشاركة يا بطلنا زين الدين! 🌟 إجابتك (\"$zeinSpokenOrTypedInput\") رائعة!"
        }

        emit(
            """
            $praiseLine

            📌 خلاصة القاعدة في درس ($cleanTitle):
            • المصطلحات الأساسية: ${lesson.keyTerms}
            • التوضيح المبسط:
            ${lesson.officialContent.lines().filter { it.isNotBlank() }.take(4).joinToString("\n")}

            ⭐ أحسنت يا بطل! هل تحب أن نأخذ مثالاً آخر أم ننتقل لدرس جديد؟
            """.trimIndent()
        )
    }

    /**
     * شات المدرس الذكي والاختبارات التفاعلية:
     * 1. إذا كان هناك كويز أو امتحان معلق (pendingAssessment) وأرسل الطالب إجاباته، يقوم المدرس بتصحيح الإجابات
     *    وإعطاء النتيجة والدرجة والحل الصحيح بالأسفل!
     * 2. إذا طلب الطالب كويز أو امتحاناً جديداً، يعرض الأسئلة فقط بدون كشف الإجابات وبدون كتابة اسم المصدر!
     */
    fun streamGroundedChatReply(
        userMessage: String,
        interactionMode: ChatInteractionMode,
        subjectFilter: String,
        allGroundedLessons: List<CurriculumLesson>,
        recentMessages: List<ChatMessage>,
        studentLevel: StudentLevel,
        languageMode: LanguageMode
    ): Flow<String> = flow {
        val cleanMsg = userMessage.trim()
        val activePending = pendingAssessment

        val isRequestingNewQuizOrExamOrRevision = isExplicitNewModeRequest(cleanMsg)

        // أولاً: إذا كان هناك كويز أو امتحان مفتوح ينتظر إجابة الطالب، والطالب كتب إجابته الآن -> نصحح له فوراً!
        if (activePending != null && !isRequestingNewQuizOrExamOrRevision) {
            val gradingReport = gradeStudentSubmission(
                assessment = activePending,
                studentAnswerText = cleanMsg
            )
            pendingAssessment = null
            emit(gradingReport)
            AppDiagnosticsLogger.logInfo(
                tag = "ChatAI",
                code = "ASSESSMENT_GRADED",
                message = "تم تصحيح إجابة زين الدين في (${activePending.topicTitle}) وعرض الدرجة والحل الصحيح."
            )
            return@flow
        }

        val currentStep = variationCounter.incrementAndGet()
        val topLessons = selectTopRelevantLessons(
            query = cleanMsg,
            subjectFilter = subjectFilter,
            allLessons = allGroundedLessons,
            variationSeed = currentStep,
            maxCount = 3
        )

        val effectiveMode = detectEffectiveInteractionMode(cleanMsg, interactionMode)

        when (effectiveMode) {
            ChatInteractionMode.QUIZ -> {
                val quizAssessment = buildInteractiveAssessment(
                    mode = ChatInteractionMode.QUIZ,
                    subjectFilter = subjectFilter,
                    matchedLessons = topLessons,
                    variationSeed = currentStep
                )
                pendingAssessment = quizAssessment
                val quizQuestionsOnlyText = formatAssessmentQuestionsOnly(quizAssessment)
                emit(quizQuestionsOnlyText)
                AppDiagnosticsLogger.logInfo(
                    tag = "ChatAI",
                    code = "QUIZ_QUESTIONS_SENT",
                    message = "تم إرسال أسئلة الكويز فقط (${quizAssessment.questions.size} أسئلة) بانتظار إجابة الطالب."
                )
            }

            ChatInteractionMode.EXAM -> {
                val examAssessment = buildInteractiveAssessment(
                    mode = ChatInteractionMode.EXAM,
                    subjectFilter = subjectFilter,
                    matchedLessons = topLessons,
                    variationSeed = currentStep
                )
                pendingAssessment = examAssessment
                val examQuestionsOnlyText = formatAssessmentQuestionsOnly(examAssessment)
                emit(examQuestionsOnlyText)
                AppDiagnosticsLogger.logInfo(
                    tag = "ChatAI",
                    code = "EXAM_QUESTIONS_SENT",
                    message = "تم إرسال أسئلة الامتحان فقط (${examAssessment.questions.size} أسئلة - ٢٠ درجة) بانتظار إجابة الطالب."
                )
            }

            ChatInteractionMode.REVISION -> {
                pendingAssessment = null
                val revisionText = buildCleanRevisionSummary(
                    matchedLessons = topLessons
                )
                emit(revisionText)
            }

            ChatInteractionMode.CHAT -> {
                pendingAssessment = null
                val chatExplanation = buildCleanChatExplanation(
                    userMessage = cleanMsg,
                    matchedLessons = topLessons,
                    languageMode = languageMode
                )
                emit(chatExplanation)
            }
        }
    }

    private fun isExplicitNewModeRequest(msg: String): Boolean {
        val lower = msg.lowercase()
        return lower.startsWith("اعمل لي كويز") ||
            lower.startsWith("اعمل لي امتحان") ||
            lower.startsWith("اعمل لي مراجعة") ||
            lower.startsWith("اشرح لي") ||
            lower.contains("كويز تفاعلي سريع") ||
            lower.contains("امتحان شامل بالدرجات") ||
            lower.contains("مراجعة مختصرة") ||
            lower.contains("كويز جديد")
    }

    private fun detectEffectiveInteractionMode(
        userMessage: String,
        selectedMode: ChatInteractionMode
    ): ChatInteractionMode {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("امتحان") -> ChatInteractionMode.EXAM
            lower.contains("كويز") || lower.contains("اختبرني") || lower.contains("اسألني") -> ChatInteractionMode.QUIZ
            lower.contains("مراجعة") || lower.contains("ملخص") || lower.contains("لخص") -> ChatInteractionMode.REVISION
            lower.contains("اشرح") || lower.contains("شرح") -> ChatInteractionMode.CHAT
            else -> selectedMode
        }
    }

    /**
     * يبني أسئلة كويز (٤ أسئلة من ١٠ درجات) أو امتحان شامل (٦ أسئلة من ٢٠ درجة)
     * بصياغة واضحة وسليمة ١٠٠٪ بدون ذكر اسم أي كتاب خارجي وبدون رموز ماركداون مشوهة.
     */
    private fun buildInteractiveAssessment(
        mode: ChatInteractionMode,
        subjectFilter: String,
        matchedLessons: List<CurriculumLesson>,
        variationSeed: Int
    ): PendingAssessment {
        val primaryLesson = matchedLessons.firstOrNull()
        val resolvedSubjectId = when {
            subjectFilter != "all" -> subjectFilter
            primaryLesson != null -> primaryLesson.subjectId
            else -> SubjectType.MATH.id
        }
        val subjectType = SubjectType.fromId(resolvedSubjectId)
        val topicTitle = if (primaryLesson != null) {
            cleanTopicTitle(primaryLesson.lessonTitle)
        } else {
            subjectType.titleAr
        }

        val bank = getSubjectQuestionBank(subjectType)
        val questionCount = if (mode == ChatInteractionMode.EXAM) 5 else 3
        val pointsPerQuestion = if (mode == ChatInteractionMode.EXAM) 4 else 5

        val selectedQuestions = mutableListOf<AssessmentQuestionItem>()
        for (i in 0 until questionCount) {
            val template = bank[(variationSeed + i) % bank.size]
            selectedQuestions.add(
                template.copy(
                    number = i + 1,
                    points = pointsPerQuestion
                )
            )
        }

        val totalPoints = selectedQuestions.sumOf { it.points }
        return PendingAssessment(
            id = "assess_${System.currentTimeMillis()}",
            mode = mode,
            subjectId = subjectType.id,
            subjectNameAr = subjectType.titleAr,
            topicTitle = topicTitle,
            totalPoints = totalPoints,
            questions = selectedQuestions
        )
    }

    /**
     * يعرض الأسئلة فقط للطالب بدون كشف الإجابات أو النتيجة في نفس الرسالة!
     */
    private fun formatAssessmentQuestionsOnly(assessment: PendingAssessment): String {
        val headerTitle = if (assessment.mode == ChatInteractionMode.EXAM) {
            "📝 امتحان في ${assessment.subjectNameAr} (الدرجة الكلية: ${assessment.totalPoints} درجة)"
        } else {
            "🎯 كويز تفاعلي سريع في ${assessment.subjectNameAr} (الدرجة: ${assessment.totalPoints} درجات)"
        }

        val builder = StringBuilder()
        builder.append(headerTitle).append("\n")
        builder.append("الموضوع: ").append(assessment.topicTitle).append("\n")
        builder.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        assessment.questions.forEach { q ->
            builder.append("السؤال (${q.number}) [${q.questionTypeLabel} — ${q.points} درجات]:\n")
            builder.append(q.questionPrompt).append("\n")
            if (q.options.isNotEmpty()) {
                q.options.forEach { opt ->
                    builder.append("   ").append(opt).append("\n")
                }
            }
            builder.append("\n")
        }

        builder.append("━━━━━━━━━━━━━━━━━━━━\n")
        builder.append("✍️ دورك الآن يا بطلنا زين الدين:\n")
        builder.append("اكتب إجاباتك عن الأسئلة بالأسفل (مثلاً: ١-أ ، ٢-ب ، ٣-...) ثم اضغط إرسال، وسأقوم بتصحيح إجابتك وإعطائك النتيجة والحل الصحيح فوراً!")

        return builder.toString().trim()
    }

    /**
     * يصحح إجابات الطالب ويعرض الدرجة النهائية والحل الصحيح والشرح بالأسفل
     */
    private fun gradeStudentSubmission(
        assessment: PendingAssessment,
        studentAnswerText: String
    ): String {
        val normalizedInput = normalizeArabicAndEnglish(studentAnswerText)
        val segments = splitStudentAnswerSegments(studentAnswerText, assessment.questions.size)

        var earnedPoints = 0
        val detailedCorrections = StringBuilder()

        assessment.questions.forEachIndexed { idx, q ->
            val segmentToCheck = segments.getOrNull(idx)?.let { normalizeArabicAndEnglish(it) } ?: normalizedInput
            val isCorrect = checkQuestionAnswer(q, segmentToCheck, normalizedInput)
            if (isCorrect) {
                earnedPoints += q.points
            }

            val statusIcon = if (isCorrect) "✅ إجابة صحيحة (+${q.points} درجات)" else "💡 تحتاج لمراجعة (٠ / ${q.points})"
            detailedCorrections.append("• السؤال (${q.number}): $statusIcon\n")
            detailedCorrections.append("   - السؤال: ${q.questionPrompt}\n")
            detailedCorrections.append("   - الحل الصحيح: ${q.correctAnswerText}\n")
            detailedCorrections.append("   - الشرح: ${q.explanationAr}\n\n")
        }

        val percentage = (earnedPoints * 100) / assessment.totalPoints.coerceAtLeast(1)
        val encouragement = when {
            percentage >= 85 -> "ممتاز يا بطلنا العبقري زين الدين! مستوى رائع جداً وتستحق نجمة التفوق الذهبية! 🌟🏆"
            percentage >= 60 -> "أحسنت يا بطلنا زين الدين! إجابات جميلة جداً، راجع الحلول بالأسفل لتقفل الدرجة النهائية المرة القادمة! 👏⭐"
            else -> "شكراً لمحاولتك الجميلة يا زين الدين! تعلّم من الحل الصحيح والشرح بالأسفل وجرب كويز جديد لتتفوق! 💪🌟"
        }

        return """
            🏆 النتيجة النهائية لتصحيح إجاباتك يا زين الدين:
            الدرجة: $earnedPoints من ${assessment.totalPoints} ($percentage%)
            $encouragement

            ━━━━━━━━━━━━━━━━━━━━
            📋 الحل الصحيح والشرح التفصيلي للأسئلة:

            ${detailedCorrections.toString().trim()}
        """.trimIndent()
    }

    private fun checkQuestionAnswer(
        q: AssessmentQuestionItem,
        segmentNormalized: String,
        fullNormalized: String
    ): Boolean {
        // ١. مطابقة الكلمات المفتاحية أو الإجابة الصحيحة مباشرة
        for (kw in q.acceptedKeywords) {
            val cleanKw = normalizeArabicAndEnglish(kw)
            if (cleanKw.isNotBlank() && (segmentNormalized.contains(cleanKw) || fullNormalized.contains(cleanKw))) {
                return true
            }
        }
        // ٢. مطابقة رمز الاختيار (أ / ب / ج أو a / b / c) في الجزء المخصص للسؤال
        if (q.correctOptionLetterAr.isNotBlank()) {
            val arLetter = q.correctOptionLetterAr
            val enLetter = q.correctOptionLetterEn.lowercase()
            val qNum = q.number.toString()
            val patterns = listOf(
                "$qNum$arLetter", "$qNum-$arLetter", "$qNum $arLetter", "($arLetter)",
                "$qNum$enLetter", "$qNum-$enLetter", "$qNum $enLetter", "($enLetter)"
            )
            if (patterns.any { fullNormalized.contains(it) }) {
                return true
            }
            if (segmentNormalized == arLetter || segmentNormalized == enLetter) {
                return true
            }
        }
        return false
    }

    private fun splitStudentAnswerSegments(rawText: String, count: Int): List<String> {
        val parts = rawText.split(Regex("[\\n،,;\\-]+")).map { it.trim() }.filter { it.isNotBlank() }
        return if (parts.size >= count) parts else List(count) { rawText }
    }

    private fun normalizeArabicAndEnglish(input: String): String {
        return input.lowercase()
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('٠', '0')
            .replace('١', '1')
            .replace('٢', '2')
            .replace('٣', '3')
            .replace('٤', '4')
            .replace('٥', '5')
            .replace('٦', '6')
            .replace('٧', '7')
            .replace('٨', '8')
            .replace('٩', '9')
            .trim()
    }

    /**
     * بنك أسئلة منهجي نظيف ومنظم للصف الثاني الابتدائي لغات (بدون أي أسماء مصادر خارجية)
     */
    private fun getSubjectQuestionBank(subject: SubjectType): List<AssessmentQuestionItem> {
        return when (subject) {
            SubjectType.MATH -> listOf(
                AssessmentQuestionItem(
                    number = 1,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "What is the Place Value (القيمة المكانية) of the digit 7 in the number 742?",
                    options = listOf("أ) Ones (آحاد)", "ب) Tens (عشرات)", "ج) Hundreds (مئات)"),
                    correctOptionLetterAr = "ج",
                    correctOptionLetterEn = "c",
                    correctAnswerText = "ج) Hundreds (مئات)",
                    acceptedKeywords = listOf("hundreds", "مئات", "100", "700"),
                    explanationAr = "الرقم 7 يقع في خانة المئات (Hundreds) على اليسار، وقيمته العددية (Value) تساوي 700.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 2,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "What is the Value (القيمة العددية) of the digit 5 in the number 358?",
                    options = listOf("أ) 5", "ب) 50", "ج) 500"),
                    correctOptionLetterAr = "ب",
                    correctOptionLetterEn = "b",
                    correctAnswerText = "ب) 50",
                    acceptedKeywords = listOf("50", "fifty", "خمسون", "خمسين"),
                    explanationAr = "لأن الرقم 5 في خانة العشرات (Tens)، فإن قيمته العددية (Value) تساوي 50.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 3,
                    questionTypeLabel = "Complete / أكمل",
                    questionPrompt = "Write in standard form (اكتب بالصيغة القياسية): 400 + 60 + 9 = ........",
                    options = emptyList(),
                    correctAnswerText = "469",
                    acceptedKeywords = listOf("469"),
                    explanationAr = "نجمع 4 في المئات (400) + 6 في العشرات (60) + 9 في الآحاد = 469.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 4,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "When the minute hand points to 6, we read the time as (عندما يشير عقرب الدقائق إلى 6 نقرأ الوقت):",
                    options = listOf("أ) Half past (ونصف - 30 دقيقة)", "ب) Quarter past (وربع - 15 دقيقة)", "ج) O'clock (تماماً - 00)"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) Half past (ونصف - 30 دقيقة)",
                    acceptedKeywords = listOf("half", "نصف", "30", "ونصف"),
                    explanationAr = "عقرب الدقائق على الرقم 6 يعني مرور 30 دقيقة أي النصف (Half past).",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 5,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "A square (المربع) is a 2D shape that has ........ equal sides (أضلاع متساوية):",
                    options = listOf("أ) 3 sides", "ب) 4 equal sides", "ج) 0 sides"),
                    correctOptionLetterAr = "ب",
                    correctOptionLetterEn = "b",
                    correctAnswerText = "ب) 4 equal sides (٤ أضلاع متساوية)",
                    acceptedKeywords = listOf("4", "four", "اربعه", "اربع"),
                    explanationAr = "المربع (Square) له 4 أضلاع متساوية في الطول (4 equal sides) و4 رؤوس (4 vertices).",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 6,
                    questionTypeLabel = "Complete / احسب الناتج",
                    questionPrompt = "Find the result by Mental Math (احسب الناتج): 25 + 14 = ........",
                    options = emptyList(),
                    correctAnswerText = "39",
                    acceptedKeywords = listOf("39"),
                    explanationAr = "نجمع الآحاد (5 + 4 = 9) والعشرات (20 + 10 = 30) فيكون الناتج 39.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 7,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "The number 18 is an ........ number (العدد 18 هو عدد زوجي أم فردي؟):",
                    options = listOf("أ) Even number (عدد زوجي)", "ب) Odd number (عدد فردي)", "ج) Zero"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) Even number (عدد زوجي)",
                    acceptedKeywords = listOf("even", "زوجي"),
                    explanationAr = "الأعداد التي آحادها (0, 2, 4, 6, 8) هي أعداد زوجية (Even numbers).",
                    points = 5
                )
            )

            SubjectType.CONNECT -> listOf(
                AssessmentQuestionItem(
                    number = 1,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "She ........ a new blue bag. (اختر الفعل الصحيح مع الضمير She):",
                    options = listOf("أ) have", "ب) has", "ج) am"),
                    correctOptionLetterAr = "ب",
                    correctOptionLetterEn = "b",
                    correctAnswerText = "ب) has",
                    acceptedKeywords = listOf("has"),
                    explanationAr = "مع الضمائر المفردة (He / She / It) نستخدم (has) بمعنى يملك/تملك.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 2,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "A bird ........ fly in the sky. (الطائر يستطيع الطيران):",
                    options = listOf("أ) can", "ب) can't", "ج) is"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) can",
                    acceptedKeywords = listOf("can"),
                    explanationAr = "نستخدم (can) للتعبير عن القدرة والاستطاعة: A bird can fly.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 3,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "I see with my ........ (أنا أرى بـ ........):",
                    options = listOf("أ) ears (الأذنين)", "ب) eyes (العينين)", "ج) hands (اليدين)"),
                    correctOptionLetterAr = "ب",
                    correctOptionLetterEn = "b",
                    correctAnswerText = "ب) eyes (العينين)",
                    acceptedKeywords = listOf("eyes", "eye", "عين", "العينين"),
                    explanationAr = "نحن نرى بأعيننا: I see with my eyes.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 4,
                    questionTypeLabel = "Complete / أكمل الكلمة",
                    questionPrompt = "My father and mother are my ........ (الأب والأم هما والداي - اكتب parents أو family):",
                    options = emptyList(),
                    correctAnswerText = "parents (الوالدان)",
                    acceptedKeywords = listOf("parents", "family", "والدين", "الوالدان", "اسرتي"),
                    explanationAr = "كلمة (parents) تعني الوالدين (الأب والأم).",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 5,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "........ are my grandparents. (اختر اسم الإشارة للجمع):",
                    options = listOf("أ) This", "ب) These", "ج) He"),
                    correctOptionLetterAr = "ب",
                    correctOptionLetterEn = "b",
                    correctAnswerText = "ب) These",
                    acceptedKeywords = listOf("these"),
                    explanationAr = "نستخدم (These are) للإشارة إلى الجمع القريب، بينما (This is) للمفرد.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 6,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "The cat is ........ the table. (القطة تحت المنضدة):",
                    options = listOf("أ) under (تحت)", "ب) read (يقرأ)", "ج) big (كبير)"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) under (تحت)",
                    acceptedKeywords = listOf("under", "تحت"),
                    explanationAr = "حرف الجر (under) يعني تحت، ومن حروف المكان أيضاً (in / on / next to).",
                    points = 5
                )
            )

            SubjectType.CONNECT_PLUS -> listOf(
                AssessmentQuestionItem(
                    number = 1,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "Look! He ........ playing football now. (زمن المضارع المستمر Present Continuous):",
                    options = listOf("أ) am", "ب) is", "ج) are"),
                    correctOptionLetterAr = "ب",
                    correctOptionLetterEn = "b",
                    correctAnswerText = "ب) is",
                    acceptedKeywords = listOf("is"),
                    explanationAr = "في المضارع المستمر نستخدم (He / She / It + is + verb-ing).",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 2,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "A ........ helps sick people at the hospital. (من يساعد المرضى في المستشفى؟):",
                    options = listOf("أ) doctor (الطبيب)", "ب) baker (الخباز)", "ج) farmer (الفلاح)"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) doctor (الطبيب)",
                    acceptedKeywords = listOf("doctor", "nurse", "طبيب", "الطبيب"),
                    explanationAr = "الطبيب (doctor) والممرضة (nurse) يعملان في المستشفى لمساعدة المرضى.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 3,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "He ........ to school every day. (المضارع البسيط مع He):",
                    options = listOf("أ) go", "ب) goes", "ج) going"),
                    correctOptionLetterAr = "ب",
                    correctOptionLetterEn = "b",
                    correctAnswerText = "ب) goes",
                    acceptedKeywords = listOf("goes"),
                    explanationAr = "في المضارع البسيط (Present Simple) نضيف (s/es) للفعل مع المفرد الغائب (He goes).",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 4,
                    questionTypeLabel = "Complete / أكمل",
                    questionPrompt = "A baby cat is called a ........ (صغير القطة يسمى kitten أم puppy؟):",
                    options = emptyList(),
                    correctAnswerText = "kitten (هريرة صغيرة)",
                    acceptedKeywords = listOf("kitten", "قطه", "هريره"),
                    explanationAr = "صغير القطة يسمى (kitten)، بينما صغير الكلب يسمى (puppy).",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 5,
                    questionTypeLabel = "Choose / اختر الإجابة الصحيحة",
                    questionPrompt = "........ does a firefighter work? — At the fire station. (أداة الاستفهام عن المكان):",
                    options = listOf("أ) Where (أين)", "ب) Who (مَن)", "ج) When (متى)"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) Where (أين)",
                    acceptedKeywords = listOf("where", "اين"),
                    explanationAr = "نسأل عن المكان بأداة الاستفهام (Where).",
                    points = 5
                )
            )

            SubjectType.ARABIC -> listOf(
                AssessmentQuestionItem(
                    number = 1,
                    questionTypeLabel = "اختر الإجابة الصحيحة",
                    questionPrompt = "« ........ تلميذٌ مجتهدٌ ومتفوقٌ » — اختر ضمير المتكلم المناسب:",
                    options = listOf("أ) أنا", "ب) نحن", "ج) هي"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) أنا",
                    acceptedKeywords = listOf("انا"),
                    explanationAr = "نستخدم ضمير المتكلم (أنا) للمفرد المذكر والمؤنث، و(نحن) للجمع.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 2,
                    questionTypeLabel = "اختر الإجابة الصحيحة",
                    questionPrompt = "« لا تقطفْ أزهارَ الحديقة » — ما نوع هذا الأسلوب؟",
                    options = listOf("أ) أسلوب نهي", "ب) أسلوب استفهام", "ج) أسلوب تعجب"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) أسلوب نهي",
                    acceptedKeywords = listOf("نهي", "اسلوب نهي"),
                    explanationAr = "يبدأ بـ (لا الناهية + الفعل المضارع) لطلب الامتناع عن فعل شيء غير مستحب، فهو أسلوب نهي.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 3,
                    questionTypeLabel = "أكمل باسم إشارة مناسب",
                    questionPrompt = "أكمل بـ (هذا أو هذه): « ........ شجرةٌ مثمرةٌ وجميلةٌ »",
                    options = emptyList(),
                    correctAnswerText = "هذه",
                    acceptedKeywords = listOf("هذه"),
                    explanationAr = "كلمة (شجرة) مفرد مؤنث، لذلك نستخدم معها اسم الإشارة (هذه).",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 4,
                    questionTypeLabel = "اختر الإجابة الصحيحة",
                    questionPrompt = "ما نوع المد في كلمة « صَدِيقِي »؟",
                    options = listOf("أ) مد بالألف", "ب) مد بالياء", "ج) مد بالواو"),
                    correctOptionLetterAr = "ب",
                    correctOptionLetterEn = "b",
                    correctAnswerText = "ب) مد بالياء",
                    acceptedKeywords = listOf("ياء", "بالياء"),
                    explanationAr = "كلمة (صديقي) بها حرف الياء الساكن المسبوق بكسر، فهو مد بالياء.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 5,
                    questionTypeLabel = "اختر الإجابة الصحيحة",
                    questionPrompt = "اللام في كلمة « الشَّمْسُ » هي لام ........ :",
                    options = listOf("أ) شمسية (تُكتب ولا تُنطق)", "ب) قمرية (تُكتب وتُنطق)", "ج) حرف جر"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) شمسية (تُكتب ولا تُنطق)",
                    acceptedKeywords = listOf("شمسيه", "شمسية"),
                    explanationAr = "حرف الشين من حروف اللام الشمسية ومشدد بعد اللام، فتُكتب اللام ولا تُنطق.",
                    points = 5
                )
            )

            SubjectType.RELIGION -> listOf(
                AssessmentQuestionItem(
                    number = 1,
                    questionTypeLabel = "اختر الإجابة الصحيحة",
                    questionPrompt = "في سورة الإخلاص: « قُلْ هُوَ اللَّهُ أَحَدٌ » — ما معنى (اللَّهُ الصَّمَدُ)؟",
                    options = listOf("أ) الذي يحتاج إليه جميع المخلوقات ولا يحتاج إلى أحد", "ب) الكواكب والنجوم", "ج) المطر الغزير"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) الذي يحتاج إليه جميع المخلوقات ولا يحتاج إلى أحد",
                    acceptedKeywords = listOf("يحتاج", "المقصود", "الصمد"),
                    explanationAr = "(الله الصمد) أي السيد المقصود في قضاء الحوائج الذي يحتاج إليه كل الخلق وهو غني عنهم.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 2,
                    questionTypeLabel = "اختر الإجابة الصحيحة",
                    questionPrompt = "ماذا يقول المسلم عندما يبدأ بتناول الطعام؟",
                    options = listOf("أ) بسم الله", "ب) تصبح على خير", "ج) مع السلامة"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) بسم الله",
                    acceptedKeywords = listOf("بسم الله", "يسمي"),
                    explanationAr = "علّمنا النبي ﷺ آداب الطعام: أن نسمي الله في أوله (بسم الله) ونأكل بيميننا ونحمد الله في آخره.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 3,
                    questionTypeLabel = "صح أم خطأ",
                    questionPrompt = "الصدق والأمانة من أخلاق المسلم الجميلة وكان النبي ﷺ يلقب بـ (الصادق الأمين) — (صح أم خطأ)؟",
                    options = listOf("أ) صح", "ب) خطأ"),
                    correctOptionLetterAr = "أ",
                    correctOptionLetterEn = "a",
                    correctAnswerText = "أ) صح",
                    acceptedKeywords = listOf("صح", "صحيح", "نعم"),
                    explanationAr = "عبارة صحيحة؛ عُرف النبي محمد ﷺ بالصادق الأمين، والمسلم يتحلى دائماً بالصدق والأمانة.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 4,
                    questionTypeLabel = "اختر الإجابة الصحيحة",
                    questionPrompt = "كم عدد الصلوات المفروضة على المسلم في اليوم والليلة؟",
                    options = listOf("أ) ٣ صلوات", "ب) ٥ صلوات", "ج) ٧ صلوات"),
                    correctOptionLetterAr = "ب",
                    correctOptionLetterEn = "b",
                    correctAnswerText = "ب) ٥ صلوات (الفجر، الظهر، العصر، المغرب، العشاء)",
                    acceptedKeywords = listOf("5", "خمس", "خمسه"),
                    explanationAr = "الصلوات المفروضة خمس صلوات يومياً: الفجر، الظهر، العصر، المغرب، والعشاء.",
                    points = 5
                ),
                AssessmentQuestionItem(
                    number = 5,
                    questionTypeLabel = "أكمل",
                    questionPrompt = "النظافة والوضوء قبل الصلاة يعلمان المسلم ........ (اكتب: الطهارة أو النظافة):",
                    options = emptyList(),
                    correctAnswerText = "الطهارة والنظافة",
                    acceptedKeywords = listOf("طهاره", "نظافه", "الطهاره", "النظافه"),
                    explanationAr = "الوضوء طهارة ونور للمسلم ويحافظ على نظافة الجسم كل يوم.",
                    points = 5
                )
            )
        }
    }

    private fun buildCleanRevisionSummary(
        matchedLessons: List<CurriculumLesson>
    ): String {
        val primary = matchedLessons.firstOrNull()
            ?: return "أهلاً بك يا زين الدين! اختر المادة أو الموضوع من الأعلى لعرض المراجعة المختصرة."

        val builder = StringBuilder()
        builder.append("⚡ مراجعة مختصرة ومركّزة — ${primary.subjectNameAr}\n")
        builder.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        matchedLessons.forEachIndexed { idx, lesson ->
            val cleanTitle = cleanTopicTitle(lesson.lessonTitle)
            val cleanLines = lesson.officialContent
                .lines()
                .map { it.trim() }
                .filter { it.length >= 12 }
                .take(5)
                .joinToString("\n• ", prefix = "• ")

            builder.append("${idx + 1}) الموضوع: $cleanTitle\n")
            builder.append("🔑 أهم المصطلحات والقواعد: ${lesson.keyTerms}\n")
            builder.append("📌 النقاط الأساسية:\n$cleanLines\n\n")
        }

        builder.append("━━━━━━━━━━━━━━━━━━━━\n")
        builder.append("🌟 إذا كنت مستعداً لاختبار نفسك يا زين الدين، اضغط على زر (كويز تفاعلي سريع) أو (امتحان شامل بالدرجات)!")

        return builder.toString().trim()
    }

    private fun buildCleanChatExplanation(
        userMessage: String,
        matchedLessons: List<CurriculumLesson>,
        languageMode: LanguageMode
    ): String {
        val primary = matchedLessons.firstOrNull()
            ?: return "أهلاً بك يا بطلنا زين الدين! اسألني في أي درس أو اختر موضوعاً من الشريط العلوي."

        val cleanTitle = cleanTopicTitle(primary.lessonTitle)
        val isBilingual = SubjectType.fromId(primary.subjectId).isBilingualSubject && languageMode != LanguageMode.ARABIC
        val lines = primary.officialContent
            .lines()
            .map { it.trim() }
            .filter { it.length >= 12 }
            .take(6)

        val bilingualSection = if (isBilingual) {
            "🗣️ الشرح الثنائي (English + عربي مبسّط):\n" +
                "نركز في هذا الدرس على المصطلحات الإنجليزية الأساسية (${primary.keyTerms}) مع فهم معناها وتطبيقها بالعربية خطوة بخطوة."
        } else {
            "📖 خلاصة الفكرة الأساسية:\n" +
                "يركز هذا الدرس على (${primary.keyTerms}) بأسلوب مبسط وواضح."
        }

        return """
            أهلاً بك يا بطلنا زين الدين! 👨‍🏫✨
            إليك شرح موضوع ($cleanTitle) في مادة (${primary.subjectNameAr}):

            $bilingualSection

            ━━━━━━━━━━━━━━━━━━━━
            📝 خطوات الشرح والأمثلة:
            ${lines.mapIndexed { idx, l -> "${idx + 1}. $l" }.joinToString("\n")}

            ━━━━━━━━━━━━━━━━━━━━
            💡 هل تحب أن أسألك كويز سريع في هذا الدرس وتجاوب عليه لأعطيك درجتك؟ اضغط على (كويز تفاعلي سريع)!
        """.trimIndent()
    }

    private fun selectTopRelevantLessons(
        query: String,
        subjectFilter: String,
        allLessons: List<CurriculumLesson>,
        variationSeed: Int = 0,
        maxCount: Int = 3
    ): List<CurriculumLesson> {
        val pool = if (subjectFilter == "all") {
            allLessons
        } else {
            allLessons.filter { it.subjectId == subjectFilter }.ifEmpty { allLessons }
        }
        if (pool.isEmpty()) return emptyList()

        val queryLower = query.lowercase()
        val keywords = queryLower
            .split(Regex("[\\s،,.؟?!()\\-]+"))
            .filter { it.length >= 2 && it !in setOf("اعمل", "لي", "كويز", "سريع", "امتحان", "شامل", "مراجعة", "مختصرة", "من", "في", "عن") }

        val scored = pool.mapIndexed { index, lesson ->
            var score = 0
            val searchable = "${lesson.subjectNameAr} ${lesson.unitTitle} ${lesson.lessonTitle} ${lesson.keyTerms} ${lesson.officialContent}".lowercase()
            for (kw in keywords) {
                if (searchable.contains(kw)) score += 5
                if (lesson.lessonTitle.lowercase().contains(kw) || lesson.unitTitle.lowercase().contains(kw)) score += 10
                if (lesson.keyTerms.lowercase().contains(kw)) score += 8
            }
            if ((queryLower.contains("ماث") || queryLower.contains("math") || queryLower.contains("حساب")) && lesson.subjectId == SubjectType.MATH.id) {
                score += 12
            }
            if ((queryLower.contains("انجليزي") || queryLower.contains("إنجليزي") || queryLower.contains("english") || queryLower.contains("connect")) &&
                (lesson.subjectId == SubjectType.CONNECT.id || lesson.subjectId == SubjectType.CONNECT_PLUS.id)
            ) {
                score += 12
            }
            if ((queryLower.contains("عربي") || queryLower.contains("عربية") || queryLower.contains("نحو") || queryLower.contains("أساليب")) &&
                lesson.subjectId == SubjectType.ARABIC.id
            ) {
                score += 12
            }
            if ((queryLower.contains("دين") || queryLower.contains("إسلامية") || queryLower.contains("قرآن") || queryLower.contains("سورة")) &&
                lesson.subjectId == SubjectType.RELIGION.id
            ) {
                score += 12
            }
            val rotationBonus = ((index + variationSeed) % pool.size.coerceAtLeast(1))
            lesson to (score + rotationBonus)
        }

        return scored
            .sortedByDescending { it.second }
            .take(maxCount)
            .map { it.first }
    }
}
