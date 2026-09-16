package com.personal.momo.UI_Screens.Notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.personal.momo.Cache.CacheManager
import com.personal.momo.MainActivity
import com.personal.momo.R
import java.time.LocalDate
import java.util.Calendar

class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // Guarantee local cache availability for background processing
        CacheManager.init(context)

        // Always schedule next day's 7:00 AM alarm first
        scheduleDaily7AmAlarm(context)

        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        // Trigger notifications if permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        pushDailyNotifications(context)
    }

    private fun pushDailyNotifications(context: Context) {
        ensureNotificationChannel(context)

        val today = LocalDate.now()
        val allEvents = CacheManager.eventsFlow.value
        val loggedPeriodDates = CacheManager.periodDatesFlow.value

        val activeNotifications = MomoNotificationEngine.computeNotifications(
            today = today,
            allEvents = allEvents,
            loggedPeriodDates = loggedPeriodDates
        )

        if (activeNotifications.isEmpty()) return

        val notificationManager = NotificationManagerCompat.from(context)

        activeNotifications.forEach { item ->
            val largeIconBitmap = when (item) {
                is MomoNotificationItem.EventNotification -> {
                    createCircularDateBadge(item.dayNumber, item.isMilestone)
                }
                is MomoNotificationItem.PeriodNotification -> {
                    createCircularDateBadge(item.dayNumber, isMilestone = false)
                }
            }

            // Tap notification to open app directly into NotificationsScreen
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                item.id.hashCode(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(largeIconBitmap)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            when (item) {
                is MomoNotificationItem.EventNotification -> {
                    val titleText = "${item.headline} • MOMO"
                    val contentLine = if (item.countdownTag.isNotBlank()) {
                        "${item.countdownTag} • ${item.event.title}"
                    } else {
                        item.event.title
                    }

                    builder.setContentTitle(titleText)
                    builder.setContentText(contentLine)

                    if (item.event.description.isNotBlank()) {
                        val bigTextStyle = NotificationCompat.BigTextStyle()
                            .setBigContentTitle(titleText)
                            .bigText("$contentLine\n\n${item.event.description}")
                        builder.setStyle(bigTextStyle)
                    }
                }

                is MomoNotificationItem.PeriodNotification -> {
                    val titleText = "Hpy Bday • MOMO"
                    val contentLine = "${item.alertMessage} (${item.formattedDate})"

                    builder.setContentTitle(titleText)
                    builder.setContentText(contentLine)
                }
            }

            runCatching {
                notificationManager.notify(item.id.hashCode(), builder.build())
            }
        }
    }

    private fun createCircularDateBadge(dayNumber: Int, isMilestone: Boolean): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = if (isMilestone) {
                LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    intArrayOf(
                        AndroidColor.parseColor("#FF4365"),
                        AndroidColor.parseColor("#9B51E0")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            } else {
                LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    intArrayOf(
                        AndroidColor.parseColor("#FF3366"),
                        AndroidColor.parseColor("#FF6B8B")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, circlePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val text = dayNumber.toString()
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val yOffset = radius - textBounds.exactCenterY()

        canvas.drawText(text, radius, yOffset, textPaint)
        return bitmap
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily morning alerts for milestone memories and cycle updates"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "momo_daily_notifications"
        const val CHANNEL_NAME = "Momo Alerts"
        const val EXTRA_OPEN_NOTIFICATIONS = "extra_open_notifications"
        private const val ALARM_REQUEST_CODE = 7001
        private const val ACTION_TRIGGER_7AM = "com.personal.momo.ACTION_TRIGGER_7AM"

        fun scheduleDaily7AmAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val targetCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If 7:00 AM has already passed today, target tomorrow 7:00 AM
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_7AM
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCalendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCalendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetCalendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}
