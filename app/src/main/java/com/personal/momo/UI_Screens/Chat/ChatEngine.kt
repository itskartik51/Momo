package com.personal.momo.UI_Screens.Chat

import android.content.Context
import android.net.Uri
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Chat.Media.FirebaseMediaManager
import com.personal.momo.UI_Screens.Chat.Media.MediaCompressor
import com.personal.momo.UI_Screens.Chat.Media.MediaCrypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class PartnerPresence(
    val inChat: Boolean = false,
    val lastSeen: Long = 0L
)

data class MediaTransferProgress(
    val stage: String,
    val progress: Float
)

object ChatEngine {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    @Volatile
    private var appContextRef: Context? = null

    @Volatile
    private var isEngineRunning = false

    @Volatile
    private var isChatScreenActive = false

    @Volatile
    private var boundMyCode: Int = -1

    private var inboxRef: DatabaseReference? = null
    private var inboxListener: ChildEventListener? = null

    private var receiptsRef: DatabaseReference? = null
    private var receiptsListener: ChildEventListener? = null

    private var partnerPresenceRef: DatabaseReference? = null
    private var partnerPresenceListener: ValueEventListener? = null

    private val _partnerPresenceFlow = MutableStateFlow(PartnerPresence())
    val partnerPresenceFlow: StateFlow<PartnerPresence> = _partnerPresenceFlow.asStateFlow()

    private val _mediaProgressFlow = MutableStateFlow(emptyMap<String, MediaTransferProgress>())
    val mediaProgressFlow: StateFlow> = _mediaProgressFlow.asStateFlow()

    fun getMySenderCode(context: Context? = appContextRef): Int {
        val currentUserId = if (context != null) {
            CacheManager.getAppUserId(context.applicationContext)
        } else {
            CacheManager.appUserIdFlow.value
        }
        return if (currentUserId.equals("Momo", ignoreCase = true)) 2 else 1
    }

    fun getPartnerSenderCode(context: Context? = appContextRef): Int {
        return if (getMySenderCode(context) == 1) 2 else 1
    }

    fun updateMediaProgress(messageId: String, stage: String, progress: Float) {
        _mediaProgressFlow.value = _mediaProgressFlow.value + (messageId to MediaTransferProgress(stage, progress))
    }

    fun clearMediaProgress(messageId: String) {
        _mediaProgressFlow.value = _mediaProgressFlow.value - messageId
    }

    fun startEngine(context: Context) {
        val appContext = context.applicationContext
        appContextRef = appContext

        CacheManager.getAppUserId(appContext)

        if (isEngineRunning) {
            val currentCode = getMySenderCode(appContext)
            if (boundMyCode != -1 && boundMyCode != currentCode) {
                bindUserListeners(appContext, currentCode)
            }
            return
        }
        isEngineRunning = true

        engineScope.launch {
            ChatRepository.init(appContext)
            val initialCode = getMySenderCode(appContext)
            bindUserListeners(appContext, initialCode)
            listenForNetworkReconnect(appContext)

            CacheManager.appUserIdFlow.collect { userId ->
                val newCode = if (userId.equals("Momo", ignoreCase = true)) 2 else 1
                if (boundMyCode != newCode) {
                    bindUserListeners(appContext, newCode)
                }
            }
        }
    }

    @Synchronized
    private fun bindUserListeners(context: Context, myCode: Int) {
        detachUserListeners()
        boundMyCode = myCode
        val partnerCode = if (myCode == 1) 2 else 1

        syncMyPresenceWithCurrentState(myCode)
        listenToPartnerPresence(partnerCode)
        listenForIncomingMessages(context, myCode, partnerCode)
        listenForReceipts(context, myCode)
    }

    @Synchronized
    private fun detachUserListeners() {
        inboxListener?.let { inboxRef?.removeEventListener(it) }
        inboxListener = null
        inboxRef = null

        receiptsListener?.let { receiptsRef?.removeEventListener(it) }
        receiptsListener = null
        receiptsRef = null

        partnerPresenceListener?.let { partnerPresenceRef?.removeEventListener(it) }
        partnerPresenceListener = null
        partnerPresenceRef = null
    }

    private fun syncMyPresenceWithCurrentState(myCode: Int = getMySenderCode()) {
        val presenceRef = database.reference
            .child("momo_chat")
            .child("presence")
            .child(myCode.toString())

        if (isChatScreenActive) {
            val disconnectUpdates = mapOf<String, Any>(
                "inChat" to false,
                "lastSeen" to ServerValue.TIMESTAMP
            )
            presenceRef.onDisconnect().updateChildren(disconnectUpdates)

            val activeUpdates = mapOf<String, Any>(
                "inChat" to true,
                "lastSeen" to ServerValue.TIMESTAMP
            )
            presenceRef.updateChildren(activeUpdates)
        } else {
            presenceRef.onDisconnect().cancel()
            presenceRef.child("inChat").onDisconnect().setValue(false)
            presenceRef.child("inChat").setValue(false)
        }
    }

