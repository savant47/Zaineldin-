package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.curriculum.OfficialCurriculumSeed
import com.example.data.model.CurriculumLesson
import com.example.data.model.LanguageMode
import com.example.data.model.StudentLevel
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TutorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val fallbackUserId: String = DEFAULT_DIRECT_USER_ID
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth,
        DEFAULT_DIRECT_USER_ID
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: fallbackUserId
    }

    companion object {
        const val DEFAULT_DIRECT_USER_ID = "zein_eldin_student_2027"
    }

    fun observeProfile(userId: String): Flow<StudentProfile?> {
        val path = "users/$userId/profile/main"
        return db.collection("users")
            .document(userId)
            .collection("profile")
            .document("main")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        StudentProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeLessons(userId: String): Flow<List<CurriculumLesson>> {
        val path = "users/$userId/lessons"
        return db.collection("users")
            .document(userId)
            .collection("lessons")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    CurriculumLesson::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy({ it.subjectId }, { it.lessonOrder }))
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getLessonsOnce(userId: String): Result<List<CurriculumLesson>> {
        val path = "users/$userId/lessons"
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("lessons")
                .get()
                .await()
            val lessons = snapshot.toObjects(
                CurriculumLesson::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedWith(compareBy({ it.subjectId }, { it.lessonOrder }))
            Result.success(lessons)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getLessonById(userId: String, lessonId: String): Result<CurriculumLesson?> {
        val path = "users/$userId/lessons/$lessonId"
        return try {
            val doc = db.collection("users")
                .document(userId)
                .collection("lessons")
                .document(lessonId)
                .get()
                .await()
            Result.success(
                doc.toObject(
                    CurriculumLesson::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun ensureInitialProfileAndCurriculum(): Result<Unit> {
        val uid = requireUserId()
        val profileRef = db.collection("users").document(uid).collection("profile").document("main")
        val lessonsCol = db.collection("users").document(uid).collection("lessons")

        return try {
            val profileSnap = profileRef.get().await()
            if (!profileSnap.exists()) {
                val initialProfileMap = mapOf(
                    "userId" to uid,
                    "studentName" to "زين الدين آسر الشربيني",
                    "gradeLabel" to "الصف الثاني الابتدائي لغات",
                    "studentLevel" to StudentLevel.BEGINNER.code,
                    "preferredLanguageMode" to LanguageMode.BILINGUAL.code,
                    "points" to 100L,
                    "completedLessonsCount" to 0L,
                    "streakDays" to 1L,
                    "reminderEnabled" to true,
                    "reminderHour" to 17L,
                    "reminderMinute" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.set(initialProfileMap).await()
            }

            val existingDocs = lessonsCol.get().await().documents.associateBy { it.id }
            val seedLessons = OfficialCurriculumSeed.getInitialLessons(uid)
            val batch = db.batch()
            var hasChanges = false
            for ((docId, lesson) in seedLessons) {
                val docRef = lessonsCol.document(docId)
                val existingDoc = existingDocs[docId]
                if (existingDoc == null) {
                    val payload = mapOf(
                        "userId" to uid,
                        "subjectId" to lesson.subjectId,
                        "subjectNameAr" to lesson.subjectNameAr,
                        "unitTitle" to lesson.unitTitle,
                        "lessonTitle" to lesson.lessonTitle,
                        "lessonOrder" to lesson.lessonOrder,
                        "officialContent" to lesson.officialContent,
                        "keyTerms" to lesson.keyTerms,
                        "sourceBookName" to lesson.sourceBookName,
                        "isCompleted" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                    hasChanges = true
                } else if (existingDoc.getString("officialContent") != lesson.officialContent ||
                    existingDoc.getString("lessonTitle") != lesson.lessonTitle
                ) {
                    val updatePayload = mapOf(
                        "subjectId" to lesson.subjectId,
                        "subjectNameAr" to lesson.subjectNameAr,
                        "unitTitle" to lesson.unitTitle,
                        "lessonTitle" to lesson.lessonTitle,
                        "lessonOrder" to lesson.lessonOrder,
                        "officialContent" to lesson.officialContent,
                        "keyTerms" to lesson.keyTerms,
                        "sourceBookName" to lesson.sourceBookName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.update(docRef, updatePayload)
                    hasChanges = true
                }
            }
            if (hasChanges) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun syncUploadedBooksToDatabase(): Result<Int> {
        val uid = requireUserId()
        return ensureInitialProfileAndCurriculum().map {
            OfficialCurriculumSeed.getInitialLessons(uid).size
        }
    }

    suspend fun updateStudentLevel(newLevel: StudentLevel): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("profile").document("main")
        return try {
            docRef.update(
                mapOf(
                    "studentLevel" to newLevel.code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateLanguageMode(newMode: LanguageMode): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("profile").document("main")
        return try {
            docRef.update(
                mapOf(
                    "preferredLanguageMode" to newMode.code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateReminderSettings(
        enabled: Boolean,
        hour: Int,
        minute: Int
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("profile").document("main")
        return try {
            docRef.update(
                mapOf(
                    "reminderEnabled" to enabled,
                    "reminderHour" to hour.toLong().coerceIn(0L, 23L),
                    "reminderMinute" to minute.toLong().coerceIn(0L, 59L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveLessonAiExplanation(
        lessonId: String,
        explanationText: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("lessons").document(lessonId)
        return try {
            docRef.update(
                mapOf(
                    "lastAiExplanation" to explanationText.take(14500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun completeLessonAndAwardPoints(
        lessonId: String,
        wasAlreadyCompleted: Boolean,
        currentPoints: Long,
        currentCompletedCount: Long,
        pointsReward: Long = 25L
    ): Result<Unit> {
        val uid = requireUserId()
        val lessonRef = db.collection("users").document(uid).collection("lessons").document(lessonId)
        val profileRef = db.collection("users").document(uid).collection("profile").document("main")

        return try {
            val batch = db.batch()
            batch.update(
                lessonRef,
                mapOf(
                    "isCompleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val newCompleted = if (wasAlreadyCompleted) currentCompletedCount else (currentCompletedCount + 1L)
            batch.update(
                profileRef,
                mapOf(
                    "points" to (currentPoints + pointsReward),
                    "completedLessonsCount" to newCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, lessonRef.path)
            Result.failure(e)
        }
    }

    suspend fun addGroundedLesson(
        subject: SubjectType,
        unitTitle: String,
        lessonTitle: String,
        lessonOrder: Long,
        officialContent: String,
        keyTerms: String,
        sourceBookName: String
    ): Result<String> {
        val uid = requireUserId()
        val cleanDocId = "${subject.id}_custom_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid).collection("lessons").document(cleanDocId)
        val payload = mapOf(
            "userId" to uid,
            "subjectId" to subject.id,
            "subjectNameAr" to subject.titleAr,
            "unitTitle" to unitTitle.trim().take(140).ifBlank { "الوحدة الأولى" },
            "lessonTitle" to lessonTitle.trim().take(140).ifBlank { "درس جديد" },
            "lessonOrder" to lessonOrder.coerceIn(1L, 500L),
            "officialContent" to officialContent.trim().take(14500),
            "keyTerms" to keyTerms.trim().take(1900).ifBlank { "مصطلحات الدرس الأساسية" },
            "sourceBookName" to sourceBookName.trim().take(190).ifBlank { "ملف PDF المنهج المدرسي" },
            "isCompleted" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(cleanDocId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeChatMessages(userId: String): Flow<List< com.example.data.model.ChatMessage>> {
        val path = "users/$userId/chat_messages"
        return db.collection("users")
            .document(userId)
            .collection("chat_messages")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    com.example.data.model.ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun addChatMessage(
        sender: String,
        messageText: String,
        subjectFilter: String,
        interactionMode: String
    ): Result<String> {
        val uid = requireUserId()
        val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val docRef = db.collection("users").document(uid).collection("chat_messages").document(messageId)
        val cleanSubject = if (subjectFilter in setOf("all", "arabic", "religion", "math", "connect", "connect_plus")) {
            subjectFilter
        } else {
            "all"
        }
        val cleanMode = if (interactionMode in setOf("chat", "quiz", "exam", "revision")) {
            interactionMode
        } else {
            "chat"
        }
        val payload = mapOf(
            "userId" to uid,
            "sender" to if (sender == "ai") "ai" else "user",
            "messageText" to messageText.trim().take(14500).ifBlank { "..." },
            "subjectFilter" to cleanSubject,
            "interactionMode" to cleanMode,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(messageId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearChatHistory(): Result<Unit> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("chat_messages")
        return try {
            val snap = colRef.get().await()
            if (!snap.isEmpty) {
                val batch = db.batch()
                for (doc in snap.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun awardPointsForChatInteraction(
        currentPoints: Long,
        pointsReward: Long = 15L
    ): Result<Unit> {
        val uid = requireUserId()
        val profileRef = db.collection("users").document(uid).collection("profile").document("main")
        return try {
            profileRef.update(
                mapOf(
                    "points" to (currentPoints + pointsReward),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path)
            Result.failure(e)
        }
    }
}
