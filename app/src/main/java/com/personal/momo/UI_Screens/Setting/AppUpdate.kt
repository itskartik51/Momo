package com.personal.momo.UI_Screens.Settings

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class MomoUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String
)

fun cleanOldUpdateApks(context: Context) {
    try {
        val sharedPrefs = context.getSharedPreferences("Momo_Updates", Context.MODE_PRIVATE)
        val savedPath = sharedPrefs.getString("last_downloaded_apk_path", null)
        val downloadedVersionCode = sharedPrefs.getInt("last_downloaded_version_code", -1)
        val downloadId = sharedPrefs.getLong("last_update_download_id", -1L)

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            packageInfo.versionCode
        }

        if (downloadedVersionCode != -1 && currentVersionCode >= downloadedVersionCode) {
            if (savedPath != null) {
                val file = File(savedPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            if (downloadId != -1L) {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                dm?.remove(downloadId)
            }
            sharedPrefs.edit().clear().apply()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

suspend fun checkIsUpdateAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
    cleanOldUpdateApks(context)

    try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            packageInfo.versionCode
        }

        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/itskartik51/Momo/main/Updates/version.json")
            .build()

        val response = OkHttpClient().newCall(request).execute()
        val jsonData = response.body?.string()

        if (jsonData != null) {
            val json = JSONObject(jsonData)
            val serverCode = json.optInt("latest_version_code", 0)
            return@withContext serverCode > currentVersionCode
        }
    } catch (e: Exception) {
        return@withContext false
    }
    return@withContext false
}

@Composable
fun AppUpdateContent(isExpanded: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var checkState by remember { mutableStateOf("IDLE") }
    var updateInfo by remember { mutableStateOf<MomoUpdateInfo?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedApkUri by remember { mutableStateOf<Uri?>(null) }

    val animatedDownloadProgress by animateFloatAsState(
        targetValue = downloadProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "MomoDownloadAnim"
    )

    val packageInfo = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
    val currentVersionCode = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode.toInt() else packageInfo.versionCode
    }
    val currentVersionName = remember { packageInfo.versionName ?: "1.0.0" }

    val installLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.packageManager.canRequestPackageInstalls()) {
            downloadedApkUri?.let { uri -> installApk(context, uri) }
        } else {
            Toast.makeText(context, "Permission Denied. Cannot Install.", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notifications disabled.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded && (checkState == "IDLE" || checkState == "UP_TO_DATE")) {
            checkState = "CHECKING"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            withContext(Dispatchers.IO) {
                try {
                    delay(500)
                    val request = Request.Builder()
                        .url("https://raw.githubusercontent.com/itskartik51/Momo/main/Updates/version.json")
                        .build()
                    val response = OkHttpClient().newCall(request).execute()
                    val jsonData = response.body?.string()

                    if (jsonData != null) {
                        val json = JSONObject(jsonData)
                        val serverCode = json.optInt("latest_version_code", 0)

                        withContext(Dispatchers.Main) {
                            if (serverCode > currentVersionCode) {
                                updateInfo = MomoUpdateInfo(
                                    versionCode = serverCode,
                                    versionName = json.optString("latest_version_name", ""),
                                    releaseNotes = json.optString("release_notes", "Performance improvements."),
                                    apkUrl = json.optString("apk_url", "")
                                )
                                checkState = "AVAILABLE"
                            } else {
                                checkState = "UP_TO_DATE"
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) { checkState = "UP_TO_DATE" }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { checkState = "UP_TO_DATE" }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 20.dp),
        verticalAlignment = Alignment.Top
    ) {
        // LEFT COLUMN: Fixed Brand Identity (35% Width)
        Column(
            modifier = Modifier.weight(0.35f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(brush = MomoPrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Momo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Momo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "v$currentVersionName",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // RIGHT COLUMN: Dynamic State Switcher (65% Width)
        Box(
            modifier = Modifier
                .weight(0.65f)
                .padding(start = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = checkState, label = "MomoUpdateStateTransition") { state ->
                when (state) {
                    "IDLE", "CHECKING" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Checking...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Box(modifier = Modifier.height(16.dp))
                        }
                    }

                    "UP_TO_DATE" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(brush = MomoPrimaryGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Up to date",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "You're on the latest version!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No new updates available.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    "AVAILABLE" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "v${updateInfo?.versionName} Available",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(brush = MomoPrimaryGradient)
                                    .bounceClick(scaleDown = 0.94f) {
                                        checkState = "DOWNLOADING"
                                        downloadProgress = 0f
                                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                        val uri = Uri.parse(updateInfo?.apkUrl)
                                        val fileName = "Momo_Update_${updateInfo?.versionCode}.apk"

                                        val request = DownloadManager.Request(uri)
                                            .setTitle("Momo Update")
                                            .setDescription("Downloading latest version...")
                                            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                                        val downloadId = downloadManager.enqueue(request)

                                        context.getSharedPreferences("Momo_Updates", Context.MODE_PRIVATE)
                                            .edit()
                                            .putLong("last_update_download_id", downloadId)
                                            .apply()

                                        coroutineScope.launch(Dispatchers.IO) {
                                            var isDownloading = true
                                            while (isDownloading) {
                                                val query = DownloadManager.Query().setFilterById(downloadId)
                                                val cursor = downloadManager.query(query)
                                                if (cursor != null && cursor.moveToFirst()) {
                                                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                                        isDownloading = false
                                                        val localUriStr = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                                                        val downloadedFile = File(Uri.parse(localUriStr).path ?: "")
                                                        val finalUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", downloadedFile)

                                                        context.getSharedPreferences("Momo_Updates", Context.MODE_PRIVATE)
                                                            .edit()
                                                            .putString("last_downloaded_apk_path", downloadedFile.absolutePath)
                                                            .putInt("last_downloaded_version_code", updateInfo?.versionCode ?: 0)
                                                            .apply()

                                                        withContext(Dispatchers.Main) { downloadProgress = 1f }
                                                        delay(120)
                                                        withContext(Dispatchers.Main) {
                                                            downloadedApkUri = finalUri
                                                            checkState = "READY"
                                                            showUpdateReadyNotification(context, finalUri)
                                                        }
                                                    } else if (status == DownloadManager.STATUS_FAILED) {
                                                        isDownloading = false
                                                        withContext(Dispatchers.Main) {
                                                            checkState = "AVAILABLE"
                                                            Toast.makeText(context, "Download Failed", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                                        val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                                        if (bytesTotal > 0) {
                                                            withContext(Dispatchers.Main) {
                                                                downloadProgress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                                                            }
                                                        }
                                                    }
                                                }
                                                cursor?.close()
                                                if (isDownloading) delay(250)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Download",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    "DOWNLOADING" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Downloading...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 12.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedDownloadProgress)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(brush = MomoPrimaryGradient)
                                    )
                                }
                            }
                            Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${(animatedDownloadProgress * 100).toInt()}%",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    "READY" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Ready to Install",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(brush = MomoPrimaryGradient)
                                    .bounceClick(scaleDown = 0.94f) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            installLauncher.launch(intent)
                                            Toast.makeText(context, "Allow installs to update", Toast.LENGTH_LONG).show()
                                        } else {
                                            downloadedApkUri?.let { uri -> installApk(context, uri) }
                                        }
                                    }
                                    .padding(horizontal = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Install",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun installApk(context: Context, apkUri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error starting installer.", Toast.LENGTH_SHORT).show()
    }
}

fun showUpdateReadyNotification(context: Context, apkUri: Uri) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "momo_update_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "App Updates",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        installIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("\uD83D\uDE80 Momo Update Ready")
        .setContentText("Download complete. Tap here to install.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    notificationManager.notify(1001, notification)
}
