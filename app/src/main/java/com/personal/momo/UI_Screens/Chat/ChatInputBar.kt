package com.personal.momo.UI_Screens.Chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.MomoRedLight
import com.personal.momo.UI_Screens.bounceClick

private val AquaMintColor = Color(0xFF43CEA2)
private val DarkInputSurfaceColor = Color(0xFF171922)
private val DarkInputBorderColor = Color(0xFF262936)

private val SendIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Send",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val CloseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Close",
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

@Composable
internal fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    replyingToMessage: ChatMessage?,
    onCancelReply: () -> Unit,
    myCode: Int,
    partnerDisplayName: String,
    onSend: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(replyingToMessage) {
        if (replyingToMessage != null) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = replyingToMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            replyingToMessage?.let { targetMessage ->
                ReplyPreviewBar(
                    message = targetMessage,
                    myCode = myCode,
                    partnerDisplayName = partnerDisplayName,
                    onCancelReply = onCancelReply
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 110.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DarkInputSurfaceColor)
                    .border(
                        width = 1.dp,
                        color = DarkInputBorderColor,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Message...",
                        fontSize = 15.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 15.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrect = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MomoPrimaryGradient)
                    .bounceClick(scaleDown = 0.94f) {
                        onSend()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SendIcon,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier
                        .size(19.dp)
                        .padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ReplyPreviewBar(
    message: ChatMessage,
    myCode: Int,
    partnerDisplayName: String,
    onCancelReply: () -> Unit
) {
    val isMyMessage = message.senderId == myCode
    val accentColor = if (isMyMessage) MomoRedLight else AquaMintColor
    val senderName = if (isMyMessage) "You" else partnerDisplayName

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = DarkInputSurfaceColor,
        border = BorderStroke(
            width = 1.dp,
            color = DarkInputBorderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 7.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = senderName,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = message.text,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.82f),
                    lineHeight = 16.5.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .bounceClick(scaleDown = 0.94f) {
                        onCancelReply()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CloseIcon,
                    contentDescription = "Cancel Reply",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
