package com.personal.momo.UI_Screens.Chat

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Central storage and directory manager for Momo Chat.
 * Paths:
 * Android/media/com.personal.momo/Momo/
 *   ├── Chat/
 *   │   └── momo_chat_history.json
 *   └── Media/
 *       ├── Photos/
 *       │   └── Sent/
 *       └── Videos/
 *           └── Sent/
 */
object MomoStorage {
    fun getMomoDir(context: Context): File {
        val mediaDirs = context.externalMediaDirs
        val base = mediaDirs.firstOrNull() ?: context.filesDir
        val momoDir = File(base, "Momo")
        if (!momoDir.exists()) momoDir.mkdirs()
        return momoDir
    }

    fun getChatDir(context: Context): File = File(getMomoDir(context), "Chat").apply {
        if (!exists()) mkdirs()
    }

    fun getPhotosDir(context: Context): File = File(getMomoDir(context), "Media/Photos").apply {
        if (!exists()) mkdirs()
    }

    fun getPhotosSentDir(context: Context): File = File(getPhotosDir(context), "Sent").apply {
        if (!exists()) mkdirs()
    }

    fun getVideosDir(context: Context): File = File(getMomoDir(context), "Media/Videos").apply {
        if (!exists()) mkdirs()
    }

    fun getVideosSentDir(context: Context): File = File(getVideosDir(context), "Sent").apply {
        if (!exists()) mkdirs()
    }

    fun resolveLocalMediaFile(context: Context, localPath: String?): File? {
        if (localPath.isNullOrBlank()) return null

        val fileInMomo = File(getMomoDir(context), localPath)
        if (fileInMomo.exists() && fileInMomo.length() > 0L) return fileInMomo

        if (!localPath.contains("/") && !localPath.contains("\\")) {
            val candidateSentPhoto = File(getPhotosSentDir(context), localPath)
            if (candidateSentPhoto.exists() && candidateSentPhoto.length() > 0L) return candidateSentPhoto

            val candidateRecvPhoto = File(getPhotosDir(context), localPath)
            if (candidateRecvPhoto.exists() && candidateRecvPhoto.length() > 0L) return candidateRecvPhoto

            val candidateSentVideo = File(getVideosSentDir(context), localPath)
            if (candidateSentVideo.exists() && candidateSentVideo.length() > 0L) return candidateSentVideo

            val candidateRecvVideo = File(getVideosDir(context), localPath)
            if (candidateRecvVideo.exists() && candidateRecvVideo.length() > 0L) return candidateRecvVideo

            val legacyFile = File(File(context.filesDir, "chat_media"), localPath)
            if (legacyFile.exists() && legacyFile.length() > 0L) return legacyFile
        }

        val directFile = File(localPath)
        if (directFile.exists() && directFile.length() > 0L) return directFile

        return null
    }
}

/**
 * Message Model based on Ultra-Compact Tuple Format:
 * [id, send, text, time, status, replyToId?, replyToText?, replyToSenderId?, mediaType?, fileId?, cryptoKey?, cryptoIv?, localPath?, fileSize?, blurPreview?]
 */
data class ChatMessage(
    val id: String,
    val senderId: Int,
    val text: String,
    val timestamp: Long,
    val status: Int,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderId: Int? = null,
    val mediaType: Int = 0,
    val fileId: String? = null,
    val cryptoKey: String? = null,
    val cryptoIv: String? = null,
    val localPath: String? = null,
    val fileSize: Long = 0L,
    val blurPreview: String? = null
)

object ChatRepository {
    private const val FILE_NAME = "momo_chat_history.json"
    private val mutex = Mutex()

    private val _messagesFlow = MutableStateFlow(emptyList<ChatMessage>())
    val messagesFlow: StateFlow<List<ChatMessage>> = _messagesFlow.asStateFlow()

    @Volatile
    private var isInitialized = false

    suspend fun init(context: Context) {
        if (isInitialized) return
        loadMessages(context.applicationContext)
    }

