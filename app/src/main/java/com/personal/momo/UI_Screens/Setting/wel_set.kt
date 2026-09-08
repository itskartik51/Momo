package com.personal.momo.UI_Screens.Settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.personal.momo.UI_Screens.WelcomeLayoutMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

val SelectedCapsuleGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFC91D3B),
        Color(0xFFFF5E79)
    )
)

@Composable
fun WelcomeSettingsContent(
    onModeChanged: ((WelcomeLayoutMode) -> Unit)? = null,
    onShowToast: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentMode by remember {
        mutableStateOf(WelcomeSettingsPrefs.getSavedLayoutMode(context))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        WelcomeModeCapsuleSelector(
            currentMode = currentMode,
            onModeSelected = { selectedMode, label ->
                if (currentMode != selectedMode) {
                    currentMode = selectedMode
                    WelcomeSettingsPrefs.saveLayoutMode(context, selectedMode)
                    coroutineScope.launch {
                        delay(260)
                        onShowToast?.invoke("$label mode applied")
                        onModeChanged?.invoke(selectedMode)
                    }
                }
            }
        )
    }
}

@Composable
fun WelcomeModeCapsuleSelector(
    currentMode: WelcomeLayoutMode,
    onModeSelected: (WelcomeLayoutMode, String) -> Unit
) {
    val modes = listOf(
        WelcomeLayoutMode.OVERLAY to "Overlay",
        WelcomeLayoutMode.INLINE to "Inline",
        WelcomeLayoutMode.STACKED to "Stacked"
    )

    val selectedIndex = when (currentMode) {
        WelcomeLayoutMode.OVERLAY -> 0
        WelcomeLayoutMode.INLINE -> 1
        WelcomeLayoutMode.STACKED -> 2
    }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "CapsuleSlideSpring"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / 3

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
            modes.forEach { (mode, label) ->
                val isSelected = currentMode == mode

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "TextColorFade_$label"
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
                            onModeSelected(mode, label)
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

@Composable
fun WelcomeAppliedToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message == null) return

    var isToastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        isToastVisible = true
        delay(2000)
        isToastVisible = false
        delay(300)
        onDismiss()
    }

    AnimatedVisibility(
        visible = isToastVisible,
        modifier = modifier,
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
                    text = message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
