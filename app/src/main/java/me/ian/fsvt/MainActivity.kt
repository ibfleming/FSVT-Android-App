package me.ian.fsvt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.ian.fsvt.bluetooth.ConnectionManager
import me.ian.fsvt.databinding.ActivityMainBinding
import org.jetbrains.anko.alert
import timber.log.Timber

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val MAC = "B4:52:A9:04:28:DC"
private const val SCAN_PERIOD = 3000L

class MainActivity : AppCompatActivity() {

    private var tag = "MainActivity"

    /*******************************************
     * Properties
     *******************************************/

    private lateinit var binding: ActivityMainBinding

    private var _isRunning   : Boolean = false
    private var _isConnected : Boolean = false
    private var _isScanning  : Boolean = false

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanFilter = ScanFilter.Builder().setDeviceAddress(MAC).build()
    private val scanResults = mutableListOf<ScanResult>()

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    /*******************************************
     * Activity function overrides
     *******************************************/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Timber.tag(tag).d("MainActivity Initialized!")

        /*******************************************
         * TESTING: Run Graph Fragment
         *******************************************/
        supportFragmentManager.beginTransaction().replace(R.id.Graph_Container, GraphFragment()).addToBackStack(null).commit()
        val graphFragment = supportFragmentManager.findFragmentById(R.id.Graph_Container) as? GraphFragment

        /*******************************************
         * Observe Connection State of Device
         *******************************************/

        ConnectionManager.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                binding.ConnectButton.setText(R.string.Connected_Button)
                _isConnected = true
            }
            else {
                binding.ConnectButton.setText(R.string.Connect_Button)
                _isConnected = false
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "The device disconnected unexpectedly from the app. " +
                                  "Please connect to the device again."
                        isCancelable = false
                        positiveButton(android.R.string.ok) { /* nop */ }
                    }.show()
                }
            }
        }

        /*******************************************
         * Observe Live Probe Data
         *******************************************/

        ConnectionManager.probe1Data.observe(this) { data ->
            if( data != -1F ) {
                binding.Probe1Data.text = data.toInt().toString()
            }
        }

        ConnectionManager.probe2Data.observe(this) { data ->
            if( data != -1F ) {
                binding.Probe2Data.text =  data.toInt().toString()
            }
        }

        /*******************************************
         * Button Click Listeners
         *******************************************/

        binding.ConnectButton.setOnClickListener { startScan() }

        binding.TestButton.setOnClickListener {
            if (ConnectionManager.isConnected.value == true) {
                if (_isRunning) {
                    ConnectionManager.sendStopCommand()
                    binding.TestButton.setText(R.string.Start_Program)
                    _isRunning = false
                    Timber.tag(tag).d("Sending Stop Command!")
                    graphFragment?.resetGraphData()
                } else {
                    ConnectionManager.sendStartCommand()
                    binding.TestButton.setText(R.string.Stop_Program)
                    _isRunning = true
                    Timber.tag(tag).d("Sending Start Command!")
                }
            }
        }

        binding.ResetButton.setOnClickListener {
            if( _isConnected && !_isRunning ) {
                Timber.d("SUCCESS")
            }
        }

        /*binding.DisconnectButton.setOnClickListener {
            if (ConnectionManager.isConnected.value == true) {
                ConnectionManager.disconnect()
            }
        }*/
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startScan()
                }
            }
        }
    }

    /*******************************************
     * Private functions
     *******************************************/

    @SuppressLint("MissingPermission")
    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            alert {
                title = "Location permission required"
                message = "Starting from Android M (6.0), the system requires apps to be granted " +
                        "location access in order to scan for BLE devices."
                isCancelable = false
                positiveButton(android.R.string.ok) {
                    requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }.show()
        }
    }

    private var scanTimeoutHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    private fun startScan() {
        Timber.tag(tag).d("Started BLE scan!")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        } else {
            scanResults.clear()
            val scanFilters = mutableListOf(scanFilter)
            bleScanner.startScan(scanFilters, scanSettings, scanCallback)
            _isScanning = true
            binding.ConnectButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            //binding.ConnectButton.setText(R.string.Scanning_Button)

            scanTimeoutHandler.postDelayed({
                stopScan();
                showDeviceNotFoundAlert()
                binding.ConnectButton.setText(R.string.Connect_Button)
            }, SCAN_PERIOD)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (_isScanning) {
            Timber.tag(tag).d("Stopped BLE scan!")
            bleScanner.stopScan(scanCallback)
            _isScanning = false
            scanTimeoutHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun showDeviceNotFoundAlert() {
        runOnUiThread {
            alert {
                title = "No device found"
                message = "Could not find the specified device. " +
                        "Please ensure the devices are powered on and try again."
                isCancelable = false
                positiveButton(android.R.string.ok) { /* nop */ }
            }.show()
        }
    }

    /*******************************************
     * Callback bodies
     *******************************************/

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { scanResult ->
                val device = scanResult.device
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address

                Timber.tag(tag).d("Found the device: $deviceName ($deviceAddress)")
                stopScan()
                ConnectionManager.connect(device, this@MainActivity)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.tag(tag).e("Scan Failed! CODE: $errorCode")
            runOnUiThread {
                alert {
                    title = "Scanning error"
                    message = "Scanning procedure failed unexpectedly. Please try again."
                    isCancelable = false
                    positiveButton(android.R.string.ok) { /* nop */ }
                }.show()
            }
        }
    }

    /*******************************************
     * Extension functions
     *******************************************/

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }
}