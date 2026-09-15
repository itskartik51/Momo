package com.personal.momo

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.setContent
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
import com.personal.momo.UI_Screens.MainScreen
import com.personal.momo.UI_Screens.MomoTheme
import com.personal.momo.UI_Screens.Settings.cleanOldUpdateApks
import com.personal.momo.UI_Screens.bounceClick
import java.security.MessageDigest

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Automatically clean up old update APK files and download manager history
        cleanOldUpdateApks(this)

        if (!isDeviceAuthorized()) {
            finishAndRemoveTask()
            Process.killProcess(Process.myPid())
            return
        }

        setContent {
            MomoTheme {
                val isLockConfigured = remember { CacheManager.isSecurityLockEnabled(this) }
                var isUnlocked by remember { mutableStateOf(!isLockConfigured) }

                LaunchedEffect(isLockConfigured) {
                    if (isLockConfigured && !isUnlocked) {
                        promptBiometricUnlock(
                            onSuccess = { isUnlocked = true },
                            onExit = { finishAndRemoveTask() }
                        )
                    }
                }

                if (isUnlocked) {
                    MainScreen()
                } else {
                    // Minimalistic Premium Security Lock Gatekeeper
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
            }
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
