package com.personal.momo.UI_Screens.Call

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.personal.momo.SecurityConfig

// Data Model for Past Call Records
data class CallLogItem(
    val id: String = "",
    val callerId: Int = 1,
    val durationSeconds: Int = 0,
    val timestamp: Long = 0L
)

object FirestoreCallService {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var signalingListener: ListenerRegistration? = null

    fun ensureAuth(onReady: () -> Unit, onError: (String) -> Unit = {}) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onReady()
        } else {
            if (SecurityConfig.AUTH_EMAIL.isNotBlank() && SecurityConfig.AUTH_PASS.isNotBlank()) {
                auth.signInWithEmailAndPassword(SecurityConfig.AUTH_EMAIL, SecurityConfig.AUTH_PASS)
                    .addOnSuccessListener { onReady() }
                    .addOnFailureListener { e -> onError(e.localizedMessage ?: "Auth failed") }
            } else {
                onReady()
            }
        }
    }

    fun startSignalingListener(
        myUserId: String,
        onIncomingCall: (caller: String, channel: String, timestamp: Long) -> Unit,
        onCallConnected: () -> Unit,
        onCallEnded: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (signalingListener != null) return

        ensureAuth(
            onReady = {
                signalingListener = firestore.collection("App").document("current_call")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            onError(error.localizedMessage ?: "Signaling error")
                            return@addSnapshotListener
                        }
                        if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                        val status = snapshot.getString("status") ?: ""
                        val caller = snapshot.getString("caller") ?: ""
                        val receiver = snapshot.getString("receiver") ?: ""
                        val channel = snapshot.getString("channelName") ?: "momo_private_voice_room"
                        val timestamp = snapshot.getLong("timestamp") ?: 0L
                        val isRecent = (System.currentTimeMillis() - timestamp) < 60_000L

                        when (status) {
                            "calling" -> {
                                if (isRecent && receiver.equals(myUserId, ignoreCase = true)) {
                                    onIncomingCall(caller, channel, timestamp)
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
            },
            onError = onError
        )
    }

    fun stopSignalingListener() {
        signalingListener?.remove()
        signalingListener = null
    }

    fun sendCallSignal(
        caller: String,
        receiver: String,
        channelName: String,
        timestamp: Long,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        ensureAuth(
            onReady = {
                val callData = hashMapOf(
                    "caller" to caller,
                    "receiver" to receiver,
                    "status" to "calling",
                    "channelName" to channelName,
                    "timestamp" to timestamp
                )
                firestore.collection("App").document("current_call").set(callData)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            },
            onError = { err -> onFailure(Exception(err)) }
        )
    }

    fun updateCallStatus(status: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        ensureAuth(
            onReady = {
                firestore.collection("App").document("current_call")
                    .update("status", status)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            },
            onError = { err -> onFailure(Exception(err)) }
        )
    }

    fun logCall(timestampKey: Long, callerId: Int, durationSeconds: Int) {
        ensureAuth(
            onReady = {
                val logKey = if (timestampKey > 0L) timestampKey.toString() else System.currentTimeMillis().toString()
                val logValue = listOf(callerId, durationSeconds) // [caller_id, duration_seconds]

                firestore.collection("App").document("call_logs")
                    .update(logKey, logValue)
                    .addOnFailureListener {
                        firestore.collection("App").document("call_logs")
                            .set(mapOf(logKey to logValue), SetOptions.merge())
                    }
            }
        )
    }

    fun observeCallLogs(onLogsUpdated: (List<CallLogItem>) -> Unit): ListenerRegistration {
        return firestore.collection("App").document("call_logs")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    onLogsUpdated(emptyList())
                    return@addSnapshotListener
                }

                val dataMap = snapshot.data ?: emptyMap()
                val parsedList = mutableListOf<CallLogItem>()

                for ((key, value) in dataMap) {
                    val ts = key.toLongOrNull() ?: 0L
                    if (ts == 0L) continue

                    val list = value as? List<*> ?: continue
                    if (list.size >= 2) {
                        val callerId = (list[0] as? Number)?.toInt() ?: 1
                        val duration = (list[1] as? Number)?.toInt() ?: 0

                        parsedList.add(
                            CallLogItem(
                                id = key,
                                callerId = callerId,
                                durationSeconds = duration,
                                timestamp = ts
                            )
                        )
                    }
                }

                val sortedLogs = parsedList.sortedByDescending { it.timestamp }.take(100)
                onLogsUpdated(sortedLogs)
            }
    }
}
