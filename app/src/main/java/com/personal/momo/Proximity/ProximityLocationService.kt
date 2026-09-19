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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.personal.momo.Cache.CacheManager
import com.personal.momo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ProximityLocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var firestoreListener: ListenerRegistration? = null

    private var mySafeZone: GeoPoint? = null
    private var partnerSafeZone: GeoPoint? = null
    private var safeZoneRadiusMeters: Double = 100.0

    private var partnerLastLocation: GeoPoint? = null
    private var partnerLastAccuracy: Float = 0.0f

    private var myLastLocation: Location? = null
    private var currentActivePriority: Int? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return

            // Step 1: Ghost Exit Filter check
            if (!ProximityMath.isValidAccuracy(location.accuracy)) {
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
        startFirestoreSync()
        requestLocationUpdates(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 600_000L) // 10 mins passive initial
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        firestoreListener?.remove()
        BleProximityManager.stopHandshake()
        serviceScope.cancel()
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

                    if (partnerGeo != null && ProximityMath.isValidAccuracy(partnerAcc)) {
                        partnerLastLocation = partnerGeo
                        partnerLastAccuracy = partnerAcc
                        evaluateProximityStateMachine()
                    }
                }
            }
        }
    }

    private fun uploadMyLocationToFirestore(location: Location) {
        serviceScope.launch {
            try {
                val currentUserId = CacheManager.getAppUserId(this@ProximityLocationService)
                val userKey = if (currentUserId.equals("Kanu", ignoreCase = true)) "kanu" else "momo"
                val geoPoint = GeoPoint(location.latitude, location.longitude)

                val payload = listOf(
                    geoPoint,
                    location.accuracy.toInt(),
                    FieldValue.serverTimestamp()
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

    private fun evaluateProximityStateMachine() {
        val myLoc = myLastLocation ?: return
        val partnerGeo = partnerLastLocation

        // Rule 1: Dual Safe Zone Check (Sleep Mode)
        val myHome = mySafeZone
        val partnerHome = partnerSafeZone

        if (myHome != null && partnerHome != null && partnerGeo != null) {
            val myDistanceToMyHome = ProximityMath.calculateDistanceMeters(
                myLoc.latitude, myLoc.longitude,
                myHome.latitude, myHome.longitude
            )
            val partnerDistanceToPartnerHome = ProximityMath.calculateDistanceMeters(
                partnerGeo.latitude, partnerGeo.longitude,
                partnerHome.latitude, partnerHome.longitude
            )

            if (myDistanceToMyHome <= safeZoneRadiusMeters && partnerDistanceToPartnerHome <= safeZoneRadiusMeters) {
                // Both are inside home safe zones -> Zero battery drain passive mode
                BleProximityManager.stopHandshake()
                requestLocationUpdates(Priority.PRIORITY_PASSIVE, 900_000L) // 15 mins
                return
            }
        }

        // Rule 2: Inter-User Distance Tracking
        if (partnerGeo != null) {
            val relativeDistance = ProximityMath.calculateDistanceMeters(
                myLoc.latitude, myLoc.longitude,
                partnerGeo.latitude, partnerGeo.longitude
            )

            val currentUserId = CacheManager.getAppUserId(this)
            val partnerName = if (currentUserId.equals("Kanu", ignoreCase = true)) "Momo" else "Kanu"

            // Trigger 200m Notification Alert (with debounce/hysteresis)
            ProximityNotificationHelper.evaluateAlert(this, partnerName, relativeDistance)

            when {
                relativeDistance <= 50.0 -> {
                    // <= 50m: High accuracy GPS + Start BLE Handshake
                    requestLocationUpdates(Priority.PRIORITY_HIGH_ACCURACY, 15_000L)
                    BleProximityManager.startHandshake(this) { rssi ->
                        // RSSI captured for radar phase
                    }
                }
                relativeDistance <= 200.0 -> {
                    // 50m -> 200m: High accuracy GPS for exact alert, BLE terminated
                    BleProximityManager.stopHandshake()
                    requestLocationUpdates(Priority.PRIORITY_HIGH_ACCURACY, 15_000L)
                }
                relativeDistance <= 500.0 -> {
                    // 200m -> 500m: Transition phase to high accuracy
                    BleProximityManager.stopHandshake()
                    requestLocationUpdates(Priority.PRIORITY_HIGH_ACCURACY, 20_000L)
                }
                else -> {
                    // > 500m: Low-power balanced network mode
                    BleProximityManager.stopHandshake()
                    requestLocationUpdates(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 600_000L)
                }
            }
        } else {
            // Fallback when partner coordinates not yet available
            BleProximityManager.stopHandshake()
            requestLocationUpdates(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 600_000L)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(priority: Int, intervalMillis: Long) {
        if (currentActivePriority == priority) return

        val fineLoc = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLoc != PackageManager.PERMISSION_GRANTED) {
            return
        }

        currentActivePriority = priority
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val locationRequest = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
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
