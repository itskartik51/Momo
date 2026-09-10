package com.personal.momo.UI_Screens.Add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.Cache.CacheManager
import java.time.LocalDate

@Composable
fun AddApyBdayContent(
    onDismiss: () -> Unit,
    onDateConfirmed: ((LocalDate) -> Unit)? = null
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    AddSheetContainer(
        title = "Log Period",
        onClose = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Select period start date",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            SegmentedDatePicker(
                selectedDate = selectedDate,
                onDateChanged = { newDate ->
                    selectedDate = newDate
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            AddActionButton(
                text = "Confirm Period Date",
                onClick = {
                    CacheManager.addPeriodDate(selectedDate)
                    onDateConfirmed?.invoke(selectedDate)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
