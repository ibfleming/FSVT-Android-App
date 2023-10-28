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
import android.text.Editable
import android.text.TextWatcher
import android.view.View.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import me.ian.fsvt.bluetooth.ConnectionManager
import me.ian.fsvt.databinding.ActivityMainBinding
import me.ian.fsvt.graph.GraphDataViewModel
import me.ian.fsvt.graph.GraphOneFragment
import me.ian.fsvt.graph.GraphTwoFragment
import me.ian.fsvt.graph.MyObjects
import org.jetbrains.anko.*
import timber.log.Timber
import kotlin.math.abs

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val MAC = "B4:52:A9:04:28:DC"
private const val SCAN_PERIOD = 3000L

class MainActivity: AppCompatActivity() {

    private var tag = "MainActivity"

    /*******************************************
     * Properties
     *******************************************/

    private lateinit var binding: ActivityMainBinding

    private var _isRunning   : Boolean = false
    private var _isConnected : Boolean = false
    private var _fileReady   : Boolean = false
    private var _fileName    : String? = null
    private var _distance    : Float = 0.0f
    private var _unitType = "feet"

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
         * Initialize Graph Fragments
         *******************************************/

        MyObjects.graphOne = binding.GraphOneFragment.findViewById(R.id.GraphOne)
        MyObjects.graphTwo = binding.GraphTwoFragment.findViewById(R.id.GraphTwo)
        MyObjects.graphDataViewModel = GraphDataViewModel()
        MyObjects.graphOneFragment = GraphOneFragment()
        MyObjects.graphTwoFragment = GraphTwoFragment()
        ConnectionManager.graphDataViewModel = MyObjects.graphDataViewModel

        supportFragmentManager.beginTransaction().apply {MyObjects
            replace(R.id.GraphOneFragment, MyObjects.graphOneFragment)
            commit()
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.GraphTwoFragment, MyObjects.graphTwoFragment)
            commit()
        }

        /*******************************************
         * Observe Live Data
         *******************************************/

        MyObjects.graphDataViewModel.dataPoint1.observe(this) { data ->
            if( data != -1F ) {
                binding.Probe1Data.text = data.toInt().toString()
            }
        }

        MyObjects.graphDataViewModel.dataPoint2.observe(this) { data ->
            if( data != -1F ) {
                binding.Probe2Data.text =  data.toInt().toString()
            }
        }

        /*******************************************
         * Observe Connection State of Device
         *******************************************/

        ConnectionManager.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                binding.ConnectButton.setBackgroundColor(ContextCompat.getColor(this, R.color.connect_color))
                binding.StartButton.isEnabled = true
                binding.ResetButton.isEnabled = true
                _isConnected = true
            }
            else {
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
         * Button Click Listeners
         *******************************************/

        /** CONNECT BUTTON **/
        binding.ConnectButton.setOnClickListener { startScan() }

        /** SETTINGS BUTTON **/
        binding.SettingsButton.setOnClickListener {
            promptFileName { fileName, distance, unitType ->
                if (fileName != null && distance != null && unitType != null ) {
                    _fileName = fileName
                    _distance = distance
                    _unitType = unitType
                    _fileReady = true
            }
            }
        }

        /** START BUTTON **/
        binding.StartButton.isEnabled = false
        binding.StartButton.setOnClickListener {
            if (ConnectionManager.isConnected.value == true) {
                if( !_isRunning ) {
                    Timber.tag(tag).d("START")
                    ConnectionManager.sendStartCommand()
                    binding.StartButton.isEnabled = false
                    binding.StopButton.isEnabled = true
                    _isRunning = true
                }
            }

        }

        /** STOP BUTTON **/
        binding.StopButton.isEnabled = false
        binding.StopButton.setOnClickListener {
            if( ConnectionManager.isConnected.value == true ) {
                if(_isRunning) {
                    Timber.tag(tag).d("STOP")
                    ConnectionManager.sendStopCommand()
                    calculateVelocity()
                    binding.StopButton.isEnabled = false
                    binding.StartButton.isEnabled = true
                    _isRunning = false
                }
            }
        }

        /** RESET BUTTON **/
        binding.ResetButton.isEnabled = false
        binding.ResetButton.setOnClickListener {
            MyObjects.graphOneFragment.clearGraph()
            MyObjects.graphTwoFragment.clearGraph()
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

        var velocity = if( _unitType == "meters" ) {
            val distanceMeters = _distance * 0.3048
            (distanceMeters / deltaTime).toFloat()
        } else {
            _distance / deltaTime
        }

        Timber.d("(1) Max Pair: " + graph1Pair.toString())
        Timber.d("(2) Max Pair: " + graph2Pair.toString())
        Timber.d("VELOCITY: $velocity")

        if( velocity.isNaN() || velocity.isInfinite() ) {
            velocity = 0.0f
        }

        showVelocityDialog(velocity)
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
            if( _unitType == "meters" ) {
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

    private fun promptFileName(callback: (String?, Float?, String?) -> Unit) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater

        val dialogView = inflater.inflate(R.layout.layout_input_dialog, null)
        builder.setView(dialogView)

        var unitType = "feet"

        val editTitle = dialogView.findViewById<EditText>(R.id.Input_Title)
        val editDist = dialogView.findViewById<EditText>(R.id.Input_Distance)
        val switchUnit = dialogView.findViewById<SwitchMaterial>(R.id.Unit_Type_Switch)
        val submit = dialogView.findViewById<Button>(R.id.Submit_Button)
        val cancel = dialogView.findViewById<Button>(R.id.Cancel_Button)
        submit.isEnabled = false

        val dialog = builder.create()

        // Request focus on the EditText
        editTitle.requestFocus()

        fun isFloat(value: String): Boolean {
            return try {
                value.toFloat()
                true
            } catch (e: NumberFormatException) {
                false
            }
        }

        fun updateSubmitButtonState() {
            val titleText = editTitle.text.toString().trim()
            val distText = editDist.text.toString().trim()

            submit.isEnabled = titleText.isNotEmpty() && distText.isNotEmpty() && isFloat(distText)
        }

        switchUnit.setOnCheckedChangeListener { _, isChecked ->
            unitType = if (isChecked) "meters" else "feet"
        }

        submit.setOnClickListener {
            val enteredText = editTitle.text.toString().trim().replace("\\s+".toRegex(), "")
            val enteredDist = editDist.text.toString().toFloat()

            Toast.makeText(this, "Name: $enteredText\nDistance: $enteredDist\nUnit: $unitType", Toast.LENGTH_LONG).show()
            dialog.dismiss()
            callback(enteredText, enteredDist, unitType)
        }

        cancel.setOnClickListener {
            dialog.dismiss()
            callback(null, null, null)
        }

        editTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                updateSubmitButtonState()
            }
        })

        editDist.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                updateSubmitButtonState()
            }
        })

        dialog.show()
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


}