package com.personal.momo.UI_Screens.Chat

import android.content.Context
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.personal.momo.Cache.CacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

object ChatEngine {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    private var isEngineRunning = false
    private var isChatScreenActive = false

    // Numeric mapping: 1 for Kanu, 2 for Momo
    fun getMySenderCode(): Int {
        val currentUserId = CacheManager.appUserIdFlow.value ?: ""
        return if (currentUserId.equals("Momo", ignoreCase = true)) 2 else 1
    }

    fun getPartnerSenderCode(): Int {
        return if (getMySenderCode() == 1) 2 else 1
    }

    fun startEngine(context: Context) {
        val appContext = context.applicationContext
        if (isEngineRunning) return
        isEngineRunning = true

        engineScope.launch {
            ChatRepository.init(appContext)
            listenForIncomingMessages(appContext)
            listenForReceipts(appContext)
            listenForNetworkReconnect(appContext)
        }
    }

    fun setChatScreenActive(context: Context, active: Boolean) {
        isChatScreenActive = active
        if (active) {
            markChatAsRead(context.applicationContext)
        }
    }

    fun sendMessage(context: Context, text: String) {
        val appContext = context.applicationContext
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val myCode = getMySenderCode()
        val partnerCode = getPartnerSenderCode()
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val pendingMessage = ChatMessage(
            id = messageId,
            senderId = myCode,
            text = cleanText,
            timestamp = timestamp,
            status = 0 // 0: Pending (Clock icon)
        )

        engineScope.launch {
            ChatRepository.saveMessage(appContext, pendingMessage)
            pushToFirebaseQueue(appContext, pendingMessage, partnerCode)
        }
    }

    private fun pushToFirebaseQueue(context: Context, message: ChatMessage, receiverCode: Int) {
        val payload = mapOf(
            "id" to message.id,
            "send" to message.senderId,
            "text" to message.text,
            "time" to message.timestamp,
            "status" to 1 // 1: Sent
        )

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

    private fun listenForIncomingMessages(context: Context) {
        val myCode = getMySenderCode()
        val myInboxRef = database.reference
            .child("momo_chat")
            .child("messages")
            .child(myCode.toString())

        myInboxRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val messageId = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: return
                val senderCode = snapshot.child("send").getValue(Long::class.java)?.toInt() ?: getPartnerSenderCode()
                val text = snapshot.child("text").getValue(String::class.java) ?: ""
                val timestamp = snapshot.child("time").getValue(Long::class.java)?.toLong() ?: System.currentTimeMillis()

                val initialDeliveryStatus = if (isChatScreenActive) 3 else 2

                val incomingMessage = ChatMessage(
                    id = messageId,
                    senderId = senderCode,
                    text = text,
                    timestamp = timestamp,
                    status = initialDeliveryStatus
                )

                engineScope.launch {
                    ChatRepository.saveMessage(context, incomingMessage)
                    snapshot.ref.removeValue()
                    sendReceipt(senderCode, messageId, initialDeliveryStatus)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForReceipts(context: Context) {
        val myCode = getMySenderCode()
        val myReceiptsRef = database.reference
            .child("momo_chat")
            .child("receipts")
            .child(myCode.toString())

        myReceiptsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val messageId = snapshot.key ?: return
                val newStatus = snapshot.getValue(Long::class.java)?.toInt() ?: 2

                engineScope.launch {
                    ChatRepository.updateMessageStatus(context, messageId, newStatus)
                    snapshot.ref.removeValue()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val messageId = snapshot.key ?: return
                val newStatus = snapshot.getValue(Long::class.java)?.toInt() ?: return

                engineScope.launch {
                    ChatRepository.updateMessageStatus(context, messageId, newStatus)
                    snapshot.ref.removeValue()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun markChatAsRead(context: Context) {
        val appContext = context.applicationContext
        val myCode = getMySenderCode()
        val partnerCode = getPartnerSenderCode()

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
                    retryPendingMessages(context)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun retryPendingMessages(context: Context) {
        val partnerCode = getPartnerSenderCode()
        engineScope.launch {
            val pendingList = ChatRepository.getPendingMessages()
            for (message in pendingList) {
                pushToFirebaseQueue(context, message, partnerCode)
            }
        }
    }
}
