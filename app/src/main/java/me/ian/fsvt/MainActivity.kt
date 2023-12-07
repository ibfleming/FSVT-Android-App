package me.ian.fsvt

 import android.Manifest
 import android.annotation.SuppressLint
 import android.bluetooth.*
 import android.bluetooth.le.ScanCallback
 import android.bluetooth.le.ScanFilter
 import android.bluetooth.le.ScanResult
 import android.bluetooth.le.ScanSettings
 import android.content.BroadcastReceiver
 import android.content.Context
 import android.content.DialogInterface
 import android.content.Intent
 import android.content.IntentFilter
 import android.content.pm.ActivityInfo
 import android.content.pm.PackageManager
 import android.location.LocationManager
 import android.net.Uri
 import android.os.Build
 import android.os.Bundle
 import android.os.Environment
 import android.os.Handler
 import android.os.Looper
 import android.provider.Settings
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
 import androidx.core.content.ContextCompat
 import me.ian.fsvt.bluetooth.ConnectionManager
 import me.ian.fsvt.csv.CSVProcessing
 import me.ian.fsvt.databinding.ActivityMainBinding
 import me.ian.fsvt.graph.GraphOneFragment
 import me.ian.fsvt.graph.GraphTwoFragment
 import org.jetbrains.anko.*
 import timber.log.Timber
 import kotlin.math.abs

private const val REQUEST_BLUETOOTH_PERMISSIONS = 0
private const val REQUEST_STORAGE_PERMISSION    = 1
private const val REQUEST_ENABLE_BLUETOOTH      = 2
private const val REQUEST_ENABLE_LOCATION       = 3

private const val MAC_ADDRESS = "B4:52:A9:04:28:DC"
private const val SCAN_PERIOD = 3000L

class MainActivity: AppCompatActivity() {

    private var tag = "MAIN"

    private var bluetoothPermissionsGranted = false

    /*******************************************
     * Properties
     *******************************************/

    private lateinit var binding: ActivityMainBinding

