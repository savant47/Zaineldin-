package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.LanguageMode
import com.example.data.model.StudentLevel
import com.example.data.model.SubjectType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config
import java.util.UUID

@Config(sdk = [36])
class TutorRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureInitialProfileAndCurriculum_authenticatedUser_seedsProfileAnd5Subjects() = runBlocking {
        val uniqueEmail = "zein_${UUID.randomUUID().toString().take(8)}@school.edu.eg"
        val zeinUid = signInTestUser(uniqueEmail)
        val repository = TutorRepository(firestore, auth)

        val initResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureInitialProfileAndCurriculum()
        }
        assertTrue("Init failed: ${initResult.exceptionOrNull()}", initResult.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(zeinUid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("زين الدين آسر الشربيني", profile?.studentName)

        val lessonsResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getLessonsOnce(zeinUid)
        }
        assertTrue(lessonsResult.isSuccess)
        val lessons = lessonsResult.getOrThrow()
        assertEquals(24, lessons.size)
        val subjectIds = lessons.map { it.subjectId }.toSet()
        assertTrue(subjectIds.contains(SubjectType.ARABIC.id))
        assertTrue(subjectIds.contains(SubjectType.RELIGION.id))
        assertTrue(subjectIds.contains(SubjectType.MATH.id))
        assertTrue(subjectIds.contains(SubjectType.CONNECT.id))
        assertTrue(subjectIds.contains(SubjectType.CONNECT_PLUS.id))
        assertTrue(!subjectIds.contains("discover"))

        val chatRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addChatMessage(
                sender = "user",
                messageText = "اعمل لي كويز سريع في الماث",
                subjectFilter = "math",
                interactionMode = "quiz"
            )
        }
        assertTrue("Chat message creation failed: ${chatRes.exceptionOrNull()}", chatRes.isSuccess)
    }

    @Test
    fun updateLevelAndLanguage_authenticatedOwner_succeedsAndSyncs() = runBlocking {
        val uniqueEmail = "zein_lvl_${UUID.randomUUID().toString().take(8)}@school.edu.eg"
        val zeinUid = signInTestUser(uniqueEmail)
        val repository = TutorRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureInitialProfileAndCurriculum().getOrThrow()
        }

        val levelRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateStudentLevel(StudentLevel.ADVANCED)
        }
        assertTrue("Update level failed: ${levelRes.exceptionOrNull()}", levelRes.isSuccess)

        val langRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateLanguageMode(LanguageMode.ARABIC)
        }
        assertTrue("Update lang failed: ${langRes.exceptionOrNull()}", langRes.isSuccess)

        val updatedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(zeinUid).first { it?.studentLevel == StudentLevel.ADVANCED.code }
        }
        assertEquals(LanguageMode.ARABIC.code, updatedProfile?.preferredLanguageMode)
    }

    @Test
    fun addGroundedLesson_andCrossUserAccess_enforcesIsolation() = runBlocking {
        val zeinUid = signInTestUser("zein_iso_${UUID.randomUUID().toString().take(8)}@school.edu.eg")
        val zeinRepo = TutorRepository(firestore, auth)

        val lessonId = withTimeout(DEFAULT_TIMEOUT_MS) {
            zeinRepo.addGroundedLesson(
                subject = SubjectType.MATH,
                unitTitle = "Unit 3: Addition",
                lessonTitle = "Lesson 1: Regrouping Ones",
                lessonOrder = 10L,
                officialContent = "When adding 2-digit numbers, if the ones add up to 10 or more, regroup 10 ones as 1 ten.",
                keyTerms = "Regroup (إعادة التجميع), Ones (الآحاد), Tens (العشرات)",
                sourceBookName = "Math Grade 2 PDF"
            ).getOrThrow()
        }
        assertTrue(lessonId.isNotBlank())

        signInTestUser("other_${UUID.randomUUID().toString().take(8)}@school.edu.eg")
        val otherRepo = TutorRepository(firestore, auth)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            otherRepo.getLessonById(zeinUid, lessonId)
        }
        assertTrue(crossReadResult.isFailure)
        val fsError = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    @Test
    fun observeLessons_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        val zeinUid = signInTestUser("zein_unauth_${UUID.randomUUID().toString().take(8)}@school.edu.eg")
        signOutTestUser()
        val repository = TutorRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeLessons(zeinUid).first()
            }
            fail("Expected unauthenticated observeLessons to fail")
        } catch (e: Exception) {
            val fsError = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
