package com.personal.momo.Cache

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

object CacheManager {

    private const val PREFS_NAME = "momo_local_cache"
    private const val KEY_AVATAR_URL = "cached_avatar_url"
    private const val KEY_PERIOD_DATES = "cached_period_dates"

    private var prefs: SharedPreferences? = null

    private val _avatarUrlFlow = MutableStateFlow<String?>(null)
    val avatarUrlFlow: StateFlow<String?> = _avatarUrlFlow.asStateFlow()

    private val _periodDatesFlow = MutableStateFlow<List<LocalDate>>(emptyList())
    val periodDatesFlow: StateFlow<List<LocalDate>> = _periodDatesFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // 1. Load cached avatar
            val cachedUrl = prefs?.getString(KEY_AVATAR_URL, null)
            _avatarUrlFlow.value = cachedUrl

            // 2. Load cached period dates
            val cachedDatesString = prefs?.getString(KEY_PERIOD_DATES, null)
            if (!cachedDatesString.isNullOrBlank()) {
                val parsedDates = cachedDatesString.split(",")
                    .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
                    .sorted()
                _periodDatesFlow.value = parsedDates
            }
        }
        syncFromFirestore()
    }

    fun syncFromFirestore() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()

            // 1. Sync Avatar Configuration
            try {
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
                        // Silent fail for offline resiliency
                    }
            } catch (e: Exception) {
                // Ignore network exceptions
            }

            // 2. Sync Entire ApyBday Collection (All Years)
            try {
                db.collection("ApyBday")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot != null && !querySnapshot.isEmpty) {
                            val zoneId = ZoneId.systemDefault()
                            val collectedDates = mutableSetOf<LocalDate>()

                            for (document in querySnapshot.documents) {
                                val rawTimestamps = document.get("peri_date") as? List<*>
                                rawTimestamps?.forEach { item ->
                                    if (item is Timestamp) {
                                        val localDate = item.toDate()
                                            .toInstant()
                                            .atZone(zoneId)
                                            .toLocalDate()
                                        collectedDates.add(localDate)
                                    }
                                }
                            }

                            val sortedDates = collectedDates.sorted()
                            if (sortedDates != _periodDatesFlow.value) {
                                savePeriodDates(sortedDates)
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Silent fail: Keeps existing cached dates during offline use
                    }
            } catch (e: Exception) {
                // Ignore network exceptions
            }
        }
    }

    private fun saveAvatarUrl(url: String) {
        prefs?.edit()?.putString(KEY_AVATAR_URL, url)?.apply()
        _avatarUrlFlow.value = url
    }

    private fun savePeriodDates(dates: List<LocalDate>) {
        val serializedString = dates.joinToString(",") { it.toString() }
        prefs?.edit()?.putString(KEY_PERIOD_DATES, serializedString)?.apply()
        _periodDatesFlow.value = dates
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
