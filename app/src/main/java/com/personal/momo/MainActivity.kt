package com.personal.momo

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.personal.momo.UI_Screens.MainScreen
import com.personal.momo.UI_Screens.MomoTheme
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isDeviceAuthorized()) {
            finishAndRemoveTask()
            Process.killProcess(Process.myPid())
            return
        }

        setContent {
            MomoTheme {
                MainScreen()
            }
        }
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
