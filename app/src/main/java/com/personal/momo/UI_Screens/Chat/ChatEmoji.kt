package com.personal.momo.UI_Screens.Chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.momo.UI_Screens.MomoRedLight
import com.personal.momo.UI_Screens.bounceClick
import com.vanniktech.emoji.Emoji
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import kotlinx.coroutines.launch

object KeyboardHeightHelper {
    private const val PREFS_NAME = "momo_keyboard_prefs"
    private const val KEY_HEIGHT = "cached_keyboard_height_dp"
    const val DEFAULT_KEYBOARD_HEIGHT_DP = 280f

    fun getCachedKeyboardHeight(context: Context): Float {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getFloat(KEY_HEIGHT, DEFAULT_KEYBOARD_HEIGHT_DP)
    }

    fun saveKeyboardHeight(context: Context, heightDp: Float) {
        if (heightDp in 180f..460f) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (sp.getFloat(KEY_HEIGHT, 0f) != heightDp) {
                sp.edit().putFloat(KEY_HEIGHT, heightDp).apply()
            }
        }
    }
}

object RecentEmojiHelper {
    private const val PREFS_NAME = "momo_recent_emojis"
    private const val KEY_RECENTS = "recent_emojis_csv"
    private const val MAX_RECENTS = 35

    fun getRecentEmojis(context: Context): List<String> {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = sp.getString(KEY_RECENTS, "") ?: ""
        if (csv.isBlank()) return emptyList()
        return csv.split(",").filter { it.isNotBlank() }
    }

    fun recordEmoji(context: Context, emojiStr: String) {
        if (emojiStr.isBlank()) return
        val current = getRecentEmojis(context).toMutableList()
        current.remove(emojiStr)
        current.add(0, emojiStr)
        val trimmed = current.take(MAX_RECENTS)
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_RECENTS, trimmed.joinToString(",")).apply()
    }
}

fun deleteLastCharacterOrEmoji(text: String): String {
    if (text.isEmpty()) return ""
    return try {
        val codePointCount = text.codePointCount(0, text.length)
        if (codePointCount <= 1) return ""
        val offset = text.offsetByCodePoints(0, codePointCount - 1)
        text.substring(0, offset)
    } catch (_: Exception) {
        text.dropLast(1)
    }
}

private object EmojiInitializer {
    private var isInstalled = false

    fun ensureInstalled() {
        if (!isInstalled) {
            try {
                EmojiManager.install(IosEmojiProvider())
                isInstalled = true
            } catch (_: Exception) {}
        }
    }
}

private object EmojiDrawableHelper {
    private var method: java.lang.reflect.Method? = null
    private var methodSearched = false

    fun getDrawable(emoji: Emoji, context: Context): android.graphics.drawable.Drawable? {
        return try {
            if (!methodSearched) {
                method = emoji.javaClass.methods.firstOrNull { m ->
                    (m.name == "drawable" || m.name == "getDrawable") &&
                    m.parameterTypes.size == 1 &&
                    Context::class.java.isAssignableFrom(m.parameterTypes[0])
                }?.apply { isAccessible = true }
                methodSearched = true
            }
            method?.invoke(emoji, context) as? android.graphics.drawable.Drawable
        } catch (_: Exception) {
            null
        }
    }
}

private val RecentClockIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "RecentClock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val TabSmileIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TabSmile",
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

private val TabAnimalsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TabAnimals",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M4.5 11c1.38 0 2.5-1.12 2.5-2.5S5.88 6 4.5 6 2 7.12 2 8.5 3.12 11 4.5 11zm15 0c1.38 0 2.5-1.12 2.5-2.5S20.88 6 19.5 6 17 7.12 17 8.5s1.12 2.5 2.5 2.5zm-11-4C9.88 7 11 5.88 11 4.5S9.88 2 8.5 2 6 3.12 6 4.5 7.12 7 8.5 7zm7 0c1.38 0 2.5-1.12 2.5-2.5S16.88 2 15.5 2 13 3.12 13 4.5s1.12 2.5 2.5 2.5zM12 9.5c-3.04 0-5.5 2.46-5.5 5.5 0 2.76 1.84 5 4.5 5.4v1.6h2v-1.6c2.66-.4 4.5-2.64 4.5-5.4 0-3.04-2.46-5.5-5.5-5.5z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val TabFoodIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TabFood",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M18.06 22.99h1.66c.84 0 1.53-.64 1.63-1.46L23 5.05h-5V1h-1.97v4.05h-4.97l.3 2.34c1.71.47 3.31 1.32 4.27 2.26 1.44 1.42 2.43 2.89 2.43 5.29v8.05zM1 21.99V21h15v.99c0 .55-.45 1-1 1H2c-.55 0-1-.45-1-1zm15-4H1v-2c0-2.21 3.58-4 8-4s8 1.79 8 4v2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val TabActivityIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TabActivity",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 3.09c2.8.49 5 2.68 5.5 5.47L15 12l-2-2.5V5.09zM5.5 13.56c.49-2.8 2.68-5 5.47-5.5L12 9.9l-2 2.1-4.5 1.56zm.53 2.94L9 15l2 2.1v4.81c-2.8-.49-5-2.68-5.47-5.41zm7.97 4.41V16.9l2-2 3 1.5c-.49 2.76-2.68 4.96-5 5.51z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val TabTravelIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TabTravel",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val TabObjectsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TabObjects",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val TabSymbolsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TabSymbols",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val TabFlagsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TabFlags",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val EmojiBackspaceIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "EmojiBackspace",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M22 3H7c-.69 0-1.23.35-1.59.88L0 12l5.41 8.11c.36.53.9.89 1.59.89h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3 12.59L17.59 17 14 13.41 10.41 17 9 15.59 12.59 12 9 8.41 10.41 7 14 10.59 17.59 7 19 8.41 15.41 12 19 15.59z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private data class EmojiCategoryHeader(
    val title: String,
    val icon: ImageVector
)

