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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

enum class AddSheetType {
    NONE,
    APY_BDAY,
    EVENT,
    REMINDER
}

enum class DatePickerSegment {
    DAY,
    MONTH,
    YEAR
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
    // Pure Box implementation bypasses Material 3 tonal elevation color tinting, matching the form sheet background 1:1
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = CircleShape
            )
            .bounceClick(scaleDown = 0.94f) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
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
fun SegmentedDatePicker(
    selectedDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    var activeSegment by remember { mutableStateOf(DatePickerSegment.DAY) }

    val currentYear = selectedDate.year
    val currentMonth = selectedDate.monthValue
    val currentDay = selectedDate.dayOfMonth

    val daysInMonth = remember(currentYear, currentMonth) {
        YearMonth.of(currentYear, currentMonth).lengthOfMonth()
    }

    val yearsList = remember(minDate, maxDate) {
        val start = minDate?.year ?: 2022
        val end = maxDate?.year ?: 2035
        (start..end).toList()
    }

    val monthsList = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        // 1. Top 3 Cells: Day | Month | Year
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SegmentHeaderCell(
                text = String.format(Locale.ENGLISH, "%02d", currentDay),
                isSelected = activeSegment == DatePickerSegment.DAY,
                modifier = Modifier.weight(1f),
                onClick = { activeSegment = DatePickerSegment.DAY }
            )

            SegmentHeaderCell(
                text = Month.of(currentMonth).getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                isSelected = activeSegment == DatePickerSegment.MONTH,
                modifier = Modifier.weight(1.2f),
                onClick = { activeSegment = DatePickerSegment.MONTH }
            )

            SegmentHeaderCell(
                text = currentYear.toString(),
                isSelected = activeSegment == DatePickerSegment.YEAR,
                modifier = Modifier.weight(1.1f),
                onClick = { activeSegment = DatePickerSegment.YEAR }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Dynamic Selection Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            when (activeSegment) {
                DatePickerSegment.DAY -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(daysInMonth) { index ->
                            val day = index + 1
                            val isSelected = day == currentDay
                            val isEnabled = when {
                                minDate != null && currentYear == minDate.year && currentMonth == minDate.monthValue -> day >= minDate.dayOfMonth
                                maxDate != null && currentYear == maxDate.year && currentMonth == maxDate.monthValue -> day <= maxDate.dayOfMonth
                                else -> true
                            }

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(brush = MomoPrimaryGradient)
                                        } else if (isEnabled) {
                                            Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .then(
                                        if (isEnabled) {
                                            Modifier.bounceClick(scaleDown = 0.88f) {
                                                onDateChanged(LocalDate.of(currentYear, currentMonth, day))
                                            }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> Color.White
                                        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                DatePickerSegment.MONTH -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(12) { index ->
                            val monthNum = index + 1
                            val isSelected = monthNum == currentMonth
                            val isEnabled = when {
                                minDate != null && currentYear == minDate.year -> monthNum >= minDate.monthValue
                                maxDate != null && currentYear == maxDate.year -> monthNum <= maxDate.monthValue
                                else -> true
                            }

                            Box(
                                modifier = Modifier
                                    .height(42.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(brush = MomoPrimaryGradient)
                                        } else if (isEnabled) {
                                            Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .then(
                                        if (isEnabled) {
                                            Modifier.bounceClick(scaleDown = 0.9f) {
                                                val maxDays = YearMonth.of(currentYear, monthNum).lengthOfMonth()
                                                var clampedDay = currentDay.coerceAtMost(maxDays)
                                                if (minDate != null && currentYear == minDate.year && monthNum == minDate.monthValue && clampedDay < minDate.dayOfMonth) {
                                                    clampedDay = minDate.dayOfMonth
                                                }
                                                onDateChanged(LocalDate.of(currentYear, monthNum, clampedDay))
                                                activeSegment = DatePickerSegment.DAY
                                            }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monthsList[index],
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> Color.White
                                        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                DatePickerSegment.YEAR -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(yearsList) { year ->
                            val isSelected = year == currentYear

                            Box(
                                modifier = Modifier
                                    .height(42.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(brush = MomoPrimaryGradient)
                                        } else {
                                            Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        }
                                    )
                                    .bounceClick(scaleDown = 0.9f) {
                                        var safeMonth = currentMonth
                                        if (minDate != null && year == minDate.year && safeMonth < minDate.monthValue) {
                                            safeMonth = minDate.monthValue
                                        }
                                        val maxDays = YearMonth.of(year, safeMonth).lengthOfMonth()
                                        var safeDay = currentDay.coerceAtMost(maxDays)
                                        if (minDate != null && year == minDate.year && safeMonth == minDate.monthValue && safeDay < minDate.dayOfMonth) {
                                            safeDay = minDate.dayOfMonth
                                        }
                                        onDateChanged(LocalDate.of(year, safeMonth, safeDay))
                                        activeSegment = DatePickerSegment.MONTH
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$year",
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentHeaderCell(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.5.dp,
                            brush = MomoPrimaryGradient,
                            shape = RoundedCornerShape(14.dp)
                        )
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp)
                        )
                }
            )
            .bounceClick(scaleDown = 0.95f) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
