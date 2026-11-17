package com.personal.momo

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.personal.momo.Cache.CacheManager
import com.personal.momo.Proximity.ProximityLocationService
import com.personal.momo.UI_Screens.Call.ActiveCallScreen
import com.personal.momo.UI_Screens.Call.CallManager
import com.personal.momo.UI_Screens.Call.IncomingCallNotifier
import com.personal.momo.UI_Screens.Call.IncomingCallView
import com.personal.momo.UI_Screens.Chat.ChatEngine
import com.personal.momo.UI_Screens.MainScreen
import com.personal.momo.UI_Screens.MomoTheme
import com.personal.momo.UI_Screens.Settings.cleanOldUpdateApks
import com.personal.momo.UI_Screens.bounceClick
import java.security.MessageDigest

class MainActivity : FragmentActivity() {

    private var isLaunchedForCall = false
    private var servicesStarted = false
    private var isDeviceVerifiedState = mutableStateOf(false)

    private var allowedHashesRef: DatabaseReference? = null
    private var allowedHashesListener: ValueEventListener? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var verificationTimeoutRunnable: Runnable? = null
    private var focusFallbackRunnable: Runnable? = null

    private var pendingHashToCopy: String? = null
    private var isHandlingUnauthorized = false
    private var hasCopiedAndScheduledExit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cleanOldUpdateApks(this)

        val currentDeviceHash = computeDeviceHash()
        if (currentDeviceHash.isNullOrBlank()) {
            killAppImmediately()
            return
        }

        val cachedHash = CacheManager.getVerifiedDeviceHash(applicationContext)
        val isCachedMatch = (cachedHash == currentDeviceHash)
        isDeviceVerifiedState.value = isCachedMatch

        if (isCachedMatch) {
            startAuthorizedServices(intent)
        }

        startDeviceAuthSnapshotSync(
            currentDeviceHash = currentDeviceHash,
            initialIntent = intent,
            wasInitiallyCached = isCachedMatch
        )

