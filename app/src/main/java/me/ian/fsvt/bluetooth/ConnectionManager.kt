package me.ian.fsvt.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import me.ian.fsvt.DeviceState
import me.ian.fsvt.MyObjects
import me.ian.fsvt.graph.GraphDataViewModel
import timber.log.Timber
import java.util.UUID

private val bluetoothService = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
private val bluetoothCharRW  = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

object ConnectionManager {

    /*******************************************
     * Properties
     *******************************************/

    private val handler = Handler(Looper.getMainLooper())
    private var viewModel : GraphDataViewModel = MyObjects.graphDataViewModel

    private var bluetoothGatt           : BluetoothGatt? = null
    private var readCharacteristic      : BluetoothGattCharacteristic? = null
    private var writeCharacteristic     : BluetoothGattCharacteristic? = null
    private var hm10Delegate            : DeviceDelegate? = null
    private var receivedAcknowledgement : Boolean = false

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
                handleUnexpectedDisconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if( status == BluetoothGatt.GATT_SUCCESS ) {
                Timber.v("Discovered services successfully!")
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
         * On Write
         *******************************************/

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.v("[CHARACTERISTIC WRITE SUCCESS]")
            } else {
                Timber.e("Characteristic write failed with status: $status")
            }
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        /*******************************************
         * On Change
         *******************************************/

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if( characteristic === readCharacteristic) {
                val data = readCharacteristic!!.value

                when (val msg = String(data.map { it.toInt().toChar() }.toCharArray())) {
                    "A" -> {
                        Timber.v( "[READ ACKNOWLEDGE] -> '$msg'")
                        receivedAcknowledgement = true
                    }
                    else -> {
                        Timber.d( "[READ DATA] -> '$msg'")
                        processData(msg)
                    }
                }
            }
        }
    }

    private fun handleUnexpectedDisconnect() {
        bluetoothGatt           = null
        readCharacteristic      = null
        writeCharacteristic     = null
        hm10Delegate            = null
        receivedAcknowledgement = false
        viewModel.setConnectionStatus(false)
    }

    /*******************************************
     * Process the Data from BLE Device
     *******************************************/
    private fun processData(data : String) {
        if( data.contains(":") ) {
            // READING TDS DATA
            val values = data.split(":").map { it.trim() }
            if(values.size == 2) {
                try {
                    val p1Value = values[0].toFloat()
                    val p2Value = values[1].toFloat()
                    viewModel.updateGraphs(p1Value, p2Value)
                } catch (e: NumberFormatException) {
                    Timber.e("Error parsing data: $data")
                }
            }
        }
        else if ( data.contains("V") ) {
            // READING BATTERY DATA
            val values = data.split("V").map { it.trim() }
            if(values.size == 2) {
                // PROCESS BATTERY DATA
            }
        }
        else {
            Timber.e("INVALID DATA OVER BLUETOOTH -> $data")
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
        MyObjects.deviceState = DeviceState.RUNNING
    }

    private const val MAX_ATTEMPTS = 25
    private const val TIMEOUT_DURATION = 15L

    fun sendStopCommand() {
        var attempts = 0

        fun keepSendingStop() {
            if (attempts < MAX_ATTEMPTS) {
                writeCommand('E')

                handler.postDelayed({
                    if (receivedAcknowledgement) {
                        // Acknowledgement received
                        MyObjects.deviceState = DeviceState.STOPPED
                        receivedAcknowledgement = false
                    } else {
                        // No acknowledgment received, retry...
                        attempts++
                        keepSendingStop()
                    }
                }, TIMEOUT_DURATION)
            } else {
                Timber.e("No acknowledgment received after $MAX_ATTEMPTS attempts.")
            }
        }
        keepSendingStop()
    }

    /*******************************************
     * Connect to Characteristics
     *******************************************/

    private fun connectCharacteristics(gatt: BluetoothGatt) {
        for( service in gatt.services ) {
            when( service.uuid ) {
                bluetoothService -> {
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
            readCharacteristic = service.getCharacteristic(bluetoothCharRW)
            writeCharacteristic = service.getCharacteristic(bluetoothCharRW)
            if( readCharacteristic != null && writeCharacteristic != null ) {
                if( bluetoothGatt?.setCharacteristicNotification(readCharacteristic, true)!! ) {
                    Timber.w("The read characteristic has notification enabled...")
                    viewModel.setConnectionStatus(true)
                }
            }
        }
    }
}