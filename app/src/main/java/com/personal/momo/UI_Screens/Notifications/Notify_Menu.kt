package com.personal.momo.UI_Screens.Notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.personal.momo.UI_Screens.bounceClick

@Composable
fun NotifyMenuButton(
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .bounceClick(scaleDown = 0.90f) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
