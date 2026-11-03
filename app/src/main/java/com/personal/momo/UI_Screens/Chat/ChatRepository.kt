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
 *       ├── Videos/
 *       │   └── Sent/
 *       └── Updates/
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

    fun getUpdatesDir(context: Context): File = File(getMomoDir(context), "Media/Updates").apply {
        if (!exists()) mkdirs()
    }

    fun saveStatusMediaToUpdates(
        context: Context,
        sourceFile: File,
        statusId: String,
        isVideo: Boolean
    ): Boolean {
        return try {
            if (!sourceFile.exists() || sourceFile.length() <= 0L) return false
            val updatesDir = getUpdatesDir(context)
            val ext = if (isVideo) "mp4" else "jpg"
            val prefix = if (isVideo) "VID" else "IMG"
            val safeId = statusId.ifBlank { System.currentTimeMillis().toString() }
            val targetFile = File(updatesDir, "${prefix}_${safeId}.$ext")

            if (targetFile.exists() && targetFile.length() > 0L) return true
            sourceFile.copyTo(targetFile, overwrite = true)
            targetFile.exists() && targetFile.length() > 0L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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

            val candidateUpdates = File(getUpdatesDir(context), localPath)
            if (candidateUpdates.exists() && candidateUpdates.length() > 0L) return candidateUpdates
        }

        val directFile = File(localPath)
        if (directFile.exists() && directFile.length() > 0L) return directFile

        return null
    }
}

/**
 * Optimized Message Model:
 * Storing only [replyToId] for message references to prevent redundant text duplicates.
 * Optional [replyToText] and [replyToSenderId] are maintained with null defaults
 * for zero-breakage backward compatibility during CI/CD builds.
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

    private fun ensureLoadedLocked(context: Context) {
        if (isInitialized) return

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
                    val isOldSchema = tuple.length() == 15 || tuple.length() == 8

                    val replyToId = if (tuple.length() > 5 && !tuple.isNull(5)) {
                        tuple.optString(5).takeIf { it.isNotEmpty() }
                    } else null

                    val replyToText = if (isOldSchema && tuple.length() > 6 && !tuple.isNull(6)) {
                        tuple.optString(6).takeIf { it.isNotEmpty() }
                    } else null

                    val replyToSenderId = if (isOldSchema && tuple.length() > 7 && !tuple.isNull(7)) {
                        tuple.optInt(7).takeIf { it > 0 }
                    } else null

                    val mediaOffset = if (isOldSchema) 8 else 6

                    val mediaType = if (tuple.length() > mediaOffset && !tuple.isNull(mediaOffset)) {
                        tuple.optInt(mediaOffset, 0)
                    } else 0

                    val fileId = if (tuple.length() > mediaOffset + 1 && !tuple.isNull(mediaOffset + 1)) {
                        tuple.optString(mediaOffset + 1).takeIf { it.isNotEmpty() }
                    } else null

                    val cryptoKey = if (tuple.length() > mediaOffset + 2 && !tuple.isNull(mediaOffset + 2)) {
                        tuple.optString(mediaOffset + 2).takeIf { it.isNotEmpty() }
                    } else null

                    val cryptoIv = if (tuple.length() > mediaOffset + 3 && !tuple.isNull(mediaOffset + 3)) {
                        tuple.optString(mediaOffset + 3).takeIf { it.isNotEmpty() }
                    } else null

                    val localPath = if (tuple.length() > mediaOffset + 4 && !tuple.isNull(mediaOffset + 4)) {
                        tuple.optString(mediaOffset + 4).takeIf { it.isNotEmpty() }
                    } else null

                    val fileSize = if (tuple.length() > mediaOffset + 5 && !tuple.isNull(mediaOffset + 5)) {
                        tuple.optLong(mediaOffset + 5, 0L)
                    } else 0L

                    val blurPreview = if (tuple.length() > mediaOffset + 6 && !tuple.isNull(mediaOffset + 6)) {
                        tuple.optString(mediaOffset + 6).takeIf { it.isNotEmpty() }
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

            val validIds = list.map { it.id }.toSet()
            var hadOrphanQuotes = false
            val now = System.currentTimeMillis()

            for (i in list.indices) {
                val msg = list[i]
                if (msg.replyToId != null) {
                    val isStatusQuote = msg.replyToId.startsWith("STATUS_")
                    val isStatusExpired = if (isStatusQuote) {
                        val ts = msg.replyToId.removePrefix("STATUS_").toLongOrNull() ?: 0L
                        ts > 0L && (now - ts >= 24 * 60 * 60 * 1000L)
                    } else false

                    if ((!isStatusQuote && msg.replyToId !in validIds) || isStatusExpired) {
                        list[i] = msg.copy(
                            replyToId = null,
                            replyToText = null,
                            replyToSenderId = null
                        )
                        hadOrphanQuotes = true
                    }
                }
            }

            if (hadOrphanQuotes) {
                writeMessagesToFile(context, list)
            }

            _messagesFlow.value = list
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = true
        }
    }

    suspend fun purgeExpiredStatusQuotes(context: Context, activeStatusIds: Set<String>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appContext = context.applicationContext
            ensureLoadedLocked(appContext)

            val currentList = _messagesFlow.value.toMutableList()
            var modified = false

            for (i in currentList.indices) {
                val msg = currentList[i]
                if (msg.replyToId != null && msg.replyToId.startsWith("STATUS_")) {
                    if (msg.replyToId !in activeStatusIds) {
                        currentList[i] = msg.copy(
                            replyToId = null,
                            replyToText = null,
                            replyToSenderId = null
                        )
                        modified = true
                    }
                }
            }

            if (modified) {
                writeMessagesToFile(appContext, currentList)
                _messagesFlow.value = currentList
            }
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

    suspend fun editMessageText(context: Context, messageId: String, newText: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appContext = context.applicationContext
            ensureLoadedLocked(appContext)

            val currentList = _messagesFlow.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == messageId }

            if (index >= 0) {
                val existing = currentList[index]
                if (existing.text != newText) {
                    currentList[index] = existing.copy(text = newText)
                    writeMessagesToFile(appContext, currentList)
                    _messagesFlow.value = currentList
                }
                true
            } else {
                false
            }
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

            var quotesPurged = false
            for (i in currentList.indices) {
                val msg = currentList[i]
                if (msg.replyToId != null && msg.replyToId in messageIds) {
                    currentList[i] = msg.copy(
                        replyToId = null,
                        replyToText = null,
                        replyToSenderId = null
                    )
                    quotesPurged = true
                }
            }

            if (wasRemoved || quotesPurged) {
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

                val hasReply = !msg.replyToId.isNullOrEmpty()
                val hasMedia = msg.mediaType > 0 || !msg.fileId.isNullOrEmpty() || !msg.localPath.isNullOrEmpty()

                if (hasReply || hasMedia) {
                    tuple.put(msg.replyToId ?: JSONObject.NULL)

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
