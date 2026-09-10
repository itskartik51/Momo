package com.personal.momo.UI_Screens.Settings

import android.content.Context
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
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

object ThemeSettingsPrefs {
    private const val PREFS_NAME = "momo_theme_settings"
    private const val KEY_THEME_MODE = "app_theme_mode"

    private val _themeModeFlow = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeModeFlow: StateFlow<AppThemeMode> = _themeModeFlow.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        val mode = try {
            AppThemeMode.valueOf(modeName ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
        _themeModeFlow.value = mode
        isInitialized = true
    }

    fun getSavedThemeMode(context: Context): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(modeName ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun saveThemeMode(context: Context, mode: AppThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeModeFlow.value = mode
    }
}

@Composable
fun ThemeSettingsContent(
    onShowToast: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentMode by remember {
        mutableStateOf(ThemeSettingsPrefs.getSavedThemeMode(context))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        ThemeModeCapsuleSelector(
            currentMode = currentMode,
            onModeSelected = { selectedMode, label ->
                if (currentMode != selectedMode) {
                    currentMode = selectedMode
                    ThemeSettingsPrefs.saveThemeMode(context, selectedMode)
                    coroutineScope.launch {
                        delay(260)
                        onShowToast?.invoke("$label theme applied")
                    }
                }
            }
        )
    }
}

@Composable
fun ThemeModeCapsuleSelector(
    currentMode: AppThemeMode,
    onModeSelected: (AppThemeMode, String) -> Unit
) {
    val modes = listOf(
        AppThemeMode.SYSTEM to "System",
        AppThemeMode.LIGHT to "Light",
        AppThemeMode.DARK to "Dark"
    )

    val selectedIndex = when (currentMode) {
        AppThemeMode.SYSTEM -> 0
        AppThemeMode.LIGHT -> 1
        AppThemeMode.DARK -> 2
    }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ThemeCapsuleSlideSpring"
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
                .background(MomoPrimaryGradient)
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
                    label = "ThemeTextColorFade_$label"
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
