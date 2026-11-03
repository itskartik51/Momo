package com.personal.momo.UI_Screens.Chat.Preference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object PrefProfileManager {
    private const val PREFS_NAME = "momo_chat_profile_prefs"
    private const val KEY_MY_LOCAL_PATH = "cached_my_avatar_local_path"
    private const val KEY_MY_REMOTE_URL = "cached_my_avatar_remote_url"
    private const val KEY_PARTNER_LOCAL_PATH = "cached_partner_avatar_local_path"
    private const val KEY_PARTNER_REMOTE_URL = "cached_partner_avatar_remote_url"

    private const val COLLECTION_APP = "App"
    private const val DOC_HOME_CONFIG = "home_config"
    private const val FIELD_PROFILE = "Profile"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var snapshotListener: ListenerRegistration? = null

    private val _myAvatarUrlFlow = MutableStateFlow("")
    val myAvatarUrlFlow: StateFlow<String> = _myAvatarUrlFlow.asStateFlow()

    private val _partnerAvatarUrlFlow = MutableStateFlow("")
    val partnerAvatarUrlFlow: StateFlow<String> = _partnerAvatarUrlFlow.asStateFlow()

    @Volatile
    private var isInitialized = false

    private fun getAvatarsDirectory(context: Context): File {
        val dir = File(context.applicationContext.filesDir, "chat_avatars")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun deleteOldAvatars(directory: File, prefix: String, keepFile: File) {
        directory.listFiles { f -> f.name.startsWith(prefix) && f.name != keepFile.name }?.forEach {
            try { it.delete() } catch (_: Exception) {}
        }
    }

    fun init(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val myLocalPath = prefs.getString(KEY_MY_LOCAL_PATH, null)
        if (!myLocalPath.isNullOrBlank()) {
            val file = File(myLocalPath)
            if (file.exists() && file.length() > 0L) {
                _myAvatarUrlFlow.value = file.absolutePath
            }
        }

        val partnerLocalPath = prefs.getString(KEY_PARTNER_LOCAL_PATH, null)
        if (!partnerLocalPath.isNullOrBlank()) {
            val file = File(partnerLocalPath)
            if (file.exists() && file.length() > 0L) {
                _partnerAvatarUrlFlow.value = file.absolutePath
            }
        }

        if (isInitialized) return
        isInitialized = true

        listenToFirestoreProfiles(appContext)
    }

    private fun listenToFirestoreProfiles(appContext: Context) {
        snapshotListener?.remove()

        val docRef = firestore.collection(COLLECTION_APP).document(DOC_HOME_CONFIG)
        snapshotListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val currentUserId = CacheManager.getAppUserId(appContext)
            val isMomo = currentUserId.equals("Momo", ignoreCase = true)

            val profileList = snapshot.get(FIELD_PROFILE) as? List<*>
            val kanuUrl = profileList?.getOrNull(0)?.toString().orEmpty().trim()
            val momoUrl = profileList?.getOrNull(1)?.toString().orEmpty().trim()

            val myUrl = if (isMomo) momoUrl else kanuUrl
            val partnerUrl = if (isMomo) kanuUrl else momoUrl

            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val avatarsDir = getAvatarsDirectory(appContext)

            if (partnerUrl.isNotBlank()) {
                val cachedPartnerRemote = prefs.getString(KEY_PARTNER_REMOTE_URL, "").orEmpty()
                val cachedPartnerLocal = prefs.getString(KEY_PARTNER_LOCAL_PATH, "").orEmpty()
                val partnerFile = if (cachedPartnerLocal.isNotBlank()) File(cachedPartnerLocal) else null
                val hasValidLocalPartner = partnerFile != null && partnerFile.exists() && partnerFile.length() > 0L

                if (partnerUrl != cachedPartnerRemote || !hasValidLocalPartner) {
                    scope.launch {
                        val destinationFile = File(avatarsDir, "partner_avatar_${System.currentTimeMillis()}.jpg")
                        val downloaded = downloadUrlToFile(partnerUrl, destinationFile)
                        if (downloaded) {
                            deleteOldAvatars(avatarsDir, "partner_avatar_", destinationFile)
                            prefs.edit()
                                .putString(KEY_PARTNER_REMOTE_URL, partnerUrl)
                                .putString(KEY_PARTNER_LOCAL_PATH, destinationFile.absolutePath)
                                .apply()
                            _partnerAvatarUrlFlow.value = destinationFile.absolutePath
                        } else if (!hasValidLocalPartner) {
                            _partnerAvatarUrlFlow.value = partnerUrl
                        }
                    }
                } else {
                    _partnerAvatarUrlFlow.value = partnerFile?.absolutePath ?: partnerUrl
                }
            }

            if (myUrl.isNotBlank()) {
                val cachedMyRemote = prefs.getString(KEY_MY_REMOTE_URL, "").orEmpty()
                val cachedMyLocal = prefs.getString(KEY_MY_LOCAL_PATH, "").orEmpty()
                val myFile = if (cachedMyLocal.isNotBlank()) File(cachedMyLocal) else null
                val hasValidLocalMy = myFile != null && myFile.exists() && myFile.length() > 0L

                if (myUrl != cachedMyRemote || !hasValidLocalMy) {
                    scope.launch {
                        val destinationFile = File(avatarsDir, "my_avatar_${System.currentTimeMillis()}.jpg")
                        val downloaded = downloadUrlToFile(myUrl, destinationFile)
                        if (downloaded) {
                            deleteOldAvatars(avatarsDir, "my_avatar_", destinationFile)
                            prefs.edit()
                                .putString(KEY_MY_REMOTE_URL, myUrl)
                                .putString(KEY_MY_LOCAL_PATH, destinationFile.absolutePath)
                                .apply()
                            _myAvatarUrlFlow.value = destinationFile.absolutePath
                        } else if (!hasValidLocalMy) {
                            _myAvatarUrlFlow.value = myUrl
                        }
                    }
                } else {
                    _myAvatarUrlFlow.value = myFile?.absolutePath ?: myUrl
                }
            }
        }
    }

    private suspend fun downloadUrlToFile(urlStr: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true

            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun uploadProfilePicture(
        context: Context,
        imageUri: Uri,
        onProgress: (Boolean) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        val currentUserId = CacheManager.getAppUserId(appContext)
        val isMomo = currentUserId.equals("Momo", ignoreCase = true)
        val targetIndex = if (isMomo) 1 else 0
        val fileName = if (isMomo) "momo_profile.jpg" else "kanu_profile.jpg"

        onProgress(true)

        scope.launch {
            try {
                val compressedBytes = compressImageUriToBytes(appContext, imageUri)
                if (compressedBytes == null) {
                    withContext(Dispatchers.Main) {
                        onProgress(false)
                        onComplete(false)
                    }
                    return@launch
                }

                val avatarsDir = getAvatarsDirectory(appContext)
                val newLocalFile = File(avatarsDir, "my_avatar_${System.currentTimeMillis()}.jpg")
                newLocalFile.writeBytes(compressedBytes)
                deleteOldAvatars(avatarsDir, "my_avatar_", newLocalFile)

                val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_MY_LOCAL_PATH, newLocalFile.absolutePath).apply()
                _myAvatarUrlFlow.value = newLocalFile.absolutePath

                val storageRef = storage.reference.child("avatars").child(fileName)
                storageRef.putBytes(compressedBytes)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            val downloadUrl = downloadUri.toString()
                            updateFirestoreProfileArray(targetIndex, downloadUrl) { success ->
                                if (success) {
                                    prefs.edit().putString(KEY_MY_REMOTE_URL, downloadUrl).apply()
                                }
                                onProgress(false)
                                onComplete(success)
                            }
                        }.addOnFailureListener {
                            onProgress(false)
                            onComplete(false)
                        }
                    }
                    .addOnFailureListener {
                        onProgress(false)
                        onComplete(false)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onProgress(false)
                    onComplete(false)
                }
            }
        }
    }

    private fun updateFirestoreProfileArray(
        targetIndex: Int,
        newUrl: String,
        callback: (Boolean) -> Unit
    ) {
        val docRef = firestore.collection(COLLECTION_APP).document(DOC_HOME_CONFIG)
        docRef.get().addOnSuccessListener { snapshot ->
            val existingList = (snapshot?.get(FIELD_PROFILE) as? List<*>)
                ?.map { it?.toString().orEmpty() }
                ?.toMutableList()
                ?: mutableListOf("", "")

            while (existingList.size <= targetIndex) {
                existingList.add("")
            }

            existingList[targetIndex] = newUrl

            docRef.update(FIELD_PROFILE, existingList)
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { callback(false) }
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun compressImageUriToBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(input)
            input.close()

            if (originalBitmap == null) return null

            val maxDimension = 1080
            val width = originalBitmap.width
            val height = originalBitmap.height

            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                val targetHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private val DialogSurfaceColor = Color(0xFF1E1F22)

private val ProfileHeaderIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ProfileHeaderIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 6c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2m0 10c2.7 0 5.8 1.29 6 2H6c.23-.72 3.31-2 6-2m0-12C9.79 4 8 5.79 8 8s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm0 10c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val CloseDialogIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CloseDialogIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val CameraBadgeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CameraBadgeIcon",
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

@Composable
fun PrefProfileDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val myAvatarUrl by PrefProfileManager.myAvatarUrlFlow.collectAsState()
    var isUploading by remember { mutableStateOf(false) }
    var showFullScreenMyAvatar by remember { mutableStateOf(false) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            PrefProfileManager.uploadProfilePicture(
                context = context,
                imageUri = uri,
                onProgress = { isUploading = it },
                onComplete = { isUploading = false }
            )
        }
    }

    if (showFullScreenMyAvatar && myAvatarUrl.isNotBlank()) {
        PrefPlayer(
            mediaPath = myAvatarUrl,
            isVideo = false,
            onDismiss = { showFullScreenMyAvatar = false }
        )
    }

    Dialog(
        onDismissRequest = {
            if (!isUploading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isUploading,
            dismissOnClickOutside = !isUploading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = DialogSurfaceColor,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MomoPrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ProfileHeaderIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(11.dp))

                        Text(
                            text = "Profile Photo",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .bounceClick(scaleDown = 0.94f) {
                                if (!isUploading) onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CloseDialogIcon,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(142.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .bounceClick(scaleDown = 0.94f) {
                                if (!isUploading && myAvatarUrl.isNotBlank()) {
                                    showFullScreenMyAvatar = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (myAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = myAvatarUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                                modifier = Modifier.size(70.dp)
                            )
                        }

                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MomoPrimaryGradient)
                            .border(2.5.dp, DialogSurfaceColor, CircleShape)
                            .bounceClick(scaleDown = 0.94f) {
                                if (!isUploading) {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CameraBadgeIcon,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = if (isUploading) "Uploading picture..." else "Tap the camera to update your photo",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
