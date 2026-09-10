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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
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

// Baseline boundary constraint: November 2022
val MIN_YEAR_MONTH: YearMonth = YearMonth.of(2022, 11)
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
        pathData = PathParser().parsePathString(
            "M12 2.69L6.34 8.35a8 8 0 1 0 11.32 0L12 2.69z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionMenuAnchor(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(AddSheetType.NONE) }

    val fabRotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "FabRotationAnimation"
    )

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        // FAB Button with Gradient 3 & Clockwise Rotation Physics
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(brush = Gradient3)
                .bounceClick(scaleDown = 0.88f) {
                    isExpanded = !isExpanded
                },
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

        // Dropdown Menu Window Popup
        if (isExpanded) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(
                    x = 0,
                    y = with(LocalDensity.current) { 46.dp.roundToPx() }
                ),
                onDismissRequest = { isExpanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                )
            ) {
                var isVisible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    isVisible = true
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        expandFrom = Alignment.Top
                    ) + fadeIn(animationSpec = tween(180)) + scaleIn(
                        initialScale = 0.88f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(150),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(animationSpec = tween(120)) + scaleOut(
                        targetScale = 0.88f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(150)
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .width(190.dp)
                            .shadow(elevation = 14.dp, shape = RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            // 1. Add Hpy Bday (Water droplet with MomoPrimaryGradient)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick(scaleDown = 0.96f) {
                                        isExpanded = false
                                        activeSheet = AddSheetType.APY_BDAY
                                    }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(alpha = 0.99f)
                                        .drawWithCache {
                                            onDrawWithContent {
                                                drawContent()
                                                drawRect(
                                                    brush = MomoPrimaryGradient,
                                                    blendMode = BlendMode.SrcIn
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = DropletIcon,
                                        contentDescription = "Hpy Bday",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Text(
                                    text = "Add Hpy Bday",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // 2. Add Event (Calendar Icon)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick(scaleDown = 0.96f) {
                                        isExpanded = false
                                        activeSheet = AddSheetType.EVENT
                                    }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Add Event",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = "Add Event",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // 3. Add Reminder (Circle Check Icon)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick(scaleDown = 0.96f) {
                                        isExpanded = false
                                        activeSheet = AddSheetType.REMINDER
                                    }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Add Reminder",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = "Add Reminder",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets
    if (activeSheet != AddSheetType.NONE) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = AddSheetType.NONE },
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.45f),
            dragHandle = null
        ) {
            when (activeSheet) {
                AddSheetType.APY_BDAY -> AddApyBdayContent(
                    onDismiss = { activeSheet = AddSheetType.NONE }
                )
                AddSheetType.EVENT -> AddEventContent(
                    onDismiss = { activeSheet = AddSheetType.NONE }
                )
                AddSheetType.REMINDER -> AddReminderContent(
                    onDismiss = { activeSheet = AddSheetType.NONE }
                )
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

    // 1. Year List (2022..2100)
    val years = remember { (MIN_YEAR_MONTH.year..MAX_YEAR).toList() }

    // 2. Month List (strictly Nov..Dec for 2022, Jan..Dec for later years)
    val availableMonths = remember(currentYear) {
        if (currentYear == MIN_YEAR_MONTH.year) {
            (MIN_YEAR_MONTH.monthValue..12).toList()
        } else {
            (1..12).toList()
        }
    }

    // 3. Day List (calculated dynamically via Android java.time engine)
    val daysInMonth = remember(currentYear, currentMonth) {
        YearMonth.of(currentYear, currentMonth).lengthOfMonth()
    }
    val availableDays = remember(daysInMonth) { (1..daysInMonth).toList() }

    // Clamp values if year/month change caused out-of-range selection
    LaunchedEffect(currentYear, currentMonth, daysInMonth) {
        var adjustedMonth = currentMonth
        var adjustedDay = currentDay

        if (currentYear == MIN_YEAR_MONTH.year && adjustedMonth < MIN_YEAR_MONTH.monthValue) {
            adjustedMonth = MIN_YEAR_MONTH.monthValue
        }
        if (adjustedDay > daysInMonth) {
            adjustedDay = daysInMonth
        }

        if (adjustedMonth != currentMonth || adjustedDay != currentDay) {
            onDateChanged(LocalDate.of(currentYear, adjustedMonth, adjustedDay))
        }
    }

    val itemHeight = 44.dp
    val visibleItems = 3
    val wheelHeight = itemHeight * visibleItems

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(wheelHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        // Central selection pill highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        )

        // 3 Vertical Snap Wheels: Day | Month | Year
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day Drum
            Box(modifier = Modifier.weight(1f)) {
                SingleWheelDrum(
                    items = availableDays,
                    selectedItem = currentDay,
                    itemHeight = itemHeight,
                    format = { String.format(Locale.ENGLISH, "%02d", it) },
                    onItemSelected = { newDay ->
                        if (newDay != currentDay) {
                            onDateChanged(LocalDate.of(currentYear, currentMonth, newDay))
                        }
                    }
                )
            }

            // Month Drum
            Box(modifier = Modifier.weight(1.2f)) {
                SingleWheelDrum(
                    items = availableMonths,
                    selectedItem = currentMonth,
                    itemHeight = itemHeight,
                    format = { monthNum ->
                        Month.of(monthNum).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    },
                    onItemSelected = { newMonth ->
                        if (newMonth != currentMonth) {
                            val maxDays = YearMonth.of(currentYear, newMonth).lengthOfMonth()
                            val clampedDay = currentDay.coerceAtMost(maxDays)
                            onDateChanged(LocalDate.of(currentYear, newMonth, clampedDay))
                        }
                    }
                )
            }

            // Year Drum
            Box(modifier = Modifier.weight(1.1f)) {
                SingleWheelDrum(
                    items = years,
                    selectedItem = currentYear,
                    itemHeight = itemHeight,
                    format = { it.toString() },
                    onItemSelected = { newYear ->
                        if (newYear != currentYear) {
                            var targetMonth = currentMonth
                            if (newYear == MIN_YEAR_MONTH.year && targetMonth < MIN_YEAR_MONTH.monthValue) {
                                targetMonth = MIN_YEAR_MONTH.monthValue
                            }
                            val maxDays = YearMonth.of(newYear, targetMonth).lengthOfMonth()
                            val clampedDay = currentDay.coerceAtMost(maxDays)
                            onDateChanged(LocalDate.of(newYear, targetMonth, clampedDay))
                        }
                    }
                )
            }
        }

        // Top & Bottom subtle fade gradients for visual drum depth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                        )
                    )
                )
        )
    }
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
    val initialIndex = remember(items, selectedItem) {
        val idx = items.indexOf(selectedItem)
        if (idx >= 0) idx else 0
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(items, selectedItem) {
        val targetIndex = items.indexOf(selectedItem)
        if (targetIndex >= 0 && listState.firstVisibleItemIndex != targetIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index in items.indices) {
                    onItemSelected(items[index])
                }
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
            val isSelected by remember {
                derivedStateOf { listState.firstVisibleItemIndex == index }
            }

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
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
            // Header Row
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
            .then(
                if (enabled) {
                    Modifier.bounceClick(scaleDown = 0.95f) { onClick() }
                } else {
                    Modifier
                }
            ),
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
