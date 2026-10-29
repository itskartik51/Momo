package com.personal.momo.UI_Screens.Call

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.PathParser
import com.personal.momo.MainActivity
import com.personal.momo.R

class CallService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var callerDisplayName: String = "Momo"
    private var callStartTimeRealtime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireCpuWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                callerDisplayName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Momo"
                val connectedTs = intent.getLongExtra(EXTRA_START_TIMESTAMP, 0L)
                if (callStartTimeRealtime == 0L) {
                    val timeSinceConnect = if (connectedTs > 0L) System.currentTimeMillis() - connectedTs else 0L
                    callStartTimeRealtime = SystemClock.elapsedRealtime() - timeSinceConnect
                }

                createNotificationChannel()
                val notification = buildCallNotification()

                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                } else {
                    0
                }

                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    serviceType
                )
            }
            ACTION_TOGGLE_MUTE -> {
                CallManager.toggleMute(this)
                updateNotification()
            }
            ACTION_TOGGLE_SPEAKER -> {
                AudioMan.toggleSpeaker(this)
                updateNotification()
            }
            ACTION_END_CALL -> {
                CallManager.endCall(this)
                stopForegroundAndSelf()
            }
            ACTION_UPDATE -> {
                updateNotification()
            }
            ACTION_STOP -> {
                stopForegroundAndSelf()
            }
        }
        return START_STICKY
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = buildCallNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildCallNotification(): Notification {
        val remoteViews = RemoteViews(packageName, R.layout.call).apply {
            setTextViewText(R.id.tv_notification_sub, "MOMO")
            setTextViewText(R.id.tv_caller_name, callerDisplayName)

            // Hardware-driven chronometer (stays in perfect sync without manual wake timers)
            setChronometer(R.id.chrono_call_timer, callStartTimeRealtime, null, true)

            val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val neutralIconColor = if (isDark) Color.WHITE else Color.parseColor("#212121")

            // 1. Dynamic Official Material Mic Icon
            val isMuted = CallManager.isMuted
            val micPath = if (isMuted) PATH_MIC_OFF else PATH_MIC
            setImageViewBitmap(R.id.btn_capsule_mic, createIconBitmap(micPath, neutralIconColor))

            // 2. Dynamic Official Material Audio Route Icon
            val routePath = when {
                AudioMan.isWiredConnected -> PATH_HEADSET
                AudioMan.isBluetoothConnected -> PATH_BLUETOOTH
                AudioMan.isSpeakerOn -> PATH_SPEAKER
                else -> PATH_SPEAKER
            }
            setImageViewBitmap(R.id.btn_capsule_speaker, createIconBitmap(routePath, neutralIconColor))

            // 3. Dynamic Official Material Call End Icon (Pure White over Red Background)
            setImageViewBitmap(R.id.btn_capsule_end, createIconBitmap(PATH_CALL_END, Color.WHITE))

            // Button Click Pending Intents
            setOnClickPendingIntent(R.id.btn_capsule_mic, createServicePendingIntent(ACTION_TOGGLE_MUTE, 401))
            setOnClickPendingIntent(R.id.btn_capsule_speaker, createServicePendingIntent(ACTION_TOGGLE_SPEAKER, 402))
            setOnClickPendingIntent(R.id.btn_capsule_end, createServicePendingIntent(ACTION_END_CALL, 403))
        }

        // Tapping card body opens MainActivity directly to ActiveCallScreen
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            400,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createServicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, CallService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ongoing Voice Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent in-call notification with live timer and quick controls"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireCpuWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "momo:call_service_wakelock"
            )
            wakeLock?.acquire()
        } catch (_: Exception) {}
    }

    private fun releaseCpuWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun stopForegroundAndSelf() {
        releaseCpuWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseCpuWakeLock()
        super.onDestroy()
    }

    private fun createIconBitmap(pathData: String, fillColor: Int, sizeDp: Int = 24): Bitmap {
        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val path = PathParser.createPathFromPathData(pathData)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        val matrix = Matrix().apply {
            postScale(sizePx / 24f, sizePx / 24f)
        }
        path.transform(matrix)
        canvas.drawPath(path, paint)
        return bitmap
    }

    companion object {
        const val CHANNEL_ID = "momo_ongoing_call_channel"
        const val NOTIFICATION_ID = 9119

        const val ACTION_START = "com.personal.momo.action.START_CALL_SERVICE"
        const val ACTION_STOP = "com.personal.momo.action.STOP_CALL_SERVICE"
        const val ACTION_UPDATE = "com.personal.momo.action.UPDATE_CALL_SERVICE"
        const val ACTION_TOGGLE_MUTE = "com.personal.momo.action.TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.personal.momo.action.TOGGLE_SPEAKER"
        const val ACTION_END_CALL = "com.personal.momo.action.END_CALL"

        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_START_TIMESTAMP = "extra_start_timestamp"

        // Official Google Material Vector Paths (24x24 standard viewport)
        private const val PATH_MIC = "M12,14c1.66,0 3,-1.34 3,-3V5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11H5c0,3.41 2.72,6.23 6,6.72V21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z"
        private const val PATH_MIC_OFF = "M19,11h-1.7c0,0.74 -0.16,1.43 -0.43,2.05l1.23,1.23c0.56,-0.98 0.9,-2.09 0.9,-3.28zm-4.02,0.17L15,11.02V5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v0.18L10.82,7l4.16,4.17zM4.27,3L3,4.27l6.01,6.01V11c0,1.66 1.33,3 2.99,3 0.22,0 0.44,-0.03 0.65,-0.08l1.66,1.66c-0.71,0.33 -1.5,0.52 -2.31,0.52 -2.76,0 -5.3,-2.1 -5.3,-5.1H5c0,3.41 2.72,6.23 6,6.72V21h2v-3.28c0.91,-0.13 1.77,-0.45 2.54,-0.9L19.73,21 21,19.73 4.27,3z"
        private const val PATH_SPEAKER = "M3,9v6h4l5,5V4L7,9H3zm13.5,3c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z"
        private const val PATH_HEADSET = "M12,1a9,9 0 0,0 -9,9v7c0,1.66 1.34,3 3,3h3v-8H5v-2c0,-3.87 3.13,-7 7,-7s7,3.13 7,7v2h-4v8h3c1.66,0 3,-1.34 3,-3v-7a9,9 0 0,0 -9,-9z"
        private const val PATH_BLUETOOTH = "M17.71,7.71L12,2h-1v7.59L6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 11,14.41V22h1l5.71,-5.71 -4.3,-4.29 4.3,-4.29zM13,5.83l1.88,1.88L13,9.59V5.83zm1.88,10.46L13,18.17v-3.76l1.88,1.88z"
        private const val PATH_CALL_END = "M12,9c-1.6,0 -3.15,0.25 -4.6,0.72v3.1c0,0.39 -0.23,0.74 -0.56,0.9 -0.98,0.49 -1.87,1.12 -2.66,1.85 -0.18,0.18 -0.43,0.28 -0.7,0.28 -0.28,0 -0.53,-0.11 -0.71,-0.29L0.29,13.08c-0.18,-0.17 -0.29,-0.42 -0.29,-0.7 0,-0.28 0.11,-0.53 0.29,-0.71C3.34,8.78 7.46,7 12,7s8.66,1.78 11.71,4.67c0.18,0.18 0.29,0.43 0.29,0.71 0,0.28 -0.11,0.53 -0.29,0.71l-2.48,2.48c-0.18,0.18 -0.43,0.29 -0.71,0.29 -0.27,0 -0.52,-0.11 -0.7,-0.28 -0.79,-0.74 -1.69,-1.36 -2.67,-1.85 -0.33,-0.16 -0.56,-0.5 -0.56,-0.9v-3.1C15.15,9.25 13.6,9 12,9z"

        fun start(context: Context, callerName: String, startTimestamp: Long) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_START_TIMESTAMP, startTimestamp)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun update(context: Context) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_UPDATE
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
