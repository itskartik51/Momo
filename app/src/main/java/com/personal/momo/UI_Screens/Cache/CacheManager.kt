package com.personal.momo.Cache

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object CacheManager {

    private const val PREFS_NAME = "momo_local_cache"
    private const val KEY_AVATAR_URL = "cached_avatar_url"

    private var prefs: SharedPreferences? = null

    private val _avatarUrlFlow = MutableStateFlow<String?>(null)
    val avatarUrlFlow: StateFlow<String?> = _avatarUrlFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedUrl = prefs?.getString(KEY_AVATAR_URL, null)
            _avatarUrlFlow.value = cachedUrl
        }
        syncFromFirestore()
    }

    fun syncFromFirestore() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("App")
                    .document("home_config")
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val rawUrl = document.getString("ic_avt")
                            if (!rawUrl.isNullOrBlank()) {
                                val directUrl = resolveDriveUrl(rawUrl)
                                if (directUrl != _avatarUrlFlow.value) {
                                    saveAvatarUrl(directUrl)
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Silent fail: Keeps existing cached image during offline use
                    }
            } catch (e: Exception) {
                // Ignore runtime network exceptions
            }
        }
    }

    private fun saveAvatarUrl(url: String) {
        prefs?.edit()?.putString(KEY_AVATAR_URL, url)?.apply()
        _avatarUrlFlow.value = url
    }

    private fun resolveDriveUrl(url: String): String {
        val driveRegex = Regex("drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)")
        val match = driveRegex.find(url)
        return if (match != null) {
            val fileId = match.groupValues[1]
            "https://lh3.googleusercontent.com/d/$fileId"
        } else {
            url.trim()
        }
    }
}
