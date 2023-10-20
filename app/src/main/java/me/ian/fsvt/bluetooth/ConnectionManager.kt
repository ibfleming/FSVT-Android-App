package me.ian.fsvt.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.UUID

private val BLUETOOTH_LE_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
private val BLUETOOTH_LE_CHAR_RW = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

object ConnectionManager {

    private const val tag = "ConnectionManager"
    private val handler = Handler(Looper.getMainLooper())
    private var receivedAcknowledgement : Boolean = false

    /*******************************************
     * Properties
     *******************************************/

    private var bluetoothGatt: BluetoothGatt? = null   // Make this nullable in events the device disconnects
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var hm10Delegate: DeviceDelegate? = null

    /*
    This variable is set based if the device that is being
    connected established connection with the characteristics successfully.
    Using LiveData.
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
                    handleUnexpectedDisconnect()
                }
            } else {
                Timber.e("Failure to connect to Gatt!")
                Timber.e("Device disconnected!")
                handleUnexpectedDisconnect()
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
                handleUnexpectedDisconnect()
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("Characteristic write successful!")
            } else {
                Timber.e("Characteristic write failed with status: $status")
            }
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        /*******************************************
         * On Change
         *******************************************/

        val probe1Data = MutableLiveData<Float>()
        val probe2Data = MutableLiveData<Float>()

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if( characteristic === readCharacteristic) {
                val data = readCharacteristic!!.value
                val msg = String(data.map { it.toInt().toChar() }.toCharArray())

                when (msg) {
                    "A" -> {
                        Timber.w( "[ACKNOWLEDGE] -> '$msg'")
                        receivedAcknowledgement = true
                    }
                    else -> {
                        Timber.d( "[TDS] -> $msg")
                        // Process data...
                        processData(msg)
                    }
                }
            }
        }
    }

    private fun handleUnexpectedDisconnect() {
        bluetoothGatt = null
        readCharacteristic = null
        writeCharacteristic = null
        hm10Delegate = null
        _isConnected.postValue(false)
    }

    /*******************************************
     * Process the Data from BLE Device
     *******************************************/

    val probe1Data = MutableLiveData<Float>()
    val probe2Data = MutableLiveData<Float>()

    private fun processData(data : String) {
        val values = data.split(":").map { it.trim() }
        if(values.size == 2) {
            try {
                val p1Value = values[0].toFloat()
                val p2Value = values[1].toFloat()

                // CSV IMPLEMENTATIONS HERE -> ADD TO FILE?
                probe1Data.postValue(p1Value)
                probe2Data.postValue(p2Value)

            } catch (e: NumberFormatException) {
                Timber.e("Error parsing data: $data")
            }
        }
        else {
            Timber.e("Invalid data format: $data")
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

    private const val MAX_ATTEMPTS = 3
    private const val TIMEOUT_DURATION = 100L

    fun sendStopCommand() {
        var attempts = 0

        fun keepSendingStop() {

            if (attempts < MAX_ATTEMPTS) {
                writeCommand('E')

                handler.postDelayed({
                    if (receivedAcknowledgement) {
                        // Acknowledgement received
                        receivedAcknowledgement = false
                    } else {
                        // No acknowledgment received, retry...
                        attempts++
                        keepSendingStop()
                    }
                }, TIMEOUT_DURATION)
            } else {
                // Maximum attempts reached, handle error or timeout
            }
        }
        keepSendingStop()
    }

    /*******************************************
     * Disconnect
     *******************************************/

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _isConnected.postValue(false)
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