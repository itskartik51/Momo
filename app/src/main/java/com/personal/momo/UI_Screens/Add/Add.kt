package com.personal.momo.UI_Screens.Add

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