    private fun listenToPartnerPresence(partnerCode: Int) {
        val ref = database.reference
            .child("momo_chat")
            .child("presence")
            .child(partnerCode.toString())

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inChat = snapshot.child("inChat").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                _partnerPresenceFlow.value = PartnerPresence(
                    inChat = inChat,
                    lastSeen = lastSeen
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        partnerPresenceRef = ref
        partnerPresenceListener = listener
        ref.addValueEventListener(listener)
    }

    fun setChatScreenActive(context: Context, active: Boolean) {
        val appContext = context.applicationContext
        appContextRef = appContext
        val wasActive = isChatScreenActive
        isChatScreenActive = active

        val myCode = getMySenderCode(appContext)
        val presenceRef = database.reference
            .child("momo_chat")
            .child("presence")
            .child(myCode.toString())

        if (active) {
            val disconnectUpdates = mapOf<String, Any>(
                "inChat" to false,
                "lastSeen" to ServerValue.TIMESTAMP
            )
            presenceRef.onDisconnect().updateChildren(disconnectUpdates)

            val activeUpdates = mapOf<String, Any>(
                "inChat" to true,
                "lastSeen" to ServerValue.TIMESTAMP
            )
            presenceRef.updateChildren(activeUpdates)
            markChatAsRead(appContext)
        } else {
            presenceRef.onDisconnect().cancel()
            presenceRef.child("inChat").onDisconnect().setValue(false)

            if (wasActive) {
                val inactiveUpdates = mapOf<String, Any>(
                    "inChat" to false,
                    "lastSeen" to ServerValue.TIMESTAMP
                )
                presenceRef.updateChildren(inactiveUpdates)
            } else {
                presenceRef.child("inChat").setValue(false)
            }
        }
    }

    fun sendMessage(
        context: Context,
        text: String,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSenderId: Int? = null
    ) {
        val appContext = context.applicationContext
        appContextRef = appContext
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val myCode = getMySenderCode(appContext)
        val partnerCode = getPartnerSenderCode(appContext)
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val pendingMessage = ChatMessage(
            id = messageId,
            senderId = myCode,
            text = cleanText,
            timestamp = timestamp,
            status = 0,
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSenderId = replyToSenderId
        )

        engineScope.launch {
            ChatRepository.saveMessage(appContext, pendingMessage)
            pushToFirebaseQueue(appContext, pendingMessage, partnerCode)
        }
    }

    fun sendMediaMessage(
        context: Context,
        mediaUri: Uri,
        mediaType: Int,
        caption: String = "",
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSenderId: Int? = null
    ) {
        val appContext = context.applicationContext
        appContextRef = appContext

        val myCode = getMySenderCode(appContext)
        val partnerCode = getPartnerSenderCode(appContext)
        val messageId = UUID.randomUUID().toString()
        val fileId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val cleanCaption = caption.trim()

        engineScope.launch {
            try {
                val mediaFolder = File(appContext.filesDir, "chat_media").apply {
                    if (!exists()) mkdirs()
                }
                val extension = if (mediaType == 2) "mp4" else "jpg"
                val permanentFileName = "MEDIA_${UUID.randomUUID().toString().take(8)}.$extension"
                val permanentLocalFile = File(mediaFolder, permanentFileName)

                context.contentResolver.openInputStream(mediaUri)?.use { input ->
                    FileOutputStream(permanentLocalFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalArgumentException("Cannot open stream for mediaUri: $mediaUri")

                val pendingMessage = ChatMessage(
                    id = messageId,
                    senderId = myCode,
                    text = cleanCaption,
                    timestamp = timestamp,
                    status = 0,
                    replyToId = replyToId,
                    replyToText = replyToText,
                    replyToSenderId = replyToSenderId,
                    mediaType = mediaType,
                    fileId = fileId,
                    cryptoKey = null,
                    cryptoIv = null,
                    localPath = permanentFileName
                )
                ChatRepository.saveMessage(appContext, pendingMessage)

                val fileToEncrypt = try {
                    val fileUri = Uri.fromFile(permanentLocalFile)
                    if (mediaType == 2) {
                        updateMediaProgress(messageId, "Compressing", 0f)
                        val compressed = MediaCompressor.compressVideo(appContext, fileUri) { progress ->
                            updateMediaProgress(messageId, "Compressing", progress)
                        }
                        if (compressed.exists() && compressed.length() > 0L) {
                            compressed.copyTo(permanentLocalFile, overwrite = true)
                            compressed.delete()
                        }
                        permanentLocalFile
                    } else {
                        val compressed = MediaCompressor.compressImage(appContext, fileUri)
                        if (compressed.exists() && compressed.length() > 0L) {
                            compressed.copyTo(permanentLocalFile, overwrite = true)
                            compressed.delete()
                        }
                        permanentLocalFile
                    }
                } catch (compEx: Exception) {
                    compEx.printStackTrace()
                    permanentLocalFile
                }

                val encResult = MediaCrypto.encryptFile(appContext, fileToEncrypt)

                val readyMessage = pendingMessage.copy(
                    cryptoKey = encResult.base64Key,
                    cryptoIv = encResult.base64Iv
                )
                ChatRepository.saveMessage(appContext, readyMessage)

                updateMediaProgress(messageId, "Sending", 0f)
                val uploadResult = FirebaseMediaManager.uploadEncryptedMedia(
                    encryptedFile = encResult.encryptedFile,
                    fileId = fileId
                ) { progress ->
                    updateMediaProgress(messageId, "Sending", progress)
                }

                if (encResult.encryptedFile.exists()) {
                    encResult.encryptedFile.delete()
                }

                if (uploadResult.isSuccess) {
                    updateMediaProgress(messageId, "Sending", 1f)
                    delay(200L)
                    pushToFirebaseQueue(appContext, readyMessage, partnerCode)
                } else {
                    uploadResult.exceptionOrNull()?.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                clearMediaProgress(messageId)
            }
        }
    }

    private fun pushToFirebaseQueue(context: Context, message: ChatMessage, receiverCode: Int) {
        val payload = mutableMapOf<String, Any>(
            "id" to message.id,
            "send" to message.senderId,
            "text" to message.text,
            "time" to message.timestamp,
            "status" to 1
        )

        if (!message.replyToId.isNullOrEmpty()) {
            payload["rId"] = message.replyToId
        }
        if (!message.replyToText.isNullOrEmpty()) {
            payload["rText"] = message.replyToText
        }
        if (message.replyToSenderId != null) {
            payload["rSend"] = message.replyToSenderId
        }

        if (message.mediaType > 0) {
            payload["mType"] = message.mediaType
            message.fileId?.let { payload["fId"] = it }
            message.cryptoKey?.let { payload["cKey"] = it }
            message.cryptoIv?.let { payload["cIv"] = it }
        }

        val queueRef = database.reference
            .child("momo_chat")
            .child("messages")
            .child(receiverCode.toString())
            .child(message.id)

        queueRef.setValue(payload).addOnSuccessListener {
            engineScope.launch {
                ChatRepository.updateMessageStatus(context, message.id, 1)
            }
        }
    }

    private fun listenForIncomingMessages(context: Context, myCode: Int, partnerCode: Int) {
        val ref = database.reference
            .child("momo_chat")
            .child("messages")
            .child(myCode.toString())

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val messageId = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: return
                val senderCode = snapshot.child("send").getValue(Long::class.java)?.toInt() ?: partnerCode

                if (senderCode == myCode) {
                    snapshot.ref.removeValue()
                    return
                }

                val text = snapshot.child("text").getValue(String::class.java) ?: ""
                val timestamp = snapshot.child("time").getValue(Long::class.java)?.toLong() ?: System.currentTimeMillis()

                val replyToId = snapshot.child("rId").getValue(String::class.java)
                    ?: snapshot.child("replyToId").getValue(String::class.java)
                val replyToText = snapshot.child("rText").getValue(String::class.java)
                    ?: snapshot.child("replyToText").getValue(String::class.java)
                val replyToSenderId = snapshot.child("rSend").getValue(Long::class.java)?.toInt()
                    ?: snapshot.child("replyToSenderId").getValue(Long::class.java)?.toInt()

                val mType = snapshot.child("mType").getValue(Long::class.java)?.toInt()
                    ?: snapshot.child("mediaType").getValue(Long::class.java)?.toInt() ?: 0
                val fId = snapshot.child("fId").getValue(String::class.java)
                    ?: snapshot.child("fileId").getValue(String::class.java)
                val cKey = snapshot.child("cKey").getValue(String::class.java)
                    ?: snapshot.child("cryptoKey").getValue(String::class.java)
                val cIv = snapshot.child("cIv").getValue(String::class.java)
                    ?: snapshot.child("cryptoIv").getValue(String::class.java)

                val initialDeliveryStatus = if (isChatScreenActive) 3 else 2

                val incomingMessage = ChatMessage(
                    id = messageId,
                    senderId = senderCode,
                    text = text,
                    timestamp = timestamp,
                    status = initialDeliveryStatus,
                    replyToId = replyToId,
                    replyToText = replyToText,
                    replyToSenderId = replyToSenderId,
                    mediaType = mType,
                    fileId = fId,
                    cryptoKey = cKey,
                    cryptoIv = cIv,
                    localPath = null
                )

                engineScope.launch {
                    ChatRepository.saveMessage(context, incomingMessage)
                    snapshot.ref.removeValue()
                    sendReceipt(senderCode, messageId, initialDeliveryStatus)

                    if (mType > 0 && !fId.isNullOrEmpty() && !cKey.isNullOrEmpty() && !cIv.isNullOrEmpty()) {
                        downloadAndDecryptMedia(context, incomingMessage)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        inboxRef = ref
        inboxListener = listener
        ref.addChildEventListener(listener)
    }

    private fun downloadAndDecryptMedia(context: Context, message: ChatMessage) {
        val fileId = message.fileId ?: return
        val cryptoKey = message.cryptoKey ?: return
        val cryptoIv = message.cryptoIv ?: return

        engineScope.launch {
            val cacheFolder = File(context.cacheDir, "media_cache").apply {
                if (!exists()) mkdirs()
            }
            val tempEncFile = File(cacheFolder, "TEMP_${fileId}.enc")

            val downloadResult = FirebaseMediaManager.downloadEncryptedMedia(fileId, tempEncFile)
            if (downloadResult.isSuccess) {
                val extension = if (message.mediaType == 2) "mp4" else "jpg"
                try {
                    val decryptedFile = MediaCrypto.decryptFile(
                        context = context,
                        encryptedFile = tempEncFile,
                        base64Key = cryptoKey,
                        base64Iv = cryptoIv,
                        outputExtension = extension
                    )

                    ChatRepository.saveMessage(
                        context,
                        message.copy(localPath = decryptedFile.name)
                    )

                    FirebaseMediaManager.deleteMediaFromFirebase(fileId)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (tempEncFile.exists()) {
                        tempEncFile.delete()
                    }
                }
            }
        }
    }

    private fun listenForReceipts(context: Context, myCode: Int) {
        val ref = database.reference
            .child("momo_chat")
            .child("receipts")
            .child(myCode.toString())

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleReceiptSnapshot(context, snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleReceiptSnapshot(context, snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        receiptsRef = ref
        receiptsListener = listener
        ref.addChildEventListener(listener)
    }

    private fun handleReceiptSnapshot(context: Context, snapshot: DataSnapshot) {
        val messageId = snapshot.key ?: return
        val newStatus = snapshot.getValue(Long::class.java)?.toInt() ?: return

        engineScope.launch {
            var updated = ChatRepository.updateMessageStatus(context, messageId, newStatus)
            if (!updated) {
                delay(150L)
                updated = ChatRepository.updateMessageStatus(context, messageId, newStatus)
            }
            snapshot.ref.removeValue()
        }
    }

    fun markChatAsRead(context: Context) {
        val appContext = context.applicationContext
        appContextRef = appContext
        val myCode = getMySenderCode(appContext)
        val partnerCode = getPartnerSenderCode(appContext)

        engineScope.launch {
            val readMessageIds = ChatRepository.markIncomingMessagesAsRead(appContext, myCode)
            for (msgId in readMessageIds) {
                sendReceipt(partnerCode, msgId, 3)
            }
        }
    }

    private fun sendReceipt(targetSenderCode: Int, messageId: String, status: Int) {
        database.reference
            .child("momo_chat")
            .child("receipts")
            .child(targetSenderCode.toString())
            .child(messageId)
            .setValue(status)
    }

    private fun listenForNetworkReconnect(context: Context) {
        val connectedRef = database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isConnected = snapshot.getValue(Boolean::class.java) ?: false
                if (isConnected) {
                    val myCode = getMySenderCode(context)
                    syncMyPresenceWithCurrentState(myCode)
                    retryPendingMessages(context)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun retryPendingMessages(context: Context) {
        val partnerCode = getPartnerSenderCode(context)
        engineScope.launch {
            val pendingList = ChatRepository.getPendingMessages()
            for (message in pendingList) {
                if (message.mediaType == 0) {
                    pushToFirebaseQueue(context, message, partnerCode)
                } else if (!message.fileId.isNullOrEmpty() && !message.cryptoKey.isNullOrEmpty() && !message.cryptoIv.isNullOrEmpty()) {
                    pushToFirebaseQueue(context, message, partnerCode)
                }
            }
        }
    }
}
