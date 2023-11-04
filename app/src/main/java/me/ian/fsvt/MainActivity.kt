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
 import android.graphics.Color
 import android.os.Build
 import android.os.Bundle
 import android.os.Handler
 import android.os.Looper
 import android.text.Editable
 import android.text.TextWatcher
 import android.view.View.*
 import android.widget.Button
 import android.widget.EditText
 import android.widget.TextView
 import android.widget.Toast
 import androidx.annotation.RequiresApi
 import androidx.appcompat.app.AlertDialog
 import androidx.appcompat.app.AppCompatActivity
 import androidx.core.app.ActivityCompat
 import androidx.core.content.ContextCompat
 import com.google.android.material.switchmaterial.SwitchMaterial
 import me.ian.fsvt.bluetooth.ConnectionManager
 import me.ian.fsvt.csv.CSVProcessing
 import me.ian.fsvt.databinding.ActivityMainBinding
 import me.ian.fsvt.graph.ConnectionState
 import me.ian.fsvt.graph.DeviceState
 import me.ian.fsvt.graph.GraphDataViewModel
 import me.ian.fsvt.graph.GraphOneFragment
 import me.ian.fsvt.graph.GraphTwoFragment
 import me.ian.fsvt.graph.MyObjects
 import me.ian.fsvt.graph.UnitType
 import org.jetbrains.anko.*
 import timber.log.Timber
 import java.io.File
 import kotlin.math.abs


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val STORAGE_REQUEST_CODE = 3
private const val MAC = "B4:52:A9:04:28:DC"
private const val SCAN_PERIOD = 3000L

class MainActivity: AppCompatActivity() {

    private var tag = "MainActivity"

    /*******************************************
     * Properties
     *******************************************/

    private lateinit var binding: ActivityMainBinding

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

    private val isStoragePermissionGranted
        get() = hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /*******************************************
     * Activity function overrides
     *******************************************/

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Timber.tag(tag).d("MainActivity Initialized!")

        /*******************************************
         * Initialize Graph Fragments
         *******************************************/

        MyObjects.graphDataViewModel = GraphDataViewModel()
        MyObjects.graphOneFragment = GraphOneFragment()
        MyObjects.graphTwoFragment = GraphTwoFragment()

