package com.personal.momo.Proximity

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.personal.momo.Cache.CacheManager
import com.personal.momo.R
import com.personal.momo.SecurityConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.roundToInt

class ProximityLocationService : Service() {

    private enum class TrackingMode {
        FAR_RANGE_PERIODIC,
        APPROACH_CONTINUOUS
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var firestoreListener: ListenerRegistration? = null

    private var rawLocationMap: Map<*, *>? = null

    private var partnerLastLocation: GeoPoint? = null
    private var partnerLastAccuracy: Float = 0.0f
    private var partnerLastTimestamp: Timestamp? = null

    private var myLastLocation: Location? = null
    private var currentTrackingMode: TrackingMode? = null
    private var currentTrackingIntervalMillis: Long = 0L
    private var periodicLoopJob: Job? = null
    private var isContinuousUpdatesActive = false

    private var lastFirestoreUploadTime: Long = 0L
    private var isManualLiveActive: Boolean = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleLocationUpdate(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundServiceNotification()

        CacheManager.init(this)

        serviceScope.launch {
            CacheManager.appUserIdFlow.collect { userId ->
                updateUserIdentity(userId)
                lastFirestoreUploadTime = 0L
                fetchAndProcessSingleLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            }
        }

        ensureAuth {
            startFirestoreSync()
            serviceScope.launch {
                fetchAndProcessSingleLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LIVE -> {
                isManualLiveActive = true
                setTrackingMode(TrackingMode.APPROACH_CONTINUOUS, 10_000L)
                serviceScope.launch {
                    fetchAndProcessSingleLocation(Priority.PRIORITY_HIGH_ACCURACY)
                }
            }
            ACTION_STOP_LIVE -> {
                isManualLiveActive = false
                evaluateProximityStateMachine()
            }
            ACTION_FORCE_SYNC -> {
                serviceScope.launch {
                    fetchAndProcessSingleLocation(Priority.PRIORITY_HIGH_ACCURACY)
                }
            }
            else -> {
                val currentUserId = CacheManager.getAppUserId(this)
                updateUserIdentity(currentUserId)
                lastFirestoreUploadTime = 0L
                serviceScope.launch {
                    fetchAndProcessSingleLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopContinuousLocationUpdates()
        periodicLoopJob?.cancel()
        firestoreListener?.remove()
        BleProximityManager.stopHandshake()
        serviceScope.cancel()
    }

    private fun updateUserIdentity(userId: String) {
        val isKanu = userId.equals("Kanu", ignoreCase = true)

        rawLocationMap?.let { locationMap ->
            val partnerKey = if (isKanu) "momo" else "kanu"
            val partnerData = locationMap[partnerKey] as? List<*>
            if (partnerData != null && partnerData.size >= 3) {
                val partnerGeo = partnerData[0] as? GeoPoint
                val partnerAcc = (partnerData[1] as? Number)?.toFloat() ?: 0.0f
                val partnerTime = partnerData[2] as? Timestamp

                if (partnerGeo != null && partnerAcc <= 200.0f && partnerTime != null) {
                    partnerLastLocation = partnerGeo
                    partnerLastAccuracy = partnerAcc
                    partnerLastTimestamp = partnerTime
                }
            }
        }
    }

    private fun ensureAuth(onReady: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onReady()
        } else {
            if (SecurityConfig.AUTH_EMAIL.isNotBlank() && SecurityConfig.AUTH_PASS.isNotBlank()) {
                auth.signInWithEmailAndPassword(SecurityConfig.AUTH_EMAIL, SecurityConfig.AUTH_PASS)
                    .addOnSuccessListener {
                        onReady()
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        onReady()
                    }
            } else {
                onReady()
            }
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "momo_proximity_silent_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Proximity Detection Engine",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitors partner proximity in background with minimal battery impact"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Momo Proximity Active")
            .setContentText("Relative distance tracking is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            startForeground(
                NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startFirestoreSync() {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("App").document("home_config")

        firestoreListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                return@addSnapshotListener
            }

            rawLocationMap = snapshot.get("location") as? Map<*, *>

            val currentUserId = CacheManager.getAppUserId(this)
            updateUserIdentity(currentUserId)
            evaluateProximityStateMachine()
        }
    }

    private fun isPartnerDataFresh(): Boolean {
        val timestamp = partnerLastTimestamp ?: return false
        val ageMillis = System.currentTimeMillis() - timestamp.toDate().time
        return ageMillis in -30_000L..(45 * 60 * 1000L)
    }

    private fun handleLocationUpdate(location: Location) {
        if (location.accuracy > 200.0f) {
            handleLocationFetchFailure()
            return
        }

        myLastLocation = location
        uploadMyLocationToFirestore(location)
        evaluateProximityStateMachine()
    }

    private fun handleLocationFetchFailure() {
        evaluateProximityStateMachine()
    }

    private fun uploadMyLocationToFirestore(location: Location) {
        ensureAuth {
            serviceScope.launch {
                try {
                    val currentUserId = CacheManager.getAppUserId(this@ProximityLocationService)
                    val userKey = if (currentUserId.equals("Kanu", ignoreCase = true)) "kanu" else "momo"
                    val geoPoint = GeoPoint(location.latitude, location.longitude)

                    val payload = listOf(
                        geoPoint,
                        location.accuracy.toInt(),
                        Timestamp.now()
                    )

                    FirebaseFirestore.getInstance()
                        .collection("App")
                        .document("home_config")
                        .update("location.$userKey", payload)
                        .addOnSuccessListener {
                            lastFirestoreUploadTime = System.currentTimeMillis()
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun pushTrackerStateToCache(relativeDistance: Double?) {
        val currentUserId = CacheManager.getAppUserId(this)
        val isKanu = currentUserId.equals("Kanu", ignoreCase = true)
        val partnerName = if (isKanu) "Momo" else "Kanu"
        val selfName = if (isKanu) "Kanu" else "Momo"

        val partnerInfo = CacheManager.UserLocationInfo(
            name = partnerName,
            latitude = partnerLastLocation?.latitude,
            longitude = partnerLastLocation?.longitude,
            accuracy = partnerLastAccuracy.takeIf { it > 0f },
            timestamp = partnerLastTimestamp?.toDate()?.time
        )

        val myLoc = myLastLocation
        val selfInfo = CacheManager.UserLocationInfo(
            name = selfName,
            latitude = myLoc?.latitude,
            longitude = myLoc?.longitude,
            accuracy = myLoc?.accuracy?.takeIf { it > 0f },
            timestamp = myLoc?.time ?: System.currentTimeMillis()
        )

        val distanceInt = relativeDistance?.roundToInt()

        CacheManager.updateTrackerState(
            CacheManager.TrackerState(
                partnerInfo = partnerInfo,
                selfInfo = selfInfo,
                distanceMeters = distanceInt
            )
        )
    }

    private fun evaluateProximityStateMachine() {
        val myLoc = myLastLocation
        val partnerGeo = if (isPartnerDataFresh()) partnerLastLocation else null

        val currentRelativeDistance = if (myLoc != null && partnerLastLocation != null) {
            ProximityMath.calculateDistanceMeters(
                myLoc.latitude, myLoc.longitude,
                partnerLastLocation!!.latitude, partnerLastLocation!!.longitude
            )
        } else {
            null
        }

        pushTrackerStateToCache(currentRelativeDistance)

        if (myLoc == null) {
            serviceScope.launch {
                fetchAndProcessSingleLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            }
            return
        }

        val currentUserId = CacheManager.getAppUserId(this)
        val partnerName = if (currentUserId.equals("Kanu", ignoreCase = true)) "Momo" else "Kanu"

        // Proximity notifications & BLE handshake evaluation
        if (partnerGeo != null && currentRelativeDistance != null) {
            ProximityNotificationHelper.evaluateAlert(this, partnerName, currentRelativeDistance)

            if (currentRelativeDistance <= 50.0) {
                BleProximityManager.startHandshake(this) { rssi -> }
            } else {
                BleProximityManager.stopHandshake()
            }
        } else {
            BleProximityManager.stopHandshake()
            ProximityNotificationHelper.dismiss(this)
        }

        // Live Mode Override: Stays 10s Continuous while Tracker / Finder Screen is active
        if (isManualLiveActive) {
            setTrackingMode(TrackingMode.APPROACH_CONTINUOUS, 10_000L)
            return
        }

        // Pure Relative Distance Engine
        if (partnerGeo != null && currentRelativeDistance != null) {
            when {
                currentRelativeDistance <= 150.0 -> {
                    setTrackingMode(TrackingMode.APPROACH_CONTINUOUS, 10_000L)
                }
                currentRelativeDistance <= 200.0 -> {
                    setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 30_000L)
                }
                currentRelativeDistance <= 500.0 -> {
                    setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 5 * 60 * 1000L)
                }
                else -> {
                    setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 10 * 60 * 1000L)
                }
            }
        } else {
            setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 10 * 60 * 1000L)
        }
    }

    private fun setTrackingMode(mode: TrackingMode, intervalMillis: Long) {
        if (currentTrackingMode == mode && currentTrackingIntervalMillis == intervalMillis) return
        currentTrackingMode = mode
        currentTrackingIntervalMillis = intervalMillis

        when (mode) {
            TrackingMode.FAR_RANGE_PERIODIC -> {
                stopContinuousLocationUpdates()
                periodicLoopJob?.cancel()
                periodicLoopJob = serviceScope.launch {
                    while (isActive) {
                        delay(intervalMillis)
                        fetchAndProcessSingleLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    }
                }
            }
            TrackingMode.APPROACH_CONTINUOUS -> {
                periodicLoopJob?.cancel()
                periodicLoopJob = null
                startContinuousLocationUpdates(intervalMillis)
            }
        }
    }

    private suspend fun fetchAndProcessSingleLocation(priority: Int) {
        val location = withTimeoutOrNull(15_000L) {
            fetchSingleLocation(priority)
        }

        if (location != null && location.accuracy <= 200.0f) {
            handleLocationUpdate(location)
        } else {
            handleLocationFetchFailure()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchSingleLocation(priority: Int): Location? {
        val fineLoc = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLoc = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineLoc != PackageManager.PERMISSION_GRANTED && coarseLoc != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        if (continuation.isActive) continuation.resume(loc)
                    } else {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) continuation.resume(lastLoc)
                            }
                            .addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                    }
                }
                .addOnFailureListener {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLoc ->
                            if (continuation.isActive) continuation.resume(lastLoc)
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                }
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startContinuousLocationUpdates(intervalMillis: Long) {
        val fineLoc = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLoc != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.removeLocationUpdates(locationCallback)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        isContinuousUpdatesActive = true
    }

    private fun stopContinuousLocationUpdates() {
        if (isContinuousUpdatesActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isContinuousUpdatesActive = false
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 8001
        const val ACTION_START_LIVE = "com.personal.momo.action.START_LIVE"
        const val ACTION_STOP_LIVE = "com.personal.momo.action.STOP_LIVE"
        const val ACTION_FORCE_SYNC = "com.personal.momo.action.FORCE_SYNC"

        fun startService(context: Context) {
            val intent = Intent(context, ProximityLocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ProximityLocationService::class.java)
            context.stopService(intent)
        }

        fun startLive(context: Context) {
            val intent = Intent(context, ProximityLocationService::class.java).apply {
                action = ACTION_START_LIVE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopLive(context: Context) {
            val intent = Intent(context, ProximityLocationService::class.java).apply {
                action = ACTION_STOP_LIVE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun forceSync(context: Context) {
            val intent = Intent(context, ProximityLocationService::class.java).apply {
                action = ACTION_FORCE_SYNC
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