    private fun performLegacyMigrationIfNeeded(context: Context) {
        try {
            val oldJsonFile = File(context.filesDir, FILE_NAME)
            val newJsonFile = File(MomoStorage.getChatDir(context), FILE_NAME)

            if (oldJsonFile.exists()) {
                if (!newJsonFile.exists() || newJsonFile.length() == 0L) {
                    oldJsonFile.copyTo(newJsonFile, overwrite = true)
                }
                oldJsonFile.delete()
            }

            val oldMediaFolder = File(context.filesDir, "chat_media")
            if (oldMediaFolder.exists() && oldMediaFolder.isDirectory) {
                val files = oldMediaFolder.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.isFile && file.length() > 0L) {
                        val isVideo = file.name.endsWith(".mp4", ignoreCase = true)
                        val targetDir = if (isVideo) {
                            MomoStorage.getVideosDir(context)
                        } else {
                            MomoStorage.getPhotosDir(context)
                        }
                        val targetFile = File(targetDir, file.name)
                        file.copyTo(targetFile, overwrite = true)
                        file.delete()
                    }
                }
                oldMediaFolder.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureLoadedLocked(context: Context) {
        if (isInitialized) return

        performLegacyMigrationIfNeeded(context)

        val file = File(MomoStorage.getChatDir(context), FILE_NAME)
        if (!file.exists()) {
            _messagesFlow.value = emptyList()
            isInitialized = true
            return
        }

        try {
            val content = file.readText().trim()
            if (content.isEmpty() || content == "[]") {
                _messagesFlow.value = emptyList()
                isInitialized = true
                return
            }

            val jsonArray = JSONArray(content)
            val list = ArrayList<ChatMessage>(jsonArray.length())

            for (i in 0 until jsonArray.length()) {
                val tuple = jsonArray.optJSONArray(i) ?: continue
                if (tuple.length() >= 5) {
                    val replyToId = if (tuple.length() > 5 && !tuple.isNull(5)) {
                        tuple.optString(5).takeIf { it.isNotEmpty() }
                    } else null

                    val replyToText = if (tuple.length() > 6 && !tuple.isNull(6)) {
                        tuple.optString(6).takeIf { it.isNotEmpty() }
                    } else null

                    val replyToSenderId = if (tuple.length() > 7 && !tuple.isNull(7)) {
                        tuple.optInt(7).takeIf { it > 0 }
                    } else null

                    val mediaType = if (tuple.length() > 8 && !tuple.isNull(8)) {
                        tuple.optInt(8, 0)
                    } else 0

                    val fileId = if (tuple.length() > 9 && !tuple.isNull(9)) {
                        tuple.optString(9).takeIf { it.isNotEmpty() }
                    } else null

                    val cryptoKey = if (tuple.length() > 10 && !tuple.isNull(10)) {
                        tuple.optString(10).takeIf { it.isNotEmpty() }
                    } else null

                    val cryptoIv = if (tuple.length() > 11 && !tuple.isNull(11)) {
                        tuple.optString(11).takeIf { it.isNotEmpty() }
                    } else null

                    val localPath = if (tuple.length() > 12 && !tuple.isNull(12)) {
                        tuple.optString(12).takeIf { it.isNotEmpty() }
                    } else null

                    val fileSize = if (tuple.length() > 13 && !tuple.isNull(13)) {
                        tuple.optLong(13, 0L)
                    } else 0L

                    val blurPreview = if (tuple.length() > 14 && !tuple.isNull(14)) {
                        tuple.optString(14).takeIf { it.isNotEmpty() }
                    } else null

                    list.add(
                        ChatMessage(
                            id = tuple.optString(0, ""),
                            senderId = tuple.optInt(1, 1),
                            text = tuple.optString(2, ""),
                            timestamp = tuple.optLong(3, 0L),
                            status = tuple.optInt(4, 0),
                            replyToId = replyToId,
                            replyToText = replyToText,
                            replyToSenderId = replyToSenderId,
                            mediaType = mediaType,
                            fileId = fileId,
                            cryptoKey = cryptoKey,
                            cryptoIv = cryptoIv,
                            localPath = localPath,
                            fileSize = fileSize,
                            blurPreview = blurPreview
                        )
                    )
                }
            }

            list.sortBy { it.timestamp }
            _messagesFlow.value = list
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = true
        }
    }

    suspend fun loadMessages(context: Context): List<ChatMessage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            isInitialized = false
            ensureLoadedLocked(context.applicationContext)
            _messagesFlow.value
        }
    }

    suspend fun saveMessage(context: Context, message: ChatMessage) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appContext = context.applicationContext
            ensureLoadedLocked(appContext)

            val currentList = _messagesFlow.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.id == message.id }

            if (existingIndex >= 0) {
                val existing = currentList[existingIndex]
                val resolvedStatus = maxOf(existing.status, message.status)
                currentList[existingIndex] = message.copy(status = resolvedStatus)
            } else {
                currentList.add(message)
            }

            currentList.sortBy { it.timestamp }
            writeMessagesToFile(appContext, currentList)
            _messagesFlow.value = currentList
        }
    }

    suspend fun updateMessageStatus(context: Context, messageId: String, newStatus: Int): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appContext = context.applicationContext
            ensureLoadedLocked(appContext)

            val currentList = _messagesFlow.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == messageId }

            if (index >= 0) {
                val existing = currentList[index]
                if (newStatus > existing.status) {
                    currentList[index] = existing.copy(status = newStatus)
                    writeMessagesToFile(appContext, currentList)
                    _messagesFlow.value = currentList
                }
                true
            } else {
                false
            }
        }
    }

    suspend fun deleteMessages(context: Context, messageIds: Set<String>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appContext = context.applicationContext
            ensureLoadedLocked(appContext)

            val currentList = _messagesFlow.value.toMutableList()
            val wasRemoved = currentList.removeAll { it.id in messageIds }

            if (wasRemoved) {
                writeMessagesToFile(appContext, currentList)
                _messagesFlow.value = currentList
            }
        }
    }

    suspend fun getPendingMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            _messagesFlow.value.filter { it.status == 0 }
        }
    }

    suspend fun markIncomingMessagesAsRead(context: Context, mySenderId: Int): List<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appContext = context.applicationContext
            ensureLoadedLocked(appContext)

            val currentList = _messagesFlow.value.toMutableList()
            val updatedMessageIds = mutableListOf<String>()
            var hasModifications = false

            for (i in currentList.indices) {
                val msg = currentList[i]
                if (msg.senderId != mySenderId && msg.status < 3) {
                    currentList[i] = msg.copy(status = 3)
                    updatedMessageIds.add(msg.id)
                    hasModifications = true
                }
            }

            if (hasModifications) {
                writeMessagesToFile(appContext, currentList)
                _messagesFlow.value = currentList
            }
            updatedMessageIds
        }
    }

    private fun writeMessagesToFile(context: Context, messages: List<ChatMessage>) {
        try {
            val rootArray = JSONArray()
            for (msg in messages) {
                val tuple = JSONArray()
                tuple.put(msg.id)
                tuple.put(msg.senderId)
                tuple.put(msg.text)
                tuple.put(msg.timestamp)
                tuple.put(msg.status)

                val hasReply = !msg.replyToId.isNullOrEmpty() || !msg.replyToText.isNullOrEmpty() || msg.replyToSenderId != null
                val hasMedia = msg.mediaType > 0 || !msg.fileId.isNullOrEmpty() || !msg.localPath.isNullOrEmpty()

                if (hasReply || hasMedia) {
                    tuple.put(msg.replyToId ?: JSONObject.NULL)
                    tuple.put(msg.replyToText ?: JSONObject.NULL)
                    tuple.put(msg.replyToSenderId ?: JSONObject.NULL)

                    if (hasMedia) {
                        tuple.put(msg.mediaType)
                        tuple.put(msg.fileId ?: JSONObject.NULL)
                        tuple.put(msg.cryptoKey ?: JSONObject.NULL)
                        tuple.put(msg.cryptoIv ?: JSONObject.NULL)
                        tuple.put(msg.localPath ?: JSONObject.NULL)
                        tuple.put(msg.fileSize)
                        tuple.put(msg.blurPreview ?: JSONObject.NULL)
                    }
                }

                rootArray.put(tuple)
            }

            val file = File(MomoStorage.getChatDir(context), FILE_NAME)
            file.writeText(rootArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
