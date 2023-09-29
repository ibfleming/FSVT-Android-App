package me.example.fsvt

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.TextView
import java.util.UUID

// MAC ADDRESS OF HM10: B0:D2:78:32:F5:7F

@SuppressLint("MissingPermission")

class BLEManager(private val context: Context)
{

    private val tag = "BLE Manager"
    private val serviceUUID = "0000FFE0-0000-1000-8000-00805F9B34FB"
    private val characteristicUUID = "0000FFE1-0000-1000-8000-00805F9B34FB"

    private lateinit var tvStatus : TextView
    private var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    fun connectToDevice(device: BluetoothDevice, textView: TextView) {
        tvStatus = textView
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if( newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(tag, "Connected to GATT server.")
                // Discover services
                gatt?.discoverServices()
            } else if ( newState == BluetoothProfile.STATE_DISCONNECTED ) {
                Log.i(tag, "Disconnect from GATT server.")
                // Handle disconnection
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Services discovered, you can now work with them

                val service = gatt?.getService(UUID.fromString(serviceUUID))
                val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUUID))

                gatt?.readCharacteristic(characteristic)
            } else {
                Log.w(tag, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if( status == BluetoothGatt.GATT_SUCCESS ) {
                val data = characteristic.value
            }
        }

        // Add other overrides as needed
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

}