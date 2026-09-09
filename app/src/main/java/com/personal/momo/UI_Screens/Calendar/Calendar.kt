package com.personal.momo.UI_Screens.Calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Gradient4
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

private enum class CalendarViewMode {
    DAYS,
    YEARS,
    MONTHS
}

@Composable
fun MomoCalendar(
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val minYearMonth = remember { YearMonth.of(2023, 11) } // Baseline Lock: Nov 2023

    var selectedDate by remember { mutableStateOf(today) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var currentViewMode by remember { mutableStateOf(CalendarViewMode.DAYS) }
    var drillDownYear by remember { mutableIntStateOf(currentYearMonth.year) }

    // 1. Collect real historical period dates from CacheManager
    val loggedPeriodDates by CacheManager.periodDatesFlow.collectAsState()

    // 2. Collect cached events from CacheManager
    val allEvents by CacheManager.eventsFlow.collectAsState()

    // 3. Single source of truth for all cycle calculations & pre-generated date sets
    val calendarCycleData = remember(loggedPeriodDates) {
        ApyBdayCalculator.getCalendarData(loggedPeriodDates)
    }

    // 4. Pre-computed active event dates for the visible month provided directly by Events.kt
    val eventDatesInMonth = remember(allEvents, currentYearMonth) {
        MomoEventsCalculator.getEventDatesInMonth(currentYearMonth, allEvents)
    }

    val canGoBack = currentYearMonth.isAfter(minYearMonth)
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")

    val yearsList = remember { (2023..2040).toList() }
    val monthsList = remember {
        listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (currentViewMode != CalendarViewMode.DAYS) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "DropdownArrowRotation"
    )

    val calendarShape = RoundedCornerShape(24.dp)
    val ovulationSkyBlue = Color(0xFF2BA4B5)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = calendarShape,
                clip = false,
                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)
            ),
        shape = calendarShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // 1. Hero Date Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(DateTextStyle.SHORT, Locale.ENGLISH)} ",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${selectedDate.year}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(brush = MomoPrimaryGradient)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Inline Mode Selector Row + Month Navigation Arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            currentViewMode = if (currentViewMode == CalendarViewMode.DAYS) {
                                drillDownYear = currentYearMonth.year
                                CalendarViewMode.YEARS
                            } else {
                                CalendarViewMode.DAYS
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (currentViewMode) {
                            CalendarViewMode.DAYS -> "${currentYearMonth.month.getDisplayName(DateTextStyle.FULL, Locale.ENGLISH)} ${currentYearMonth.year}"
                            CalendarViewMode.YEARS -> "Select Year"
                            CalendarViewMode.MONTHS -> "Select Month ($drillDownYear)"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Toggle Year/Month View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(arrowRotation)
                    )
                }

                AnimatedVisibility(
                    visible = currentViewMode == CalendarViewMode.DAYS,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(100))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .then(
                                    if (canGoBack) {
                                        Modifier.bounceClick(scaleDown = 0.85f) {
                                            currentYearMonth = currentYearMonth.minusMonths(1)
                                        }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = if (canGoBack) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .bounceClick(scaleDown = 0.85f) {
                                    currentYearMonth = currentYearMonth.plusMonths(1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Stage Container with Animated Views
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AnimatedContent(
                    targetState = currentViewMode,
                    transitionSpec = {
                        when {
                            initialState == CalendarViewMode.DAYS && targetState == CalendarViewMode.YEARS -> {
                                (slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    initialOffsetY = { -it / 2 }
                                ) + fadeIn(tween(220)))
                                    .togetherWith(
                                        slideOutVertically(
                                            animationSpec = tween(180),
                                            targetOffsetY = { it / 3 }
                                        ) + fadeOut(tween(160))
                                    )
                            }
                            initialState == CalendarViewMode.YEARS && targetState == CalendarViewMode.MONTHS -> {
                                (scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    initialScale = 0.84f
                                ) + fadeIn(tween(200)))
                                    .togetherWith(
                                        scaleOut(
                                            animationSpec = tween(160),
                                            targetScale = 1.12f
                                        ) + fadeOut(tween(140))
                                    )
                            }
                            targetState == CalendarViewMode.DAYS -> {
                                (scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    initialScale = 0.94f
                                ) + fadeIn(tween(240)))
                                    .togetherWith(
                                        scaleOut(
                                            animationSpec = tween(180),
                                            targetScale = 0.94f
                                        ) + fadeOut(tween(160))
                                    )
                            }
                            else -> {
                                fadeIn(tween(180)).togetherWith(fadeOut(tween(180)))
                            }
                        }
                    },
                    label = "CalendarDrillDownTransition"
                ) { viewMode ->
                    when (viewMode) {
                        CalendarViewMode.DAYS -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Weekday Labels
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    weekDays.forEach { day ->
                                        Text(
                                            text = day,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Days Grid Container with Swipe Gestures and Directional Sliding Transitions
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .pointerInput(currentYearMonth, canGoBack) {
                                            var totalDrag = 0f
                                            detectHorizontalDragGestures(
                                                onDragStart = { totalDrag = 0f },
                                                onDragEnd = {
                                                    val swipeThreshold = 40.dp.toPx()
                                                    if (totalDrag < -swipeThreshold) {
                                                        // Swipe Left -> Next Month
                                                        currentYearMonth = currentYearMonth.plusMonths(1)
                                                    } else if (totalDrag > swipeThreshold && canGoBack) {
                                                        // Swipe Right -> Previous Month
                                                        currentYearMonth = currentYearMonth.minusMonths(1)
                                                    }
                                                },
                                                onDragCancel = { totalDrag = 0f },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    totalDrag += dragAmount
                                                }
                                            )
                                        }
                                ) {
                                    AnimatedContent(
                                        targetState = currentYearMonth,
                                        transitionSpec = {
                                            if (targetState.isAfter(initialState)) {
                                                (slideInHorizontally(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    ),
                                                    initialOffsetX = { it }
                                                ) + fadeIn(animationSpec = tween(180))).togetherWith(
                                                    slideOutHorizontally(
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        ),
                                                        targetOffsetX = { -it }
                                                    ) + fadeOut(animationSpec = tween(150))
                                                )
                                            } else {
                                                (slideInHorizontally(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    ),
                                                    initialOffsetX = { -it }
                                                ) + fadeIn(animationSpec = tween(180))).togetherWith(
                                                    slideOutHorizontally(
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        ),
                                                        targetOffsetX = { it }
                                                    ) + fadeOut(animationSpec = tween(150))
                                                )
                                            }
                                        },
                                        label = "MonthGridSlideTransition"
                                    ) { targetMonth ->
                                        val monthDaysCount = targetMonth.lengthOfMonth()
                                        val monthStartOffset = targetMonth.atDay(1).dayOfWeek.value % 7

                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            for (row in 0 until 6) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    for (col in 0 until 7) {
                                                        val cellIndex = row * 7 + col
                                                        val dayNumber = cellIndex - monthStartOffset + 1

                                                        if (dayNumber in 1..monthDaysCount) {
                                                            val currentDate = targetMonth.atDay(dayNumber)

                                                            // Fast Set membership checks directly from pre-computed CalendarCycleData
                                                            val isPeriod = currentDate in calendarCycleData.confirmedBleedDates
                                                            val isPredictedPeriod = (currentDate in calendarCycleData.predictedBleedDates) && !isPeriod
                                                            val isFertile = (currentDate in calendarCycleData.fertileDates) && !isPeriod && !isPredictedPeriod
                                                            val isOvulation = (currentDate in calendarCycleData.ovulationDates) && !isPeriod && !isPredictedPeriod
                                                            val isSelected = selectedDate == currentDate
                                                            val isToday = today == currentDate

                                                            // Purely dumb check from Events.kt pre-computed monthly summary
                                                            val hasEvent = currentDate in eventDatesInMonth

                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(42.dp)
                                                                    .drawBehind {
                                                                        val cx = size.width / 2f
                                                                        val cy = size.height / 2f
                                                                        val r = 19.dp.toPx()

                                                                        // 1. Peak Ovulation Accent Dotted Circle
                                                                        if (isOvulation) {
                                                                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                                                            drawCircle(
                                                                                color = ovulationSkyBlue.copy(alpha = 0.08f),
                                                                                radius = 16.5.dp.toPx(),
                                                                                center = Offset(cx, cy)
                                                                            )
                                                                            drawCircle(
                                                                                color = ovulationSkyBlue,
                                                                                radius = 16.5.dp.toPx(),
                                                                                center = Offset(cx, cy),
                                                                                style = Stroke(width = 1.8.dp.toPx(), pathEffect = dashEffect)
                                                                            )
                                                                        }

                                                                        // 2. Future Predicted Period Dotted Circle in MomoPrimaryGradient
                                                                        if (isPredictedPeriod) {
                                                                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.5f, 4.5f), 0f)
                                                                            drawCircle(
                                                                                brush = MomoPrimaryGradient,
                                                                                radius = 16.5.dp.toPx(),
                                                                                center = Offset(cx, cy),
                                                                                style = Stroke(width = 1.6.dp.toPx(), pathEffect = dashEffect)
                                                                            )
                                                                        }

                                                                        // 3. Selection Highlight Ring / Solid Fill
                                                                        if (isSelected) {
                                                                            if (isPeriod || isPredictedPeriod) {
                                                                                drawCircle(
                                                                                    brush = MomoPrimaryGradient,
                                                                                    radius = r,
                                                                                    center = Offset(cx, cy),
                                                                                    style = Stroke(width = 1.8.dp.toPx())
                                                                                )
                                                                            } else {
                                                                                drawCircle(
                                                                                    brush = MomoPrimaryGradient,
                                                                                    radius = r,
                                                                                    center = Offset(cx, cy)
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                    .clickable(
                                                                        interactionSource = remember { MutableInteractionSource() },
                                                                        indication = null
                                                                    ) {
                                                                        selectedDate = currentDate
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "$dayNumber",
                                                                    style = when {
                                                                        isPeriod -> TextStyle(brush = MomoPrimaryGradient)
                                                                        isPredictedPeriod -> TextStyle(brush = MomoPrimaryGradient)
                                                                        isSelected -> TextStyle(color = Color.White)
                                                                        isFertile -> TextStyle(color = ovulationSkyBlue)
                                                                        isToday -> TextStyle(color = MaterialTheme.colorScheme.primary)
                                                                        else -> TextStyle(color = MaterialTheme.colorScheme.onSurface)
                                                                    },
                                                                    fontSize = 15.sp,
                                                                    fontWeight = if (isPeriod || isPredictedPeriod || isSelected || isToday || isOvulation) FontWeight.Bold else FontWeight.Medium,
                                                                    textAlign = TextAlign.Center
                                                                )

                                                                if (hasEvent) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .align(Alignment.BottomCenter)
                                                                            .padding(bottom = 3.5.dp)
                                                                            .size(4.dp)
                                                                            .clip(CircleShape)
                                                                            .background(
                                                                                brush = if (isSelected) {
                                                                                    SolidColor(Color.White)
                                                                                } else {
                                                                                    Gradient4
                                                                                }
                                                                            )
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            // Empty cell for alignment
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(42.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        CalendarViewMode.YEARS -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp)
                            ) {
                                items(yearsList) { year ->
                                    val isSelected = year == currentYearMonth.year

                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(brush = MomoPrimaryGradient)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                drillDownYear = year
                                                currentViewMode = CalendarViewMode.MONTHS
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$year",
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        CalendarViewMode.MONTHS -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp)
                            ) {
                                items(12) { index ->
                                    val monthIndex = index + 1
                                    val monthName = monthsList[index]
                                    val isSelected = currentYearMonth.year == drillDownYear && currentYearMonth.monthValue == monthIndex

                                    val isLocked = drillDownYear == minYearMonth.year && monthIndex < minYearMonth.monthValue

                                    Box(
                                        modifier = Modifier
                                            .height(46.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(brush = MomoPrimaryGradient)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .then(
                                                if (!isLocked) {
                                                    Modifier.clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        val targetYearMonth = YearMonth.of(drillDownYear, monthIndex)
                                                        currentYearMonth = targetYearMonth
                                                        val clampedDay = selectedDate.dayOfMonth.coerceAtMost(targetYearMonth.lengthOfMonth())
                                                        selectedDate = targetYearMonth.atDay(clampedDay)
                                                        currentViewMode = CalendarViewMode.DAYS
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = monthName,
                                            color = when {
                                                isSelected -> Color.White
                                                isLocked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
