package com.personal.momo.UI_Screens.Finder

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.personal.momo.R
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import java.util.Locale

@Composable
fun TogetherAnimationCard(
    partnerName: String,
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("couple_anim.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )

    val scriptFont = remember { FontFamily(Font(R.font.momo_script)) }
    val boldFont = remember { FontFamily(Font(R.font.momo_bold)) }

    val partnerDisplayName = partnerName.ifBlank { "Partner" }.uppercase(Locale.ROOT)

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
                .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lottie Animation Box with Double-Tap Exit Test Gesture
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { onDoubleTap() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(240.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Hybrid Typography Header: Cursive Intro + Bold Gradient Name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "You're with ",
                    fontFamily = scriptFont,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = partnerDisplayName,
                    fontFamily = boldFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(brush = MomoPrimaryGradient)
                )
            }
        }
    }
}
