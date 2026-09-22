package com.personal.momo.UI_Screens.Call

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class CallLogItem(
    val id: String = "",
    val timestamp: Long = 0L,
    val callerCode: Int = 1, // 1: Kanu, 2: Momo
    val durationSeconds: Int = 0 // 0 = Missed/Declined
)

object FirestoreCallService {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var signalingListener: ListenerRegistration? = null

    private const val SIGNAL_DOC_PATH = "App/current_call"
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

        firestore.document(SIGNAL_DOC_PATH)
            .set(payload)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateCallStatus(status: String) {
        firestore.document(SIGNAL_DOC_PATH)
            .update("status", status)
    }

    fun startSignalingListener(
        myUserId: String,
        onIncomingCall: (caller: String, channel: String, timestamp: Long) -> Unit,
        onCallAccepted: () -> Unit,
        onCallConnected: () -> Unit,
        onCallEnded: () -> Unit
    ) {
        signalingListener?.remove()

        signalingListener = firestore.document(SIGNAL_DOC_PATH)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val caller = snapshot.getString("caller") ?: ""
                val receiver = snapshot.getString("receiver") ?: ""
                val channel = snapshot.getString("channelName") ?: ""
                val status = snapshot.getString("status") ?: ""
                val timestamp = snapshot.getLong("timestamp") ?: 0L

                when (status) {
                    "calling" -> {
                        if (receiver.equals(myUserId, ignoreCase = true)) {
                            onIncomingCall(caller, channel, timestamp)
                        }
                    }
                    "accepted" -> {
                        // Triggered when receiver taps accept (signals caller to join Agora)
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
    }

    fun stopSignalingListener() {
        signalingListener?.remove()
        signalingListener = null
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
