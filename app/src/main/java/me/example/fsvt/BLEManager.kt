package me.example.fsvt

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
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
    private var bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var bluetoothGatt: BluetoothGatt? = null

    private lateinit var device : BluetoothDevice
    private lateinit var service : BluetoothGattService
    private lateinit var characteristic : BluetoothGattCharacteristic
    private var isConnected = false

    fun connectToDevice(device: BluetoothDevice, textView: TextView) {
        this.device = device
        tvStatus = textView
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if( newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(tag, "Connected to GATT server.")
                isConnected = true
                gatt?.discoverServices()
            } else if ( newState == BluetoothProfile.STATE_DISCONNECTED ) {
                Log.i(tag, "Disconnect from GATT server.")
                isConnected = false
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                Log.i(tag, "Discovered services successfully!")
                // Services discovered, you can now work with them

                service = gatt?.getService(UUID.fromString(serviceUUID))!!
                Log.i(tag, service.toString())
                characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID))!!
                Log.i(tag, service.includedServices.toString())
                Log.i(tag, characteristic.toString())

            } else {
                Log.i(tag, "onServicesDiscovered received: $status")
            }
        }

        // Add other overrides as needed
    }

    fun writeCommandToBLE(command : String) : Boolean {
        val success = characteristic.setValue(command.toByteArray())

        return if(success) {
            val writeSuccess = bluetoothGatt?.writeCharacteristic(characteristic)
            Log.i(tag, "Write operation success: $writeSuccess")
            true
        } else {
            Log.i(tag, "Failed to set characteristic value.")
            false
        }
    }

    fun isDeviceConnected() : Boolean {
        return isConnected
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

}
