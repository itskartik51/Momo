package com.personal.momo.Proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.util.UUID

object BleProximityManager {

    private val SERVICE_UUID: ParcelUuid = ParcelUuid(UUID.fromString("7f3b8c20-8e1b-4f9e-9d9e-5b7d6a4f2e1a"))

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null

    private var isAdvertising = false
    private var isScanning = false

    private var onRssiReceived: ((Int) -> Unit)? = null

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                onRssiReceived?.invoke(it.rssi)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
        }
    }

    fun startHandshake(context: Context, onRssiUpdate: (Int) -> Unit) {
        if (!hasBlePermissions(context)) return

        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return
        bluetoothAdapter = manager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) return

        onRssiReceived = onRssiUpdate

        startAdvertising()
        startScanning()
    }

    fun stopHandshake() {
        stopAdvertising()
        stopScanning()
        onRssiReceived = null
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        if (isAdvertising) return
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(SERVICE_UUID)
            .setIncludeDeviceName(false)
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        if (!isAdvertising) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isAdvertising = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        if (isScanning) return
        scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        val filter = ScanFilter.Builder()
            .setServiceUuid(SERVICE_UUID)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
            isScanning = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        if (!isScanning) return
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isScanning = false
        }
    }

    private fun hasBlePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }
}
