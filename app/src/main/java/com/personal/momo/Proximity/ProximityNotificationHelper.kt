package com.personal.momo.Proximity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.personal.momo.R
import kotlin.math.roundToInt

object ProximityNotificationHelper {

    private const val CHANNEL_ID = "momo_proximity_alerts_channel"
    private const val NOTIFICATION_ID = 8002
    private const val COOLDOWN_MILLIS = 15 * 60 * 1000L // 15 Minutes debounce
    private const val RESET_DISTANCE_METERS = 350.0

    private var lastAlertTimestamp: Long = 0L
    private var isCooldownActive: Boolean = false

    fun evaluateAlert(context: Context, partnerName: String, distanceMeters: Double) {
        val currentTime = System.currentTimeMillis()

        // Hysteresis Reset: Reset cooldown if distance exceeds 350m AND 15 mins have passed
        if (distanceMeters > RESET_DISTANCE_METERS && (currentTime - lastAlertTimestamp > COOLDOWN_MILLIS)) {
            isCooldownActive = false
        }

        // 200m Notification Alert Boundary
        if (distanceMeters <= 200.0) {
            if (!isCooldownActive && (currentTime - lastAlertTimestamp > COOLDOWN_MILLIS)) {
                val roundedDistance = distanceMeters.roundToInt()
                triggerNotification(context, "$partnerName is nearby (~$roundedDistance m)")
                lastAlertTimestamp = currentTime
                isCooldownActive = true
            }
        }
    }

    private fun triggerNotification(context: Context, titleText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proximity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up alert when partner is nearby"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Android 13+ runtime permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText("")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
