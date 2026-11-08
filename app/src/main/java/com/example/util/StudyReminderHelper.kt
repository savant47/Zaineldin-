package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R

object StudyReminderHelper {
    private const val CHANNEL_ID = "zein_study_reminders"
    private const val NOTIFICATION_ID = 2027

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات مراجعة دروس زين الدين",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكير يومي مشجع للطفل زين الدين بمراجعة دروس الصف الثاني الابتدائي لغات"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendStudyReminderNotification(
        context: Context,
        studentName: String,
        nextSubjectTitle: String
    ): Boolean {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("حان وقت المغامرة التعليمية يا $studentName!")
            .setContentText("مدرّسك الودود بانتظارك على السبورة لمراجعة مادة ($nextSubjectTitle) وجمع +٢٥ نجمة جديدة!")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "أهلاً يا بطل الصف الثاني الابتدائي! مدرّسك الخصوصي جهّز لك السبورة الخضراء لمراجعة درس جديد في ($nextSubjectTitle) بالصوت التفاعلي وجمع النجوم الذهبية."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: Exception) {
            false
        }
    }
}
