package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatInteractionMode
import com.example.data.model.ChatMessage
import com.example.data.model.CurriculumLesson
import com.example.data.model.LanguageMode
import com.example.data.model.StudentLevel
import com.example.data.model.SubjectType
import com.example.util.OnDeviceVoiceManager

/**
 * شاشة الشات والامتحانات الذكية (ChatScreen)
 * متصلة بقاعدة بيانات الكتب المرفوعة بالكامل (٢٤ وحدة شاملة من كتب الوزارة والمعاصر ٢٠٢٦/٢٠٢٧)،
 * وتتيح للطالب "زين الدين" السؤال في أي وقت أو طلب امتحانات شاملة، كويزات سريعة، أو مراجعات مختصرة.
 */
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    allLessons: List<CurriculumLesson>,
    selectedSubjectFilter: String,
    selectedInteractionMode: ChatInteractionMode,
    streamingReplyText: String?,
    isGeneratingReply: Boolean,
    studentLevel: StudentLevel,
    languageMode: LanguageMode,
    onSelectSubjectFilter: (String) -> Unit,
    onSelectInteractionMode: (ChatInteractionMode) -> Unit,
    onSendQuery: (String, ChatInteractionMode) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val voiceManager = remember { OnDeviceVoiceManager(context) }
    val isSpeaking by voiceManager.isSpeaking.collectAsStateWithLifecycle()
    val isListening by voiceManager.isListening.collectAsStateWithLifecycle()
    val voiceStatus by voiceManager.voiceStatusMessage.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.release()
        }
    }

    var queryInput by rememberSaveable { mutableStateOf("") }
    var showAuditReportCard by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filteredLessonsForChat = remember(allLessons, selectedSubjectFilter) {
        if (selectedSubjectFilter == "all") {
            allLessons
        } else {
            allLessons.filter { it.subjectId == selectedSubjectFilter }.ifEmpty { allLessons }
        }
    }
    val filteredBooksCount = filteredLessonsForChat.size

    val preferEnglishVoice = remember(selectedSubjectFilter, languageMode) {
        val isBilingualSubj = selectedSubjectFilter in setOf("math", "connect", "connect_plus")
        isBilingualSubj && languageMode == LanguageMode.ENGLISH
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceManager.startListening(recognizeEnglish = preferEnglishVoice) { transcript ->
                queryInput = transcript
            }
        }
    }

    // التمرير التلقائي لآخر رسالة عند وصول رد جديد أو أثناء التوليد
    LaunchedEffect(messages.size, streamingReplyText) {
        val totalItems = messages.size + (if (streamingReplyText != null || isGeneratingReply) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("chat_screen_root")
    ) {
        // 1. شريط رأس الشات المتصل بقاعدة بيانات الكتب المرفوعة (٢٤ وحدة)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "المدرس الذكي",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "شات المدرس الخصوصي والامتحانات الذكية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "متصل بـ $filteredBooksCount وحدة دراسية • مستوى (${studentLevel.labelAr})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = onClearHistory,
                                modifier = Modifier.testTag("clear_chat_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "مسح سجل المحادثة",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. شريط اختيار المادة الدراسية (يحدد المادة ويعرض مواضيعها فقط بدون تشغيل امتحان تلقائي)
                val subjectFilters = listOf(
                    "all" to "كل المواد ($filteredBooksCount)",
                    SubjectType.MATH.id to "Math - الرياضيات",
                    SubjectType.CONNECT.id to "English Connect",
                    SubjectType.CONNECT_PLUS.id to "Connect Plus لغات",
                    SubjectType.ARABIC.id to "اللغة العربية",
                    SubjectType.RELIGION.id to "التربية الدينية"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjectFilters.forEach { (filterId, label) ->
                        val isSelected = selectedSubjectFilter == filterId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectSubjectFilter(filterId)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("chat_filter_chip_$filterId")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2-ب. شريط المواضيع والدروس التفاعلية للمادة المختارة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المواضيع:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    filteredLessonsForChat.forEach { lesson ->
                        val cleanTopic = cleanChatTopicLabel(lesson.lessonTitle)
                        AssistChip(
                            onClick = {
                                val actionPrompt = when (selectedInteractionMode) {
                                    ChatInteractionMode.QUIZ ->
                                        "اعمل لي كويز تفاعلي سريع في موضوع: $cleanTopic"
                                    ChatInteractionMode.EXAM ->
                                        "اعمل لي امتحاناً شاملاً بالدرجات في موضوع: $cleanTopic"
                                    ChatInteractionMode.REVISION ->
                                        "اعمل لي مراجعة مختصرة وملخصاً شاملاً لموضوع: $cleanTopic"
                                    ChatInteractionMode.CHAT ->
                                        "اشرح لي بالتفصيل والأمثلة موضوع: $cleanTopic"
                                }
                                onSendQuery(actionPrompt, selectedInteractionMode)
                            },
                            label = {
                                Text(
                                    text = cleanTopic.take(32),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("chat_topic_chip_${lesson.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. أزرار الأنماط السريعة (امتحان شامل / كويز سريع / مراجعة مختصرة / سؤال وشرح حر)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionModeChip(
                        mode = ChatInteractionMode.QUIZ,
                        selectedMode = selectedInteractionMode,
                        icon = { Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onSelect = { mode ->
                            onSelectInteractionMode(mode)
                            onSendQuery(mode.defaultPromptAr, mode)
                        },
                        testTag = "quick_action_quiz_button"
                    )
                    QuickActionModeChip(
                        mode = ChatInteractionMode.EXAM,
                        selectedMode = selectedInteractionMode,
                        icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onSelect = { mode ->
                            onSelectInteractionMode(mode)
                            onSendQuery(mode.defaultPromptAr, mode)
                        },
                        testTag = "quick_action_exam_button"
                    )
                    QuickActionModeChip(
                        mode = ChatInteractionMode.REVISION,
                        selectedMode = selectedInteractionMode,
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onSelect = { mode ->
                            onSelectInteractionMode(mode)
                            onSendQuery(mode.defaultPromptAr, mode)
                        },
                        testTag = "quick_action_revision_button"
                    )
                    QuickActionModeChip(
                        mode = ChatInteractionMode.CHAT,
                        selectedMode = selectedInteractionMode,
                        icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onSelect = { mode ->
                            onSelectInteractionMode(mode)
                            onSendQuery(mode.defaultPromptAr, mode)
                        },
                        testTag = "quick_action_chat_button"
                    )
                }
            }
        }

        // 4. قائمة سجل المحادثة (Conversation History LazyColumn)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && streamingReplyText == null && !isGeneratingReply) {
                item {
                    EmptyChatWelcomeCard(
                        totalUnitsCount = allLessons.size,
                        onTriggerQuickPrompt = { promptText, mode ->
                            onSelectInteractionMode(mode)
                            onSendQuery(promptText, mode)
                        }
                    )
                }
            }

            items(
                items = messages,
                key = { msg -> msg.id.ifBlank { "${msg.sender}_${msg.createdAt?.seconds ?: 0L}_${msg.messageText.hashCode()}" } }
            ) { message ->
                ChatMessageBubble(
                    message = message,
                    isSpeaking = isSpeaking,
                    onSpeakMessage = { textToSpeak ->
                        if (isSpeaking) {
                            voiceManager.stopSpeaking()
                        } else {
                            voiceManager.speakSingleText(
                                text = textToSpeak,
                                preferEnglish = preferEnglishVoice
                            )
                        }
                    },
                    onQuickFollowUp = { promptText, mode ->
                        onSelectInteractionMode(mode)
                        onSendQuery(promptText, mode)
                    }
                )
            }

            if (isGeneratingReply || !streamingReplyText.isNullOrBlank()) {
                item(key = "streaming_ai_bubble") {
                    StreamingAiBubble(
                        streamingText = streamingReplyText.orEmpty(),
                        mode = selectedInteractionMode
                    )
                }
            }
        }

        // 5. شريط حالة الصوت المحلي + حقل إدخال استفسارات الطالب (Text Input Field + Mic + Send)
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = voiceStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSpeaking) {
                        OutlinedButton(
                            onClick = { voiceManager.stopSpeaking() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "إيقاف القراءة",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إيقاف الصوت", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // زر المايكروفون المحلي المجاني (On-Device STT)
                    FilledIconButton(
                        onClick = {
                            if (isListening) {
                                voiceManager.stopListening()
                            } else {
                                val hasMicPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMicPerm) {
                                    voiceManager.startListening(recognizeEnglish = preferEnglishVoice) { transcript ->
                                        queryInput = transcript
                                    }
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isListening) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "تحدث مع المدرس بالمايكروفون",
                            tint = if (isListening) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }

                    // حقل إدخال النص (User Query TextField)
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { queryInput = it },
                        placeholder = {
                            Text(
                                text = "اسأل في أي كتاب، أو اطلب امتحاناً / كويز / مراجعة يا زين...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    // زر الإرسال (Send Query Button)
                    FilledIconButton(
                        onClick = {
                            val trimmed = queryInput.trim()
                            if (trimmed.isNotEmpty() && !isGeneratingReply) {
                                onSendQuery(trimmed, selectedInteractionMode)
                                queryInput = ""
                            }
                        },
                        enabled = queryInput.trim().isNotEmpty() && !isGeneratingReply,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال السؤال للمدرس الذكي",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionModeChip(
    mode: ChatInteractionMode,
    selectedMode: ChatInteractionMode,
    icon: @Composable () -> Unit,
    onSelect: (ChatInteractionMode) -> Unit,
    testTag: String
) {
    val isSelected = mode == selectedMode
    AssistChip(
        onClick = { onSelect(mode) },
        label = {
            Text(
                text = mode.labelAr,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = icon,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            labelColor = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
private fun EmptyChatWelcomeCard(
    totalUnitsCount: Int,
    onTriggerQuickPrompt: (String, ChatInteractionMode) -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_chat_welcome_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "أهلاً بك يا بطلنا زين الدين في شات المدرس الخصوصي الشامل! 🌟",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "اختر المادة من الأعلى، ثم اطلب شرح أي درس أو كويز تفاعلي أو امتحان شامل، وعندما تجيب عن الأسئلة سيقوم المدرس بتصحيحها لك وإعطائك درجتك والحل الصحيح:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val starterPrompts = listOf(
                Triple(
                    "🎯 كويز سريع في الماث (Place Value & Time)",
                    "اعمل لي كويز تفاعلي سريع في مادة الماث عن القيمة المكانية (Place Value vs Value) والوقت (Time).",
                    ChatInteractionMode.QUIZ
                ),
                Triple(
                    "📝 امتحان إنجليزي شامل (Vocabulary & Grammar)",
                    "اعمل لي امتحاناً شاملاً بالدرجات في اللغة الإنجليزية يشمل Vocabulary و Grammar.",
                    ChatInteractionMode.EXAM
                ),
                Triple(
                    "⚡ مراجعة مختصرة لقواعد وأساليب اللغة العربية",
                    "اعمل لي مراجعة مختصرة وملخصاً جميلاً لأهم الأساليب والتراكيب في اللغة العربية.",
                    ChatInteractionMode.REVISION
                ),
                Triple(
                    "🌙 كويز في التربية الدينية الإسلامية",
                    "اعمل لي كويز تفاعلي سريع في سور القرآن الكريم وآداب المسلم.",
                    ChatInteractionMode.QUIZ
                )
            )

            starterPrompts.forEachIndexed { index, (title, prompt, mode) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTriggerQuickPrompt(prompt, mode) }
                        .testTag("starter_prompt_card_$index")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeakMessage: (String) -> Unit,
    onQuickFollowUp: (String, ChatInteractionMode) -> Unit = { _, _ -> }
) {
    val isUser = message.sender == "user"
    val modeLabel = ChatInteractionMode.fromCode(message.interactionMode).labelAr

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 4.dp else 18.dp,
                bottomEnd = if (isUser) 18.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isUser) {
                            "زين الدين آسر الشربيني 👦"
                        } else {
                            "المدرس الذكي 👨‍🏫 • $modeLabel"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )

                    if (!isUser) {
                        IconButton(
                            onClick = { onSpeakMessage(message.messageText) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isSpeaking) "إيقاف القراءة الصوتية" else "استمع للشرح صوتياً",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                onQuickFollowUp(
                                    "اعمل لي كويز تفاعلي سريع جديد بأسئلة مختلفة في هذه المادة",
                                    ChatInteractionMode.QUIZ
                                )
                            },
                            label = { Text("🎯 كويز جديد", style = MaterialTheme.typography.labelSmall) }
                        )
                        AssistChip(
                            onClick = {
                                onQuickFollowUp(
                                    "اعمل لي امتحاناً شاملاً بالدرجات (٢٠ درجة) في هذه المادة",
                                    ChatInteractionMode.EXAM
                                )
                            },
                            label = { Text("📝 امتحان ٢٠ درجة", style = MaterialTheme.typography.labelSmall) }
                        )
                        AssistChip(
                            onClick = {
                                onQuickFollowUp(
                                    "اعمل لي مراجعة مختصرة وملخصاً لأهم القواعد والمصطلحات",
                                    ChatInteractionMode.REVISION
                                )
                            },
                            label = { Text("⚡ مراجعة مختصرة", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingAiBubble(
    streamingText: String,
    mode: ChatInteractionMode
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .testTag("chat_streaming_bubble")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "يقوم المدرس الآن بتحضير (${mode.labelAr}) من كتب المنهج المرفوعة...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                if (streamingText.isNotBlank()) {
                    Text(
                        text = streamingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

private fun cleanChatTopicLabel(rawTitle: String): String {
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