private val CategoryTabs = listOf(
    EmojiCategoryHeader("Recent", RecentClockIcon),
    EmojiCategoryHeader("Smileys & People", TabSmileIcon),
    EmojiCategoryHeader("Animals & Nature", TabAnimalsIcon),
    EmojiCategoryHeader("Food & Drink", TabFoodIcon),
    EmojiCategoryHeader("Activity", TabActivityIcon),
    EmojiCategoryHeader("Travel & Places", TabTravelIcon),
    EmojiCategoryHeader("Objects", TabObjectsIcon),
    EmojiCategoryHeader("Symbols", TabSymbolsIcon),
    EmojiCategoryHeader("Flags", TabFlagsIcon)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatEmojiPicker(
    heightDp: Float,
    onEmojiClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    LaunchedEffect(Unit) {
        EmojiInitializer.ensureInstalled()
    }

    var selectedTabIndex by remember { mutableIntStateOf(1) }
    var recentEmojis by remember { mutableStateOf(RecentEmojiHelper.getRecentEmojis(context)) }

    val categories = remember {
        try {
            EmojiInitializer.ensureInstalled()
            EmojiManager.categories().toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    val listState = rememberLazyListState()
    val screenWidth = configuration.screenWidthDp.dp
    val itemSlotWidth = remember(screenWidth) { (screenWidth - 16.dp) / 8f }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .background(Color(0xFF13151B))
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = 0.08f)
            )
            .navigationBarsPadding()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(Color(0xFF181A22)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            itemsIndexed(CategoryTabs) { index, tab ->
                val isSelected = selectedTabIndex == index
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .padding(horizontal = 11.dp)
                        .bounceClick(scaleDown = 0.90f) {
                            selectedTabIndex = index
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) MomoRedLight else Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(20.dp)
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .height(2.5.dp)
                                .width(22.dp)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(MomoRedLight)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                item(key = "section_recent") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Frequently Used",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp, start = 4.dp)
                        )

                        if (recentEmojis.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = 8,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                recentEmojis.forEach { emojiStr ->
                                    Box(
                                        modifier = Modifier
                                            .size(itemSlotWidth)
                                            .bounceClick(scaleDown = 0.88f) {
                                                onEmojiClick(emojiStr)
                                                RecentEmojiHelper.recordEmoji(context, emojiStr)
                                                recentEmojis = RecentEmojiHelper.getRecentEmojis(context)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = emojiStr,
                                            fontSize = 24.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No recent emojis",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
                            )
                        }
                    }
                }

                itemsIndexed(categories, key = { idx, _ -> "cat_$idx" }) { index, category ->
                    val tabInfo = CategoryTabs.getOrNull(index + 1)
                    val sectionTitle = tabInfo?.title ?: "Emojis"
                    val emojiList = remember(category) { category.emojis }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = sectionTitle,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = 8,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            emojiList.forEach { emoji ->
                                EmojiGridCell(
                                    emoji = emoji,
                                    sizeDp = itemSlotWidth,
                                    onClick = { clickedEmojiStr ->
                                        onEmojiClick(clickedEmojiStr)
                                        RecentEmojiHelper.recordEmoji(context, clickedEmojiStr)
                                        recentEmojis = RecentEmojiHelper.getRecentEmojis(context)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .size(46.dp)
                    .bounceClick(scaleDown = 0.90f) {
                        onBackspaceClick()
                    },
                shape = CircleShape,
                color = Color(0xFF1E212B),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f)
                ),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = EmojiBackspaceIcon,
                        contentDescription = "Backspace",
                        tint = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiGridCell(
    emoji: Emoji,
    sizeDp: androidx.compose.ui.unit.Dp,
    onClick: (String) -> Unit
) {
    val context = LocalContext.current
    val drawable = remember(emoji) {
        EmojiDrawableHelper.getDrawable(emoji, context)
    }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .bounceClick(scaleDown = 0.88f) {
                onClick(emoji.unicode)
            },
        contentAlignment = Alignment.Center
    ) {
        if (drawable != null) {
            AsyncImage(
                model = drawable,
                contentDescription = emoji.unicode,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Text(
                text = emoji.unicode,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
