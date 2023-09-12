package me.example.fsvt

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
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
import me.example.fsvt.ui.theme.FVSTAppTheme

class MainActivity : ComponentActivity() {

    private val logTag = "MY LOGGING"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FVSTAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //Greeting("Test")
                }
            }
        }

        val bluetoothManager : BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter : BluetoothAdapter? = bluetoothManager.adapter

        // Check to see if the Bluetooth classic feature is available.
        val bluetoothAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        // Check to see if the BLE feature is available.
        val bluetoothLEAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

        if(bluetoothAvailable) {
            Log.i(logTag, "INFO: HAS BLUETOOTH CLASSIC")
        }
        if(bluetoothLEAvailable) {
            Log.i(logTag, "INFO: HAS BLUETOOTH LOW ENERGY")
        }

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

        if( bluetoothAdapter != null ) {
            Log.i(logTag, "INFO: DEVICE HAS BLUETOOTH ADAPTER")
            if(!bluetoothAdapter.isEnabled) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBTLauncher.launch(enableBTIntent)
            }
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