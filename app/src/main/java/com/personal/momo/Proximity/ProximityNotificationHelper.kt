package com.personal.momo.Proximity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.personal.momo.R
import kotlin.math.roundToInt

object ProximityNotificationHelper {

    private const val CHANNEL_ID = "momo_proximity_alerts_channel"
    private const val NOTIFICATION_ID = 8002
    private const val DISMISS_DISTANCE_METERS = 250.0

    private var isAlertActive: Boolean = false

    fun evaluateAlert(context: Context, partnerName: String, distanceMeters: Double) {
        // Under 200m: Trigger initial alert or silent live-meter update
        if (distanceMeters <= 200.0) {
            val roundedDistance = distanceMeters.roundToInt()
            val titleText: String
            val messageText: String

            if (distanceMeters <= 50.0) {
                titleText = "$partnerName is very close (~$roundedDistance m)"
                messageText = "Radar Available • Tap to track with precision"
            } else {
                titleText = "$partnerName is nearby (~$roundedDistance m)"
                messageText = "Moving closer • Live distance tracking"
            }

            triggerNotification(context, titleText, messageText)
            isAlertActive = true
        } else if (distanceMeters > DISMISS_DISTANCE_METERS && isAlertActive) {
            // Exit Range (> 250m): Auto-dismiss notification from drawer
            dismissNotification(context)
            isAlertActive = false
        }
    }

    private fun triggerNotification(context: Context, titleText: String, messageText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proximity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up alert and live distance tracking when partner is nearby"
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

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText(messageText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun dismissNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