    /*******************************************
     * BLUETOOTH
     *******************************************/

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> {
                        Timber.tag(tag).v("[BLUETOOTH DISABLED]")
                        checkBluetoothAdapter()
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Timber.tag(tag).v("[BLUETOOTH ENABLED]")
                    }
                }
            }
        }
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanFilter = ScanFilter.Builder().setDeviceAddress(MAC_ADDRESS).build()
    private val scanResults = mutableListOf<ScanResult>()

    @SuppressLint("MissingPermission")
    private fun checkBluetoothAdapter() {
        if( bluetoothPermissionsGranted ) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bluetoothConnectPermission = Manifest.permission.BLUETOOTH_CONNECT
                if (checkSelfPermission(bluetoothConnectPermission) == PackageManager.PERMISSION_GRANTED) {
                    val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
                }
            }
            else {
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
            }
        }
    }

    /*******************************************
     * LOCATION SERVICE
     *******************************************/

    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val locationServiceReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (!locationManager.isLocationEnabled) {
                    enableLocationService(context)
                }
                else {
                    Timber.tag(tag).v("Location service is enabled.")
                }
            }
        }
    }

    private fun enableLocationService(context: Context) {
        val enableLocationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        enableLocationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(enableLocationIntent)
    }

    private fun checkLocationService() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if( !locationManager.isLocationEnabled ) {
                Timber.tag(tag).e("Location service is disabled.")
                enableLocationService(this)
            } else {
                Timber.tag(tag).v("Location service is enabled.")
            }
        }
    }

    /*******************************************
     * PERMISSIONS
     *******************************************/

    private fun checkAndRequestBluetoothPermissions() {
        // For devices SDK >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val bluetoothPermissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            val permissionsToRequest = mutableListOf<String>()

            Timber.tag(tag).v("Bluetooth Permissions (SDK >= 31):")
            for (permission in bluetoothPermissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
                else {
                    Timber.tag(tag).v("\t$permission")
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            } else {
                bluetoothPermissionsGranted = true
                checkBluetoothAdapter()
                checkLocationService()
            }
        } else {

            val bluetoothPermissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            val permissionsToRequest = mutableListOf<String>()

            Timber.tag(tag).v("Bluetooth Permissions:")
            for (permission in bluetoothPermissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
                else {
                    Timber.tag(tag).v("\t$permission")
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            } else {
                bluetoothPermissionsGranted = true
                checkBluetoothAdapter()
                checkLocationService()
            }
        }
    }

    private fun checkAndRequestStoragePermission() {
        // For devices between SDK 23-30
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_PERMISSION
                )
            } else {
                Timber.tag(tag).v("Storage permission is granted.")
                CSVProcessing.createDirectory()
            }
        }
        // For devices >= 33
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_STORAGE_PERMISSION)
            }
            else {
                Timber.tag(tag).v("Storage permission is granted.")
                CSVProcessing.createDirectory()
            }
        }
    }

    /*******************************************
     * PERMISSION RESULTS
     *******************************************/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSIONS -> {
                val deniedPermissions = mutableListOf<String>()
                val grantedPermissions = mutableListOf<String>()

                for (i in permissions.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permissions[i])
                    } else {
                        grantedPermissions.add(permissions[i])
                    }
                }

                if (grantedPermissions.isNotEmpty()) {
                    Timber.tag(tag).v("Successfully Granted Permissions:")
                    for (permission in grantedPermissions) {
                        Timber.tag(tag).v("\t$permission")
                    }
                }

                if (deniedPermissions.isNotEmpty()) {
                    Timber.tag(tag).e("Denied Permissions:")
                    for (permission in deniedPermissions) {
                        Timber.tag(tag).e("\t$permission")
                    }
                } else {
                    bluetoothPermissionsGranted = true
                    checkBluetoothAdapter()
                    checkLocationService()
                }
            }
        }
    }

    /*******************************************
     * ACTIVITY RESULTS
     *******************************************/

    @RequiresApi(Build.VERSION_CODES.R)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BLUETOOTH -> {
                if( !bluetoothAdapter.isEnabled ) {
                    checkBluetoothAdapter()
                }
                else {
                    Timber.tag(tag).v("Bluetooth is enabled.")
                }
            }
            REQUEST_ENABLE_LOCATION -> {
                if(locationManager.isLocationEnabled) {
                    Timber.tag(tag).v("Location service is enabled.")
                }
            }
            REQUEST_STORAGE_PERMISSION -> {
                if (Environment.isExternalStorageManager()) {
                    Timber.tag(tag).v("Successfully granted storage permission.")
                    CSVProcessing.createDirectory()
                } else {
                    Timber.tag(tag).e("Failed to enable storage permission.")
                }
            }
        }
    }

    /*******************************************
     * Activity function overrides
     *******************************************/

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Timber.tag("DEVICE SDK").i("DEVICE SDK: ${Build.VERSION.SDK_INT}")
        Timber.tag(tag).v("[MAIN INITIALIZING]")

        /*******************************************
         * IMPORTANT: Check Permissions
         *******************************************/
        Timber.tag(tag).v("Checking permissions...")

        // Check bluetooth and location permissions
        checkAndRequestBluetoothPermissions()

        // Register Receivers (Check Bluetooth and Location states)
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(locationServiceReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

        // Check storage permission
        checkAndRequestStoragePermission()

        /*******************************************
         * Initialize Graph Fragments
         *******************************************/

        AppGlobals.graphOneFragment = GraphOneFragment()
        AppGlobals.graphTwoFragment = GraphTwoFragment()

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.GraphOneFragment, AppGlobals.graphOneFragment)
            commit()
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.GraphTwoFragment, AppGlobals.graphTwoFragment)
            commit()
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
                disable(binding.ConnectButton)
            }
            /** Behavior of APP when we disconnect **/
            else {
                Timber.tag(tag).v("[DISCONNECTED]")
                // Set disconnect state
                AppGlobals.connectionState = ConnectionState.DISCONNECTED

                // Show disconnection dialog
                showDisconnectedDialog()

                // Button Logic
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
            disable(it as Button)
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
            disable(button as Button)

            // Send Command to Devices to RUN
            ConnectionManager.sendStartCommand()
            if (AppGlobals.receivedAcknowledgement) {

                // CSV Logic
                CSVProcessing.openBuffer()
                AppGlobals.testCount++

                // Set Start Time
                AppGlobals.startProgramTime = System.currentTimeMillis()

                // Button Logic
                enable(binding.StopButton)

                // Set acknowledgement to false
                AppGlobals.receivedAcknowledgement = false
            } else {
                enable(button)
            }
        }

        /** STOP BUTTON **/
        binding.StopButton.setOnClickListener { button ->
            Timber.tag(tag).v("[STOP]")
           disable(button as Button)

            // Send Command to Devices to STOP
            ConnectionManager.sendStopCommand()
            if( AppGlobals.receivedAcknowledgement ) {
                // Calculate velocity
                calculateVelocity()
                showVelocityDialog()

                // Write to CSV
                CSVProcessing.writeToCSV()
                CSVProcessing.closeBuffer()

                // Reset objects and variables
                AppGlobals.stopDirective()

                // Button Logic
                enable(binding.StartButton)

                // Set acknowledgement to false
                AppGlobals.receivedAcknowledgement = false
            } else {
                enable(button)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        unregisterReceiver(locationServiceReceiver)
    }

    /*******************************************
     * Private functions
     *******************************************/

    private var scanTimeoutHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    private fun startScan() {
        Timber.tag(tag).v("Starting BLE scanning...")
        scanResults.clear()
        val scanFilters = mutableListOf(scanFilter)
        bleScanner?.startScan(scanFilters, scanSettings, scanCallback)

        scanTimeoutHandler.postDelayed({
            stopScan()
            showDeviceNotFoundDialog()
            enable(binding.ConnectButton)
        }, SCAN_PERIOD)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        Timber.tag(tag).v("Stopped BLE scan!")
        bleScanner?.stopScan(scanCallback)
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
        enable(binding.ConnectButton)
        enable(binding.SettingsButton)
        disable(binding.StartButton)
        disable(binding.StopButton)
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
     * Custom Dialog Alerts
     *******************************************/

    private fun showVelocityDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.velocity_layout, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        // Custom Background
        dialog.window?.setBackgroundDrawableResource(R.drawable.custom_dialog)

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

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.settings_layout, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        // Custom Background
        dialog.window?.setBackgroundDrawableResource(R.drawable.custom_dialog)

        // Layout Views
        val editTitle = dialogView.findViewById<EditText>(R.id.Input_Title)
        val editDist = dialogView.findViewById<EditText>(R.id.Input_Distance)
        val feetCheckbox = dialogView.findViewById<CheckBox>(R.id.Feet_CheckBox)
        val metersCheckbox = dialogView.findViewById<CheckBox>(R.id.Meters_CheckBox)
        val batteryViewProbe1 = dialogView.findViewById<TextView>(R.id.Battery_Probe_1)
        val batteryViewProbe2 = dialogView.findViewById<TextView>(R.id.Battery_Probe_2)
        val submit = dialogView.findViewById<Button>(R.id.Submit_Button)
        val back = dialogView.findViewById<Button>(R.id.Back_Button)
        val rotate = dialogView.findViewById<Button>(R.id.RotateButton)

        // Input Logic for All
        if( AppGlobals.deviceState == DeviceState.STOPPED && AppGlobals.connectionState == ConnectionState.DISCONNECTED ) {
            editTitle.isEnabled      = false
            editDist.isEnabled       = false
            feetCheckbox.isEnabled   = false
            metersCheckbox.isEnabled = false
        }
        else if( AppGlobals.deviceState == DeviceState.RUNNING && AppGlobals.connectionState == ConnectionState.CONNECTED ) {
            editTitle.isEnabled      = false
            editDist.isEnabled       = false
            feetCheckbox.isEnabled   = false
            metersCheckbox.isEnabled = false
            disable(rotate)
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

            if( (titleText.isNotEmpty() && distText.isNotEmpty()) && isFloat(distText) ) {
                submit.isEnabled = true
                submit.background.alpha = 255
            } else {
                submit.isEnabled = false
                submit.background.alpha = 64
            }
        }

        // Input Box Listeners (Only Enable if No Test is RUNNING)
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

        val batteryText1 = getString(R.string.Battery_Format, AppGlobals.batteryProbe1)
        val batteryText2 = getString(R.string.Battery_Format, AppGlobals.batteryProbe2)
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
        submit.background.alpha = 64
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
                AppGlobals.fileName = editTitle.text.toString().replace("\\s+".toRegex(), "")
                AppGlobals.distance = editDist.text.toString().toFloat()
                AppGlobals.testCount = 0

                if ( CSVProcessing.createFile() ) {
                    Timber.tag("Settings").v("Created the file successfully.")
                    if (CSVProcessing.openBuffer())  Timber.tag("Settings").v("File is open for writes.")
                    enable(binding.StartButton)
                }
                else {
                    Timber.tag("Settings").e("Failed to create the file.")
                }
            }
            else {
                if ( CSVProcessing.closeBuffer() ) {

                    // There is a file buffer open but close it and return true if successful
                    // Set parameters
                    AppGlobals.fileName = editTitle.text.toString().replace("\\s+".toRegex(), "")
                    AppGlobals.distance = editDist.text.toString().toFloat()
                    AppGlobals.testCount = 0

                    if ( CSVProcessing.createFile() ) {
                        Timber.tag("Settings").v("Created the file successfully.")
                        if (CSVProcessing.openBuffer())  Timber.tag("Settings").v("File Buffer OPENED!")
                        enable(binding.StartButton)
                    }
                    else {
                        Timber.tag("Settings").e("Failed to create the file.")
                    }
                }
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
            val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("No device found")
                .setMessage("Could not find the specified device. " +
                        "Please ensure the devices are powered on and try again.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

            // Change color of the button
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        }
    }

    private fun showDisconnectedDialog() {
        runOnUiThread {
            val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Disconnected")
                .setMessage("The device disconnected unexpectedly from the app. "
                        + "Please connect to the device again. "
                        + "If this occurred during a running test, please power cycle the devices.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

            // Change color of the button
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        }
    }

    private fun showScanFailedDialog() {
        runOnUiThread {
            val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Scanning error")
                .setMessage("Scanning procedure failed unexpectedly. Please try again.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _,_ -> /* NOP */ }
                .show()

            // Change color of the button
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        }
    }

    private fun showOrientationChangeDialog() {
        runOnUiThread {
            val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Orientation change")
                .setMessage("WARNING: Changing the orientation will restart the app and any tests!")
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { _,_ ->
                    changeOrientation()
                    AppGlobals.resetDirective()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()

            // Change color of the button
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
        }
    }

    @SuppressLint("InflateParams")
    @Deprecated("Deprecated in Java")
    private fun showCustomToast(context: Context, message: String) {
        val layoutInflater: LayoutInflater = LayoutInflater.from(context)
        val layout: View = layoutInflater.inflate(R.layout.toast_layout, null)

        val textView: TextView = layout.findViewById(R.id.Toast_Text)
        textView.text = message

        val toast = Toast(context)
        toast.setGravity(Gravity.TOP, 0, 96)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }

    private fun enable(button: Button) {
        button.isEnabled = true
        button.background.alpha = 255
    }

    private fun disable(button: Button) {
        button.isEnabled = false
        button.background.alpha = 64
    }
}