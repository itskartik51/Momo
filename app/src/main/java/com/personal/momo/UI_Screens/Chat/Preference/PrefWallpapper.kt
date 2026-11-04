package com.personal.momo.UI_Screens.Chat.Preference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.personal.momo.UI_Screens.Chat.Media.FirebaseMediaManager
import com.personal.momo.UI_Screens.Chat.Media.MediaCompressor
import com.personal.momo.UI_Screens.Chat.Media.MediaCrypto
import com.personal.momo.UI_Screens.Chat.MomoStorage
import com.personal.momo.UI_Screens.Chat.WhatsAppBubbleShape
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.MomoRedLight
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

data class WallpaperConfig(
    val fileId: String = "",
    val cryptoKey: String = "",
    val cryptoIv: String = "",
    val focusX: Float = 0.5f,
    val focusY: Float = 0.5f,
    val userZoom: Float = 1.0f,
    val dimLevel: Float = 0.35f,
    val timestamp: Long = 0L
)

private val BrightnessSunIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BrightnessSunIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M20 8.69V4h-4.69L12 .69 8.69 4H4v4.69L.69 12 4 15.31V20h4.69L12 23.31 15.31 20H20v-4.69L23.31 12 20 8.69zM12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6 6 2.69 6 6-2.69 6-6 6zm0-10c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val ChangeGalleryIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChangeGalleryIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val PreviewCameraIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PreviewCameraIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 12m-3.2 0a3.2 3.2 0 1 0 6.4 0a3.2 3.2 0 1 0 -6.4 0 M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val PreviewSendIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PreviewSendIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val DeleteWallpaperIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DeleteWallpaperIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

object PrefWallpaperManager {
    private const val RTDB_WALLPAPER_PATH = "momo_chat/wallpaper"
    private const val PREFS_NAME = "momo_wallpaper_prefs"

