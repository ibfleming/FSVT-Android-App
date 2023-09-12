package me.example.fsvt

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign.Companion
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.example.fsvt.ui.theme.FVSTAppTheme
import java.util.logging.Logger

class MainActivity : ComponentActivity() {

    private val logTag = "IAN"

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var scanning = false

    // Scan for 10 seconds
    private val SCAN_PERIOD: Long = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FVSTAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Test")
                }
            }
        }

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
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier,
        textAlign = Companion.Center,
        fontFamily = FontFamily.Monospace
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FVSTAppTheme {
        Greeting("Android")
    }
}