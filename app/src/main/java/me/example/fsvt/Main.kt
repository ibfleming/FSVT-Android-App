package me.example.fsvt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.StringBuilder

// MAC ADDRESS OF HM10: B0:D2:78:32:F5:7F

@SuppressLint("MissingPermission")
class Main : ComponentActivity() {

    // Bluetooth Connection STATUS
    private val status: Boolean = false

    // 1) Create U.I. Objects
    private lateinit var bStart : Button
    private lateinit var tvStatus : TextView
    private lateinit var tvList : TextView

    // 2) Create Bluetooth Objects
    private lateinit var bleManager : BLEManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner : BLEScanner

    private val requestEnableBTLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if( result.resultCode == RESULT_OK ) {
                // Bluetooth is enabled, start scanning
                startBLEScan()
            } else {
                // Bluetooth is disabled
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        // 3) Fetch Object References
        bStart = findViewById(R.id.InitButton)
        tvStatus = findViewById(R.id.BLE_STATUS)
        tvList = findViewById(R.id.BLE_LIST)

        // 4) Fetch B.T. Object References
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        bleScanner = BLEScanner(bluetoothAdapter)
        bleManager = BLEManager(this)

        // ... Respectively, the Start button will be pressed...
        bStart.setOnClickListener{ _ ->
            checkBluetoothStatus()
        }
    }

    private fun checkBluetoothStatus() {
        if(!bluetoothAdapter.isEnabled) {
            requestEnableBluetooth()
        } else {
            startBLEScan()
        }
    }

    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestEnableBTLauncher.launch(enableBtIntent)
    }

    private fun startBLEScan() {
        bleScanner.startScan { scanResults ->
            // Handle the list of scan results here
            val deviceList = StringBuilder() // <--- FOR DEBUGGING
            for( result in scanResults) {
                val device = result.device
                // Process device information as needed
                deviceList.append("\n" + device.name + "\n")

                // To connect to the device... (or GATT)
                //bleManager.connectToDevice(device)
                //status = true
            }
            tvList.text = deviceList.toString()
            tvStatus.setText(R.string.ui_on_status)
        }

        lifecycleScope.launch {
            delay(SCAN_PERIOD)
            bleScanner.stopScan()
        }
    }

    companion object {
        private const val SCAN_PERIOD: Long = 10000 // 10sec
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

