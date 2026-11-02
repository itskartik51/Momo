package com.personal.momo.UI_Screens.Chat.Preference

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.personal.momo.UI_Screens.Chat.ChatMessage
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import java.text.NumberFormat
import java.util.Locale

private val DialogSurfaceColor = Color(0xFF1E1F22)
private val InnerCardColor = Color(0xFF27292E)
private val PartnerAccentColor = Color(0xFF34B7F1)

private val CloseDialogIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CloseDialogIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val ChatStatsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChatStatsIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zM7 9h2v2H7V9zm4 0h2v2h-2V9zm4 0h2v2h-2V9z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

@Composable
fun PrefCountDialog(
    messages: List<ChatMessage>,
    mySenderCode: Int,
    onDismiss: () -> Unit
) {
    val totalMessages = messages.size
    val kanuCount = remember(messages) { messages.count { it.senderId == 1 } }
    val momoCount = remember(messages) { messages.count { it.senderId == 2 } }

    val myName = if (mySenderCode == 2) "Momo" else "Kanu"
    val partnerName = if (mySenderCode == 2) "Kanu" else "Momo"
    val myCount = if (mySenderCode == 2) momoCount else kanuCount
    val partnerCount = if (mySenderCode == 2) kanuCount else momoCount

    val myFraction = if (totalMessages > 0) {
        (myCount.toFloat() / totalMessages.toFloat()).coerceIn(0f, 1f)
    } else {
        0.5f
    }

    val myPercentage = if (totalMessages > 0) {
        ((myCount * 100f) / totalMessages.toFloat()).toInt()
    } else {
        0
    }

    val partnerPercentage = if (totalMessages > 0) {
        100 - myPercentage
    } else {
        0
    }

    val numberFormatter = remember { NumberFormat.getIntegerInstance(Locale.US) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
            shape = RoundedCornerShape(22.dp),
            color = DialogSurfaceColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MomoPrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ChatStatsIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(11.dp))

                        Text(
                            text = "Message Count",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .bounceClick(scaleDown = 0.94f) {
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CloseDialogIcon,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(InnerCardColor)
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Messages",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.62f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = numberFormatter.format(totalMessages),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (totalMessages > 0) PartnerAccentColor.copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.12f)
                        )
                ) {
                    if (totalMessages > 0 && myFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(myFraction)
                                .background(MomoPrimaryGradient)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SenderCountRow(
                    name = "$myName (You)",
                    countText = numberFormatter.format(myCount),
                    percentageText = "$myPercentage%",
                    usePrimaryGradientDot = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                SenderCountRow(
                    name = partnerName,
                    countText = numberFormatter.format(partnerCount),
                    percentageText = "$partnerPercentage%",
                    usePrimaryGradientDot = false
                )
            }
        }
    }
}

@Composable
private fun SenderCountRow(
    name: String,
    countText: String,
    percentageText: String,
    usePrimaryGradientDot: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InnerCardColor.copy(alpha = 0.75f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .then(
                        if (usePrimaryGradientDot) {
                            Modifier.background(MomoPrimaryGradient)
                        } else {
                            Modifier.background(PartnerAccentColor)
                        }
                    )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = name,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.92f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = countText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "($percentageText)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}
