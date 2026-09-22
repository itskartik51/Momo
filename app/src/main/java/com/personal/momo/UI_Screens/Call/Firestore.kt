package com.personal.momo.UI_Screens.Call

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

data class CallLogItem(
    val id: String = "",
    val timestamp: Long = 0L,
    val callerCode: Int = 1, // 1: Kanu, 2: Momo
    val durationSeconds: Int = 0 // 0 = Missed/Declined
)

/**
 * Hybrid Signaling & Logging Architecture:
 * - Realtime Database (RTDB): Dedicated WebSocket for instant calling state handshakes.
 * - Firestore: Document 'App/call_logs' storing call records as timestamp -> [callerCode, durationSeconds].
 */
object FirestoreCallService {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val rtdb: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    // Persistent WebSocket node for microsecond call signaling
    private val signalRef by lazy { rtdb.getReference("calls/current_call") }
    private var signalListener: ValueEventListener? = null

    private const val APP_COLLECTION = "App"
    private const val CALL_LOGS_DOCUMENT = "call_logs"

    fun sendCallSignal(
        caller: String,
        receiver: String,
        channelName: String,
        timestamp: Long,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val payload = hashMapOf(
            "caller" to caller,
            "receiver" to receiver,
            "channelName" to channelName,
            "status" to "calling",
            "timestamp" to timestamp
        )

        signalRef.setValue(payload)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateCallStatus(status: String) {
        signalRef.child("status").setValue(status)
    }

    fun startSignalingListener(
        myUserId: String,
        onIncomingCall: (caller: String, channel: String, timestamp: Long) -> Unit,
        onCallAccepted: () -> Unit,
        onCallConnected: () -> Unit,
        onCallEnded: () -> Unit
    ) {
        stopSignalingListener()

        signalListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val caller = snapshot.child("caller").getValue(String::class.java) ?: ""
                val receiver = snapshot.child("receiver").getValue(String::class.java) ?: ""
                val channel = snapshot.child("channelName").getValue(String::class.java) ?: ""
                val status = snapshot.child("status").getValue(String::class.java) ?: ""
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                when (status) {
                    "calling" -> {
                        if (receiver.equals(myUserId, ignoreCase = true)) {
                            onIncomingCall(caller, channel, timestamp)
                        }
                    }
                    "accepted" -> {
                        if (caller.equals(myUserId, ignoreCase = true)) {
                            onCallAccepted()
                        }
                    }
                    "connected" -> {
                        onCallConnected()
                    }
                    "ended" -> {
                        onCallEnded()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Connection listener fallback
            }
        }

        signalRef.addValueEventListener(signalListener as ValueEventListener)
    }

    fun stopSignalingListener() {
        signalListener?.let {
            signalRef.removeEventListener(it)
            signalListener = null
        }
    }

    /**
     * Saves call record into Firestore:
     * Collection: "App" -> Document: "call_logs"
     * Format: "timestamp" : [callerCode, durationSeconds]
     * Uses SetOptions.merge() so existing logs are never overwritten.
     */
    fun logCall(timestamp: Long, callerCode: Int, durationSeconds: Int) {
        val key = timestamp.toString()
        val entryData = mapOf(
            key to listOf(callerCode, durationSeconds)
        )

        firestore.collection(APP_COLLECTION)
            .document(CALL_LOGS_DOCUMENT)
            .set(entryData, SetOptions.merge())
    }

    /**
     * Observes 'App/call_logs' document in real-time, parses array fields,
     * and sorts items in descending order (latest calls on top).
     */
    fun observeCallLogs(onLogsUpdated: (List<CallLogItem>) -> Unit): ListenerRegistration {
        return firestore.collection(APP_COLLECTION)
            .document(CALL_LOGS_DOCUMENT)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) {
                    onLogsUpdated(emptyList())
                    return@addSnapshotListener
                }

                val rawData = snapshot.data ?: emptyMap<String, Any>()
                val parsedLogs = rawData.mapNotNull { (key, value) ->
                    val ts = key.toLongOrNull() ?: return@mapNotNull null
                    val arrayList = value as? List<*> ?: return@mapNotNull null
                    val callerCode = (arrayList.getOrNull(0) as? Number)?.toInt() ?: 1
                    val duration = (arrayList.getOrNull(1) as? Number)?.toInt() ?: 0

                    CallLogItem(
                        id = key,
                        timestamp = ts,
                        callerCode = callerCode,
                        durationSeconds = duration
                    )
                }.sortedByDescending { it.timestamp }

                onLogsUpdated(parsedLogs)
            }
    }
}
