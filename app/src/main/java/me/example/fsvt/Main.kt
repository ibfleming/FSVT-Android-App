package me.example.fsvt

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// MAC ADDRESS OF HM10: B0:D2:78:32:F5:7F
// © 2023 - 2023 https://github.com/ibfleming/FSVT-Android-App.git - All Rights Reserved.

@SuppressLint("MissingPermission")
class Main : ComponentActivity() {

    private val tag = "IAN"

    // Bluetooth Connection STATUS
    private var bluetoothOn = false
    private var locationOn = false
    //private var connected = false

    // 1) Create U.I. Objects
    private lateinit var bConnect : Button
    private lateinit var bGraph : Button
    private lateinit var bStop : Button
    private lateinit var tvStatus : TextView
    private lateinit  var bleList : TextView
    private lateinit var graph : LineChart

    // 2) Create Bluetooth Objects
    private lateinit var bleManager : BLEManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner : BLEScanner
    private var accelerometerActivity: AccelerometerActivity? = null
    private var graphActivity : GraphActivity? = null

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if( isGranted ) {
                // Location service is enabled
                checkLocationService()
            } else {
                // Location service is disabled
            }
        }

    private val requestEnableBTLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if( result.resultCode == RESULT_OK ) {
                // Bluetooth is enabled, start scanning
                bluetoothOn = true
            } else {
                // Bluetooth is disabled
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(tag, "App Initialized!")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        // 3) Fetch Object References
        bConnect = findViewById(R.id.bConnect)
        bGraph = findViewById(R.id.bGraph)
        bStop = findViewById(R.id.bStop)
        tvStatus = findViewById(R.id.tvBLEStatus)
        bleList = findViewById(R.id.bleList)
        graph = findViewById(R.id.G_Main)

        // 4) Fetch B.T. Object References
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        bleScanner = BLEScanner(bluetoothAdapter)
        bleManager = BLEManager(this)

        graphActivity = GraphActivity(graph)
        graphActivity!!.createGraph()
        accelerometerActivity = AccelerometerActivity(graphActivity!!)

        // ... Respectively, the Start button will be pressed...

        bConnect.setOnClickListener { _ ->
            startBLEScan()
        }

        bGraph.setOnClickListener {
            accelerometerActivity!!.onCreate(this)
        }

        bStop.setOnClickListener {
            accelerometerActivity!!.stopPopulating()
        }
    }

    override fun onResume() {
        super.onResume()
        if(!bluetoothOn && !locationOn) {
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if(!hasFineLocationPermission) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkLocationService()
        }
    }

    private fun checkLocationService() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if( isLocationEnabled ) {
            locationOn = true
            checkBluetoothStatus()
        }
        else {
            showLocationPrompt()
            checkBluetoothStatus()
        }
    }

    private fun showLocationPrompt() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enable Location Service")
        builder.setMessage("Enabling the location services is required for this app to function.")
        builder.setPositiveButton("Settings") { dialog, _ ->
            val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            dialog.dismiss()
            checkLocationPermission()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            checkLocationPermission()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun checkBluetoothStatus() {
        if(!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBTLauncher.launch(enableBtIntent)
        } else {
            bluetoothOn = true
        }
    }

    private fun startBLEScan() {
        val deviceList = HashSet<String>() // <--- FOR DEBUGGING

        tvStatus.setText(R.string.ui_scanning_status)
        bleScanner.startScan { scanResults ->
            // Handle the list of scan results here
            for(result in scanResults) {
                val device = result.device
                val deviceName = device.name

                if (!deviceName.isNullOrBlank() && deviceName != "null") {
                    deviceList.add(deviceName + " " + device.address)
                }

                if( device.address == "40:6E:21:FC:26:71" ) {
                    Log.i(tag, "FOUND FVST DEVICE!")
                }

            }
        }

        lifecycleScope.launch {
            delay(SCAN_PERIOD)
            bleScanner.stopScan()
            tvStatus.setText(R.string.ui_found_devices)

            bleList.text = deviceList.toString()
            val deviceNameString = buildString {
                append("\n")
                for(deviceName in deviceList) {
                    append("$deviceName\n")
                }
            }

            bleList.text = deviceNameString
            tvStatus.setText(R.string.ui_on_status)
            tvStatus.setTextColor(Color.GREEN)
        }
    }

    companion object {
        private const val SCAN_PERIOD: Long = 5000 // 5sec
    }
}



/*  ON BUTTON PRESS LISTENER EXAMPLE
bStart.setOnClickListener{ _ ->
    if( !bluetoothAdapter.isEnabled ) {
        val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBTIntent)
    }
}

bShowDevices.setOnClickListener{ _ ->
    val sb = StringBuilder()
    bondedDevices = bluetoothAdapter.bondedDevices
    for( temp in (bondedDevices as MutableSet<BluetoothDevice>?)!!) {
        sb.append("\n" + temp.name + "\n")
    }
    tvDevices.text = sb.toString()
}
*/

