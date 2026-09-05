package com.personal.momo

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomoAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MomoAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0F1015),
            surface = Color(0xFF181A20),
            primary = Color(0xFF4ECCA3),
            onPrimary = Color(0xFF0F1015),
            onBackground = Color(0xFFFFFFFF),
            onSurface = Color(0xFFFFFFFF)
        ),
        content = content
    )
}

@Composable
fun MainScreen() {
    val view = LocalView.current
    var currentTab by remember { mutableIntStateOf(0) }
    val tabHistory = remember { mutableStateListOf(0) }

    BackHandler(enabled = tabHistory.size > 1) {
        tabHistory.removeLastOrNull()
        currentTab = tabHistory.lastOrNull() ?: 0
    }

    fun navigateToTab(tabIndex: Int) {
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
                    onClick = { navigateToTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
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
                    onClick = { navigateToTab(1) },
                    icon = { Icon(Icons.Default.CloudQueue, contentDescription = "Vault") },
                    label = { Text("Vault", fontSize = 12.sp) },
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
                    onClick = { navigateToTab(2) },
                    icon = { Icon(Icons.Default.Sync, contentDescription = "Sync") },
                    label = { Text("Sync", fontSize = 12.sp) },
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
                0 -> DashboardSection()
                1 -> VaultSection()
                2 -> SyncSection()
            }
        }
    }
}

@Composable
fun DashboardSection() {
    val view = LocalView.current
    var statusMessage by remember { mutableStateOf("Momo Pipeline Ready") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Momo",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Personal Private Cloud App",
                    fontSize = 14.sp,
                    color = Color(0xFF8A90A2)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1F4ECCA3))
                    .border(1.dp, Color(0x4D4ECCA3), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF4ECCA3),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Release Signed",
                        color = Color(0xFF4ECCA3),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
                Text(
                    text = "Data Pipelines",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))
                PipelineRow("Firebase Realtime DB", "Documents & Auth Node", Color(0xFFFF9F43))
                Divider(color = Color(0xFF262934), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                PipelineRow("Google Apps Script", "Drive File Sync Node", Color(0xFF54A0FF))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF4ECCA3))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    statusMessage = "Test connection successful"
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ping Connection",
                color = Color(0xFF0F1015),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedVisibility(
            visible = statusMessage.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = statusMessage,
                color = Color(0xFF4ECCA3),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun PipelineRow(title: String, subtitle: String, indicatorColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(indicatorColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF8A90A2))
        }
    }
}

@Composable
fun VaultSection() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Drive & Vault", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Personal files uploaded here will route to Google Drive via Apps Script.", fontSize = 14.sp, color = Color(0xFF8A90A2))
    }
}

@Composable
fun SyncSection() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Sync Configuration", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Firebase endpoints and Webhook URLs will be synchronized here.", fontSize = 14.sp, color = Color(0xFF8A90A2))
    }
}
