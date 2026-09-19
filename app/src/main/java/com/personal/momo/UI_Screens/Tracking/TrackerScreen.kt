package com.personal.momo.UI_Screens.Tracking

import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.bounceClick
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class TrackerUserData(
    val name: String,
    val coordinatesText: String,
    val accuracyText: String,
    val timeText: String,
    val geoPoint: GeoPoint?
)

@Composable
fun TrackerScreen(
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val currentUserId by CacheManager.appUserIdFlow.collectAsState(initial = "kanu")

    var partnerTrackerData by remember { mutableStateOf<TrackerUserData?>(null) }
    var selfTrackerData by remember { mutableStateOf<TrackerUserData?>(null) }
    var interDeviceDistance by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(currentUserId) {
        val firestore = FirebaseFirestore.getInstance()
        val listenerRegistration = firestore.collection("App")
            .document("home_config")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val locationMap = snapshot.get("location") as? Map<*, *> ?: return@addSnapshotListener
                val kanuRawList = locationMap["kanu"] as? List<*>
                val momoRawList = locationMap["momo"] as? List<*>

                val kanuData = parseUserData("Kanu", kanuRawList)
                val momoData = parseUserData("Momo", momoRawList)

                val isKanu = currentUserId.lowercase(Locale.ENGLISH) == "kanu"

                val self = if (isKanu) kanuData else momoData
                val partner = if (isKanu) momoData else kanuData

                selfTrackerData = self
                partnerTrackerData = partner

                if (self.geoPoint != null && partner.geoPoint != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        self.geoPoint.latitude,
                        self.geoPoint.longitude,
                        partner.geoPoint.latitude,
                        partner.geoPoint.longitude,
                        results
                    )
                    interDeviceDistance = results[0].roundToInt()
                } else {
                    interDeviceDistance = null
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    val isKanuDevice = currentUserId.lowercase(Locale.ENGLISH) == "kanu"
    val defaultPartnerName = if (isKanuDevice) "Momo" else "Kanu"
    val defaultSelfName = if (isKanuDevice) "Kanu" else "Momo"

    val displayPartner = partnerTrackerData ?: TrackerUserData(
        name = defaultPartnerName,
        coordinatesText = "--",
        accuracyText = "--",
        timeText = "--",
        geoPoint = null
    )

    val displaySelf = selfTrackerData ?: TrackerUserData(
        name = defaultSelfName,
        coordinatesText = "--",
        accuracyText = "--",
        timeText = "--",
        geoPoint = null
    )

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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .bounceClick(scaleDown = 0.94f) { onBack() },
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
                    TrackerRowItem(item = displayPartner)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 2: Self Info
                    TrackerRowItem(item = displaySelf)

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 3: Inter-Device Distance
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (interDeviceDistance != null) "Distance : $interDeviceDistance m" else "Distance : -- m",
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
private fun TrackerRowItem(item: TrackerUserData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        Text(
            text = item.coordinatesText,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Text(
            text = item.accuracyText,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Text(
            text = item.timeText,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun parseUserData(name: String, rawList: List<*>?): TrackerUserData {
    if (rawList == null) {
        return TrackerUserData(
            name = name,
            coordinatesText = "--",
            accuracyText = "--",
            timeText = "--",
            geoPoint = null
        )
    }

    val geoPoint = rawList.getOrNull(0) as? GeoPoint
    val accuracyNum = (rawList.getOrNull(1) as? Number)?.toDouble()
    val timestamp = rawList.getOrNull(2) as? Timestamp

    val coordinatesText = if (geoPoint != null) {
        val latDir = if (geoPoint.latitude >= 0.0) "N" else "S"
        val lngDir = if (geoPoint.longitude >= 0.0) "E" else "W"
        String.format(
            Locale.ENGLISH,
            "%.6f° %s, %.6f° %s",
            abs(geoPoint.latitude),
            latDir,
            abs(geoPoint.longitude),
            lngDir
        )
    } else {
        "--"
    }

    val accuracyText = if (accuracyNum != null) {
        "~${accuracyNum.roundToInt()} m"
    } else {
        "--"
    }

    val timeText = if (timestamp != null) {
        val formatter = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        formatter.format(timestamp.toDate())
    } else {
        "--"
    }

    return TrackerUserData(
        name = name,
        coordinatesText = coordinatesText,
        accuracyText = accuracyText,
        timeText = timeText,
        geoPoint = geoPoint
    )
}
