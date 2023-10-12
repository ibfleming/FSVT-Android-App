package me.example.fsvt

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.widget.TextView
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

// MAC ADDRESS OF HM10: B0:D2:78:32:F5:7F

@SuppressLint("MissingPermission")

class BLEManager(private var context: Context)
{
    private open class DeviceDelegate {
        open fun connectCharacteristics(service: BluetoothGattService) : Boolean { return true }
        //open fun onDescriptorWrite(g: BluetoothGatt?, d: BluetoothGattDescriptor?, status: Int) { /*nop*/ }
        //open fun onCharacteristicChanged(g: BluetoothGatt?, c: BluetoothGattCharacteristic?) { /*nop*/ }
        //open fun onCharacteristicWrite(g: BluetoothGatt?, c: BluetoothGattCharacteristic?, status: Int) { /*nop*/ }
        //open fun canWrite(): Boolean { return true }
        //open fun disconnect() { /*nop*/ }
    }

    private var delegate: DeviceDelegate? = null
    private var callback: BLECallback? = null

    private var writeBuffer: ArrayList<ByteArray>? = null
    private val commandQueue : Queue<ByteArray> = LinkedList()
    private var isConnected = false

    companion object {
        private const val tag = "BLE Manager"

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

    fun setCallback(callback: BLECallback) {
        this.callback = callback
    }

    fun connectToDevice(device : BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        // (1) Are we connected?
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

        // (2) Since we are connected, what services does the BLE have?
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

        //
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            if (characteristic === readCharacteristic) {
                @Suppress("DEPRECATION") val data = readCharacteristic!!.value
                val charArray = CharArray(data.size) { data[it].toInt().toChar() }
                val receivedString = String(charArray)
                Log.d(tag, "Read, content = $receivedString")

                callback?.onDataReceived(receivedString)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic === writeCharacteristic) {
                Log.d(tag, "write finished, status = $status")
                synchronized(commandQueue) {
                    commandQueue.poll()
                    if( commandQueue.isNotEmpty() ) {
                        writeNextCommand()
                    }
                }
            }
        }
    }

    fun write(data: ByteArray) {
        synchronized(commandQueue) {
            commandQueue.offer(data)
            if( commandQueue.size == 1 ) {
                writeNextCommand()
            }
        }
    }

    private fun writeNextCommand() {
        synchronized(commandQueue) {
            val data = commandQueue.peek()
            @Suppress("DEPRECATION")
            writeCharacteristic?.value = data
            @Suppress("DEPRECATION")
            if(!bluetoothGatt!!.writeCharacteristic(writeCharacteristic)) {
                Log.d(tag, "FAILED TO WRITE COMMAND")
            } else {
                Log.d(tag, "Write started, size = " + data?.size)
            }
        }
    }

    // (3) We have services, let's connect to them.
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

    /**
     *  This class caters specifically to the specific service profile our HM-10 uses.
     */
    private class CC254xDelegate : DeviceDelegate() {
        override fun connectCharacteristics(service: BluetoothGattService) : Boolean {
            Log.d(tag, "Service: CC254x UART")
            readCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CHAR_RW)
            writeCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CHAR_RW)
            if( readCharacteristic != null ) {
                if( bluetoothGatt?.setCharacteristicNotification(readCharacteristic, true)!! ) {
                    Log.i(tag, "HAS NOTIFICATION FOR READ!")
                }
            }
            return true
        }
    }

    fun isDeviceConnected() : Boolean {
        return isConnected
    }

    /* Don't see any software use for this...
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
    */
}
