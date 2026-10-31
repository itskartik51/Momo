package com.personal.momo.UI_Screens.Chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.personal.momo.UI_Screens.bounceClick
import java.io.File

private val PlayArrowIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PlayArrow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M8 5v14l11-7z"
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
internal fun MediaBubbleMessageContent(
    message: ChatMessage,
    isOutgoing: Boolean,
    isLastInGroup: Boolean,
    formattedTime: String,
    myCode: Int,
    partnerDisplayName: String,
    bubbleShape: RoundedCornerShape,
    onReplyClick: (String) -> Unit
) {
    val context = LocalContext.current
    val hasReply = !message.replyToText.isNullOrBlank()
    val hasCaption = message.text.isNotBlank()

    val mediaFile = remember(message.localPath) {
        message.localPath?.let { fileName ->
            File(File(context.filesDir, "chat_media"), fileName).takeIf { it.exists() && it.length() > 0L }
        }
    }

    var showFullScreenPreview by remember { mutableStateOf(false) }

    if (showFullScreenPreview && mediaFile != null) {
        Dialog(
            onDismissRequest = { showFullScreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = mediaFile,
                    contentDescription = "Full Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 42.dp, end = 20.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .bounceClick(scaleDown = 0.94f) {
                            showFullScreenPreview = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CloseIcon,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.widthIn(max = 290.dp)
    ) {
        if (hasReply) {
            Box(
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 4.dp)
            ) {
                QuotedReplySnippet(
                    replyToId = message.replyToId,
                    replyToText = message.replyToText ?: "",
                    replyToSenderId = message.replyToSenderId,
                    myCode = myCode,
                    partnerDisplayName = partnerDisplayName,
                    isOutgoing = isOutgoing,
                    onReplyClick = onReplyClick
                )
            }
        }

        val mediaClipShape = if (hasCaption) {
            RoundedCornerShape(
                topStart = bubbleShape.topStart,
                topEnd = bubbleShape.topEnd,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )
        } else {
            bubbleShape
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 260.dp)
                .padding(
                    start = if (hasCaption) 4.dp else 0.dp,
                    end = if (hasCaption) 4.dp else 0.dp,
                    top = if (hasCaption || hasReply) 4.dp else 0.dp,
                    bottom = if (hasCaption) 0.dp else 0.dp
                )
                .clip(mediaClipShape)
                .background(Color.Black.copy(alpha = 0.25f))
                .bounceClick(scaleDown = 0.97f) {
                    if (mediaFile != null) {
                        if (message.mediaType == 2) {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    mediaFile
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Play Video"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            showFullScreenPreview = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (mediaFile != null) {
                AsyncImage(
                    model = mediaFile,
                    contentDescription = if (message.mediaType == 2) "Video Preview" else "Photo Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (message.mediaType == 2) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PlayArrowIcon,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier
                                .size(26.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }

                if (!hasCaption && isLastInGroup) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.5.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )

                        if (isOutgoing) {
                            BubbleStatusTicks(status = message.status)
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (message.mediaType == 2) "Loading video..." else "Loading photo...",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        if (hasCaption) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 5.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                if (isLastInGroup) {
                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                        ) {
                            Text(
                                text = formattedTime,
                                fontSize = 11.sp,
                                color = if (isOutgoing)
                                    Color.White.copy(alpha = 0.80f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )

                            if (isOutgoing) {
                                BubbleStatusTicks(status = message.status)
                            }
                        }
                    }
                }
            }
        }
    }
}
