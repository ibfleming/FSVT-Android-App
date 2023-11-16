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
 import android.content.DialogInterface
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
 import android.widget.CheckBox
 import android.widget.EditText
 import android.widget.TextView
 import android.widget.Toast
 import androidx.annotation.RequiresApi
 import androidx.appcompat.app.AlertDialog
 import androidx.appcompat.app.AppCompatActivity
 import androidx.core.app.ActivityCompat
 import androidx.core.content.ContextCompat
 import me.ian.fsvt.bluetooth.ConnectionManager
 import me.ian.fsvt.csv.CSVProcessing
 import me.ian.fsvt.databinding.ActivityMainBinding
 import me.ian.fsvt.graph.GraphDataViewModel
 import me.ian.fsvt.graph.GraphOneFragment
 import me.ian.fsvt.graph.GraphTwoFragment
 import org.jetbrains.anko.*
 import timber.log.Timber
 import kotlin.math.abs

private const val ENABLE_BLUETOOTH_REQUEST_CODE    = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val STORAGE_PERMISSION_REQUEST_CODE  = 3

private const val MAC_ADDRESS = "B4:52:A9:04:28:DC"
private const val SCAN_PERIOD = 3000L

class MainActivity: AppCompatActivity() {

    private var tag = "MAIN"

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

    private val scanFilter = ScanFilter.Builder().setDeviceAddress(MAC_ADDRESS).build()
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

        Timber.tag(tag).v("[MAIN INITIALIZED]")

        /*******************************************
         * Initialize Graph Fragments
         *******************************************/

        MyObjects.graphDataViewModel = GraphDataViewModel()
        MyObjects.graphOneFragment = GraphOneFragment()
        MyObjects.graphTwoFragment = GraphTwoFragment()

