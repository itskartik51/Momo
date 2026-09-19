package com.personal.momo.UI_Screens.Tracking

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.personal.momo.Cache.CacheManager
import com.personal.momo.Proximity.ProximityLocationService
import com.personal.momo.UI_Screens.bounceClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TrackerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLiveActive by remember { mutableStateOf(false) }
    val trackerState by CacheManager.trackerStateFlow.collectAsState()

    // Scoped Lifecycle: Auto-turn off Live mode on Back, Navigation, or App Minimize
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (isLiveActive) {
                    isLiveActive = false
                    ProximityLocationService.stopLive(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isLiveActive) {
                isLiveActive = false
                ProximityLocationService.stopLive(context)
            }
        }
    }

    BackHandler {
        if (isLiveActive) {
            isLiveActive = false
            ProximityLocationService.stopLive(context)
        }
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Section: Back Button + "Tracker" Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .bounceClick(scaleDown = 0.94f) {
                                    if (isLiveActive) {
                                        isLiveActive = false
                                        ProximityLocationService.stopLive(context)
                                    }
                                    onBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = "Tracker",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Right Section: Pill Buttons (Live & Sync)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pill 1: Live Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isLiveActive) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .then(
                                    if (isLiveActive) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(50)
                                        )
                                    } else Modifier
                                )
                                .bounceClick(scaleDown = 0.94f) {
                                    isLiveActive = !isLiveActive
                                    if (isLiveActive) {
                                        ProximityLocationService.startLive(context)
                                        Toast.makeText(context, "10s Live Tracking Active", Toast.LENGTH_SHORT).show()
                                    } else {
                                        ProximityLocationService.stopLive(context)
                                        Toast.makeText(context, "Live Tracking Stopped", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isLiveActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }

                                Text(
                                    text = "Live",
                                    fontSize = 12.sp,
                                    fontWeight = if (isLiveActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLiveActive) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Pill 2: Sync One-Shot Action
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .bounceClick(scaleDown = 0.94f) {
                                    ProximityLocationService.forceSync(context)
                                    Toast.makeText(context, "Syncing location...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sync",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Tabular Tracking Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Row 1: Partner Info
                    TrackerRowItem(item = trackerState.partnerInfo)

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Self Info
                    TrackerRowItem(item = trackerState.selfInfo)

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 3: Inter-Device Distance
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val distanceText = if (trackerState.distanceMeters != null) {
                            "Distance : ${trackerState.distanceMeters} m"
                        } else {
                            "Distance : -- m"
                        }

                        Text(
                            text = distanceText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerRowItem(item: CacheManager.UserLocationInfo) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Line 1: Name (Click opens Google Maps) on Left, Time on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasValidCoords = item.latitude != null && item.longitude != null

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (hasValidCoords) {
                            Modifier.bounceClick(scaleDown = 0.94f) {
                                openGoogleMaps(context, item.latitude!!, item.longitude!!, item.name)
                            }
                        } else Modifier
                    )
            ) {
                Text(
                    text = item.name.ifBlank { "--" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            Text(
                text = formatTimestamp(item.timestamp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Line 2: Coordinates (Click copies to clipboard) on Left, Accuracy on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formattedCoords = formatCoordinates(item.latitude, item.longitude)
            val isClickable = item.latitude != null && item.longitude != null

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (isClickable) {
                            Modifier.bounceClick(scaleDown = 0.95f) {
                                copyToClipboard(context, "${item.latitude}, ${item.longitude}")
                            }
                        } else Modifier
                    )
            ) {
                Text(
                    text = formattedCoords,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Text(
                text = formatAccuracy(item.accuracy),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun openGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, webUri)
        context.startActivity(fallbackIntent)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Coordinates", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Coordinates copied", Toast.LENGTH_SHORT).show()
}

private fun formatCoordinates(lat: Double?, lng: Double?): String {
    if (lat == null || lng == null) return "--"
    val latDir = if (lat >= 0.0) "N" else "S"
    val lngDir = if (lng >= 0.0) "E" else "W"
    return String.format(
        Locale.ENGLISH,
        "%.6f° %s, %.6f° %s",
        abs(lat),
        latDir,
        abs(lng),
        lngDir
    )
}

private fun formatAccuracy(accuracy: Float?): String {
    return if (accuracy != null) "~${accuracy.roundToInt()} m" else "--"
}

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "--"
    val formatter = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    return formatter.format(Date(timestamp))
}
