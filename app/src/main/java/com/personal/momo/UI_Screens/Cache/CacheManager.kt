package com.personal.momo.Cache

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.personal.momo.UI_Screens.Calendar.MomoEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

object CacheManager {

    private const val PREFS_NAME = "momo_local_cache"
    private const val KEY_AVATAR_URL = "cached_avatar_url"
    private const val KEY_PERIOD_DATES = "cached_period_dates"
    private const val KEY_EVENTS = "cached_events"

    private var prefs: SharedPreferences? = null

    private val _avatarUrlFlow = MutableStateFlow<String?>(null)
    val avatarUrlFlow: StateFlow<String?> = _avatarUrlFlow.asStateFlow()

    private val _periodDatesFlow = MutableStateFlow<List<LocalDate>>(emptyList())
    val periodDatesFlow: StateFlow<List<LocalDate>> = _periodDatesFlow.asStateFlow()

    private val _eventsFlow = MutableStateFlow<List<MomoEvent>>(emptyList())
    val eventsFlow: StateFlow<List<MomoEvent>> = _eventsFlow.asStateFlow()

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

            // 3. Load cached events
            val cachedEventsString = prefs?.getString(KEY_EVENTS, null)
            if (!cachedEventsString.isNullOrBlank()) {
                try {
                    val jsonArray = JSONArray(cachedEventsString)
                    val list = ArrayList<MomoEvent>(jsonArray.length())
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            MomoEvent(
                                id = obj.optString("id"),
                                title = obj.optString("title"),
                                description = obj.optString("description"),
                                date = LocalDate.parse(obj.optString("date")),
                                epochMillis = obj.optLong("epochMillis"),
                                isSpecial = obj.optBoolean("isSpecial", false)
                            )
                        )
                    }
                    _eventsFlow.value = list.sortedBy { it.date }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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

            // 3. Sync Entire Events Collection (All Documents & Years)
            try {
                db.collection("Events")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot != null && !querySnapshot.isEmpty) {
                            val zoneId = ZoneId.systemDefault()
                            val collectedEvents = mutableListOf<MomoEvent>()

                            for (document in querySnapshot.documents) {
                                val docId = document.id
                                val data = document.data ?: continue

                                for ((key, value) in data) {
                                    val array = value as? List<*> ?: continue
                                    if (array.size >= 3) {
                                        val title = array.getOrNull(0) as? String ?: ""
                                        val description = array.getOrNull(1) as? String ?: ""
                                        val timestamp = array.getOrNull(2) as? Timestamp ?: continue
                                        val isSpecial = (array.getOrNull(3) as? Boolean) ?: false

                                        val localDate = timestamp.toDate()
                                            .toInstant()
                                            .atZone(zoneId)
                                            .toLocalDate()
                                        val epochMillis = timestamp.toDate().time

                                        collectedEvents.add(
                                            MomoEvent(
                                                id = "${docId}_$key",
                                                title = title,
                                                description = description,
                                                date = localDate,
                                                epochMillis = epochMillis,
                                                isSpecial = isSpecial
                                            )
                                        )
                                    }
                                }
                            }

                            val sortedEvents = collectedEvents.sortedBy { it.date }
                            if (sortedEvents != _eventsFlow.value) {
                                saveEvents(sortedEvents)
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Silent fail for offline resiliency
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

    private fun saveEvents(events: List<MomoEvent>) {
        try {
            val jsonArray = JSONArray()
            for (event in events) {
                val obj = JSONObject()
                obj.put("id", event.id)
                obj.put("title", event.title)
                obj.put("description", event.description)
                obj.put("date", event.date.toString())
                obj.put("epochMillis", event.epochMillis)
                obj.put("isSpecial", event.isSpecial)
                jsonArray.put(obj)
            }
            prefs?.edit()?.putString(KEY_EVENTS, jsonArray.toString())?.apply()
            _eventsFlow.value = events
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
