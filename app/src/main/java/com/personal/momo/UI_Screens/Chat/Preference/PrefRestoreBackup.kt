package com.personal.momo.UI_Screens.Chat.Preference

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.personal.momo.UI_Screens.Chat.ChatRepository
import kotlinx.coroutines.launch

@Composable
fun rememberPreRestoreBackupLauncher(): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = ChatRepository.restoreFromUri(context, uri)
                result.onSuccess { count ->
                    Toast.makeText(
                        context,
                        "Restored $count messages",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Failed to restore chat backup",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    return {
        scope.launch {
            val directResult = ChatRepository.restoreFromDefaultFile(context)
            directResult.onSuccess { count ->
                Toast.makeText(
                    context,
                    "Restored $count messages",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    context,
                    "Select momo_chat_history.json to restore",
                    Toast.LENGTH_SHORT
                ).show()
                documentPickerLauncher.launch(
                    arrayOf("application/json", "text/plain", "*/*")
                )
            }
        }
    }
}
