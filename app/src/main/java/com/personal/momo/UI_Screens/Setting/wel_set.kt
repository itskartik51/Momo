package com.personal.momo.UI_Screens.Settings

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.personal.momo.UI_Screens.WelcomeLayoutMode

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
    if (!isOpen) return

    val context = LocalContext.current
    var currentMode by remember {
        mutableStateOf(WelcomeSettingsPrefs.getSavedLayoutMode(context))
    }
    var isExpanded by remember { mutableStateOf(false) }

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = -20, y = 140),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        // Floating Card Base (niche green dot wala tone: MaterialTheme.colorScheme.surface)
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
                // Clickable "Welcome Note" title (Arrow-free trigger)
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

                // Expandable Section
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bada Outer Capsule (upar green tick wala tone: MaterialTheme.colorScheme.background, borderless)
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
