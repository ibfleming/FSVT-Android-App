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
 import android.content.pm.ActivityInfo
 import android.content.pm.PackageManager
 import android.graphics.Color
 import android.os.Build
 import android.os.Bundle
 import android.os.Handler
 import android.os.Looper
 import android.text.Editable
 import android.text.TextWatcher
 import android.view.Gravity
 import android.view.LayoutInflater
 import android.view.View
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

        Timber.tag(tag).v("[MAIN INIT]")

        /*******************************************
         * Initialize Graph Fragments
         *******************************************/

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.GraphOneFragment, AppGlobals.graphOneFragment)
            commit()
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.GraphTwoFragment, AppGlobals.graphTwoFragment)
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
         * Observe Connection State of Device
         *******************************************/

        AppGlobals.graphDataViewModel.isConnected.observe(this) { isConnected ->
            /** Behavior of APP when we successfully connected **/
            if (isConnected) {
                Timber.tag(tag).v("[CONNECTED]")
                // Set connection state
                AppGlobals.connectionState = ConnectionState.CONNECTED

                /**
                 * Send a STOP command to devices initially...
                 * In the event the program on the devices is running.
                 * Better to be safe than sorry.
                 */
                ConnectionManager.sendStopCommand()

                // Show toast
                showCustomToast(this, "Successfully connected!")

                /// Button Logic
                binding.ConnectButton.backgroundColor = R.color.connect_color
                binding.ConnectButton.isEnabled  = false
                binding.SettingsButton.isEnabled = true
            }
            /** Behavior of APP when we disconnect **/
            else {
                Timber.tag(tag).v("[DISCONNECTED]")
                // Set disconnect state
                AppGlobals.connectionState = ConnectionState.DISCONNECTED

                // Show dialog
                showDisconnectedDialog()

                // Reset
                AppGlobals.resetDirective()

                // Button Logic
                binding.ConnectButton.backgroundColor = Color.TRANSPARENT
                disableButtons()
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
         *  5) Repeat from (2), if the device disconnects then this process starts over
         *
         **/

        /** DISABLE ALL BUTTONS BUT CONNECT INITIALLY **/
        disableButtons()

        /** CONNECT BUTTON **/
        binding.ConnectButton.setOnClickListener {
            it.isEnabled = false
            startScan()
        }

        /** SETTINGS BUTTON **/
        binding.SettingsButton.setOnClickListener {
            /**
             * Mutable values in the settings dialog are only changeable
             * when the devices are in stopped state.
             */
            showSettingsDialog()
        }

        /** START BUTTON **/
        binding.StartButton.setOnClickListener { button ->
            Timber.tag(tag).v("[START]")
            button.isEnabled = false

            // CSV Logic
            CSVProcessing.openBuffer()
            AppGlobals.testCount++

            // Send Command to Devices to RUN
            ConnectionManager.sendStartCommand()

            // Set Start Time

            if( AppGlobals.startProgramTime == null ) {
                AppGlobals.startProgramTime = System.currentTimeMillis()
            }

            // Button Logic
            binding.StopButton.isEnabled = true
        }

        /** STOP BUTTON **/
        binding.StopButton.setOnClickListener { button ->
            Timber.tag(tag).v("[STOP]")
            button.isEnabled = false

            // Calculate velocity
            calculateVelocity()
            showVelocityDialog()

            // Write to CSV
            CSVProcessing.writeToCSV()
            CSVProcessing.closeBuffer()

            // Send Command to Devices to STOP
            ConnectionManager.sendStopCommand()

            // Reset objects and variables
            AppGlobals.stopDirective()

            // Button Logic
            binding.StartButton.isEnabled = true
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
        showLocationPermissionDialog()
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
                showDeviceNotFoundDialog()
                binding.ConnectButton.isEnabled = true
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
     * Callbacks
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
            showScanFailedDialog()
        }
    }

    /*******************************************
     * Helper Functions
     *******************************************/

    private fun calculateVelocity() {
        val graph1Pair = AppGlobals.graphOneFragment.maxY()
        val graph2Pair = AppGlobals.graphTwoFragment.maxY()

        val graph1X = graph1Pair?.first ?: Float.NaN
        val graph2X = graph2Pair?.first ?: Float.NaN

        val deltaTime = abs(graph2X - graph1X)

        if( AppGlobals.distance == null ) return

        /** DEFAULT VALUE OF DISTANCE IS 0F **/
        val velocity = if( AppGlobals.unitType == UnitType.METERS ) {
            val distanceMeters = AppGlobals.distance!!.times(0.3048)
            (distanceMeters.div(deltaTime)).toFloat()
        } else {
            AppGlobals.distance?.div(deltaTime)
        }

        AppGlobals.velocity = String.format("%.2f", velocity).toFloat()
        if( AppGlobals.velocity!!.isInfinite() || AppGlobals.velocity!!.isNaN() ) {
            Timber.e("Invalid Velocity!")
            AppGlobals.velocity = 0F
        }

        Timber.d( "------------------------------"
            + "\n\t[Graph One] : " + graph1Pair.toString()
            + "\n\t[Graph Two] : " + graph2Pair.toString()
            + "\n\t[Velocity]  : ${AppGlobals.velocity}"
            + "\n------------------------------")
    }

    /***
     * Resets all the buttons to DEFAULT.
     * Connect button is the only button enabled.
     * The rest are disabled.
     */
    private fun disableButtons() {
        binding.ConnectButton.isEnabled  = true
        binding.SettingsButton.isEnabled = false
        binding.StartButton.isEnabled    = false
        binding.StopButton.isEnabled     = false
    }

    private fun changeOrientation() {
        val newOrientation = when (requestedOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        requestedOrientation = newOrientation
    }

    /*******************************************
     * Permissions
     *******************************************/

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    /*******************************************
     * Custom Dialog Alerts
     *******************************************/

    private fun showVelocityDialog() {
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
            if( AppGlobals.unitType == UnitType.METERS ) {
                getString(R.string.Velocity_Meters, String.format("%.2f", AppGlobals.velocity))
            } else {
                getString(R.string.Velocity_Feet, String.format("%.2f", AppGlobals.velocity))
            }

        tvVelocity.text = velocityText

        // OK Button
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK") { _, _ ->
            dialog.dismiss()
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSettingsDialog() {
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
        val batteryViewProbe1 = dialogView.findViewById<TextView>(R.id.Battery_Probe_1)
        val batteryViewProbe2 = dialogView.findViewById<TextView>(R.id.Battery_Probe_2)
        val submit = dialogView.findViewById<Button>(R.id.Submit_Button)
        val back = dialogView.findViewById<Button>(R.id.Back_Button)
        val rotate = dialogView.findViewById<Button>(R.id.Rotate_Button)

        // Input Logic for All
        if( AppGlobals.deviceState == DeviceState.RUNNING ) {
            editTitle.isEnabled      = false
            editDist.isEnabled       = false
            feetCheckbox.isEnabled   = false
            metersCheckbox.isEnabled = false
            rotate.isEnabled         = false
        }

        // Checkbox Logic
        if( AppGlobals.unitType == UnitType.FEET ) feetCheckbox.isChecked = true
        else metersCheckbox.isChecked = true

        // Checkbox Listeners (Actually Sets the Unit Type value)
        feetCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                metersCheckbox.isChecked = false
                AppGlobals.unitType = UnitType.FEET
            }
        }

        metersCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                feetCheckbox.isChecked = false
                AppGlobals.unitType = UnitType.METERS
            }
        }

        // Input Box Logic
        if( AppGlobals.fileName != null ) editTitle.setText(AppGlobals.fileName)
        if( AppGlobals.distance != null ) editDist.setText(AppGlobals.distance.toString())

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

        // Battery Indicator

        val batteryText1 = getString(R.string.battery_percentage, AppGlobals.batteryProbe1)
        val batteryText2 = getString(R.string.battery_percentage, AppGlobals.batteryProbe2)
        batteryViewProbe1.text = batteryText1
        batteryViewProbe2.text = batteryText2

        // Buttons

        rotate.isEnabled = true
        rotate.setOnClickListener {
            // prompt user if this indeed what they desire -> will reset app!
            dialog.dismiss() // Must dismiss this Alert first otherwise a View Leak will occur
            showOrientationChangeDialog()
        }




        back.setOnClickListener {
            dialog.dismiss()
        }

        submit.isEnabled = false
        submit.setOnClickListener {
            /*
             * At this point, there is valid user input for the file name and distance.
             * Must check if this is the first time the user as inputted this information or not.
             * If NOT and if the test is NOT RUNNING then let's check if
             * fileBuffer != NULL (this also implies csvFile != NULL as this var is dependent on csvFile)
             * If we submit within these conditions, then we must close the current fileBuffer and make it null.
             * fileBuffer gets set (non-null) in Start Button Listener at openBuffer()
             * We shall then set the fileName to the new one (alongside distance) and call the
             * createFile() -> creates this new file for new tests presuming that this is the
             * intention of the user.
             * The other case would be by these values are null and therefore ->
             * set the filename and distance and create file...
             * NOTE: File buffer only cause on RESET button which is entirely different behavior to be handled
             */

            if( AppGlobals.fileBuffer == null ) {
                // There is currently no file buffer that is opened for writes
                // Set parameters
                AppGlobals.fileName = editTitle.text.toString().trim()
                AppGlobals.distance = editDist.text.toString().toFloat()
                AppGlobals.testCount = 0

                if ( CSVProcessing.createFile() ) {
                    Timber.tag("Settings").v("Created the file successfully.")
                    if (CSVProcessing.openBuffer())  Timber.tag("Settings").v("File Buffer OPENED!")
                    binding.StartButton.isEnabled = true
                }
                else {
                    Timber.tag("Settings").e("Failed to create the file.")
                }
            }
            else {
                Timber.tag("Settings").e("FILE BUFFER IS NOT NULL")
            }

            // Send Custom Toast to User
            val distStr = if( AppGlobals.unitType == UnitType.FEET) {
                "${AppGlobals.distance}ft"
            } else {
                "${AppGlobals.distance}m"
            }
            showCustomToast(this, "Name = ${AppGlobals.fileName}\nDistance = $distStr")

            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeviceNotFoundDialog() {
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

    private fun showDisconnectedDialog() {
        runOnUiThread {
            AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Disconnected")
                .setMessage("The device disconnected unexpectedly from the app. "
                        + "Please connect to the device again. "
                        + "If this occurred during a running test, please power cycle the devices.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun showLocationPermissionDialog() {
        runOnUiThread {
            AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Location permission required")
                .setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                        "location access in order to scan for BLE devices.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
                .show()
        }
    }

    private fun showScanFailedDialog() {
        runOnUiThread {
            AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Scanning error")
                .setMessage("Scanning procedure failed unexpectedly. Please try again.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _,_ -> /* NOP */ }
                .show()
        }
    }

    private fun showOrientationChangeDialog() {
        runOnUiThread {
            AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Orientation change")
                .setMessage("WARNING: Changing the orientation will restart the app and any tests!")
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { _,_ -> changeOrientation() }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    @SuppressLint("InflateParams")
    @Deprecated("Deprecated in Java")
    private fun showCustomToast(context: Context, message: String) {
        val layoutInflater: LayoutInflater = LayoutInflater.from(context)
        val layout: View = layoutInflater.inflate(R.layout.custom_toast_layout, null)

        val textView: TextView = layout.findViewById(R.id.Toast_Text)
        textView.text = message

        val toast = Toast(context)
        toast.setGravity(Gravity.TOP, 0, 96)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }
}