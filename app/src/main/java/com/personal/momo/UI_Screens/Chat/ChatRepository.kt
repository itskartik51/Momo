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
 * Message Model based on Ultra-Compact Tuple Format:
 * [id, send, text, time, status, replyToId?, replyToText?, replyToSenderId?]
 *
 * Index 0 -> id: String (UUID)
 * Index 1 -> senderId: Int (1: Kanu, 2: Momo)
 * Index 2 -> text: String (Message content)
 * Index 3 -> timestamp: Long (Epoch ms)
 * Index 4 -> status: Int (0: Pending, 1: Sent, 2: Delivered, 3: Read)
 * Index 5 -> replyToId: String? (Optional replied message ID)
 * Index 6 -> replyToText: String? (Optional replied message text)
 * Index 7 -> replyToSenderId: Int? (Optional replied sender code)
 */
data class ChatMessage(
    val id: String,
    val senderId: Int,
    val text: String,
    val timestamp: Long,
    val status: Int,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderId: Int? = null
)

object ChatRepository {
    private const val FILE_NAME = "momo_chat_history.json"
    private val mutex = Mutex()

    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messagesFlow: StateFlow<List<ChatMessage>> = _messagesFlow.asStateFlow()

    @Volatile
    private var isInitialized = false

    suspend fun init(context: Context) {
        if (isInitialized) return
        loadMessages(context.applicationContext)
    }

    private fun ensureLoadedLocked(context: Context) {
        if (isInitialized) return
        val file = File(context.applicationContext.filesDir, FILE_NAME)
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

                    list.add(
                        ChatMessage(
                            id = tuple.optString(0, ""),
                            senderId = tuple.optInt(1, 1),
                            text = tuple.optString(2, ""),
                            timestamp = tuple.optLong(3, 0L),
                            status = tuple.optInt(4, 0),
                            replyToId = replyToId,
                            replyToText = replyToText,
                            replyToSenderId = replyToSenderId
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

                if (!msg.replyToId.isNullOrEmpty() || !msg.replyToText.isNullOrEmpty() || msg.replyToSenderId != null) {
                    tuple.put(msg.replyToId ?: JSONObject.NULL)
                    tuple.put(msg.replyToText ?: JSONObject.NULL)
                    tuple.put(msg.replyToSenderId ?: JSONObject.NULL)
                }

                rootArray.put(tuple)
            }

            val file = File(context.filesDir, FILE_NAME)
            file.writeText(rootArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
