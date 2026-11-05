package com.personal.momo.UI_Screens.Chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.MomoRedLight
import com.personal.momo.UI_Screens.bounceClick
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

@Composable
internal fun EmojiText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
        style = style,
        onTextLayout = onTextLayout
    )
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

private data class NativeEmojiCategory(
    val title: String,
    val icon: ImageVector,
    val emojis: List<String>
)

private val CategoryTabsMeta = listOf(
    "Recent" to RecentClockIcon,
    "Smileys & People" to TabSmileIcon,
    "Animals & Nature" to TabAnimalsIcon,
    "Food & Drink" to TabFoodIcon,
    "Activity" to TabActivityIcon,
    "Travel & Places" to TabTravelIcon,
    "Objects" to TabObjectsIcon,
    "Symbols" to TabSymbolsIcon,
    "Flags" to TabFlagsIcon
)

private object NativeEmojiStore {
    val smileys: List<String> = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗",
        "😚", "😙", "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨", "😐", "😑", "😶", "😏",
        "😒", "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥴",
        "😵", "🤯", "🤠", "🥳", "🥸", "😎", "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳", "🥺", "😦", "😧",
        "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬", "😈",
        "👿", "💀", "💩", "🤡", "👹", "👺", "👻", "👽", "👾", "🤖", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿",
        "😾", "🙈", "🙉", "🙊", "💋", "💌", "💘", "💝", "💖", "💗", "💓", "💞", "💕", "💟", "❣️", "💔", "❤️", "🧡",
        "💛", "💚", "💙", "💜", "🤎", "🖤", "🤍", "💯", "💢", "💥", "💫", "💦", "💨", "🕳️", "💣", "💬", "🗨️", "🗯️",
        "💭", "💤", "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
        "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️",
        "💅", "🤳", "💪", "🦵", "🦶", "👂", "👃", "🧠", "👀", "👁️", "👅", "👄"
    )

    val animals: List<String> = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🐒",
        "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌",
        "🐞", "🐜", "🦟", "🦗", "🕷️", "🕸️", "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡",
        "🐠", "🐟", "🐬", "🐳", "🐋", "🦈", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🐘", "🦛", "🦏", "🐪", "🐫", "🦒",
        "🦘", "🐃", "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🦙", "🐐", "🦌", "🐕", "🐩", "🦮", "🐈", "🐈‍⬛", "🐓", "🦃",
        "🦚", "🦜", "🦢", "🦩", "🕊️", "🐇", "🦝", "🦨", "🦡", "🦦", "🦥", "🐁", "🐀", "🐿️", "🦔", "🐾", "🐉", "🐲",
        "🌵", "🎄", "🌲", "🌳", "🌴", "🌱", "🌿", "☘️", "🍀", "🎍", "🪴", "🎋", "🍃", "🍂", "🍁", "🍄", "🌾", "💐",
        "🌷", "🌹", "🥀", "🌺", "🌸", "🌼", "🌻", "🌞", "🌝", "🌛", "🌚", "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓",
        "🌔", "🌙", "🌎", "🌍", "🌏", "🪐", "⭐", "🌟", "✨", "⚡", "☄️", "💥", "🔥", "🌪️", "🌈", "☀️", "🌤️", "⛅",
        "🌥️", "☁️", "🌦️", "🌧️", "🌨️", "🌩️", "❄️", "☃️", "⛄", "🌬️", "💨", "💧", "💦", "☔", "🌊"
    )

    val food: List<String> = listOf(
        "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅",
        "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞", "🥖",
        "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕", "🫓", "🥪", "🥙",
        "🧆", "🌮", "🌯", "🫔", "🥗", "🥘", "🫕", "🥫", "🍝", "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🦪", "🍤", "🍙",
        "🍚", "🍘", "🍥", "🥠", "🥮", "🍢", "🍡", "🍧", "🍨", "🍦", "🥧", "🧁", "🍰", "🎂", "🍮", "🍭", "🍬", "🍫",
        "🍿", "🍩", "🍪", "🌰", "🥜", "🍯", "🥛", "🍼", "🫖", "☕", "🍵", "🧃", "🥤", "🧋", "🍶", "🍺", "🍻", "🥂",
        "🍷", "🥃", "🍸", "🍹", "🧉", "🍾", "🧊", "🥄", "🍴", "🍽️", "🥣", "🥡", "🥢", "🧂"
    )

    val activity: List<String> = listOf(
        "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "🪃",
        "🥅", "⛳", "🪁", "🏹", "🎣", "🤿", "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🎿", "⛷️", "🏂", "🪂", "🏋️",
        "🤼", "🤸", "⛹️", "🤺", "🤾", "🏌️", "🏇", "🧘", "🏄", "🏊", "🤽", "🚣", "🧗", "🚵", "🚴", "🏆", "🥇", "🥈",
        "🥉", "🏅", "🎖️", "🏵️", "🎗️", "🎫", "🎟️", "🎪", "🤹", "🎭", "🩰", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁",
        "🎷", "🎺", "🪗", "🎸", "🪕", "🎻", "🎲", "♟️", "🎯", "🎳", "🎮", "🎰", "🧩"
    )

    val travel: List<String> = listOf(
        "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🛴", "🚲", "🛵", "🏍️",
        "🛺", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠", "🚟", "🚃", "🚋", "🚞", "🚝", "🚄", "🚅", "🚈", "🚂", "🚆",
        "🚇", "🚊", "🚉", "🚁", "🛩️", "✈️", "🛫", "🛬", "🪂", "💺", "🛰️", "🚀", "🛸", "🛶", "⛵", "🚤", "🛥️", "🛳️",
        "⛴️", "🚢", "⚓", "🛟", "⛽", "🚧", "🚦", "🚥", "🚏", "🗺️", "🗿", "🗽", "🗼", "🏰", "🏯", "🏟️", "🎡", "🎢",
        "🎠", "⛲", "🏖️", "🏝️", "🏜️", "🌋", "⛰️", "🏔️", "🗻", "🏕️", "⛺", "🛖", "🏠", "🏡", "🏘️", "🏚️", "🏗️", "🏢",
        "🏬", "🏣", "🏤", "🏥", "🏦", "🏨", "🏪", "🏫", "🏩", "💒", "🏛️", "⛪", "🕌", "🛕", "🕍", "⛩️", "🕋"
    )

    val objects: List<String> = listOf(
        "⌚", "📱", "📲", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "🕹️", "💽", "💾", "💿", "📀", "📼", "📷", "📸", "📹", "🎥",
        "📽️", "🎞️", "📞", "☎️", "📟", "📠", "📺", "📻", "🎙️", "🎚️", "🎛️", "🧭", "⏱️", "⏲️", "⏰", "🕰️", "⌛", "⏳",
        "📡", "🔋", "🪫", "🔌", "💡", "🔦", "🕯️", "🧯", "🗑️", "🪣", "🛒", "💷", "💶", "💵", "💴", "💰", "🪙", "💳",
        "💎", "⚖️", "🪜", "🧰", "🪛", "🔧", "🔨", "⚒️", "🛠️", "⛏️", "🪚", "🔩", "⚙️", "🧱", "⛓️", "🧲", "🔫", "💣",
        "🧨", "🪓", "🔪", "🗡️", "⚔️", "🛡️", "🚬", "⚰️", "🪦", "⚱️", "🏺", "🔮", "📿", "🧿", "💈", "⚗️", "🔭", "🔬",
        "🩹", "🩺", "🩻", "🩼", "💊", "💉", "🩸", "🧬", "🦠", "🧫", "🧪", "🌡️", "🧹", "🪠", "🧺", "🧻", "🚽", "🚰",
        "🚿", "🛁", "🧼", "🪥", "🪒", "🧽", "🧴", "🗝️", "🔑", "🔐", "🔏", "🔒", "🔓", "🔔", "🔕", "📢", "📣", "🔍", "🔎"
    )

    val symbols: List<String> = listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🤎", "🖤", "🤍", "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝",
        "💟", "☮️", "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🛐", "⛎", "♈", "♉", "♊", "♋", "♌",
        "♍", "♎", "♏", "♐", "♑", "♒", "♓", "🆔", "⚛️", "☢️", "☣️", "📴", "📳", "🈶", "🈚", "🈸", "🈺", "🈷️", "✴️",
        "🆚", "💮", "🉐", "㊙️", "㊗️", "🈴", "🈵", "🈹", "🈲", "🅰️", "🅱️", "🆎", "🆑", "🅾️", "🆘", "❌", "⭕", "🛑",
        "⛔", "📛", "🚫", "💯", "💢", "♨️", "🚷", "🚯", "🚳", "🚱", "🔞", "📵", "🚭", "❗", "❕", "❓", "❔", "‼️",
        "⁉️", "🔅", "🔆", "〽️", "⚠️", "🚸", "🔱", "⚜️", "🔰", "♻️", "✅", "🈯", "💹", "❇️", "✳️", "❎", "🌐", "💠",
        "Ⓜ️", "🌀", "💤", "🏧", "🚾", "♿", "🅿️", "🛗", "🈳", "🈂️", "🛂", "🛃", "🛄", "🛅", "🚹", "🚺", "🚼", "🚻",
        "🚮", "🎦", "📶", "🈁", "🔣", "ℹ️", "🔤", "🔡", "🔠", "🔢", "#️⃣", "*️⃣", "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣",
        "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟", "🔲", "🔳", "🔴", "🟠", "🟡", "🟢", "🔵", "🟣", "🟤", "⚫", "⚪",
        "🟥", "🟧", "🟨", "🟩", "🟦", "🟪", "🟫", "⬛", "⬜", "🔈", "🔉", "🔊", "🔇", "🔔", "🔕", "📣", "📢", "💬",
        "💭", "🗯️", "♠️", "♣️", "♥️", "♦️", "🃏", "🎴", "🀄"
    )

    val flags: List<String> = listOf(
        "🏁", "🚩", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️", "🇮🇳", "🇺🇸", "🇬🇧", "🇨🇦", "🇦🇺", "🇩🇪", "🇫🇷", "🇯🇵",
        "🇰🇷", "🇨🇳", "🇷🇺", "🇧🇷", "🇿🇦", "🇮🇹", "🇪🇸", "🇳🇿", "🇸🇬", "🇦🇪", "🇸🇦", "🇮🇱", "🇹🇷", "🇪🇬", "🇲🇽", "🇦🇷",
        "🇦🇹", "🇧🇪", "🇨🇭", "🇩🇰", "🇫🇮", "🇬🇷", "🇮🇪", "🇳🇱", "🇳🇴", "🇵🇱", "🇵🇹", "🇸🇪", "🇹🇭", "🇻🇳", "🇲🇾", "🇵🇭",
        "🇮🇩", "🇵🇰", "🇧🇩", "🇳🇵", "🇱🇰", "🇲🇻", "🇧🇹", "🇦🇫", "🇶🇦", "🇰🇼", "🇴🇲", "🇧🇭", "🇯🇴", "🇱🇧", "🇮🇶", "🇮🇷"
    )
}

