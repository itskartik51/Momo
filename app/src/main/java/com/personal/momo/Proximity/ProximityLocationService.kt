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

class ProximityLocationService : Service() {

    private enum class TrackingMode {
        SAFE_ZONE_SLEEP,
        FAR_RANGE_PERIODIC,
        APPROACH_CONTINUOUS
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var firestoreListener: ListenerRegistration? = null

    private var rawSafeZonesMap: Map<*, *>? = null
    private var rawLocationMap: Map<*, *>? = null

    private var mySafeZone: GeoPoint? = null
    private var partnerSafeZone: GeoPoint? = null
    private var safeZoneRadiusMeters: Double = 100.0

    private var partnerLastLocation: GeoPoint? = null
    private var partnerLastAccuracy: Float = 0.0f
    private var partnerLastTimestamp: Timestamp? = null

    private var myLastLocation: Location? = null
    private var currentTrackingMode: TrackingMode? = null
    private var currentTrackingIntervalMillis: Long = 0L
    private var periodicLoopJob: Job? = null
    private var isContinuousUpdatesActive = false

    private var lastFirestoreUploadTime: Long = 0L

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
        val currentUserId = CacheManager.getAppUserId(this)
        updateUserIdentity(currentUserId)
        lastFirestoreUploadTime = 0L
        serviceScope.launch {
            fetchAndProcessSingleLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
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

        rawSafeZonesMap?.let { safeZonesMap ->
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
            .setContentText("Safe zone and relative distance tracking is running")
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

            rawSafeZonesMap = snapshot.get("safe_zones") as? Map<*, *>
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

        val myHome = mySafeZone
        val isMeInHome = if (myHome != null) {
            val myDistanceToMyHome = ProximityMath.calculateDistanceMeters(
                location.latitude, location.longitude,
                myHome.latitude, myHome.longitude
            )
            myDistanceToMyHome <= safeZoneRadiusMeters
        } else {
            false
        }

        val currentTime = System.currentTimeMillis()
        if (isMeInHome) {
            if (currentTime - lastFirestoreUploadTime >= 30 * 60 * 1000L) {
                uploadMyLocationToFirestore(location)
            }
        } else {
            uploadMyLocationToFirestore(location)
        }

        evaluateProximityStateMachine()
    }

    private fun handleLocationFetchFailure() {
        val myHome = mySafeZone
        val lastLoc = myLastLocation

        val isLikelyInHome = if (myHome != null && lastLoc != null) {
            val distance = ProximityMath.calculateDistanceMeters(
                lastLoc.latitude, lastLoc.longitude,
                myHome.latitude, myHome.longitude
            )
            distance <= safeZoneRadiusMeters
        } else if (myHome != null && (currentTrackingMode == TrackingMode.SAFE_ZONE_SLEEP || currentTrackingMode == null)) {
            true
        } else {
            false
        }

        if (isLikelyInHome && myHome != null) {
            val fallbackLocation = Location("SafeZoneFallback").apply {
                latitude = myHome.latitude
                longitude = myHome.longitude
                accuracy = 50.0f
            }
            myLastLocation = fallbackLocation

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFirestoreUploadTime >= 30 * 60 * 1000L) {
                uploadMyLocationToFirestore(fallbackLocation)
            }
            evaluateProximityStateMachine()
        } else {
            evaluateProximityStateMachine()
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

    private fun evaluateProximityStateMachine() {
        val myLoc = myLastLocation
        val partnerGeo = if (isPartnerDataFresh()) partnerLastLocation else null
        val myHome = mySafeZone

        if (myLoc == null) {
            serviceScope.launch {
                fetchAndProcessSingleLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            }
            return
        }

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
            setTrackingMode(TrackingMode.SAFE_ZONE_SLEEP, 10 * 60 * 1000L)

            if (partnerGeo != null) {
                val relativeDistance = ProximityMath.calculateDistanceMeters(
                    myLoc.latitude, myLoc.longitude,
                    partnerGeo.latitude, partnerGeo.longitude
                )

                ProximityNotificationHelper.evaluateAlert(this, partnerName, relativeDistance)

                if (relativeDistance <= 50.0) {
                    BleProximityManager.startHandshake(this) { rssi ->
                    }
                } else {
                    BleProximityManager.stopHandshake()
                }
            } else {
                BleProximityManager.stopHandshake()
            }
            return
        }

        if (partnerGeo != null) {
            val relativeDistance = ProximityMath.calculateDistanceMeters(
                myLoc.latitude, myLoc.longitude,
                partnerGeo.latitude, partnerGeo.longitude
            )

            ProximityNotificationHelper.evaluateAlert(this, partnerName, relativeDistance)

            when {
                relativeDistance <= 50.0 -> {
                    setTrackingMode(TrackingMode.APPROACH_CONTINUOUS, 15_000L)
                    BleProximityManager.startHandshake(this) { rssi ->
                    }
                }
                relativeDistance < 150.0 -> {
                    BleProximityManager.stopHandshake()
                    setTrackingMode(TrackingMode.APPROACH_CONTINUOUS, 15_000L)
                }
                relativeDistance <= 200.0 -> {
                    BleProximityManager.stopHandshake()
                    setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 1 * 60 * 1000L)
                }
                relativeDistance <= 500.0 -> {
                    BleProximityManager.stopHandshake()
                    setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 5 * 60 * 1000L)
                }
                else -> {
                    BleProximityManager.stopHandshake()
                    setTrackingMode(TrackingMode.FAR_RANGE_PERIODIC, 10 * 60 * 1000L)
                }
            }
        } else {
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
