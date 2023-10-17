package me.ian.fsvt.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import timber.log.Timber
import java.util.UUID

private val BLUETOOTH_LE_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
private val BLUETOOTH_LE_CHAR_RW = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

object ConnectionManager {

    /*******************************************
     * Properties
     *******************************************/

    private var bluetoothGatt: BluetoothGatt? = null   // Make this nullable in events the device disconnects
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var hm10Delegate: DeviceDelegate? = null

    /*
    This variable is set based if the device that is being
    connected established connection with the characteristics successfully
    */
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> get() = _isConnected


    /*******************************************
     * Connecting and Callback
     *******************************************/

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, context: Context) {
        device.connectGatt(context, false, callback)
    }

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Timber.w("Successfully connected!")
                    bluetoothGatt = gatt

                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt!!.discoverServices()
                    }

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Timber.e("Device disconnected!")
                    _isConnected.postValue(false)
                }
            } else {
                Timber.e("Failure to connect to Gatt!")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if( status == BluetoothGatt.GATT_SUCCESS ) {
                Timber.w("Discovered services successfully!")
                if (gatt != null) {
                    connectCharacteristics(gatt)
                }
            }
            else {
                Timber.e("onServicesDiscovered received $status")
            }
        }


        /*******************************************
         * On Read
         *******************************************/

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
        }

        /*******************************************
         * On Write
         *******************************************/

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        /*******************************************
         * On Change
         *******************************************/

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            /** NOT WORKING **/
            if( characteristic === readCharacteristic ) {
                val data = String(readCharacteristic!!.value.map { it.toInt().toChar() }.toCharArray())
                Timber.d("[READ] -> $data")
            }
            TODO("READ FROM BLE")
        }
    }

    /*******************************************
     * Writing to BLE Functions
     *******************************************/

    @SuppressLint("MissingPermission")
    private fun writeCommand(command: Char) {
        writeCharacteristic?.let { characteristic ->
            val packet = byteArrayOf(command.code.toByte())
            characteristic.value = packet
            bluetoothGatt?.writeCharacteristic(characteristic)
        }
    }

    fun sendStartCommand() {
        writeCommand('S')
    }

    fun sendEndCommand() {
        writeCommand('E')
    }

    /*******************************************
     * Disconnect
     *******************************************/

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null

    }

    /*******************************************
     * Connect to Characteristics
     *******************************************/

    private fun connectCharacteristics(gatt: BluetoothGatt) {
        for( service in gatt.services ) {
            when( service.uuid ) {
                BLUETOOTH_LE_SERVICE -> {
                    hm10Delegate = OurHM10Device()
                    (hm10Delegate as OurHM10Device).connectCharacteristics(service)
                }
            }
        }
        if( hm10Delegate == null ) {
            Timber.e("Connecting to characteristics failed!")
        }
    }

    private open class DeviceDelegate {
        open fun connectCharacteristics(service: BluetoothGattService) { /* nop */ }
    }

    private class OurHM10Device : DeviceDelegate() {
        @SuppressLint("MissingPermission")
        override fun connectCharacteristics(service: BluetoothGattService) {
            Timber.w("Service: CC254x UART (our HM-10 module)")
            readCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CHAR_RW)
            writeCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CHAR_RW)
            if( readCharacteristic != null && writeCharacteristic != null ) {
                if( bluetoothGatt?.setCharacteristicNotification(readCharacteristic, true)!! ) {
                    Timber.w("The read characteristic has notification enabled...")
                    _isConnected.postValue(true)
                }
            }
        }
    }
}