private val CuratedCategories: List<NativeEmojiCategory> = listOf(
    NativeEmojiCategory("Smileys & People", TabSmileIcon, NativeEmojiStore.smileys),
    NativeEmojiCategory("Animals & Nature", TabAnimalsIcon, NativeEmojiStore.animals),
    NativeEmojiCategory("Food & Drink", TabFoodIcon, NativeEmojiStore.food),
    NativeEmojiCategory("Activity", TabActivityIcon, NativeEmojiStore.activity),
    NativeEmojiCategory("Travel & Places", TabTravelIcon, NativeEmojiStore.travel),
    NativeEmojiCategory("Objects", TabObjectsIcon, NativeEmojiStore.objects),
    NativeEmojiCategory("Symbols", TabSymbolsIcon, NativeEmojiStore.symbols),
    NativeEmojiCategory("Flags", TabFlagsIcon, NativeEmojiStore.flags)
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

    var selectedTabIndex by remember { mutableIntStateOf(1) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var recentEmojis by remember { mutableStateOf(RecentEmojiHelper.getRecentEmojis(context)) }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 1)
    val tabRowState = rememberLazyListState()

    val screenWidth = configuration.screenWidthDp.dp
    val itemSlotWidth = remember(screenWidth) { (screenWidth - 16.dp) / 7f }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { visibleIndex ->
                if (!isProgrammaticScroll) {
                    val targetTab = visibleIndex.coerceIn(0, CategoryTabsMeta.lastIndex)
                    if (selectedTabIndex != targetTab) {
                        selectedTabIndex = targetTab
                        tabRowState.animateScrollToItem((targetTab - 2).coerceAtLeast(0))
                    }
                }
            }
    }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF181A22)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                state = tabRowState,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(CategoryTabsMeta) { index, (title, icon) ->
                    val isSelected = selectedTabIndex == index
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .padding(horizontal = 11.dp)
                            .bounceClick(scaleDown = 0.90f) {
                                selectedTabIndex = index
                                isProgrammaticScroll = true
                                coroutineScope.launch {
                                    tabRowState.animateScrollToItem((index - 2).coerceAtLeast(0))
                                    listState.animateScrollToItem(index)
                                    isProgrammaticScroll = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) MomoRedLight else Color.White.copy(alpha = 0.45f),
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
                    .height(22.dp)
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .bounceClick(scaleDown = 0.88f) {
                        onBackspaceClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = EmojiBackspaceIcon,
                    contentDescription = "Backspace",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

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
                            maxItemsInEachRow = 7,
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
                                        fontSize = 28.sp,
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

            itemsIndexed(CuratedCategories, key = { _, cat -> cat.title }) { _, category ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = category.title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 7,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        category.emojis.forEach { emojiStr ->
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
                                    fontSize = 28.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
