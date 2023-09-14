package me.example.fsvt

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.text.StringBuilder

class MainActivity : ComponentActivity() {

    private val logTag = "IAN"

    // 1) Create Objects
    private lateinit var B_Start : Button
    private lateinit var B_Stop : Button
    private lateinit var B_Show_Devices : Button
    private lateinit var TV_Devices : TextView
    private var bondedDevices : Set<BluetoothDevice>? = null

    // 2) Declare Constant of B.T. Class
    private val REQUEST_ENABLE_BLUETOOTH = 2
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        if(!bluetoothAdapter.isEnabled) {

            val enableBTLauncher: ActivityResultLauncher<Intent> =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if( result.resultCode == Activity.RESULT_OK ) {
                        // Bluetooth was enabled by the user manually
                        Log.i(logTag, "ACTIVITY: Bluetooth has been enabled")
                    } else {
                        // User decided not to enable Bluetooth
                        Log.i(logTag, "ACTIVITY: Bluetooth was not enabled")
                    }
                }

            val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBTLauncher.launch(enableBTIntent)
        }
        else {
            scanLeDevice()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {

        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        if(!scanning) {
            GlobalScope.launch {
                delay(SCAN_PERIOD)
                scanning = false
                bluetoothLeScanner.stopScan(scanCallback)
            }
            scanning = true
            bluetoothLeScanner.startScan(scanCallback)
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(scanCallback)
        }
    }*/

        // 3) Fetch Object References
        B_Start = findViewById(R.id.BT_ON)
        B_Stop = findViewById(R.id.BT_OFF)
        B_Show_Devices = findViewById(R.id.BT_SHOW_LIST)
        TV_Devices = findViewById(R.id.BT_DEVICE_LIST)

        // 4) Create BluetoothAdapter class
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        // 5) Check if adapter is null? -> Device supports Bluetooth?

        // 6) Enable Bluetooth

        // 6.1) Register Activity
        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if( result.resultCode == Activity.RESULT_OK ) {
                // User enabled Bluetooth
            } else {
                // The user did not enable Bluetooth
            }
        }

        B_Start.setOnClickListener{ _ ->
            if( !bluetoothAdapter.isEnabled ) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBTIntent)
            }
        }

        // 7) Disable Bluetooth

        B_Stop.setOnClickListener{ _ ->
            if( bluetoothAdapter.isEnabled ) {
                bluetoothAdapter.disable()  // THIS (CAN) WORKS!
            }
        }

        // 8) Show list of bonded devices
        B_Show_Devices.setOnClickListener{ _ ->
            val sb = StringBuilder()
            bondedDevices = bluetoothAdapter.bondedDevices
            for( temp in (bondedDevices as MutableSet<BluetoothDevice>?)!!) {
                sb.append("\n" + temp.name + "\n")
            }

            TV_Devices.text = sb.toString()
        }

    }
}