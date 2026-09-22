package com.personal.momo.UI_Screens.Call

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class CallLogItem(
    val id: String = "",
    val timestamp: Long = 0L,
    val callerCode: Int = 1, // 1: Kanu, 2: Momo
    val durationSeconds: Int = 0 // 0 = Missed/Declined
)

/**
 * Hybrid Signaling & Logging Architecture:
 * - Realtime Database (RTDB): Dedicated ~50ms WebSocket for instant calling state handshakes.
 * - Firestore: Permanent storage for structured historical call logs.
 */
object FirestoreCallService {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val rtdb: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    // Persistent WebSocket node for microsecond call signaling
    private val signalRef by lazy { rtdb.getReference("calls/current_call") }
    private var signalListener: ValueEventListener? = null

    private const val CALL_LOGS_COLLECTION = "CallLogs"

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
     * Accurate Call Logging:
     * - If connected: exact start timestamp + actual duration
     * - If missed/declined: exact cut timestamp + 0 seconds
     */
    fun logCall(timestamp: Long, callerCode: Int, durationSeconds: Int) {
        val logData = hashMapOf(
            "timestamp" to timestamp,
            "callerCode" to callerCode,
            "durationSeconds" to durationSeconds
        )

        firestore.collection(CALL_LOGS_COLLECTION)
            .document(timestamp.toString())
            .set(logData)
    }

    fun observeCallLogs(onLogsUpdated: (List<CallLogItem>) -> Unit): ListenerRegistration {
        return firestore.collection(CALL_LOGS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots == null) return@addSnapshotListener
                val list = snapshots.documents.mapNotNull { doc ->
                    val ts = doc.getLong("timestamp") ?: return@mapNotNull null
                    val cc = doc.getLong("callerCode")?.toInt() ?: 1
                    val dur = doc.getLong("durationSeconds")?.toInt() ?: 0
                    CallLogItem(
                        id = doc.id,
                        timestamp = ts,
                        callerCode = cc,
                        durationSeconds = dur
                    )
                }
                onLogsUpdated(list)
            }
    }
}
