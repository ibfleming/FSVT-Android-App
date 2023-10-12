package me.example.fsvt

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.widget.TextView
import java.util.UUID

// MAC ADDRESS OF HM10: B0:D2:78:32:F5:7F

@SuppressLint("MissingPermission")

class BLEManager(private var context: Context)
{
    private open class DeviceDelegate {
        open fun connectCharacteristics(service: BluetoothGattService) : Boolean { return true }
        open fun onDescriptorWrite(g: BluetoothGatt?, d: BluetoothGattDescriptor?, status: Int) { /*nop*/ }
        open fun onCharacteristicChanged(g: BluetoothGatt?, c: BluetoothGattCharacteristic?) { /*nop*/ }
        open fun onCharacteristicWrite(g: BluetoothGatt?, c: BluetoothGattCharacteristic?, status: Int) { /*nop*/ }
        open fun canWrite(): Boolean { return true }
        open fun disconnect() { /*nop*/ }
    }

    private lateinit var tvStatus : TextView
    private var delegate: DeviceDelegate? = null

    private var writeBuffer: ArrayList<ByteArray>? = null

    private lateinit var service : BluetoothGattService

    private var isConnected = false

    companion object {
        private const val tag = "BLE Manager"

        private val BLUETOOTH_LE_CCCD =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val BLUETOOTH_LE_SERVICE =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val BLUETOOTH_LE_CHAR_RW =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

        private var bluetoothGatt: BluetoothGatt? = null
        private var readCharacteristic: BluetoothGattCharacteristic? = null
        private var writeCharacteristic: BluetoothGattCharacteristic? = null
    }

    init {
        writeBuffer = ArrayList()
    }

    fun connectToDevice(device : BluetoothDevice, textView: TextView) {
        tvStatus = textView
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if( newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(tag, "Connected to GATT server.")
                isConnected = true
                gatt?.discoverServices()
            } else if ( newState == BluetoothProfile.STATE_DISCONNECTED ) {
                Log.i(tag, "Disconnected from GATT server.")
                isConnected = false
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(tag, "Discovered services successfully!")
                if (gatt != null) {
                    connectCharacteristics(gatt)
                }
            } else {
                Log.i(tag, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            if (characteristic === readCharacteristic) {
                val data = readCharacteristic!!.value
                val charArray = CharArray(data.size) { data[it].toInt().toChar() }
                val receivedString = String(charArray)
                Log.d(tag, "Read, content = $receivedString")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic === writeCharacteristic) {
                Log.d(tag, "write finished, status = $status")
            }
        }
    }

    fun write(data: ByteArray) {
        writeCharacteristic?.value = data
        if( !bluetoothGatt!!.writeCharacteristic(writeCharacteristic) ) {
            Log.d(tag, "Failed to write!")
        }
        else {
            Log.d(tag, "Write started, size = " + data.size)
        }
    }

    private fun connectCharacteristics(gatt: BluetoothGatt? ) {
        var sync = true
        if (gatt != null) {
            for( gattService in gatt.services ) {
                when( gattService.uuid ) {
                    BLUETOOTH_LE_SERVICE -> {
                        delegate = CC254xDelegate()
                        sync = delegate!!.connectCharacteristics(gattService)
                    }
                }
               if( delegate != null && !sync) {
                   break
               }
            }
            if( delegate == null ) {
                Log.d(tag, "Connecting to Characteristics failed!")
            }
        }
    }

    private class CC254xDelegate : DeviceDelegate() {
        override fun connectCharacteristics(service: BluetoothGattService) : Boolean {
            Log.d(tag, "Service: CC254x UART")
            readCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CHAR_RW)
            writeCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CHAR_RW)
            if( readCharacteristic != null ) {
                if( bluetoothGatt?.setCharacteristicNotification(readCharacteristic, true)!! ) {
                    Log.i(tag, "HAS NOTIFICATION FOR READ!")
                }
                /*
                val descriptor = readCharacteristic!!.getDescriptor(BLUETOOTH_LE_CHAR_RW)
                if( descriptor != null ) {
                    Log.i(tag, "HAS CCCD DESCRIPTOR FOR READ")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    if(bluetoothGatt?.writeDescriptor(descriptor)!!) {
                        Log.i(tag, "DESCRIPTOR IS WRITABLE")
                    }
                }
                */
            }
            return true
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
