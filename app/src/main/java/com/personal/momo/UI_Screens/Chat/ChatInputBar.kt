package com.personal.momo.UI_Screens.Chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.MomoRedLight
import com.personal.momo.UI_Screens.bounceClick

private val DarkInputSurfaceColor = Color(0xFF171922)
private val DarkInputBorderColor = Color(0xFF262936)

private val InputSmileIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "InputSmile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val InputKeyboardIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "InputKeyboard",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M20 5H4c-1.1 0-1.99.9-1.99 2L2 17c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val GalleryIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Gallery",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val CameraIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Camera",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 12m-3.2 0a3.2 3.2 0 1 0 6.4 0a3.2 3.2 0 1 0 -6.4 0 M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

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

private val InputPlayIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "InputPlay",
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

@Composable
internal fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    isEmojiPickerOpen: Boolean = false,
    onToggleEmojiPicker: () -> Unit = {},
    onInputClick: () -> Unit = {},
    replyingToMessage: ChatMessage?,
    onCancelReply: () -> Unit,
    editingMessage: ChatMessage? = null,
    onCancelEdit: () -> Unit = {},
    myCode: Int,
    partnerDisplayName: String,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onSend: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }

    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            val newCursor = if (text.length > textFieldValue.text.length && text.startsWith(textFieldValue.text.take(textFieldValue.selection.min))) {
                val addedLength = text.length - textFieldValue.text.length
                (textFieldValue.selection.min + addedLength).coerceIn(0, text.length)
            } else {
                text.length
            }
            textFieldValue = textFieldValue.copy(
                text = text,
                selection = TextRange(newCursor)
            )
        }
    }

    LaunchedEffect(replyingToMessage, editingMessage) {
        if (replyingToMessage != null || editingMessage != null) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = editingMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            editingMessage?.let { targetMessage ->
                EditPreviewBar(
                    message = targetMessage,
                    onCancelEdit = onCancelEdit
                )
            }
        }

        AnimatedVisibility(
            visible = replyingToMessage != null && editingMessage == null,
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
            Row(
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
                    .padding(
                        start = 12.dp,
                        end = if (text.isEmpty() && editingMessage == null) 12.dp else 16.dp,
                        top = 6.dp,
                        bottom = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .bounceClick(scaleDown = 0.90f) {
                            if (isEmojiPickerOpen) {
                                onToggleEmojiPicker()
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            } else {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onToggleEmojiPicker()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEmojiPickerOpen) InputKeyboardIcon else InputSmileIcon,
                        contentDescription = if (isEmojiPickerOpen) "Keyboard" else "Emoji",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(23.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isEmojiPickerOpen) {
                                onToggleEmojiPicker()
                            }
                            onInputClick()
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = if (editingMessage != null) "Edit message..." else "Message...",
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            if (newValue.text != text) {
                                onTextChanged(newValue.text)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && isEmojiPickerOpen) {
                                    onToggleEmojiPicker()
                                }
                            },
                        textStyle = TextStyle(
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
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

                if (text.isEmpty() && editingMessage == null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = GalleryIcon,
                        contentDescription = "Gallery",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onGalleryClick()
                            }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = CameraIcon,
                        contentDescription = "Camera",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onCameraClick()
                            }
                    )

                    Spacer(modifier = Modifier.width(2.dp))
                }
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
private fun EditPreviewBar(
    message: ChatMessage,
    onCancelEdit: () -> Unit
) {
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
                    .background(MomoPrimaryGradient)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 7.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Editing message",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MomoRedLight,
                    maxLines = 1,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                EmojiText(
                    text = message.text.ifBlank { if (message.mediaType == 2) "Video" else "Photo" },
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
                        onCancelEdit()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CloseIcon,
                    contentDescription = "Cancel Edit",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(17.dp)
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
    val context = LocalContext.current
    val isMyMessage = message.senderId == myCode
    val accentColor = if (isMyMessage) MomoRedLight else AquaMintColor
    val senderName = if (isMyMessage) "You" else partnerDisplayName

    val isMedia = message.mediaType > 0
    val previewText = when {
        message.mediaType == 1 -> if (message.text.isNotBlank()) "Photo: ${message.text}" else "Photo"
        message.mediaType == 2 -> if (message.text.isNotBlank()) "Video: ${message.text}" else "Video"
        else -> message.text.ifBlank { "Message" }
    }

    val mediaFile = remember(message.localPath) {
        val path = message.localPath
        if (path.isNullOrBlank()) null
        else {
            MediaRenderCache.getFile(path) ?: run {
                val resolved = MomoStorage.resolveLocalMediaFile(context, path)
                if (resolved != null) MediaRenderCache.putFile(path, resolved)
                resolved
            }
        }
    }

    val blurBitmap = remember(message.blurPreview) {
        val b64 = message.blurPreview
        if (b64.isNullOrBlank()) null
        else {
            MediaRenderCache.getBlurBitmap(b64) ?: run {
                val decoded = decodeBlurPreviewBitmap(b64)
                if (decoded != null) MediaRenderCache.putBlurBitmap(b64, decoded)
                decoded
            }
        }
    }

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

                EmojiText(
                    text = previewText,
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

            if (isMedia) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaFile != null || blurBitmap != null) {
                        AsyncImage(
                            model = mediaFile ?: blurBitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (message.mediaType == 2) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = InputPlayIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

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