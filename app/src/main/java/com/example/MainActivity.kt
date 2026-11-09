package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.BilingualAiTutorService
import com.example.data.model.LanguageMode
import com.example.data.model.StudentLevel
import com.example.data.model.StudentProfile
import com.example.data.repository.TutorRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CurriculumCenterScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.InteractiveClassroomScreen
import com.example.ui.screens.signOut
import com.example.ui.theme.ZeinTutorTheme
import com.example.ui.viewmodel.DataUiState
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.TutorViewModel
import com.example.util.AppDiagnosticsLogger
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 0: Install crash diagnostics catcher and log app launch
        AppDiagnosticsLogger.installCrashCatcher(this)
        AppDiagnosticsLogger.logInfo(
            tag = "MainActivity",
            code = "APP_STARTUP",
            message = "تشغيل تطبيق المدرس الخصوصي لزين الدين بنجاح."
        )

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // STEP 2: Initialize Compose UI with RTL Arabic Theme
        setContent {
            ZeinTutorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    var hasEnteredStudySession by rememberSaveable { mutableStateOf(user != null) }

    if (user == null && !hasEnteredStudySession) {
        AuthScreen(
            onAuthSuccess = {
                hasEnteredStudySession = true
            }
        )
    } else {
        val activeUserId = user?.uid ?: TutorRepository.DEFAULT_DIRECT_USER_ID
        key(activeUserId) {
            AuthenticatedTutorApp(
                currentUserId = activeUserId,
                onExitToSplash = {
                    hasEnteredStudySession = false
                }
            )
        }
    }
}

