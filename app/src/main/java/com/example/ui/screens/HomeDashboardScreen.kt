package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CurriculumLesson
import com.example.data.model.RewardBadge
import com.example.data.model.StudentLevel
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectType
import com.example.ui.theme.ChalkYellowAccent
import com.example.ui.theme.ChalkboardSurfaceBottom
import com.example.ui.theme.ChalkboardSurfaceTop
import com.example.util.StudyReminderHelper

@Composable
fun HomeDashboardScreen(
    profile: StudentProfile,
    lessons: List<CurriculumLesson>,
    selectedSubject: SubjectType,
    badges: List<RewardBadge>,
    onSelectSubject: (SubjectType) -> Unit,
    onOpenLesson: (CurriculumLesson) -> Unit,
    onChangeStudentLevel: (StudentLevel) -> Unit,
    onUpdateReminder: (Boolean, Int, Int) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var reminderNotice by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            StudyReminderHelper.sendStudyReminderNotification(
                context = context,
                studentName = "زين الدين",
                nextSubjectTitle = selectedSubject.titleAr
            )
            reminderNotice = "تم إرسال تنبيه تذكير مراجعة الدروس لزين الدين بنجاح!"
        } else {
            reminderNotice = "تم حفظ موعد التذكير في السحابة (فعّل إشعارات النظام لعرض التنبيه)."
        }
    }

    val subjectLessons = remember(lessons, selectedSubject) {
        lessons.filter { it.subjectId == selectedSubject.id }.sortedBy { it.lessonOrder }
    }

    val totalCompleted = lessons.count { it.isCompleted }
    val totalLessonsCount = lessons.size.coerceAtLeast(1)
    val overallProgress = (totalCompleted.toFloat() / totalLessonsCount.toFloat()).coerceIn(0f, 1f)
    val currentStudentLevel = StudentLevel.fromCode(profile.studentLevel)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome Splash / Hero Card with Teacher at Green Chalkboard & Zein at Desk
        item {
            WelcomeHeroBanner(
                profile = profile,
                onSignOut = onSignOut
            )
        }

        // 2. Gamification Rewards & Daily Progress Card
        item {
            GamificationRewardsCard(
                points = profile.points,
                streakDays = profile.streakDays,
                completedCount = totalCompleted,
                totalCount = lessons.size,
                progress = overallProgress,
                badges = badges
            )
        }

        // 3. Adaptive Student Level Selector (مبتدئ / متقدم)
        item {
            AdaptiveLevelSelectorCard(
                currentLevel = currentStudentLevel,
                onSelectLevel = onChangeStudentLevel
            )
        }

        // 4. 5 Official Subjects (2026/2027 - No "Discover / اكتشف")
        item {
            Column( verticalArrangement = Arrangement.spacedBy(10.dp) ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المواد الدراسية الخمس (منهج ٢٠٢٦/٢٠٢٧)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "بدون مادة اكتشف",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                val activeSubjects = SubjectType.entries.filter { profile.activeSubjectIds.contains(it.id) }.ifEmpty { SubjectType.entries }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.testTag("subjects_row")
                ) {
                    items(activeSubjects) { subject ->
                        val countForSubject = lessons.count { it.subjectId == subject.id }
                        val doneForSubject = lessons.count { it.subjectId == subject.id && it.isCompleted }
                        SubjectCardItem(
                            subject = subject,
                            isSelected = subject == selectedSubject,
                            completedCount = doneForSubject,
                            totalCount = countForSubject,
                            onClick = { onSelectSubject(subject) }
                        )
                    }
                }
            }
        }

        // 5. Lessons List for the Selected Subject (المادة -> الباب/الوحدة -> الدرس)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "دروس ${selectedSubject.titleAr}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedSubject.subtitleAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedSubject.isBilingualSubject) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "شرح ثنائي (EN + عربي)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        items(
            items = subjectLessons,
            key = { lesson -> lesson.id.ifBlank { "${lesson.subjectId}_${lesson.lessonOrder}_${lesson.lessonTitle}" } }
        ) { lesson ->
            LessonItemCard(
                lesson = lesson,
                accentColor = Color(selectedSubject.accentHex),
                onStartLesson = { onOpenLesson(lesson) }
            )
        }

        // 6. Cloud Sync & Custom Study Reminders Card
        item {
            StudyReminderAndSyncCard(
                reminderEnabled = profile.reminderEnabled,
                reminderHour = profile.reminderHour.toInt(),
                reminderMinute = profile.reminderMinute.toInt(),
                reminderNotice = reminderNotice,
                onToggleReminder = { enabled ->
                    onUpdateReminder(enabled, profile.reminderHour.toInt(), profile.reminderMinute.toInt())
                },
                onTriggerNow = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val sent = StudyReminderHelper.sendStudyReminderNotification(
                            context = context,
                            studentName = "زين الدين",
                            nextSubjectTitle = selectedSubject.titleAr
                        )
                        reminderNotice = if (sent) {
                            "تم إرسال إشعار تذكير المراجعة لزين الدين!"
                        } else {
                            "يرجى تفعيل الإشعارات من إعدادات الهاتف."
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun WelcomeHeroBanner(
    profile: StudentProfile,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("welcome_hero_banner"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ChalkboardSurfaceTop, ChalkboardSurfaceBottom)
                    )
                )
        ) {
            Box {
                Image(
                    painter = painterResource(id = R.drawable.img_classroom_hero_1794083054692),
                    contentDescription = "المدرس الودود أمام السبورة الخضراء والطفل زين الدين على مكتبه وبيده كراسة وقلم",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    color = Color(0xCC0D3321),
                    shape = RoundedCornerShape(bottomEnd = 16.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = ChalkYellowAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "متصل بالسحابة • مرجع موثّق ١٠٠٪",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تطبيق المدرس الخصوصي",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChalkYellowAccent
                        )
                        Text(
                            text = profile.studentName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = profile.gradeLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD1FAE5)
                        )
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("sign_out_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("خروج", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun GamificationRewardsCard(
    points: Long,
    streakDays: Long,
    completedCount: Int,
    totalCount: Int,
    progress: Float,
    badges: List<RewardBadge>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gamification_rewards_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "صندوق مكافآت زين الدين",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اجمع النجوم الذهبية بإكمال الدروس والإجابة عن سؤال السبورة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatHighlightBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Star,
                    iconTint = Color(0xFFD97706),
                    valueText = "$points نجمة",
                    labelText = "رصيد النقاط"
                )
                StatHighlightBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = Color(0xFFDC2626),
                    valueText = "$streakDays أيام",
                    labelText = "حماس متواصل"
                )
                StatHighlightBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconTint = Color(0xFF0F766E),
                    valueText = "$completedCount / $totalCount",
                    labelText = "دروس مكتملة"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تقدم المنهج الدراسي العام",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(badges, key = { it.id }) { badge ->
                    Surface(
                        color = if (badge.isUnlocked) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (badge.isUnlocked) Icons.Default.Verified else Icons.Default.Star,
                                contentDescription = null,
                                tint = if (badge.isUnlocked) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = badge.titleAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (badge.isUnlocked) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = if (badge.isUnlocked) "مفتوح ✓" else "يتطلب ${badge.requiredPoints} نجمة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatHighlightBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    valueText: String,
    labelText: String
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdaptiveLevelSelectorCard(
    currentLevel: StudentLevel,
    onSelectLevel: (StudentLevel) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adaptive_level_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تكيّف الشرح الذكي مع مستوى زين الدين",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudentLevel.entries.forEach { level ->
                    val selected = level == currentLevel
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectLevel(level) },
                        label = {
                            Text(
                                text = "مستوى ${level.labelAr}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("level_chip_${level.code}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Text(
                text = currentLevel.descriptionAr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubjectCardItem(
    subject: SubjectType,
    isSelected: Boolean,
    completedCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    val accent = Color(subject.accentHex)
    val icon = when (subject) {
        SubjectType.ARABIC -> Icons.AutoMirrored.Filled.MenuBook
        SubjectType.RELIGION -> Icons.Default.Mosque
        SubjectType.MATH -> Icons.Default.Calculate
        SubjectType.CONNECT -> Icons.Default.Translate
        SubjectType.CONNECT_PLUS -> Icons.Default.School
    }

    Surface(
        modifier = Modifier
            .width(195.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("subject_card_${subject.id}"),
        color = if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.18f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    color = if (subject.isBilingualSubject) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (subject.isBilingualSubject) "EN + عربي" else "عربي فصحى",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = subject.titleAr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = "المكتمل: $completedCount من $totalCount دروس",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LessonItemCard(
    lesson: CurriculumLesson,
    accentColor: Color,
    onStartLesson: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onStartLesson)
            .testTag("lesson_card_${lesson.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (lesson.isCompleted) Color(0xFFD1FAE5) else accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (lesson.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "مكتمل",
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Text(
                            text = "${lesson.lessonOrder}",
                            style = MaterialTheme.typography.titleLarge,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.unitTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
                Text(
                    text = lesson.lessonTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "المصطلحات: ${lesson.keyTerms}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onStartLesson,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.testTag("start_lesson_btn_${lesson.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "ابدأ الشرح",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("السبورة", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun StudyReminderAndSyncCard(
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    reminderNotice: String?,
    onToggleReminder: (Boolean) -> Unit,
    onTriggerNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study_reminder_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "التنبيهات اليومية والمزامنة السحابية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "موعد التذكير اليومي لزين الدين: ${reminderHour}:${reminderMinute.toString().padStart(2, '0')} مساءً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = onToggleReminder,
                    modifier = Modifier.testTag("reminder_switch")
                )
            }

            if (reminderNotice != null) {
                Text(
                    text = reminderNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedButton(
                onClick = onTriggerNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("send_test_reminder_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال تنبيه تشجيعي لزين الدين الآن")
            }
        }
    }
}
