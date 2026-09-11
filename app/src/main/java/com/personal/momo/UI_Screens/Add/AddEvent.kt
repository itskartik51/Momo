package com.personal.momo.UI_Screens.Add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Calendar.MomoEvent
import com.personal.momo.UI_Screens.Gradient6
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Composable
fun AddEventContent(
    onDismiss: () -> Unit,
    onEventCreated: ((MomoEvent) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSpecial by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showMilestoneDropdown by remember { mutableStateOf(false) }

    val isFormValid = title.isNotBlank() && description.isNotBlank()

    AddSheetContainer(
        title = "New Memory / Event",
        onClose = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Title Input with MomoPrimaryGradient focus border
            MomoGradientTextField(
                value = title,
                onValueChange = { title = it },
                label = "Event Title",
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Description Input with MomoPrimaryGradient focus border
            MomoGradientTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Side-by-side Row: 60% Date Picker Cell + 40% Milestone Dropdown Cell
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 60% Date Selector
                DateSelectorCell(
                    date = selectedDate,
                    label = "Date",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(0.6f)
                )

                // 40% Milestone Dropdown Selector
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .border(
                            width = 1.dp,
                            color = if (isSpecial) Color(0xFFFFB800).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { showMilestoneDropdown = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Milestone",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isSpecial) "True" else "False",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpecial) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Milestone",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMilestoneDropdown,
                        onDismissRequest = { showMilestoneDropdown = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "False",
                                    fontWeight = if (!isSpecial) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                isSpecial = false
                                showMilestoneDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "True",
                                    fontWeight = if (isSpecial) FontWeight.Bold else FontWeight.Medium,
                                    color = Color(0xFFFFB800)
                                )
                            },
                            onClick = {
                                isSpecial = true
                                showMilestoneDropdown = false
                            }
                        )
                    }
                }
            }

            // Native Android Date Picker Dialog
            if (showDatePicker) {
                MomoNativeDatePickerDialog(
                    initialDate = selectedDate,
                    onDateSelected = { newDate ->
                        selectedDate = newDate
                    },
                    onDismiss = { showDatePicker = false }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. Save Action Button
            AddActionButton(
                text = "Save Memory",
                enabled = isFormValid,
                brush = if (isSpecial) Gradient6 else MomoPrimaryGradient,
                onClick = {
                    if (isFormValid) {
                        val epoch = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val newEvent = MomoEvent(
                            id = UUID.randomUUID().toString(),
                            title = title.trim(),
                            description = description.trim(),
                            date = selectedDate,
                            epochMillis = epoch,
                            isSpecial = isSpecial
                        )
                        CacheManager.addEvent(newEvent)
                        onEventCreated?.invoke(newEvent)
                        onDismiss()
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
