package com.personal.momo.UI_Screens.Notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick

@Composable
fun NotifyCounter(
    currentTym: Long?
) {
    val density = LocalDensity.current
    var isCapsuleVisible by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.Center) {
        // Dynamic Number Circle Badge
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .bounceClick(scaleDown = 0.92f) {
                    if (currentTym != null) {
                        isCapsuleVisible = !isCapsuleVisible
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentTym?.toString() ?: "",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Floating Capsule / Pill (+ / - Controls)
        if (isCapsuleVisible && currentTym != null) {
            val yOffsetPx = with(density) { 42.dp.roundToPx() }
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, yOffsetPx),
                onDismissRequest = { isCapsuleVisible = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus Button (-)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(brush = MomoPrimaryGradient)
                                .bounceClick(scaleDown = 0.88f) {
                                    val updated = (currentTym ?: 0L) - 1
                                    CacheManager.updateTym(updated)
                                    isCapsuleVisible = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Plus Button (+)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(brush = MomoPrimaryGradient)
                                .bounceClick(scaleDown = 0.88f) {
                                    val updated = (currentTym ?: 0L) + 1
                                    CacheManager.updateTym(updated)
                                    isCapsuleVisible = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
