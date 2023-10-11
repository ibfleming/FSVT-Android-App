package me.example.fsvt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import android.util.Log

@SuppressLint("MissingPermission")
class BLEScanner(private val bluetoothAdapter: BluetoothAdapter)
{

    private val tag = "BLE Scanner"
    private val handler = Handler(Looper.getMainLooper())
    private var scanCallback: ScanCallback? = null

    fun startScan(callback: (List<ScanResult>) -> Unit ) {
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                callback(listOf(result))
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                if (results != null) {
                    callback(results)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "Scan failed with error code $errorCode")
            }
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        handler.postDelayed({
            stopScan()
        }, SCAN_PERIOD)

        bluetoothAdapter.bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

    fun stopScan() {
        scanCallback?.let {
            bluetoothAdapter.bluetoothLeScanner.stopScan(it)
            scanCallback = null
        }
    }

    companion object {
        private const val SCAN_PERIOD: Long = 5000 // 5sec
    }
}
