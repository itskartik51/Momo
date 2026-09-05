package com.personal.momo

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("MomoAppPrefs", Context.MODE_PRIVATE)

        setContent {
            var isSecurityLockEnabled by remember {
                mutableStateOf(sharedPreferences.getBoolean("isSecurityLockEnabled", false))
            }
            var isAuthenticated by remember { mutableStateOf(!isSecurityLockEnabled) }

            fun triggerBiometricAuth() {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(
                    this,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            isAuthenticated = true
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(this@MainActivity, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(this@MainActivity, "Authentication Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Momo Security")
                    .setSubtitle("Unlock using device fingerprint or PIN")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }

            LaunchedEffect(Unit) {
                if (isSecurityLockEnabled && !isAuthenticated) {
                    triggerBiometricAuth()
                }
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0F1015),
                    surface = Color(0xFF181A20),
                    primary = Color(0xFF4ECCA3),
                    onPrimary = Color(0xFF0F1015),
                    onBackground = Color(0xFFFFFFFF),
                    onSurface = Color(0xFFFFFFFF)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isSecurityLockEnabled && !isAuthenticated) {
                        LockScreen(onUnlockClick = { triggerBiometricAuth() })
                    } else {
                        MainContent(
                            isSecurityEnabled = isSecurityLockEnabled,
                            onToggleSecurity = { enabled ->
                                sharedPreferences.edit().putBoolean("isSecurityLockEnabled", enabled).apply()
                                isSecurityLockEnabled = enabled
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LockScreen(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color(0xFF4ECCA3),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Momo Vault Locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Device credential or fingerprint required",
                fontSize = 14.sp,
                color = Color(0xFF8A90A2)
            )
            Spacer(modifier = Modifier.height(28.dp))
            BounceButton(
                title = "Unlock Momo",
                onClick = onUnlockClick
            )
        }
    }
}

@Composable
fun MainContent(
    isSecurityEnabled: Boolean,
    onToggleSecurity: (Boolean) -> Unit
) {
    val view = LocalView.current
    var currentTab by remember { mutableIntStateOf(0) }
    val tabHistory = remember { mutableStateListOf(0) }

    BackHandler(enabled = tabHistory.size > 1) {
        tabHistory.removeLastOrNull()
        currentTab = tabHistory.lastOrNull() ?: 0
    }

    fun navigateTo(tabIndex: Int) {
        if (currentTab != tabIndex) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            currentTab = tabIndex
            tabHistory.add(tabIndex)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF14161D),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { navigateTo(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4ECCA3),
                        selectedTextColor = Color(0xFF4ECCA3),
                        indicatorColor = Color(0xFF181A20),
                        unselectedIconColor = Color(0xFF757B8C),
                        unselectedTextColor = Color(0xFF757B8C)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { navigateTo(1) },
                    icon = { Icon(Icons.Default.CloudQueue, contentDescription = null) },
                    label = { Text("Drive Vault", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4ECCA3),
                        selectedTextColor = Color(0xFF4ECCA3),
                        indicatorColor = Color(0xFF181A20),
                        unselectedIconColor = Color(0xFF757B8C),
                        unselectedTextColor = Color(0xFF757B8C)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { navigateTo(2) },
                    icon = { Icon(Icons.Default.Sync, contentDescription = null) },
                    label = { Text("Settings", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4ECCA3),
                        selectedTextColor = Color(0xFF4ECCA3),
                        indicatorColor = Color(0xFF181A20),
                        unselectedIconColor = Color(0xFF757B8C),
                        unselectedTextColor = Color(0xFF757B8C)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            when (currentTab) {
                0 -> DashboardTab()
                1 -> DriveVaultTab()
                2 -> SettingsTab(isSecurityEnabled, onToggleSecurity)
            }
        }
    }
}

@Composable
fun DashboardTab() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Momo", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Personal Vault & Cloud", fontSize = 14.sp, color = Color(0xFF8A90A2))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1F4ECCA3))
                    .border(1.dp, Color(0x4D4ECCA3), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "R8 Compressed",
                    color = Color(0xFF4ECCA3),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181A20)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF262934), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Architecture Status", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                StatusIndicator("Engine", "Jetpack Compose + Kotlin 1.9.22", Color(0xFF4ECCA3))
                Divider(color = Color(0xFF262934), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                StatusIndicator("Optimizer", "R8 Full-Mode ProGuard Active", Color(0xFF54A0FF))
            }
        }
    }
}

@Composable
fun DriveVaultTab() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Drive Vault", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("File pipelines to Google Drive Apps Script will appear here.", fontSize = 14.sp, color = Color(0xFF8A90A2))
    }
}

@Composable
fun SettingsTab(
    isSecurityEnabled: Boolean,
    onToggleSecurity: (Boolean) -> Unit
) {
    val view = LocalView.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Security & Config", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181A20)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF262934), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF4ECCA3))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("App Lock", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Biometric or PIN", fontSize = 12.sp, color = Color(0xFF8A90A2))
                    }
                }
                Switch(
                    checked = isSecurityEnabled,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onToggleSecurity(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0F1015),
                        checkedTrackColor = Color(0xFF4ECCA3),
                        uncheckedThumbColor = Color(0xFF8A90A2),
                        uncheckedTrackColor = Color(0xFF262934)
                    )
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(label: String, value: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(value, fontSize = 12.sp, color = Color(0xFF8A90A2))
        }
    }
}

@Composable
fun BounceButton(
    title: String,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1.0f, label = "button_scale")

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF4ECCA3))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onClick()
            }
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color(0xFF0F1015),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