        supportFragmentManager.beginTransaction().apply {
            MyObjects
            replace(R.id.GraphOneFragment, MyObjects.graphOneFragment)
            commit()
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.GraphTwoFragment, MyObjects.graphTwoFragment)
            commit()
        }

        /*******************************************
         * CSV Initialization (Creates directory)
         *******************************************/

        if( !isStoragePermissionGranted ) {
            requestStoragePermission()
        } else {
            CSVProcessing.createDirectory()
        }

        /*******************************************
         * Observe Live Data for TextView (OPTIONAL)
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
                    Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show()
                }
            }
            /** Behavior of APP when we disconnect **/
            else {
                MyObjects.connectionState = ConnectionState.DISCONNECTED
                binding.ConnectButton.backgroundColor = Color.TRANSPARENT
                // TODO "Perhaps make a custom layout for this?"
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "The device disconnected unexpectedly from the app. " +
                                  "Please connect to the device again."
                        isCancelable = false
                        positiveButton(android.R.string.ok) {}
                    }.show()
                }
            }
        }

        /*******************************************
         * Button Click Listeners
         *******************************************/

        /**
         *  Notes:
         *  - All buttons are accessible only when the bluetooth device is connected to the app
         *  - All the buttons but the 'Connect' check first if the device is connected
         *  - Pay attention to the other conditionals the buttons have to work.
         *
         *  Flow of execution:
         *  1) Connect Button -> Scan, and connects if device is found
         *  2) 'Settings' button is enabled -> Input a distance and file name, etc.
         *  3) If values are valid, 'Start' and 'Reset' buttons are enabled
         *  4) 'Stop' button is enabled only if 'Start' has been pressed.
         *  5) 'Reset' button will disabled all buttons but 'Settings' and resets all values/tests
         *  6) Repeat from (2), if the device disconnects then this process starts over
         *
         **/

        /** CONNECT BUTTON **/
        binding.ConnectButton.setOnClickListener {
            startScan()
        }

        /** SETTINGS BUTTON **/
        binding.SettingsButton.setOnClickListener {
            // Prompt Settings Dialog
            promptSettings()
        }

        /** START BUTTON **/
        binding.StartButton.setOnClickListener {
                Timber.tag(tag).d("[STARTING PROGRAM]")
                MyObjects.deviceState = DeviceState.RUNNING
                binding.StartButton.isEnabled = false
                binding.StopButton.isEnabled = true
                ConnectionManager.sendStartCommand()
                MyObjects.testCount++

                /** Create the file and open it for writing **/
                CSVProcessing.createFile()

                /** Set the start time of the program here **/
                if( MyObjects.startProgramTime == null ) {
                    MyObjects.startProgramTime = System.currentTimeMillis()
                }
        }

        /** STOP BUTTON **/
        binding.StopButton.setOnClickListener {
            calculateVelocity()
            // TODO "Fix this logic"
            // Device state will be set to STOPPED

            /*if( MyObjects.connectionState == ConnectionState.CONNECTED) {
                if( MyObjects.deviceState == DeviceState.RUNNING) {
                    Timber.tag(tag).d("[STOPPING PROGRAM]")
                    MyObjects.deviceState = DeviceState.STOPPED
                    binding.StopButton.isEnabled = false
                    binding.StartButton.isEnabled = true
                    ConnectionManager.sendStopCommand()

                    /** Calculate velocity and prompt the dialog **/
                    calculateVelocity()
                    /** Write the data of the test to the CSV file **/
                    CSVProcessing.writeToCSV()
                    /** Execute a soft reset to further future tests **/
                    softReset()
                }
            }*/
        }

        /** RESET BUTTON **/
        binding.ResetButton.setOnClickListener {
            // TODO "Fix this logic"
            // Device state will be set to STOPPED
        }

        /*
        private fun reset() {
            Timber.v("[RESET APP]")
            MyObjects.resetValues()
            binding.StartButton.isEnabled = false
            binding.StopButton.isEnabled  = false
            binding.ResetButton.isEnabled = false
        }

        private fun softReset() {
            Timber.v("[SOFT RESET APP]")
            MyObjects.softResetValues()
        }
        */
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
            STORAGE_PERMISSION_REQUEST_CODE -> {
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
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        // TODO "Perhaps make a custom layout for this?"
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
            // TODO "Perhaps make a custom layout for this?"
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

        if( MyObjects.distance == null ) return

        /** DEFAULT VALUE OF DISTANCE IS 0F **/
        var velocity = if( MyObjects.unitType == UnitType.METERS ) {
            val distanceMeters = MyObjects.distance!!.times(0.3048)
            (distanceMeters.div(deltaTime)).toFloat()
        } else {
            MyObjects.distance?.div(deltaTime)
        }

        if( velocity == null ) return

        if( velocity.isNaN() || velocity.isInfinite() ) {
            velocity = 0F
        }

        Timber.d( "------------------------------"
            + "\n\t[Graph One] : " + graph1Pair.toString()
            + "\n\t[Graph Two] : " + graph2Pair.toString()
            + "\n\t[Velocity]  : $velocity"
            + "\n------------------------------")

        MyObjects.velocity = String.format("%.2f", velocity).toFloat()
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
        val dialog = builder.create()

        // Custom Background
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_bg)

        // Layout Views
        val tvVelocity = dialogView.findViewById<TextView>(R.id.Velocity)

        val velocityText : String =
            if( MyObjects.unitType == UnitType.METERS ) {
                getString(R.string.Velocity_Meters, String.format("%.2f", v))
            } else {
                getString(R.string.Velocity_Feet, String.format("%.2f", v))
            }

        tvVelocity.text = velocityText

        // OK Button
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK") { _, _ ->
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun promptSettings() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.layout_input_dialog, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        // Custom Background
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_bg)

        // Layout Views
        val editTitle = dialogView.findViewById<EditText>(R.id.Input_Title)
        val editDist = dialogView.findViewById<EditText>(R.id.Input_Distance)
        val feetCheckbox = dialogView.findViewById<CheckBox>(R.id.Feet_CheckBox)
        val metersCheckbox = dialogView.findViewById<CheckBox>(R.id.Meters_CheckBox)
        val submit = dialogView.findViewById<Button>(R.id.Submit_Button)
        val back = dialogView.findViewById<Button>(R.id.Back_Button)

        // Checkbox Logic
        if( MyObjects.unitType == UnitType.FEET ) feetCheckbox.isChecked = true
        else metersCheckbox.isChecked = true

        // Checkbox Listeners (Actually Sets the Unit Type value)
        feetCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                metersCheckbox.isChecked = false
                MyObjects.unitType = UnitType.FEET
            }
        }

        metersCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                feetCheckbox.isChecked = false
                MyObjects.unitType = UnitType.METERS
            }
        }

        // Input Box Logic
        if( MyObjects.fileName != null ) editTitle.setText(MyObjects.fileName)
        if( MyObjects.distance != null ) editDist.setText(MyObjects.distance.toString())

        // Input Box Helper Functions
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
            submit.isEnabled = (titleText.isNotEmpty() && distText.isNotEmpty()) && isFloat(distText)
        }

        // Input Box Listeners (Only Enable is No Test is RUNNING)
        if( MyObjects.deviceState == DeviceState.STOPPED ) {

            editTitle.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(p0: Editable?) {
                    updateSubmitButtonState()
                }
            })

            editDist.addTextChangedListener(object :TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(p0: Editable?) {
                    updateSubmitButtonState()
                }
            })
        }

        // Buttons
        back.setOnClickListener {
            Toast.makeText(this,
                "Name: ${MyObjects.fileName}\nDistance: ${MyObjects.distance}\nUnit: ${MyObjects.unitType}", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        submit.isEnabled = false
        submit.setOnClickListener {
            MyObjects.fileName = editTitle.text.toString().trim()
            MyObjects.distance = editDist.text.toString().toFloat()
            Toast.makeText(this,
                "Name: ${MyObjects.fileName}\nDistance: ${MyObjects.distance}\nUnit: ${MyObjects.unitType}", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    // TODO "Perhaps make a custom layout for this?"
    private fun showDeviceNotFoundAlert() {
        runOnUiThread {
            AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("No device found")
                .setMessage("Could not find the specified device. " +
                        "Please ensure the devices are powered on and try again.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}