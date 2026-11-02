package com.personal.momo.UI_Screens.Chat.Preference

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PrefNameManager {
    private const val PREFS_NAME = "momo_name_preferences"
    private const val KEY_CACHED_PARTNER_NAME = "cached_partner_display_name"
    private const val COLLECTION_APP = "App"
    private const val DOC_HOME_CONFIG = "home_config"
    private const val FIELD_CUSTOM = "Custom"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var snapshotListener: ListenerRegistration? = null

    private val _partnerDisplayNameFlow = MutableStateFlow("")
    val partnerDisplayNameFlow: StateFlow<String> = _partnerDisplayNameFlow.asStateFlow()

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        val appContext = context.applicationContext
        val currentUserId = CacheManager.getAppUserId(appContext)
        val isMomo = currentUserId.equals("Momo", ignoreCase = true)
        val defaultName = if (isMomo) "Kanu" else "Momo"

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedName = prefs.getString(KEY_CACHED_PARTNER_NAME, null)
        _partnerDisplayNameFlow.value = if (!cachedName.isNullOrBlank()) cachedName else defaultName

        if (isInitialized) return
        isInitialized = true

        listenToFirestoreUpdates(appContext)
    }

    fun getPartnerDisplayName(context: Context): String {
        val appContext = context.applicationContext
        if (_partnerDisplayNameFlow.value.isNotBlank()) {
            return _partnerDisplayNameFlow.value
        }

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_CACHED_PARTNER_NAME, null)
        if (!cached.isNullOrBlank()) return cached

        val currentUserId = CacheManager.getAppUserId(appContext)
        return if (currentUserId.equals("Momo", ignoreCase = true)) "Kanu" else "Momo"
    }

    private fun listenToFirestoreUpdates(appContext: Context) {
        snapshotListener?.remove()

        val docRef = firestore.collection(COLLECTION_APP).document(DOC_HOME_CONFIG)
        snapshotListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val currentUserId = CacheManager.getAppUserId(appContext)
            val isMomo = currentUserId.equals("Momo", ignoreCase = true)
            val targetIndex = if (isMomo) 0 else 1
            val defaultName = if (isMomo) "Kanu" else "Momo"

            val customList = snapshot.get(FIELD_CUSTOM) as? List<*>
            val rawName = customList?.getOrNull(targetIndex)?.toString()?.trim()
            val resolvedName = if (!rawName.isNullOrBlank()) rawName else defaultName

            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CACHED_PARTNER_NAME, resolvedName).apply()

            _partnerDisplayNameFlow.value = resolvedName
        }
    }

    fun updatePartnerDisplayName(context: Context, newName: String, onComplete: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) {
            onComplete(false)
            return
        }

        val currentUserId = CacheManager.getAppUserId(appContext)
        val isMomo = currentUserId.equals("Momo", ignoreCase = true)
        val targetIndex = if (isMomo) 0 else 1

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CACHED_PARTNER_NAME, trimmedName).apply()
        _partnerDisplayNameFlow.value = trimmedName

        val docRef = firestore.collection(COLLECTION_APP).document(DOC_HOME_CONFIG)
        docRef.get().addOnSuccessListener { snapshot ->
            val existingList = (snapshot?.get(FIELD_CUSTOM) as? List<*>)
                ?.map { it?.toString() ?: "" }
                ?.toMutableList()
                ?: mutableListOf("Boss", "Chhana")

            while (existingList.size <= targetIndex) {
                existingList.add("")
            }

            existingList[targetIndex] = trimmedName

            docRef.update(FIELD_CUSTOM, existingList)
                .addOnSuccessListener {
                    onComplete(true)
                }
                .addOnFailureListener {
                    onComplete(false)
                }
        }.addOnFailureListener {
            onComplete(false)
        }
    }
}

private val DialogSurfaceColor = Color(0xFF1E1F22)
private val InputFieldTrackColor = Color(0xFF151619)

private val CloseDialogIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CloseDialogIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

@Composable
fun PrefNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onNameSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(initialName.length)
            )
        )
    }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = DialogSurfaceColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MomoPrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(11.dp))

                        Text(
                            text = "Partner Name",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .bounceClick(scaleDown = 0.94f) {
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CloseDialogIcon,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(InputFieldTrackColor)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = "Enter name...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.35f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )

                        if (textFieldValue.text.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .bounceClick(scaleDown = 0.90f) {
                                        textFieldValue = TextFieldValue("")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(MomoPrimaryGradient)
                        .bounceClick(scaleDown = 0.94f) {
                            val cleanName = textFieldValue.text.trim()
                            if (cleanName.isNotEmpty() && !isSaving) {
                                isSaving = true
                                PrefNameManager.updatePartnerDisplayName(context, cleanName) { success ->
                                    isSaving = false
                                    if (success) {
                                        onNameSaved(cleanName)
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSaving) "Saving..." else "Save Name",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