@Composable
fun AuthenticatedTutorApp(
    currentUserId: String,
    onExitToSplash: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val tutorViewModel: TutorViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                TutorViewModel(
                    repository = TutorRepository(db = db, fallbackUserId = currentUserId),
                    aiService = BilingualAiTutorService(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val currentTab by tutorViewModel.currentTab.collectAsStateWithLifecycle()
    val selectedSubject by tutorViewModel.selectedSubject.collectAsStateWithLifecycle()
    val activeLesson by tutorViewModel.activeLesson.collectAsStateWithLifecycle()
    val profileUiState by tutorViewModel.profileState.collectAsStateWithLifecycle()
    val lessonsUiState by tutorViewModel.lessonsState.collectAsStateWithLifecycle()
    val chalkboardState by tutorViewModel.chalkboardState.collectAsStateWithLifecycle()
    val teacherReplyState by tutorViewModel.teacherReplyState.collectAsStateWithLifecycle()
    val statusBanner by tutorViewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val chatMessagesUiState by tutorViewModel.chatMessagesState.collectAsStateWithLifecycle()
    val chatSubjectFilter by tutorViewModel.chatSubjectFilter.collectAsStateWithLifecycle()
    val chatInteractionMode by tutorViewModel.chatInteractionMode.collectAsStateWithLifecycle()
    val chatStreamingReply by tutorViewModel.chatStreamingReply.collectAsStateWithLifecycle()
    val isChatGenerating by tutorViewModel.isChatGenerating.collectAsStateWithLifecycle()

    // Handle system Back press from secondary tabs back to Home tab
    if (currentTab != MainTab.HOME_SUBJECTS) {
        BackHandler {
            tutorViewModel.selectTab(MainTab.HOME_SUBJECTS)
        }
    }

    val profile = (profileUiState as? DataUiState.Success)?.data ?: StudentProfile(userId = currentUserId)
    val lessons = (lessonsUiState as? DataUiState.Success)?.data.orEmpty()
    val chatMessages = (chatMessagesUiState as? DataUiState.Success)?.data.orEmpty()
    val badges = remember(profile.points, profile.completedLessonsCount) {
        tutorViewModel.getBadgesForPoints(profile.points, profile.completedLessonsCount)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp || isLandscape

        Row(modifier = Modifier.fillMaxSize()) {
            // In Landscape or Tablet wide screens, display ergonomic NavigationRail on the side
            if (isWideScreen) {
                NavigationRail(
                    modifier = Modifier.testTag("tablet_navigation_rail")
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = currentTab == MainTab.HOME_SUBJECTS,
                        onClick = { tutorViewModel.selectTab(MainTab.HOME_SUBJECTS) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "المواد والمكافآت") },
                        label = { Text("المواد") },
                        modifier = Modifier.testTag("rail_tab_home")
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.INTERACTIVE_CLASSROOM,
                        onClick = { tutorViewModel.selectTab(MainTab.INTERACTIVE_CLASSROOM) },
                        icon = { Icon(Icons.Default.School, contentDescription = "الفصل التفاعلي") },
                        label = { Text("السبورة") },
                        modifier = Modifier.testTag("rail_tab_classroom")
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.CHAT_TUTOR,
                        onClick = { tutorViewModel.selectTab(MainTab.CHAT_TUTOR) },
                        icon = { Icon(Icons.Default.Forum, contentDescription = "الشات والامتحانات") },
                        label = { Text("الشات") },
                        modifier = Modifier.testTag("rail_tab_chat")
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.CURRICULUM_AND_PYTHON,
                        onClick = { tutorViewModel.selectTab(MainTab.CURRICULUM_AND_PYTHON) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات والمناهج") },
                        label = { Text("الإعدادات") },
                        modifier = Modifier.testTag("rail_tab_curriculum")
                    )
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == MainTab.HOME_SUBJECTS,
                                onClick = { tutorViewModel.selectTab(MainTab.HOME_SUBJECTS) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "المواد والمكافآت") },
                                label = { Text("المواد") },
                                modifier = Modifier.testTag("nav_tab_home")
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.INTERACTIVE_CLASSROOM,
                                onClick = {
                                    tutorViewModel.selectTab(MainTab.INTERACTIVE_CLASSROOM)
                                },
                                icon = { Icon(Icons.Default.School, contentDescription = "الفصل التفاعلي") },
                                label = { Text("السبورة") },
                                modifier = Modifier.testTag("nav_tab_classroom")
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.CHAT_TUTOR,
                                onClick = { tutorViewModel.selectTab(MainTab.CHAT_TUTOR) },
                                icon = { Icon(Icons.Default.Forum, contentDescription = "الشات والامتحانات") },
                                label = { Text("الشات والامتحانات") },
                                modifier = Modifier.testTag("nav_tab_chat")
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.CURRICULUM_AND_PYTHON,
                                onClick = { tutorViewModel.selectTab(MainTab.CURRICULUM_AND_PYTHON) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات والمناهج") },
                                label = { Text("الإعدادات") },
                                modifier = Modifier.testTag("nav_tab_curriculum")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (statusBanner != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusBanner.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { tutorViewModel.clearStatusBanner() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق الإشعار"
                                    )
                                }
                            }
                        }
                    }

                    if (lessonsUiState is DataUiState.Loading && lessons.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = if (isWideScreen) 1100.dp else 600.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            when (currentTab) {
                                MainTab.HOME_SUBJECTS -> {
                                    HomeDashboardScreen(
                                        profile = profile,
                                        lessons = lessons,
                                        selectedSubject = selectedSubject,
                                        badges = badges,
                                        onSelectSubject = { tutorViewModel.selectSubject(it) },
                                        onOpenLesson = { tutorViewModel.openLessonInClassroom(it) },
                                        onChangeStudentLevel = { tutorViewModel.setStudentLevel(it) },
                                        onUpdateReminder = { enabled, h, m ->
                                            tutorViewModel.updateReminder(enabled, h, m)
                                        },
                                        onSignOut = {
                                            signOut(credentialManager, scope) {
                                                onExitToSplash()
                                            }
                                        }
                                    )
                                }

                                MainTab.INTERACTIVE_CLASSROOM -> {
                                    val subjectLessons = lessons.filter { it.subjectId == selectedSubject.id }
                                    InteractiveClassroomScreen(
                                        activeLesson = activeLesson,
                                        selectedSubject = selectedSubject,
                                        allLessons = lessons,
                                        allLessonsForSubject = subjectLessons,
                                        studentLevel = StudentLevel.fromCode(profile.studentLevel),
                                        languageMode = LanguageMode.fromCode(profile.preferredLanguageMode),
                                        chalkboardState = chalkboardState,
                                        teacherReplyState = teacherReplyState,
                                        onSelectSubject = { tutorViewModel.selectSubject(it) },
                                        onSelectLesson = { tutorViewModel.openLessonInClassroom(it) },
                                        onSwitchLanguageMode = { tutorViewModel.setLanguageMode(it) },
                                        onRegenerateExplanation = { tutorViewModel.explainCurrentLessonWithGemini() },
                                        onSendStudentQuestionOrAnswer = { tutorViewModel.askOrAnswerTeacher(it) }
                                    )
                                }

                                MainTab.CHAT_TUTOR -> {
                                    ChatScreen(
                                        messages = chatMessages,
                                        allLessons = lessons,
                                        selectedSubjectFilter = chatSubjectFilter,
                                        selectedInteractionMode = chatInteractionMode,
                                        streamingReplyText = chatStreamingReply,
                                        isGeneratingReply = isChatGenerating,
                                        studentLevel = StudentLevel.fromCode(profile.studentLevel),
                                        languageMode = LanguageMode.fromCode(profile.preferredLanguageMode),
                                        onSelectSubjectFilter = { tutorViewModel.setChatSubjectFilter(it) },
                                        onSelectInteractionMode = { tutorViewModel.setChatInteractionMode(it) },
                                        onSendQuery = { query, mode -> tutorViewModel.sendChatQuery(query, mode) },
                                        onClearHistory = { tutorViewModel.clearChatHistory() }
                                    )
                                }

                                MainTab.CURRICULUM_AND_PYTHON -> {
                                    CurriculumCenterScreen(
                                        currentUserId = currentUserId,
                                        profile = profile,
                                        allLessons = lessons,
                                        totalSyncedLessonsCount = lessons.size,
                                        onSyncUploadedBooks = { tutorViewModel.syncAllUploadedBooks() },
                                        onRunSystemDiagnostics = { tutorViewModel.runSelfDiagnosticsCheck() },
                                        onUpdateAcademicSettings = { grade, year, term, subjects, pitch, rate ->
                                            tutorViewModel.updateAcademicSettings(grade, year, term, subjects, pitch, rate)
                                        },
                                        onAddCurriculumPdfDocument = { subj, title, term, content, fileName, terms ->
                                            tutorViewModel.addCurriculumPdfDocument(subj, title, term, content, fileName, terms)
                                        },
                                        onDeleteLesson = { lessonId ->
                                            tutorViewModel.deleteLesson(lessonId)
                                        }
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

private fun lessonTitleFromForm(title: String): String = title
