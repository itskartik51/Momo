package com.personal.momo.UI_Screens.Add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Calendar.MomoEvent
import com.personal.momo.UI_Screens.Gradient6
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

    val isFormValid = title.isNotBlank() && description.isNotBlank()

    AddSheetContainer(
        title = "New Memory / Event",
        onClose = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Special Milestone Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Special Milestone",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Highlights in Gold Gradient",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isSpecial,
                    onCheckedChange = { isSpecial = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFFB800)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Cupertino Date Wheel
            CupertinoDatePickerWheel(
                selectedDate = selectedDate,
                onDateChanged = { newDate ->
                    selectedDate = newDate
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Action Button
            AddActionButton(
                text = "Save Memory",
                enabled = isFormValid,
                brush = if (isSpecial) Gradient6 else com.personal.momo.UI_Screens.MomoPrimaryGradient,
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
