package com.personal.momo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            val colorScheme = if (isDarkTheme) {
                darkColorScheme(
                    background = Color(0xFF0F1015),
                    surface = Color(0xFF181A20),
                    onBackground = Color(0xFFFFFFFF),
                    onSurface = Color(0xFFFFFFFF)
                )
            } else {
                lightColorScheme(
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFF5F5F5),
                    onBackground = Color(0xFF000000),
                    onSurface = Color(0xFF000000)
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