        setContent {
            MomoTheme {
                val isDeviceVerified by isDeviceVerifiedState

                if (!isDeviceVerified) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else {
                    val isLockConfigured = remember { CacheManager.isSecurityLockEnabled(this) }
                    var isUnlocked by remember { mutableStateOf(!isLockConfigured) }
                    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()

                    // Keep screen awake while call is ringing or connected
                    LaunchedEffect(CallManager.isIncomingCall, CallManager.isCallActive) {
                        if (CallManager.isIncomingCall || CallManager.isCallActive) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    val requiredPermissions = remember {
                        buildList {
                            add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            add(android.Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val fineLocGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
                        val coarseLocGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        if (fineLocGranted || coarseLocGranted) {
                            ProximityLocationService.startService(this@MainActivity)
                        }
                    }

                    // Biometric Gatekeeper: Bypassed during active incoming/outgoing ringing state
                    LaunchedEffect(isLockConfigured, isUnlocked, CallManager.isIncomingCall, CallManager.isCallActive) {
                        if (isLockConfigured && !isUnlocked && !CallManager.isIncomingCall && !CallManager.isCallActive) {
                            promptBiometricUnlock(
                                onSuccess = { isUnlocked = true },
                                onExit = { finishAndRemoveTask() }
                            )
                        }
                    }

                    LaunchedEffect(isUnlocked) {
                        if (isUnlocked) {
                            val hasLocationPermission = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasLocationPermission) {
                                permissionLauncher.launch(requiredPermissions.toTypedArray())
                            } else {
                                ProximityLocationService.startService(this@MainActivity)
                            }
                        }
                    }

                    // Strict Priority Hierarchy: Incoming Call > Active Call > Lock Gatekeeper > Main Screen
                    when {
                        CallManager.isIncomingCall -> {
                            IncomingCallView(
                                avatarUrl = avatarUrl,
                                callerName = CallManager.incomingCallerName,
                                onAccept = {
                                    val hasAudioPerm = ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        android.Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasAudioPerm) {
                                        CallManager.acceptIncomingCall(this@MainActivity)
                                    } else {
                                        permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
                                    }
                                },
                                onDecline = {
                                    val launchedForCallSnapshot = isLaunchedForCall
                                    CallManager.declineIncomingCall(this@MainActivity)
                                    if (launchedForCallSnapshot || !isUnlocked) {
                                        finishAndRemoveTask()
                                    }
                                }
                            )
                        }
                        CallManager.isCallActive -> {
                            ActiveCallScreen(
                                onEndCall = {
                                    val launchedForCallSnapshot = isLaunchedForCall
                                    CallManager.endCall(this@MainActivity)
                                    if (launchedForCallSnapshot || !isUnlocked) {
                                        finishAndRemoveTask()
                                    }
                                }
                            )
                        }
                        !isUnlocked -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .bounceClick(scaleDown = 0.90f) {
                                                promptBiometricUnlock(
                                                    onSuccess = { isUnlocked = true },
                                                    onExit = { finishAndRemoveTask() }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Unlock Momo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(42.dp)
                                        )
                                    }

                                    Text(
                                        text = "Momo is Locked",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Tap icon to unlock",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        else -> {
                            MainScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val hash = pendingHashToCopy
            if (!hash.isNullOrBlank()) {
                pendingHashToCopy = null
                cancelFocusFallback()
                copyHashAndScheduleExit(hash)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isDeviceVerifiedState.value) {
            configureLockScreenVisibility()
            handleIncomingIntent(intent)
        }
    }

    override fun onDestroy() {
        cancelVerificationTimeout()
        cancelFocusFallback()
        detachDeviceAuthListener()
        super.onDestroy()
    }

    private fun startAuthorizedServices(activeIntent: Intent?) {
        if (servicesStarted) return
        servicesStarted = true
        CallManager.startSignalingListener(applicationContext)
        ChatEngine.startEngine(applicationContext)
        configureLockScreenVisibility()
        handleIncomingIntent(activeIntent)
    }

    private fun startDeviceAuthSnapshotSync(
        currentDeviceHash: String,
        initialIntent: Intent?,
        wasInitiallyCached: Boolean
    ) {
        if (!wasInitiallyCached) {
            verificationTimeoutRunnable = Runnable {
                if (!isDeviceVerifiedState.value) {
                    detachDeviceAuthListener()
                    handleUnauthorizedFailure(currentDeviceHash)
                }
            }.also { runnable ->
                mainHandler.postDelayed(runnable, 5000L)
            }
        }

        ensureFirebaseAuth(
            onSuccess = {
                attachAllowedHashesSnapshotListener(currentDeviceHash, initialIntent)
            },
            onFailure = {
                if (!wasInitiallyCached) {
                    cancelVerificationTimeout()
                    handleUnauthorizedFailure(currentDeviceHash)
                }
            }
        )
    }

    private fun ensureFirebaseAuth(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onSuccess()
        } else if (SecurityConfig.AUTH_EMAIL.isNotBlank() && SecurityConfig.AUTH_PASS.isNotBlank()) {
            auth.signInWithEmailAndPassword(SecurityConfig.AUTH_EMAIL, SecurityConfig.AUTH_PASS)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure() }
        } else {
            onSuccess()
        }
    }

    private fun attachAllowedHashesSnapshotListener(
        currentDeviceHash: String,
        initialIntent: Intent?
    ) {
        detachDeviceAuthListener()

        val ref = FirebaseDatabase.getInstance().reference
            .child("momo_chat")
            .child("security")
            .child("allowed_hashes")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cancelVerificationTimeout()

                val remoteHashes = snapshot.children.mapNotNull { child ->
                    child.getValue(String::class.java)?.trim()?.lowercase()
                }.filter { it.isNotEmpty() }

                val isAuthorizedInDb = remoteHashes.contains(currentDeviceHash)

                if (isAuthorizedInDb) {
                    CacheManager.setVerifiedDeviceHash(applicationContext, currentDeviceHash)
                    if (!isDeviceVerifiedState.value) {
                        isDeviceVerifiedState.value = true
                    }
                    startAuthorizedServices(initialIntent)
                } else {
                    CacheManager.setVerifiedDeviceHash(applicationContext, null)
                    detachDeviceAuthListener()
                    handleUnauthorizedFailure(currentDeviceHash)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isDeviceVerifiedState.value) {
                    cancelVerificationTimeout()
                    detachDeviceAuthListener()
                    handleUnauthorizedFailure(currentDeviceHash)
                }
            }
        }

        allowedHashesRef = ref
        allowedHashesListener = listener
        ref.addValueEventListener(listener)
    }

    private fun detachDeviceAuthListener() {
        allowedHashesListener?.let { allowedHashesRef?.removeEventListener(it) }
        allowedHashesListener = null
        allowedHashesRef = null
    }

    private fun cancelVerificationTimeout() {
        verificationTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        verificationTimeoutRunnable = null
    }

    private fun cancelFocusFallback() {
        focusFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        focusFallbackRunnable = null
    }

    private fun handleUnauthorizedFailure(deviceHash: String) {
        if (isHandlingUnauthorized) return
        isHandlingUnauthorized = true

        if (hasWindowFocus()) {
            copyHashAndScheduleExit(deviceHash)
        } else {
            pendingHashToCopy = deviceHash
            focusFallbackRunnable = Runnable {
                val remainingHash = pendingHashToCopy
                if (!remainingHash.isNullOrBlank()) {
                    pendingHashToCopy = null
                    copyHashAndScheduleExit(remainingHash)
                }
            }.also { runnable ->
                mainHandler.postDelayed(runnable, 1500L)
            }
        }
    }

    private fun copyHashAndScheduleExit(deviceHash: String) {
        if (hasCopiedAndScheduledExit) return
        hasCopiedAndScheduledExit = true

        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Device Hash", deviceHash)
            clipboard?.setPrimaryClip(clip)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(applicationContext, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mainHandler.postDelayed({
            killAppImmediately()
        }, 750L)
    }

    private fun killAppImmediately() {
        try {
            finishAndRemoveTask()
        } catch (_: Exception) {}
        Process.killProcess(Process.myPid())
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            IncomingCallNotifier.ACTION_ACCEPT -> {
                isLaunchedForCall = true
                val hasAudioPerm = ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasAudioPerm) {
                    CallManager.acceptIncomingCall(this)
                }
            }
            IncomingCallNotifier.ACTION_INCOMING_CALL -> {
                isLaunchedForCall = true
            }
        }
    }

    private fun configureLockScreenVisibility() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }
    }

    private fun promptBiometricUnlock(onSuccess: () -> Unit, onExit: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onExit()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Momo")
            .setSubtitle("Use biometric credential or device PIN/Pattern")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    @SuppressLint("HardwareIds")
    private fun computeDeviceHash(): String? {
        return try {
            val rawId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                ?.trim()
                ?.lowercase() ?: return null

            val saltedInput = SecurityConfig.SALT + rawId
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(saltedInput.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