    private val database by lazy { FirebaseDatabase.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _configFlow = MutableStateFlow(WallpaperConfig())
    val configFlow: StateFlow<WallpaperConfig> = _configFlow.asStateFlow()

    private val _localFileFlow = MutableStateFlow<File?>(null)
    val localFileFlow: StateFlow<File?> = _localFileFlow.asStateFlow()

    private val _bitmapFlow = MutableStateFlow<ImageBitmap?>(null)
    val bitmapFlow: StateFlow<ImageBitmap?> = _bitmapFlow.asStateFlow()

    @Volatile
    private var isInitialized = false

    @Volatile
    private var activeDownloadingFileId: String? = null

    fun getWallpaperDir(context: Context): File {
        return File(MomoStorage.getMomoDir(context), "Media/Wallpapers").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getWallpaperFile(context: Context, fileId: String): File {
        return File(getWallpaperDir(context), "$fileId.jpg")
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        loadCachedPrefs(appContext)

        database.getReference(RTDB_WALLPAPER_PATH)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        val oldFileId = _configFlow.value.fileId
                        _configFlow.value = WallpaperConfig()
                        _localFileFlow.value = null
                        _bitmapFlow.value = null
                        saveCachedPrefs(appContext, WallpaperConfig())
                        if (oldFileId.isNotBlank()) {
                            cleanupLocalFilesExcept(appContext, "")
                        }
                        return
                    }

                    val fileId = snapshot.child("fileId").getValue(String::class.java).orEmpty()
                    val cryptoKey = snapshot.child("cryptoKey").getValue(String::class.java).orEmpty()
                    val cryptoIv = snapshot.child("cryptoIv").getValue(String::class.java).orEmpty()
                    val focusX = (snapshot.child("focusX").getValue(Double::class.java)?.toFloat() ?: 0.5f).coerceIn(0f, 1f)
                    val focusY = (snapshot.child("focusY").getValue(Double::class.java)?.toFloat() ?: 0.5f).coerceIn(0f, 1f)
                    val userZoom = (snapshot.child("userZoom").getValue(Double::class.java)?.toFloat() ?: 1.0f).coerceIn(1f, 4f)
                    val dimLevel = (snapshot.child("dimLevel").getValue(Double::class.java)?.toFloat() ?: 0.35f).coerceIn(0f, 0.88f)
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                    val newConfig = WallpaperConfig(
                        fileId = fileId,
                        cryptoKey = cryptoKey,
                        cryptoIv = cryptoIv,
                        focusX = focusX,
                        focusY = focusY,
                        userZoom = userZoom,
                        dimLevel = dimLevel,
                        timestamp = timestamp
                    )

                    _configFlow.value = newConfig
                    saveCachedPrefs(appContext, newConfig)

                    if (fileId.isNotBlank()) {
                        ensureWallpaperCached(appContext, newConfig)
                    } else {
                        _localFileFlow.value = null
                        _bitmapFlow.value = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadCachedPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fileId = prefs.getString("fileId", "").orEmpty()
        val cryptoKey = prefs.getString("cryptoKey", "").orEmpty()
        val cryptoIv = prefs.getString("cryptoIv", "").orEmpty()
        val focusX = prefs.getFloat("focusX", 0.5f).coerceIn(0f, 1f)
        val focusY = prefs.getFloat("focusY", 0.5f).coerceIn(0f, 1f)
        val userZoom = prefs.getFloat("userZoom", 1.0f).coerceIn(1f, 4f)
        val dimLevel = prefs.getFloat("dimLevel", 0.35f).coerceIn(0f, 0.88f)
        val timestamp = prefs.getLong("timestamp", 0L)

        val cachedConfig = WallpaperConfig(
            fileId = fileId,
            cryptoKey = cryptoKey,
            cryptoIv = cryptoIv,
            focusX = focusX,
            focusY = focusY,
            userZoom = userZoom,
            dimLevel = dimLevel,
            timestamp = timestamp
        )
        _configFlow.value = cachedConfig

        if (fileId.isNotBlank()) {
            val existingFile = getWallpaperFile(context, fileId)
            if (existingFile.exists() && existingFile.length() > 0L) {
                _localFileFlow.value = existingFile
                scope.launch {
                    val bmp = decodeImageBitmapSafe(existingFile)
                    if (bmp != null) {
                        _bitmapFlow.value = bmp
                    }
                }
            }
        }
    }

    private fun saveCachedPrefs(context: Context, config: WallpaperConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("fileId", config.fileId)
            .putString("cryptoKey", config.cryptoKey)
            .putString("cryptoIv", config.cryptoIv)
            .putFloat("focusX", config.focusX)
            .putFloat("focusY", config.focusY)
            .putFloat("userZoom", config.userZoom)
            .putFloat("dimLevel", config.dimLevel)
            .putLong("timestamp", config.timestamp)
            .apply()
    }

    private fun ensureWallpaperCached(context: Context, config: WallpaperConfig) {
        val targetFile = getWallpaperFile(context, config.fileId)
        if (targetFile.exists() && targetFile.length() > 0L) {
            if (_localFileFlow.value?.absolutePath != targetFile.absolutePath || _bitmapFlow.value == null) {
                _localFileFlow.value = targetFile
                scope.launch {
                    val bmp = decodeImageBitmapSafe(targetFile)
                    if (bmp != null) {
                        _bitmapFlow.value = bmp
                    }
                    cleanupLocalFilesExcept(context, config.fileId)
                }
            }
            return
        }

        if (config.cryptoKey.isBlank() || config.cryptoIv.isBlank()) return
        if (activeDownloadingFileId == config.fileId) return
        activeDownloadingFileId = config.fileId

        scope.launch {
            val tempEnc = File(getWallpaperDir(context), "${config.fileId}.enc.tmp")
            val tempDec = File(getWallpaperDir(context), "${config.fileId}.jpg.tmp")
            try {
                val dlResult = FirebaseMediaManager.downloadEncryptedMedia(
                    fileId = config.fileId,
                    targetFile = tempEnc
                )

                if (dlResult.isSuccess && tempEnc.exists() && tempEnc.length() > 0L) {
                    MediaCrypto.decryptFile(
                        context = context,
                        encryptedFile = tempEnc,
                        base64Key = config.cryptoKey,
                        base64Iv = config.cryptoIv,
                        destinationFile = tempDec
                    )

                    if (tempDec.exists() && tempDec.length() > 0L) {
                        tempDec.renameTo(targetFile)
                        val bmp = decodeImageBitmapSafe(targetFile)
                        _localFileFlow.value = targetFile
                        if (bmp != null) {
                            _bitmapFlow.value = bmp
                        }
                        cleanupLocalFilesExcept(context, config.fileId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (tempEnc.exists()) tempEnc.delete()
                if (tempDec.exists()) tempDec.delete()
                if (activeDownloadingFileId == config.fileId) {
                    activeDownloadingFileId = null
                }
            }
        }
    }

    private fun cleanupLocalFilesExcept(context: Context, keepFileId: String) {
        try {
            val dir = getWallpaperDir(context)
            val keepName = if (keepFileId.isNotBlank()) "$keepFileId.jpg" else ""
            dir.listFiles()?.forEach { file ->
                if (file.name != keepName && !file.name.endsWith(".tmp")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun updateWallpaperAdjustments(
        context: Context,
        focusX: Float,
        focusY: Float,
        userZoom: Float,
        dimLevel: Float,
        onComplete: (Boolean) -> Unit
    ) {
        val current = _configFlow.value
        if (current.fileId.isBlank()) {
            onComplete(false)
            return
        }

        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val updated = current.copy(
            focusX = focusX.coerceIn(0f, 1f),
            focusY = focusY.coerceIn(0f, 1f),
            userZoom = userZoom.coerceIn(1f, 4f),
            dimLevel = dimLevel.coerceIn(0f, 0.88f),
            timestamp = now
        )

        _configFlow.value = updated
        saveCachedPrefs(appContext, updated)

        val updatesMap = mapOf<String, Any>(
            "focusX" to updated.focusX.toDouble(),
            "focusY" to updated.focusY.toDouble(),
            "userZoom" to updated.userZoom.toDouble(),
            "dimLevel" to updated.dimLevel.toDouble(),
            "timestamp" to now
        )

        database.getReference(RTDB_WALLPAPER_PATH)
            .updateChildren(updatesMap)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun uploadNewWallpaper(
        context: Context,
        imageUri: Uri,
        focusX: Float,
        focusY: Float,
        userZoom: Float,
        dimLevel: Float,
        onProgress: (Boolean) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        val previousFileId = _configFlow.value.fileId
        onProgress(true)

        scope.launch {
            var tempRawFile: File? = null
            var compressedFile: File? = null
            var encryptedFile: File? = null

            try {
                val newFileId = "WALL_${System.currentTimeMillis()}"
                val cacheFolder = File(appContext.cacheDir, "media_cache").apply {
                    if (!exists()) mkdirs()
                }
                tempRawFile = File(cacheFolder, "RAW_${newFileId}.tmp")

                appContext.contentResolver.openInputStream(imageUri)?.use { input ->
                    FileOutputStream(tempRawFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalArgumentException("Cannot open stream for wallpaper URI: $imageUri")

                compressedFile = try {
                    MediaCompressor.compressImage(
                        context = appContext,
                        inputFile = tempRawFile,
                        maxDimension = 1600,
                        quality = 82
                    )
                } catch (_: Exception) {
                    tempRawFile
                }

                val localWallFile = getWallpaperFile(appContext, newFileId)
                compressedFile.copyTo(localWallFile, overwrite = true)

                val encryptionResult = MediaCrypto.encryptFile(appContext, localWallFile)
                encryptedFile = encryptionResult.encryptedFile

                if (compressedFile.exists()) {
                    compressedFile.delete()
                }
                if (tempRawFile.exists()) {
                    tempRawFile.delete()
                }

                val uploadResult = FirebaseMediaManager.uploadEncryptedMedia(
                    encryptedFile = encryptedFile,
                    fileId = newFileId
                )

                if (encryptedFile.exists()) {
                    encryptedFile.delete()
                }

                if (uploadResult.isSuccess) {
                    val now = System.currentTimeMillis()
                    val newConfig = WallpaperConfig(
                        fileId = newFileId,
                        cryptoKey = encryptionResult.base64Key,
                        cryptoIv = encryptionResult.base64Iv,
                        focusX = focusX.coerceIn(0f, 1f),
                        focusY = focusY.coerceIn(0f, 1f),
                        userZoom = userZoom.coerceIn(1f, 4f),
                        dimLevel = dimLevel.coerceIn(0f, 0.88f),
                        timestamp = now
                    )

                    val bmp = decodeImageBitmapSafe(localWallFile)
                    _localFileFlow.value = localWallFile
                    if (bmp != null) {
                        _bitmapFlow.value = bmp
                    }
                    _configFlow.value = newConfig
                    saveCachedPrefs(appContext, newConfig)
                    cleanupLocalFilesExcept(appContext, newFileId)

                    val payload = mapOf<String, Any>(
                        "fileId" to newConfig.fileId,
                        "cryptoKey" to newConfig.cryptoKey,
                        "cryptoIv" to newConfig.cryptoIv,
                        "focusX" to newConfig.focusX.toDouble(),
                        "focusY" to newConfig.focusY.toDouble(),
                        "userZoom" to newConfig.userZoom.toDouble(),
                        "dimLevel" to newConfig.dimLevel.toDouble(),
                        "timestamp" to now
                    )

                    database.getReference(RTDB_WALLPAPER_PATH)
                        .setValue(payload)
                        .addOnSuccessListener {
                            if (previousFileId.isNotBlank() && previousFileId != newFileId) {
                                scope.launch {
                                    FirebaseMediaManager.deleteMediaFromFirebase(previousFileId)
                                }
                            }
                            onProgress(false)
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onProgress(false)
                            onComplete(false)
                        }
                } else {
                    if (localWallFile.exists()) localWallFile.delete()
                    withContext(Dispatchers.Main) {
                        onProgress(false)
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    if (tempRawFile?.exists() == true) tempRawFile.delete()
                    if (compressedFile?.exists() == true) compressedFile.delete()
                    if (encryptedFile?.exists() == true) encryptedFile.delete()
                } catch (_: Exception) {}

                withContext(Dispatchers.Main) {
                    onProgress(false)
                    onComplete(false)
                }
            }
        }
    }

    fun removeWallpaper(
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        val oldFileId = _configFlow.value.fileId

        _configFlow.value = WallpaperConfig()
        _localFileFlow.value = null
        _bitmapFlow.value = null
        saveCachedPrefs(appContext, WallpaperConfig())
        cleanupLocalFilesExcept(appContext, "")

        database.getReference(RTDB_WALLPAPER_PATH)
            .removeValue()
            .addOnSuccessListener {
                if (oldFileId.isNotBlank()) {
                    scope.launch {
                        FirebaseMediaManager.deleteMediaFromFirebase(oldFileId)
                    }
                }
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    internal suspend fun decodeImageBitmapSafe(file: File): ImageBitmap? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0L) return@withContext null
        try {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (maxDim / sampleSize > 2048) {
                sampleSize *= 2
            }

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@withContext null

            var rotation = 0
            try {
                val exif = ExifInterface(file.absolutePath)
                rotation = when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } catch (_: Exception) {}

            val oriented = if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                    if (it != decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            oriented.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    internal suspend fun decodeUriBitmapSafe(context: Context, uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheFolder = File(context.cacheDir, "media_cache").apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File(cacheFolder, "WALL_PREVIEW_${System.currentTimeMillis()}.tmp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            decodeImageBitmapSafe(tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}

@Composable
fun ChatWallpaperBackground(
    modifier: Modifier = Modifier
) {
    val config by PrefWallpaperManager.configFlow.collectAsState()
    val bitmap by PrefWallpaperManager.bitmapFlow.collectAsState()

    if (config.fileId.isNotBlank() && bitmap != null) {
        Box(modifier = modifier.clipToBounds()) {
            WallpaperCroppedCanvas(
                bitmap = bitmap!!,
                focusX = config.focusX,
                focusY = config.focusY,
                userZoom = config.userZoom,
                modifier = Modifier.fillMaxSize()
            )

            if (config.dimLevel > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = config.dimLevel.coerceIn(0f, 0.90f)))
                )
            }
        }
    }
}

@Composable
internal fun WallpaperCroppedCanvas(
    bitmap: ImageBitmap,
    focusX: Float,
    focusY: Float,
    userZoom: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.clipToBounds()) {
        val boxW = size.width
        val boxH = size.height
        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()

        if (boxW > 0f && boxH > 0f && imgW > 0f && imgH > 0f) {
            val baseScale = maxOf(boxW / imgW, boxH / imgH)
            val effectiveScale = baseScale * userZoom.coerceIn(1f, 4f)

            val scaledW = imgW * effectiveScale
            val scaledH = imgH * effectiveScale

            val extraW = (scaledW - boxW).coerceAtLeast(0f)
            val extraH = (scaledH - boxH).coerceAtLeast(0f)

            val tx = (0.5f - focusX.coerceIn(0f, 1f)) * extraW
            val ty = (0.5f - focusY.coerceIn(0f, 1f)) * extraH

            val left = (boxW - scaledW) / 2f + tx
            val top = (boxH - scaledH) / 2f + ty

            withTransform({
                translate(left = left, top = top)
            }) {
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(scaledW.roundToInt(), scaledH.roundToInt())
                )
            }
        }
    }
}

@Composable
fun PrefWallpaperDialog(
    partnerDisplayName: String = "",
    onDismiss: () -> Unit,
    onWallpaperApplied: (String) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val savedConfig by PrefWallpaperManager.configFlow.collectAsState()
    val savedBitmap by PrefWallpaperManager.bitmapFlow.collectAsState()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var pickSequence by remember { mutableIntStateOf(0) }

    var focusX by remember(savedConfig.fileId) { mutableFloatStateOf(savedConfig.focusX) }
    var focusY by remember(savedConfig.fileId) { mutableFloatStateOf(savedConfig.focusY) }
    var userZoom by remember(savedConfig.fileId) { mutableFloatStateOf(savedConfig.userZoom) }
    var dimLevel by remember(savedConfig.fileId) { mutableFloatStateOf(savedConfig.dimLevel) }

    var isUploading by remember { mutableStateOf(false) }
    var previewBoxSize by remember { mutableStateOf(IntSize.Zero) }

    val activeBitmap = pendingBitmap ?: savedBitmap
    val hasActiveImage = activeBitmap != null

    val deviceAspectRatio = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val w = configuration.screenWidthDp.toFloat().coerceAtLeast(1f)
        val h = configuration.screenHeightDp.toFloat().coerceAtLeast(1f)
        (w / h).coerceIn(0.42f, 0.72f)
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUri = uri
            pickSequence++
            focusX = 0.5f
            focusY = 0.5f
            userZoom = 1.0f
        }
    }

    LaunchedEffect(pendingUri, pickSequence) {
        val targetUri = pendingUri
        if (targetUri != null) {
            val decoded = PrefWallpaperManager.decodeUriBitmapSafe(context, targetUri)
            if (decoded != null) {
                pendingBitmap = decoded
            }
        }
    }

    BackHandler {
        if (!isUploading) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isUploading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF191B22),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Chat Background",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasActiveImage) "Pinch to zoom & drag to adjust" else "Default dark theme active",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.62f)
                        )
                    }

                    if (savedConfig.fileId.isNotBlank() || pendingUri != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .bounceClick(scaleDown = 0.94f) {
                                    if (!isUploading) {
                                        if (pendingUri != null && savedConfig.fileId.isBlank()) {
                                            pendingUri = null
                                            pendingBitmap = null
                                        } else {
                                            PrefWallpaperManager.removeWallpaper(context) {
                                                onWallpaperApplied("Background reset")
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = DeleteWallpaperIcon,
                                contentDescription = "Reset Wallpaper",
                                tint = Color(0xFFFF5C77),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .height(320.dp)
                        .aspectRatio(deviceAspectRatio)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .onSizeChanged { previewBoxSize = it }
                        .pointerInput(activeBitmap) {
                            val bmp = activeBitmap ?: return@pointerInput
                            detectTransformGestures { _, pan, zoom, _ ->
                                val boxW = previewBoxSize.width.toFloat()
                                val boxH = previewBoxSize.height.toFloat()
                                val imgW = bmp.width.toFloat()
                                val imgH = bmp.height.toFloat()

                                if (boxW > 0f && boxH > 0f && imgW > 0f && imgH > 0f) {
                                    val newZoom = (userZoom * zoom).coerceIn(1f, 4f)
                                    userZoom = newZoom

                                    val baseScale = maxOf(boxW / imgW, boxH / imgH)
                                    val effectiveScale = baseScale * newZoom
                                    val scaledW = imgW * effectiveScale
                                    val scaledH = imgH * effectiveScale

                                    val extraW = (scaledW - boxW).coerceAtLeast(0f)
                                    val extraH = (scaledH - boxH).coerceAtLeast(0f)

                                    if (extraW > 0.5f) {
                                        focusX = (focusX - (pan.x / extraW)).coerceIn(0f, 1f)
                                    } else {
                                        focusX = 0.5f
                                    }

                                    if (extraH > 0.5f) {
                                        focusY = (focusY - (pan.y / extraH)).coerceIn(0f, 1f)
                                    } else {
                                        focusY = 0.5f
                                    }
                                }
                            }
                        }
                ) {
                    if (activeBitmap != null) {
                        WallpaperCroppedCanvas(
                            bitmap = activeBitmap,
                            focusX = focusX,
                            focusY = focusY,
                            userZoom = userZoom,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = dimLevel.coerceIn(0f, 0.88f)))
                        )
                    }

                    DummyWhatsAppChatPreview(
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = BrightnessSunIcon,
                        contentDescription = "Dimming",
                        tint = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    val brightnessValue = 1f - (dimLevel / 0.88f).coerceIn(0f, 1f)
                    Slider(
                        value = brightnessValue,
                        onValueChange = { newBrightness ->
                            dimLevel = ((1f - newBrightness) * 0.88f).coerceIn(0f, 0.88f)
                        },
                        enabled = hasActiveImage && !isUploading,
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MomoRedLight,
                            inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                            disabledThumbColor = Color.White.copy(alpha = 0.30f),
                            disabledActiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${(brightnessValue * 100).roundToInt()}%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.width(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .bounceClick(scaleDown = 0.94f) {
                                if (!isUploading) {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                        shape = RoundedCornerShape(21.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = ChangeGalleryIcon,
                                contentDescription = "Change",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Change",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(
                                if (hasActiveImage) MomoPrimaryGradient
                                else SolidColor(Color.White.copy(alpha = 0.12f))
                            )
                            .bounceClick(scaleDown = 0.94f) {
                                if (!isUploading && hasActiveImage) {
                                    val chosenUri = pendingUri
                                    if (chosenUri != null) {
                                        PrefWallpaperManager.uploadNewWallpaper(
                                            context = context,
                                            imageUri = chosenUri,
                                            focusX = focusX,
                                            focusY = focusY,
                                            userZoom = userZoom,
                                            dimLevel = dimLevel,
                                            onProgress = { isUploading = it },
                                            onComplete = { success ->
                                                if (success) {
                                                    onWallpaperApplied("Background updated")
                                                }
                                            }
                                        )
                                    } else {
                                        PrefWallpaperManager.updateWallpaperAdjustments(
                                            context = context,
                                            focusX = focusX,
                                            focusY = focusY,
                                            userZoom = userZoom,
                                            dimLevel = dimLevel
                                        ) {
                                            onWallpaperApplied("Background updated")
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isUploading) "Applying..." else "Apply",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasActiveImage) Color.White else Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DummyWhatsAppChatPreview(
    modifier: Modifier = Modifier
) {
    val incomingTailShape = remember {
        WhatsAppBubbleShape(
            isOutgoing = false,
            hasTail = true,
            cornerRadius = 7.dp,
            tailWidth = 4.dp,
            tailHeight = 6.dp
        )
    }
    val outgoingTailShape = remember {
        WhatsAppBubbleShape(
            isOutgoing = true,
            hasTail = true,
            cornerRadius = 7.dp,
            tailWidth = 4.dp,
            tailHeight = 6.dp
        )
    }
    val outgoingGroupShape = remember {
        WhatsAppBubbleShape(
            isOutgoing = true,
            hasTail = false,
            cornerRadius = 7.dp,
            tailWidth = 4.dp,
            tailHeight = 6.dp
        )
    }

    Column(
        modifier = modifier.padding(horizontal = 7.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    modifier = Modifier
                        .width(86.dp)
                        .height(22.dp),
                    shape = incomingTailShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 0.7.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                    )
                ) {}
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .width(106.dp)
                        .height(22.dp)
                        .clip(outgoingTailShape)
                        .background(MomoPrimaryGradient)
                )
            }

            Spacer(modifier = Modifier.height(2.5.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .width(74.dp)
                        .height(20.dp)
                        .clip(outgoingGroupShape)
                        .background(MomoPrimaryGradient)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    modifier = Modifier
                        .width(98.dp)
                        .height(22.dp),
                    shape = incomingTailShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 0.7.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                    )
                ) {}
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF171922).copy(alpha = 0.92f))
                    .border(0.7.dp, Color(0xFF262936), RoundedCornerShape(11.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = ChangeGalleryIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = PreviewCameraIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(11.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MomoPrimaryGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PreviewSendIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(10.dp)
                        .padding(start = 1.dp)
                )
            }
        }
    }
}
