package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SubjectType(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val isBilingualSubject: Boolean,
    val accentHex: Long
) {
    ARABIC(
        id = "arabic",
        titleAr = "اللغة العربية",
        subtitleAr = "منهج تواصل - القراءة والقواعد والأساليب",
        isBilingualSubject = false,
        accentHex = 0xFF0F766E
    ),
    RELIGION(
        id = "religion",
        titleAr = "التربية الدينية",
        subtitleAr = "العقيدة والقرآن الكريم والأخلاق الحسنة",
        isBilingualSubject = false,
        accentHex = 0xFF15803D
    ),
    MATH(
        id = "math",
        titleAr = "Math - الرياضيات لغات",
        subtitleAr = "شرح إنجليزي + توضيح عربي فوري للمصطلحات",
        isBilingualSubject = true,
        accentHex = 0xFF1D4ED8
    ),
    CONNECT(
        id = "connect",
        titleAr = "English Connect",
        subtitleAr = "المنهج الأساسي - محادثة ومفردات باللغتين",
        isBilingualSubject = true,
        accentHex = 0xFFD97706
    ),
    CONNECT_PLUS(
        id = "connect_plus",
        titleAr = "English Advanced (Connect Plus)",
        subtitleAr = "المنهج المتقدم لمدارس اللغات - قواعد ومهارات",
        isBilingualSubject = true,
        accentHex = 0xFF7C3AED
    );

    companion object {
        fun fromId(id: String): SubjectType {
            return entries.firstOrNull { it.id == id } ?: ARABIC
        }
    }
}

enum class StudentLevel(val code: String, val labelAr: String, val descriptionAr: String) {
    BEGINNER(
        code = "beginner",
        labelAr = "مبتدئ",
        descriptionAr = "شرح مبسّط خطوة بخطوة مع أمثلة كثيرة وتكرار هادئ يناسب التأسيس"
    ),
    ADVANCED(
        code = "advanced",
        labelAr = "متقدم",
        descriptionAr = "شرح أعمق مع تحديات ذكية وأسئلة تفكير عليا لمدارس اللغات"
    );

    companion object {
        fun fromCode(code: String): StudentLevel {
            return entries.firstOrNull { it.code == code } ?: BEGINNER
        }
    }
}

enum class LanguageMode(val code: String, val labelAr: String, val badgeAr: String) {
    BILINGUAL(
        code = "bilingual",
        labelAr = "ثنائي اللغة (إنجليزي + عربي فوري)",
        badgeAr = "EN + عربي"
    ),
    ARABIC(
        code = "arabic",
        labelAr = "عربي فصحى مبسّط",
        badgeAr = "عربي فقط"
    ),
    ENGLISH(
        code = "english",
        labelAr = "إنجليزي مبسّط مع ترجمة المصطلحات",
        badgeAr = "English"
    );

    companion object {
        fun fromCode(code: String): LanguageMode {
            return entries.firstOrNull { it.code == code } ?: BILINGUAL
        }
    }
}

@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "زين الدين آسر الشربيني",
    val gradeLabel: String = "الصف الثاني الابتدائي لغات",
    val academicYear: String = "٢٠٢٦ / ٢٠٢٧",
    val currentTerm: String = "الترم الأول", // "الترم الأول" or "الترم الثاني"
    val activeSubjectIds: List<String> = listOf("arabic", "religion", "math", "connect", "connect_plus"),
    val speechPitch: Float = 1.0f,
    val speechRate: Float = 0.85f,
    val studentLevel: String = StudentLevel.BEGINNER.code,
    val preferredLanguageMode: String = LanguageMode.BILINGUAL.code,
    val points: Long = 100L,
    val completedLessonsCount: Long = 0L,
    val streakDays: Long = 1L,
    val reminderEnabled: Boolean = true,
    val reminderHour: Long = 17L,
    val reminderMinute: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CurriculumLesson(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = SubjectType.ARABIC.id,
    val subjectNameAr: String = SubjectType.ARABIC.titleAr,
    val term: String = "الترم الأول", // "الترم الأول" or "الترم الثاني"
    val academicYear: String = "٢٠٢٦ / ٢٠٢٧",
    val unitTitle: String = "",
    val lessonTitle: String = "",
    val lessonOrder: Long = 1L,
    val officialContent: String = "",
    val keyTerms: String = "",
    val sourceBookName: String = "",
    val isCompleted: Boolean = false,
    val lastAiExplanation: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ChalkboardStep(
    val stepTitle: String,
    val englishLine: String,
    val arabicExplanation: String
)

data class StructuredTutorResponse(
    val chalkboardHeader: String,
    val warmGreetingToZein: String,
    val steps: List<ChalkboardStep>,
    val keyTermsSummary: List<String>,
    val singleSimpleQuestion: String,
    val expectedAnswerKeywords: List<String>,
    val encouragementMessage: String,
    val fullReadableScript: String
)

data class RewardBadge(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val requiredPoints: Long,
    val isUnlocked: Boolean
)

enum class ChatInteractionMode(
    val code: String,
    val labelAr: String,
    val defaultPromptAr: String
) {
    CHAT(
        code = "chat",
        labelAr = "سؤال وشرح حر",
        defaultPromptAr = "اشرح لي أهم النقاط والأمثلة في هذا المنهج بطريقة مبسطة."
    ),
    QUIZ(
        code = "quiz",
        labelAr = "كويز تفاعلي سريع",
        defaultPromptAr = "اعمل لي كويز تفاعلي سريع (٣ أسئلة اختيار من متعدد وأكمل) من كتب المنهج والتقييمات المرفوعة مع التصحيح والشرح."
    ),
    EXAM(
        code = "exam",
        labelAr = "امتحان شامل بالدرجات",
        defaultPromptAr = "اعمل لي امتحاناً شاملاً ومرتباً بالدرجات مطابقاً لتقييمات وامتحانات المعاصر والوزارة المرفوعة (اختيار من متعدد، أكمل، ومسألة/سؤال تطبيقي)."
    ),
    REVISION(
        code = "revision",
        labelAr = "مراجعة مختصرة شاملة",
        defaultPromptAr = "اعمل لي مراجعة مختصرة وملخصاً لأهم القوانين والمصطلحات وأسئلة الامتحانات المتكررة في الكتب المرفوعة."
    );

    companion object {
        fun fromCode(code: String): ChatInteractionMode {
            return entries.firstOrNull { it.code == code } ?: CHAT
        }
    }
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "user", // "user" or "ai"
    val messageText: String = "",
    val subjectFilter: String = "all", // "all", "arabic", "religion", "math", "connect", "connect_plus"
    val interactionMode: String = ChatInteractionMode.CHAT.code,
    @ServerTimestamp val createdAt: Timestamp? = null
)

