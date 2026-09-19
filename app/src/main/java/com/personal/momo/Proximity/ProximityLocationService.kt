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
import kotlin.coroutines.resume

class ProximityLocationService : Service() {

    private enum class TrackingMode {
        SAFE_ZONE_SLEEP,
        FAR_RANGE_PERIODIC,
        APPROACH_CONTINUOUS
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var firestoreListener: ListenerRegistration? = null

    private var mySafeZone: GeoPoint? = null
    private var partnerSafeZone: GeoPoint? = null
    private var safeZoneRadiusMeters: Double = 100.0

    private var partnerLastLocation: GeoPoint? = null
    private var partnerLastAccuracy: Float = 0.0f

    private var myLastLocation: Location? = null
    private var currentTrackingMode: TrackingMode? = null
    private var currentTrackingIntervalMillis: Long = 0L
    private var periodicLoopJob: Job? = null
    private var isContinuousUpdatesActive = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return

            if (location.accuracy > 200.0f) {
                return
            }

            myLastLocation = location
            uploadMyLocationToFirestore(location)
            evaluateProximityStateMachine()
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundServiceNotification()

        ensureAuth {
            startFirestoreSync()
            serviceScope.launch {
                fetchAndProcessSingleLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Momo Proximity Active")
            .setContentText("Safe zone and relative distance tracking is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
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

            val currentUserId = CacheManager.getAppUserId(this)
            val isKanu = currentUserId.equals("Kanu", ignoreCase = true)

            // 1. Sync Safe Zones
            val safeZonesMap = snapshot.get("safe_zones") as? Map<*, *>
            if (safeZonesMap != null) {
                val kanuHomeGeo = safeZonesMap["kanu"] as? GeoPoint
                val momoHomeGeo = safeZonesMap["momo"] as? GeoPoint
                val radiusValue = (safeZonesMap["radius"] as? Number)?.toDouble() ?: 100.0

                safeZoneRadiusMeters = radiusValue
                if (isKanu) {
                    mySafeZone = kanuHomeGeo
                    partnerSafeZone = momoHomeGeo
                } else {
                    mySafeZone = momoHomeGeo
                    partnerSafeZone = kanuHomeGeo
                }
            }

            // 2. Sync Partner's Dynamic Location
            val locationMap = snapshot.get("location") as? Map<*, *>
            if (locationMap != null) {
                val partnerKey = if (isKanu) "momo" else "kanu"
                val partnerData = locationMap[partnerKey] as? List<*>
                if (partnerData != null && partnerData.size >= 2) {
                    val partnerGeo = partnerData[0] as? GeoPoint
                    val partnerAcc = (partnerData[1] as? Number)?.toFloat() ?: 0.0f

                    if (partnerGeo != null && partnerAcc <= 200.0f) {
                        partnerLastLocation = partnerGeo
                        partnerLastAccuracy = partnerAcc
                        evaluateProximityStateMachine()
                    }
                }
            }
        }
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun evaluateProximityStateMachine() {
        val myLoc = myLastLocation
        val partnerGeo = partnerLastLocation
        val myHome = mySafeZone

        if (myLoc == null) {
            serviceScope.launch {
                fetchAndProcessSingleLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            }
            return
        }

        // Rule 1: Independent Safe Zone Check (<= 100m)
        val isMeInHome = if (myHome != null) {
            val myDistanceToMyHome = ProximityMath.calculateDistanceMeters(
                myLoc.latitude, myLoc.longitude,
                myHome.latitude, myHome.longitude
            )
            myDistanceToMyHome <= safeZoneRadiusMeters
        } else {
            false
        }

        val currentUserId = CacheManager.getAppUserId(this)
        val partnerName = if (currentUserId.equals("Kanu", ignoreCase = true)) "Momo" else "Kanu"

        if (isMeInHome) {
            // Phone remains in Deep Sleep (Zero persistent GPS chip usage, zero status bar icon)
            setTrackingMode(TrackingMode.SAFE_ZONE_SLEEP, 10 * 60 * 1000L)

            // Even if I am at home, evaluate partner's proximity if partner is approaching
            if (partnerGeo != null) {
                val relativeDistance = ProximityMath.calculateDistanceMeters(
                    myLoc.latitude, myLoc.longitude,
                    partnerGeo.latitude, partnerGeo.longitude
                )

                // 200m Proximity Alert Trigger
                ProximityNotificationHelper.evaluateAlert(this, partnerName, relativeDistance)

                // <= 50m Close Encounter BLE Trigger
                if (relativeDistance <= 50.0) {
                    BleProximityManager.startHandshake(this) { rssi ->
                        // RSSI captured for radar phase
                    }
                } else {
                    BleProximityManager.stopHandshake()
                }
            } else {
                BleProximityManager.stopHandshake()
            }
            return
        }

        // Rule 2: Inter-User Distance Tracking (When I am Outside Safe Zone)
        if (partnerGeo != null) {
            val relativeDistance = ProximityMath.calculateDistanceMeters(
                myLoc.latitude, myLoc.longitude,
                partnerGeo.latitude, partnerGeo.longitude
            )

            // 200m Proximity Notification Alert (with debounce/hysteresis)
            ProximityNotificationHelper.evaluateAlert(this, partnerName, relativeDistance)

            when {
                relativeDistance <= 50.0 -> {
                    // <= 50m: Continuous tracking + BLE Handshake
                    setTrackingMode(TrackingMode.APPROACH_CONTINUOUS, 15_000L)
                    BleProximityManager.startHandshake(this) { rssi ->
                        // RSSI captured for radar phase
                    }
                }
                relativeDistance < 150.0 -> {
                    // 50m to < 150m: Continuous tracking, BLE stopped
                    BleProximityManager.stopHandshake()
                    setTrackingMode(TrackingMode.APPROACH_CONTINUOUS, 15_000L)
                }
                relativeDistance <= 200.0 -> {
                    // 150m to 200m: 1 minute periodic ping
                    BleProximityManager.stopHandshake()
                    setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 1 * 60 * 1000L)
                }
                relativeDistance <= 500.0 -> {
                    // 200m to 500m: 5 minute periodic ping
                    BleProximityManager.stopHandshake()
                    setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 5 * 60 * 1000L)
                }
                else -> {
                    // > 500m: 10 minute periodic ping
                    BleProximityManager.stopHandshake()
                    setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 10 * 60 * 1000L)
                }
            }
        } else {
            // Fallback when partner coordinates not yet loaded
            BleProximityManager.stopHandshake()
            setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 10 * 60 * 1000L)
        }
    }

    private fun setTrackingMode(mode: TrackingMode, intervalMillis: Long) {
        if (currentTrackingMode == mode && currentTrackingIntervalMillis == intervalMillis) return
        currentTrackingMode = mode
        currentTrackingIntervalMillis = intervalMillis

        when (mode) {
            TrackingMode.SAFE_ZONE_SLEEP, TrackingMode.FAR_RANGE_PERIODIC -> {
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
        val location = fetchSingleLocation(priority) ?: return

        if (location.accuracy > 200.0f) {
            return
        }

        myLastLocation = location
        uploadMyLocationToFirestore(location)
        evaluateProximityStateMachine()
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
    }
}
