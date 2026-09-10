package com.personal.momo.UI_Screens.Add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.personal.momo.UI_Screens.Gradient3
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

const val MIN_YEAR = 2022
const val MAX_YEAR = 2100

enum class AddSheetType {
    NONE,
    APY_BDAY,
    EVENT,
    REMINDER
}

private val DropletIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Droplet",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString("M12 2.69L6.34 8.35a8 8 0 1 0 11.32 0L12 2.69z").toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

@Composable
fun GradientIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    brush: Brush = MomoPrimaryGradient,
    size: Dp = 19.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer(alpha = 0.99f)
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = brush, blendMode = BlendMode.SrcIn)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .wrapContentWidth()
            .bounceClick(scaleDown = 0.94f) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GradientIcon(imageVector = icon, contentDescription = label)
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionMenuAnchor(modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(AddSheetType.NONE) }

    val fabRotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "Fab45DegreeRotation"
    )

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(brush = Gradient3)
                .bounceClick(scaleDown = 0.88f) { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Menu",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(fabRotation)
            )
        }

        if (isExpanded) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = 0, y = with(LocalDensity.current) { 46.dp.roundToPx() }),
                onDismissRequest = { isExpanded = false },
                properties = PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
            ) {
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { isVisible = true }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = expandVertically(
                        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
                        expandFrom = Alignment.Top
                    ) + fadeIn(tween(180)) + scaleIn(
                        initialScale = 0.85f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                    ),
                    exit = shrinkVertically(tween(150), Alignment.Top) + fadeOut(tween(120)) + scaleOut(
                        targetScale = 0.85f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(150)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        ActionPill(DropletIcon, "Add Hpy Bday") {
                            isExpanded = false
                            activeSheet = AddSheetType.APY_BDAY
                        }
                        ActionPill(Icons.Default.DateRange, "Add Event") {
                            isExpanded = false
                            activeSheet = AddSheetType.EVENT
                        }
                        ActionPill(Icons.Default.CheckCircle, "Add Reminder") {
                            isExpanded = false
                            activeSheet = AddSheetType.REMINDER
                        }
                    }
                }
            }
        }
    }

    if (activeSheet != AddSheetType.NONE) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = AddSheetType.NONE },
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.45f),
            dragHandle = null
        ) {
            when (activeSheet) {
                AddSheetType.APY_BDAY -> AddApyBdayContent(onDismiss = { activeSheet = AddSheetType.NONE })
                AddSheetType.EVENT -> AddEventContent(onDismiss = { activeSheet = AddSheetType.NONE })
                AddSheetType.REMINDER -> AddReminderContent(onDismiss = { activeSheet = AddSheetType.NONE })
                AddSheetType.NONE -> {}
            }
        }
    }
}

@Composable
fun CupertinoDatePickerWheel(
    selectedDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear = selectedDate.year
    val currentMonth = selectedDate.monthValue
    val currentDay = selectedDate.dayOfMonth

    val years = remember { (MIN_YEAR..MAX_YEAR).toList() }
    val months = remember { (1..12).toList() }

    val daysInMonth = remember(currentYear, currentMonth) {
        YearMonth.of(currentYear, currentMonth).lengthOfMonth()
    }
    val availableDays = remember(daysInMonth) { (1..daysInMonth).toList() }

    LaunchedEffect(daysInMonth) {
        if (currentDay > daysInMonth) {
            onDateChanged(LocalDate.of(currentYear, currentMonth, daysInMonth))
        }
    }

    val itemHeight = 44.dp
    val wheelHeight = itemHeight * 3

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(wheelHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SingleWheelDrum(
                    items = availableDays,
                    selectedItem = currentDay.coerceAtMost(daysInMonth),
                    itemHeight = itemHeight,
                    format = { String.format(Locale.ENGLISH, "%02d", it) }
                ) { newDay ->
                    if (newDay != currentDay) {
                        onDateChanged(LocalDate.of(currentYear, currentMonth, newDay))
                    }
                }
            }

            Box(modifier = Modifier.weight(1.2f)) {
                SingleWheelDrum(
                    items = months,
                    selectedItem = currentMonth,
                    itemHeight = itemHeight,
                    format = { Month.of(it).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
                ) { newMonth ->
                    if (newMonth != currentMonth) {
                        val maxDays = YearMonth.of(currentYear, newMonth).lengthOfMonth()
                        onDateChanged(LocalDate.of(currentYear, newMonth, currentDay.coerceAtMost(maxDays)))
                    }
                }
            }

            Box(modifier = Modifier.weight(1.1f)) {
                SingleWheelDrum(
                    items = years,
                    selectedItem = currentYear,
                    itemHeight = itemHeight,
                    format = { it.toString() }
                ) { newYear ->
                    if (newYear != currentYear) {
                        val maxDays = YearMonth.of(newYear, currentMonth).lengthOfMonth()
                        onDateChanged(LocalDate.of(newYear, currentMonth, currentDay.coerceAtMost(maxDays)))
                    }
                }
            }
        }

        WheelDepthOverlay(isTop = true, itemHeight = itemHeight, modifier = Modifier.align(Alignment.TopCenter))
        WheelDepthOverlay(isTop = false, itemHeight = itemHeight, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun WheelDepthOverlay(isTop: Boolean, itemHeight: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight)
            .background(
                Brush.verticalGradient(
                    colors = if (isTop) {
                        listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), Color.Transparent)
                    } else {
                        listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                    }
                )
            )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> SingleWheelDrum(
    items: List<T>,
    selectedItem: T,
    itemHeight: Dp,
    format: (T) -> String,
    onItemSelected: (T) -> Unit
) {
    val initialIndex = remember { items.indexOf(selectedItem).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(selectedItem, items) {
        val targetIndex = items.indexOf(selectedItem)
        if (targetIndex >= 0 && !listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val currentCenterIndex = layoutInfo.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index

            if (currentCenterIndex != targetIndex) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (!isScrolling) {
                    val layoutInfo = listState.layoutInfo
                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    val centerItem = layoutInfo.visibleItemsInfo
                        .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }

                    centerItem?.let { itemInfo ->
                        if (itemInfo.index in items.indices) {
                            val selectedValue = items[itemInfo.index]
                            if (selectedValue != selectedItem) {
                                onItemSelected(selectedValue)
                            }
                        }
                    }
                }
            }
    }

    val centerVisibleIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: listState.firstVisibleItemIndex
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * 3)
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = centerVisibleIndex == index

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = format(item),
                    fontSize = if (isSelected) 17.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AddSheetContainer(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .bounceClick(scaleDown = 0.88f) { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
fun AddActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = MomoPrimaryGradient,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .background(if (enabled) brush else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)))
            .then(if (enabled) Modifier.bounceClick(scaleDown = 0.95f) { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