        supportFragmentManager.beginTransaction().apply {MyObjects
            replace(R.id.GraphOneFragment, MyObjects.graphOneFragment)
            commit()
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.GraphTwoFragment, MyObjects.graphTwoFragment)
            commit()
        }

        /*******************************************
         * CSV Initialization (Create directory)
         *******************************************/

        if( !isStoragePermissionGranted ) {
            requestStoragePermission()
        } else {
            CSVProcessing.createDirectory()
        }

        /*******************************************
         * Observe Live Data for TextView (OPTIONAL?)
         *******************************************/

        MyObjects.graphDataViewModel.dataPoint1.observe(this) { data ->
            binding.Probe1Data.text = data.toInt().toString()
        }

        MyObjects.graphDataViewModel.dataPoint2.observe(this) { data ->
            binding.Probe2Data.text =  data.toInt().toString()
        }

        /*******************************************
         * Observe Connection State of Device
         *******************************************/

        MyObjects.graphDataViewModel.isConnected.observe(this) { isConnected ->
            /** Behavior of APP when we successfully connected **/
            if (isConnected) {
                MyObjects.connectionState = ConnectionState.CONNECTED
                binding.ConnectButton.setBackgroundColor(ContextCompat.getColor(this, R.color.connect_color))
                binding.ConnectButton.isEnabled  = false
                binding.SettingsButton.isEnabled = true
                runOnUiThread {
                    Toast.makeText(this, "Device connected successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            /** Behavior of APP when we disconnect **/
            else {
                MyObjects.connectionState = ConnectionState.DISCONNECTED
                binding.ConnectButton.backgroundColor = Color.TRANSPARENT
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "The device disconnected unexpectedly from the app. " +
                                  "Please connect to the device again."
                        isCancelable = false
                        positiveButton(android.R.string.ok) {
                            resetApp()
                        }
                    }.show()
                }
            }
        }

        /*******************************************
         * Button Click Listeners
         *******************************************/

        /** CONNECT BUTTON **/
        binding.ConnectButton.setOnClickListener {
            startScan()
        }

        /** SETTINGS BUTTON **/
        binding.SettingsButton.isEnabled = false // Disable by default
        binding.SettingsButton.setOnClickListener {
            promptSettings { fileName, distance, unitType ->
                if (fileName != null && distance != null && unitType != null ) {
                    MyObjects.fileName = fileName
                    MyObjects.distance = distance
                    MyObjects.unitType = unitType
                    MyObjects.fileReady = true
                    binding.StartButton.isEnabled = true
                    binding.ResetButton.isEnabled = true
                }
            }
        }

        /** START BUTTON **/
        binding.StartButton.isEnabled = false   // Disable by default
        binding.StartButton.setOnClickListener {
            if (MyObjects.connectionState == ConnectionState.CONNECTED) {
                if( MyObjects.deviceState == DeviceState.STOPPED ) {
                    if( MyObjects.fileReady ) {
                        Timber.tag(tag).d("[STARTING PROGRAM]")
                        MyObjects.deviceState = DeviceState.RUNNING
                        binding.StartButton.isEnabled = false
                        binding.StopButton.isEnabled = true
                        ConnectionManager.sendStartCommand()

                        /** Set the start time of the program here **/
                        if( MyObjects.startProgramTime == null ) {
                            MyObjects.startProgramTime = System.currentTimeMillis()
                        }
                    } else {
                        // Dialog alert to prompt user for distance and filename, etc.
                    }
                }
            }
            else {
                Timber.w("Device is not connected!")
            }
        }

        /** STOP BUTTON **/
        binding.StopButton.isEnabled = false    // Disable by default
        binding.StopButton.setOnClickListener {
            if( MyObjects.connectionState == ConnectionState.CONNECTED) {
                if( MyObjects.deviceState == DeviceState.RUNNING) {
                    Timber.tag(tag).d("[STOPPING PROGRAM]")
                    MyObjects.deviceState = DeviceState.STOPPED
                    binding.StopButton.isEnabled = false
                    binding.StartButton.isEnabled = true
                    ConnectionManager.sendStopCommand()
                    calculateVelocity()
                }
            }
        }

        /** RESET BUTTON **/
        binding.ResetButton.isEnabled = false   // Disable by default
        binding.ResetButton.setOnClickListener {
            if( MyObjects.connectionState == ConnectionState.CONNECTED && MyObjects.deviceState == DeviceState.STOPPED ) {
                MyObjects.graphOneFragment.clearGraph()
                MyObjects.graphTwoFragment.clearGraph()
                MyObjects.startProgramTime = null
                MyObjects.stopProgramTime  = null
            }
        }
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
            STORAGE_REQUEST_CODE -> {
                if( grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestStoragePermission()
                } else {
                    CSVProcessing.createDirectory()
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

    private fun requestStoragePermission() {
        if( isStoragePermissionGranted ) {
            return
        }
        requestPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            STORAGE_REQUEST_CODE
        )
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

            scanTimeoutHandler.postDelayed({
                stopScan()
                showDeviceNotFoundAlert()
            }, SCAN_PERIOD)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        Timber.tag(tag).d("Stopped BLE scan!")
        bleScanner.stopScan(scanCallback)
        scanTimeoutHandler.removeCallbacksAndMessages(null)
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

    private fun calculateVelocity() {
        val graph1Pair = MyObjects.graphOneFragment.findMaxEntry()
        val graph2Pair = MyObjects.graphTwoFragment.findMaxEntry()

        val graph1X = graph1Pair?.first ?: Float.NaN
        val graph2X = graph2Pair?.first ?: Float.NaN

        val deltaTime = abs(graph2X - graph1X)

        // DEFAULT VALUE OF DISTANCE IS 0F
        var velocity = if( MyObjects.unitType == UnitType.METERS ) {
            val distanceMeters = MyObjects.distance * 0.3048
            (distanceMeters / deltaTime).toFloat()
        } else {
            MyObjects.distance / deltaTime
        }

        if( velocity.isNaN() || velocity.isInfinite() ) {
            velocity = 0F
        }

        Timber.d("------------------------------------------------------------")
        Timber.d("\tVelocity: $velocity")
        Timber.d("\t[Graph One] Max Pair: " + graph1Pair.toString())
        Timber.d("\t[Graph Two] Max Pair: " + graph2Pair.toString())
        Timber.d("------------------------------------------------------------")

        showVelocityDialog(velocity)
    }

    private fun resetApp() {
        Timber.w("[RESET APP]")
        MyObjects.resetValues()
        binding.ConnectButton.isEnabled  = true
        binding.SettingsButton.isEnabled = false
        binding.StartButton.isEnabled    = false
        binding.ResetButton.isEnabled    = false
    }

    /*******************************************
     * Custom Dialog Alerts
     *******************************************/

    private fun showVelocityDialog(v: Float) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater

        val dialogView = inflater.inflate(R.layout.layout_velocity_dialog, null)
        builder.setView(dialogView)

        val tvVelocity = dialogView.findViewById<TextView>(R.id.Velocity)

        val velocityText : String =
            if( MyObjects.unitType == UnitType.METERS ) {
                getString(R.string.Velocity_Meters, String.format("%.2f", v))
            } else {
                getString(R.string.Velocity_Feet, String.format("%.2f", v))
            }

        tvVelocity.text = velocityText

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun promptSettings(callback: (String?, Float?, UnitType?) -> Unit) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.layout_input_dialog, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        val unitTypeSwitch = dialogView.findViewById<SwitchMaterial>(R.id.Unit_Type_Switch)
        val editTitle = dialogView.findViewById<EditText>(R.id.Input_Title)
        val editDist = dialogView.findViewById<EditText>(R.id.Input_Distance)
        val submit = dialogView.findViewById<Button>(R.id.Submit_Button)
        val cancel = dialogView.findViewById<Button>(R.id.Cancel_Button)

        // Set default values if they exist
        if (MyObjects.fileName != null) {
            editTitle.setText(MyObjects.fileName)
        }

        // FEET by default, check if otherwise
        unitTypeSwitch.isChecked = MyObjects.unitType == UnitType.METERS

        // Check if distance isn't 0
        if (MyObjects.distance != 0F) {
            editDist.setText(MyObjects.distance.toString())
        }

        // Function to check if a string can be parsed to a float
        fun isFloat(value: String): Boolean {
            return try {
                value.toFloat()
                true
            } catch (e: NumberFormatException) {
                false
            }
        }

        // Function to update the state of the Submit button
        fun updateSubmitButtonState() {
            val titleText = editTitle.text.toString().trim()
            val distText = editDist.text.toString().trim()

            submit.isEnabled = titleText.isNotEmpty() && distText.isNotEmpty() && isFloat(distText)
        }

        // Switch change listener
        unitTypeSwitch.setOnCheckedChangeListener { _, _ ->
            // Handle unit type change
            // ...
        }

        // Submit button click listener
        if( !MyObjects.fileReady ) {
            submit.isEnabled = false
        }
        submit.setOnClickListener {
            val enteredText = editTitle.text.toString().trim().replace("\\s+".toRegex(), "")
            val enteredDist = editDist.text.toString().toFloat()
            val enteredUnitType = if (unitTypeSwitch.isChecked) UnitType.METERS else UnitType.FEET

            Toast.makeText(this, "Name: $enteredText\nDistance: $enteredDist\nUnit: $enteredUnitType", Toast.LENGTH_LONG).show()
            dialog.dismiss()

            callback(enteredText, enteredDist, enteredUnitType)
        }

        // Cancel button click listener
        cancel.setOnClickListener {
            dialog.dismiss()
            callback(null, null, null)
        }

        // EditText listeners
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                updateSubmitButtonState()
            }
        }

        editTitle.addTextChangedListener(textWatcher)
        editDist.addTextChangedListener(textWatcher)

        // Show the dialog
        dialog.show()
    }

    private fun showDeviceNotFoundAlert() {
        runOnUiThread {
            alert {
                title = "No device found"
                message = "Could not find the specified device. " +
                        "Please ensure the devices are powered on and try again."
                isCancelable = false
                positiveButton(android.R.string.ok) { resetApp() }
            }.show()
        }
    }
}