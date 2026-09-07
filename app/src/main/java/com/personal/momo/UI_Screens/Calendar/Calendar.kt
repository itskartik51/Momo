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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
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
    var isDismissViaSelection by remember { mutableStateOf(false) }

    val canGoBack = currentYearMonth.isAfter(minYearMonth)
    val daysInMonth = currentYearMonth.lengthOfMonth()

    // Sunday (7 % 7 = 0) to Saturday (6 % 7 = 6)
    val startOffset = currentYearMonth.atDay(1).dayOfWeek.value % 7
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
            // 1. Hero Date Header (Date & Month standard, Year in MomoPrimaryGradient)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${selectedDate.year}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(brush = MomoPrimaryGradient)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Inline Mode Selector Row + Month Navigation Arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable Header that triggers the inline drill-down view or retracts it back up
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentViewMode == CalendarViewMode.DAYS) {
                                drillDownYear = currentYearMonth.year
                                isDismissViaSelection = false
                                currentViewMode = CalendarViewMode.YEARS
                            } else {
                                isDismissViaSelection = false
                                currentViewMode = CalendarViewMode.DAYS
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (currentViewMode) {
                            CalendarViewMode.DAYS -> "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${currentYearMonth.year}"
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

                // Month Nav Arrows (Visible only in standard Days mode)
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

            // 3. Stage Container with Directional Symmetrical Transitions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AnimatedContent(
                    targetState = currentViewMode,
                    transitionSpec = {
                        when {
                            // Standard Days -> Years (Slide In downwards from Top Header)
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
                            // Years -> Months (Fluid Zoom-In Transition)
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
                            // Returning to Days: Differentiated by Header Toggle vs Month Selection
                            targetState == CalendarViewMode.DAYS -> {
                                if (isDismissViaSelection) {
                                    // Auto-dismiss after Month Selection: Smooth Scale & Settle
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
                                } else {
                                    // Cancelled via Header Tap: Retract from bottom to UP into the header
                                    (slideInVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetY = { it / 3 }
                                    ) + fadeIn(tween(220)))
                                        .togetherWith(
                                            slideOutVertically(
                                                animationSpec = tween(200),
                                                targetOffsetY = { -it / 2 }
                                            ) + fadeOut(tween(180))
                                        )
                                }
                            }
                            else -> {
                                fadeIn(tween(180)).togetherWith(fadeOut(tween(180)))
                            }
                        }
                    },
                    label = "CalendarDrillDownTransition"
                ) { viewMode ->
                    when (viewMode) {
                        // Standard Calendar View
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

                                // Days Grid (6 Fixed Rows)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
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
                                                val dayNumber = cellIndex - startOffset + 1

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(42.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (dayNumber in 1..daysInMonth) {
                                                        val isSelected = selectedDate.year == currentYearMonth.year &&
                                                                selectedDate.monthValue == currentYearMonth.monthValue &&
                                                                selectedDate.dayOfMonth == dayNumber

                                                        val isToday = today.year == currentYearMonth.year &&
                                                                today.monthValue == currentYearMonth.monthValue &&
                                                                dayNumber == today.dayOfMonth

                                                        Box(
                                                            modifier = Modifier
                                                                .size(38.dp)
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
                                                                    selectedDate = currentYearMonth.atDay(dayNumber)
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "$dayNumber",
                                                                color = when {
                                                                    isSelected -> Color.White
                                                                    isToday -> MaterialTheme.colorScheme.primary
                                                                    else -> MaterialTheme.colorScheme.onSurface
                                                                },
                                                                fontSize = 15.sp,
                                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                                textAlign = TextAlign.Center
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

                        // Step 1: Scrollable Years Grid (Starting 2023)
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

                        // Step 2: Months Grid (Zoom-in view, auto-dismiss to Days)
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

                                    // Baseline check: Jan-Oct 2023 locked
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
                                                        isDismissViaSelection = true
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
