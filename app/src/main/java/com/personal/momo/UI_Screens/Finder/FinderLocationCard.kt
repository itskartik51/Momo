package com.personal.momo.UI_Screens.Finder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.bounceClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FinderLocationCard(
    partnerInfo: CacheManager.UserLocationInfo,
    selfInfo: CacheManager.UserLocationInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            FinderRowItem(item = partnerInfo)

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            FinderRowItem(item = selfInfo)
        }
    }
}

@Composable
private fun FinderRowItem(item: CacheManager.UserLocationInfo) {
    val context = LocalContext.current
    val hasValidCoords = item.latitude != null && item.longitude != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1 (Left): Name (Big & Bold, Click opens Google Maps)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (hasValidCoords) {
                        Modifier.bounceClick(scaleDown = 0.94f) {
                            openGoogleMaps(context, item.latitude!!, item.longitude!!, item.name)
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = item.name.ifBlank { "--" },
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Column 2 (Center): Coordinates in Option A Format (NL / EL)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (hasValidCoords) {
                        Modifier.bounceClick(scaleDown = 0.95f) {
                            val textToCopy = formatCopyCoordinates(item.latitude, item.longitude)
                            copyToClipboard(context, textToCopy)
                        }
                    } else Modifier
                )
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = formatDms(item.latitude, isLatitude = true),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDms(item.longitude, isLatitude = false),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Column 3 (Right): Meta Info (Time & Accuracy)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatTimestamp(item.timestamp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatAccuracy(item.accuracy),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun formatDms(value: Double?, isLatitude: Boolean): String {
    if (value == null) return "--"
    val absVal = abs(value)
    val degrees = absVal.toInt()
    val minutesDecimal = (absVal - degrees) * 60.0
    val minutes = minutesDecimal.toInt()
    val seconds = ((minutesDecimal - minutes) * 60.0).roundToInt()
    val prefix = if (isLatitude) {
        if (value >= 0.0) "NL" else "SL"
    } else {
        if (value >= 0.0) "EL" else "WL"
    }
    return "$prefix  $degrees°$minutes'$seconds\""
}

private fun formatCopyCoordinates(lat: Double?, lng: Double?): String {
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

private fun formatAccuracy(accuracy: Float?): String {
    return if (accuracy != null) "~${accuracy.roundToInt()} m" else "--"
}

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "--"
    val formatter = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    return formatter.format(Date(timestamp))
}
