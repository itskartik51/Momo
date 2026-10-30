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
import java.io.File

/**
 * Message Model based on Ultra-Compact Tuple Format:
 * [id, send, text, time, status]
 *
 * Index 0 -> id: String (UUID)
 * Index 1 -> senderId: Int (1: Kanu, 2: Momo)
 * Index 2 -> text: String (Message content)
 * Index 3 -> timestamp: Long (Epoch ms)
 * Index 4 -> status: Int (0: Pending, 1: Sent, 2: Delivered, 3: Read)
 */
data class ChatMessage(
    val id: String,
    val senderId: Int,
    val text: String,
    val timestamp: Long,
    val status: Int
)

object ChatRepository {
    private const val FILE_NAME = "momo_chat_history.json"
    private val mutex = Mutex()

    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messagesFlow: StateFlow<List<ChatMessage>> = _messagesFlow.asStateFlow()

    private var isInitialized = false

    suspend fun init(context: Context) {
        if (isInitialized) return
        loadMessages(context)
        isInitialized = true
    }

    suspend fun loadMessages(context: Context): List<ChatMessage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                _messagesFlow.value = emptyList()
                return@withLock emptyList()
            }

            try {
                val content = file.readText().trim()
                if (content.isEmpty() || content == "[]") {
                    _messagesFlow.value = emptyList()
                    return@withLock emptyList()
                }

                val jsonArray = JSONArray(content)
                val list = ArrayList<ChatMessage>(jsonArray.length())

                for (i in 0 until jsonArray.length()) {
                    val tuple = jsonArray.optJSONArray(i) ?: continue
                    if (tuple.length() >= 5) {
                        list.add(
                            ChatMessage(
                                id = tuple.optString(0, ""),
                                senderId = tuple.optInt(1, 1),
                                text = tuple.optString(2, ""),
                                timestamp = tuple.optLong(3, 0L),
                                status = tuple.optInt(4, 0)
                            )
                        )
                    }
                }

                _messagesFlow.value = list
                list
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun saveMessage(context: Context, message: ChatMessage) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentList = _messagesFlow.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.id == message.id }

            if (existingIndex >= 0) {
                currentList[existingIndex] = message
            } else {
                currentList.add(message)
            }

            writeMessagesToFile(context, currentList)
            _messagesFlow.value = currentList
        }
    }

    suspend fun updateMessageStatus(context: Context, messageId: String, newStatus: Int) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentList = _messagesFlow.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == messageId }

            if (index >= 0) {
                val existing = currentList[index]
                // Only move status forward (0 -> 1 -> 2 -> 3)
                if (newStatus > existing.status) {
                    currentList[index] = existing.copy(status = newStatus)
                    writeMessagesToFile(context, currentList)
                    _messagesFlow.value = currentList
                }
            }
        }
    }

    suspend fun getPendingMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        _messagesFlow.value.filter { it.status == 0 }
    }

    suspend fun markIncomingMessagesAsRead(context: Context, mySenderId: Int): List<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
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
                writeMessagesToFile(context, currentList)
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
                rootArray.put(tuple)
            }

            val file = File(context.filesDir, FILE_NAME)
            file.writeText(rootArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
