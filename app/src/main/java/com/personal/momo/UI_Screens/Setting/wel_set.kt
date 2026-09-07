package com.personal.momo.UI_Screens.Settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.personal.momo.UI_Screens.WelcomeLayoutMode
import kotlinx.coroutines.delay

object WelcomeSettingsPrefs {
    private const val PREFS_NAME = "momo_welcome_settings"
    private const val KEY_LAYOUT_MODE = "welcome_layout_mode"

    fun getSavedLayoutMode(context: Context): WelcomeLayoutMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeName = prefs.getString(KEY_LAYOUT_MODE, WelcomeLayoutMode.OVERLAY.name)
        return try {
            WelcomeLayoutMode.valueOf(modeName ?: WelcomeLayoutMode.OVERLAY.name)
        } catch (e: Exception) {
            WelcomeLayoutMode.OVERLAY
        }
    }

    fun saveLayoutMode(context: Context, mode: WelcomeLayoutMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAYOUT_MODE, mode.name).apply()
    }
}

private val SelectedCapsuleGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFC91D3B), // Darker Red (Top)
        Color(0xFFFF5E79)  // Lighter Coral Red (Bottom)
    )
)

@Composable
fun WelcomeSettingsPopup(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    onModeChanged: ((WelcomeLayoutMode) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var currentMode by remember {
        mutableStateOf(WelcomeSettingsPrefs.getSavedLayoutMode(context))
    }
    var isExpanded by remember { mutableStateOf(false) }

    // Bottom Applied Toast State
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var isToastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            isToastVisible = true
            delay(2000)
            isToastVisible = false
            delay(300)
            toastMessage = null
        }
    }

    // 1. Top-Right 3-Dot Floating Settings Popup
    if (isOpen) {
        Popup(
            alignment = Alignment.TopEnd,
            offset = IntOffset(x = -20, y = 140),
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isExpanded = !isExpanded
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Welcome Note",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val modes = listOf(
                                    WelcomeLayoutMode.OVERLAY to "Overlay",
                                    WelcomeLayoutMode.INLINE to "Inline",
                                    WelcomeLayoutMode.STACKED to "Stacked"
                                )

                                modes.forEach { (mode, label) ->
                                    val isSelected = currentMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(SelectedCapsuleGradient)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                currentMode = mode
                                                WelcomeSettingsPrefs.saveLayoutMode(context, mode)
                                                toastMessage = "$label mode applied"
                                                onDismissRequest()
                                                onModeChanged?.invoke(mode)
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Bottom Center Floating Confirmation Capsule Pop-up
    if (toastMessage != null) {
        val bottomOffsetPx = with(density) { 36.dp.roundToPx() }

        Popup(
            alignment = Alignment.BottomCenter,
            offset = IntOffset(x = 0, y = -bottomOffsetPx),
            properties = PopupProperties(focusable = false)
        ) {
            AnimatedVisibility(
                visible = isToastVisible,
                enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { it / 2 }
                ),
                exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { it / 2 }
                )
            ) {
                Surface(
                    modifier = Modifier
                        .shadow(elevation = 12.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(SelectedCapsuleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Applied",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Text(
                            text = toastMessage.orEmpty(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
