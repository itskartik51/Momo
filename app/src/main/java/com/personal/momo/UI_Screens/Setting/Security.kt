package com.personal.momo.UI_Screens.Settings

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.personal.momo.Cache.CacheManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SecurityLockSettingsContent(
    onShowToast: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLockedEnabled by remember {
        mutableStateOf(CacheManager.isSecurityLockEnabled(context))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        SecurityLockCapsuleSelector(
            isEnabled = isLockedEnabled,
            onOptionSelected = { targetEnabled ->
                if (targetEnabled == isLockedEnabled) return@SecurityLockCapsuleSelector

                val activity = context.findFragmentActivity()
                if (activity != null) {
                    val promptTitle = if (targetEnabled) {
                        "Authenticate to Enable Lock"
                    } else {
                        "Authenticate to Disable Lock"
                    }

                    authenticateSecurityLock(
                        activity = activity,
                        title = promptTitle,
                        onSuccess = {
                            isLockedEnabled = targetEnabled
                            CacheManager.setSecurityLockEnabled(context, targetEnabled)
                            coroutineScope.launch {
                                delay(260)
                                val message = if (targetEnabled) "Security lock enabled" else "Security lock disabled"
                                onShowToast?.invoke(message)
                            }
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun SecurityLockCapsuleSelector(
    isEnabled: Boolean,
    onOptionSelected: (Boolean) -> Unit
) {
    val options = listOf(
        false to "Disable",
        true to "Enable"
    )

    val selectedIndex = if (isEnabled) 1 else 0

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SecurityCapsuleSlideSpring"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / 2

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (animatedIndex * segmentWidth.toPx()).toInt(),
                        y = 0
                    )
                }
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(SelectedCapsuleGradient)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (enabledValue, label) ->
                val isSelected = isEnabled == enabledValue

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "SecurityTextColorFade_$label"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onOptionSelected(enabledValue)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

private fun authenticateSecurityLock(
    activity: FragmentActivity,
    title: String,
    onSuccess: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle("Use your biometric credential or screen lock")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(promptInfo)
}
