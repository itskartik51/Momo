package com.personal.momo.FCM

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.personal.momo.UI_Screens.Chat.ChatEngine
import com.personal.momo.UI_Screens.Chat.ChatMessage
import com.personal.momo.UI_Screens.Chat.ChatRepository
import com.personal.momo.UI_Screens.Chat.Preference.ChatNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MomoFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        ChatEngine.updateFCMToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        if (data.isEmpty()) return

        val messageId = data["id"] ?: return
        val senderCode = data["send"]?.toIntOrNull() ?: return
        val text = data["text"] ?: ""
        val timestamp = data["time"]?.toLongOrNull() ?: System.currentTimeMillis()
        val mType = data["mType"]?.toIntOrNull() ?: 0
        val fId = data["fId"]?.takeIf { it.isNotBlank() }
        val cKey = data["cKey"]?.takeIf { it.isNotBlank() }
        val cIv = data["cIv"]?.takeIf { it.isNotBlank() }
        val fSize = data["fSize"]?.toLongOrNull() ?: 0L
        val bPrev = data["bPrev"]?.takeIf { it.isNotBlank() }
        val rId = data["rId"]?.takeIf { it.isNotBlank() }
        val rText = data["rText"]?.takeIf { it.isNotBlank() }
        val rSend = data["rSend"]?.toIntOrNull()

        val myCode = ChatEngine.getMySenderCode(applicationContext)

        if (senderCode != myCode) {
            val activeNow = ChatEngine.isChatScreenActive()
            val initialDeliveryStatus = if (activeNow) 3 else 2

            val incomingMessage = ChatMessage(
                id = messageId,
                senderId = senderCode,
                text = text,
                timestamp = timestamp,
                status = initialDeliveryStatus,
                replyToId = rId,
                replyToText = rText,
                replyToSenderId = rSend,
                mediaType = mType,
                fileId = fId,
                cryptoKey = cKey,
                cryptoIv = cIv,
                localPath = null,
                fileSize = fSize,
                blurPreview = bPrev
            )

            serviceScope.launch {
                ChatRepository.init(applicationContext)
                ChatRepository.saveMessage(applicationContext, incomingMessage)

                if (!activeNow) {
                    ChatNotificationManager.onIncomingMessage(applicationContext, myCode)
                }
            }
        }
    }
}
