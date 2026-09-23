package com.personal.momo

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.WindowManager
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
import com.personal.momo.Cache.CacheManager
import com.personal.momo.Proximity.ProximityLocationService
import com.personal.momo.UI_Screens.Call.ActiveCallScreen
import com.personal.momo.UI_Screens.Call.CallManager
import com.personal.momo.UI_Screens.Call.IncomingCallNotifier
import com.personal.momo.UI_Screens.Call.IncomingCallView
import com.personal.momo.UI_Screens.MainScreen
import com.personal.momo.UI_Screens.MomoTheme
import com.personal.momo.UI_Screens.Settings.cleanOldUpdateApks
import com.personal.momo.UI_Screens.bounceClick
import java.security.MessageDigest

class MainActivity : FragmentActivity() {

    private var isLaunchedForCall = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cleanOldUpdateApks(this)

        if (!isDeviceAuthorized()) {
            finishAndRemoveTask()
            Process.killProcess(Process.myPid())
            return
        }

        // Global Call Listener
        CallManager.startSignalingListener(applicationContext)

        configureLockScreenVisibility()
        handleIncomingIntent(intent)

        setContent {
            MomoTheme {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureLockScreenVisibility()
        handleIncomingIntent(intent)
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
    private fun isDeviceAuthorized(): Boolean {
        return try {
            val rawId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                ?.trim()
                ?.lowercase() ?: return false

            val saltedInput = SecurityConfig.SALT + rawId
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(saltedInput.toByteArray(Charsets.UTF_8))
            val currentHash = hashBytes.joinToString("") { "%02x".format(it) }

            SecurityConfig.ALLOWED_HASHES.contains(currentHash)
        } catch (e: Exception) {
            false
        }
    }
}
