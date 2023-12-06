package me.ian.fsvt.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import me.ian.fsvt.AppGlobals
import me.ian.fsvt.DeviceState
import me.ian.fsvt.MainActivity
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
    private var viewModel : GraphDataViewModel = AppGlobals.graphDataViewModel

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

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when {
                status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED -> {
                    Timber.v("DEVICE CONNECTED")
                    bluetoothGatt = gatt

                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt!!.discoverServices()
                    }
                }
                status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.v("DEVICE DISCONNECTED")
                    handleDisconnect()
                    AppGlobals.resetDirective()
                }
                status != BluetoothGatt.GATT_SUCCESS -> {
                    Timber.e("FAILED TO CONNECT TO GATT (status = $status)")
                    handleDisconnect()
                    AppGlobals.resetDirective()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if( status == BluetoothGatt.GATT_SUCCESS ) {
                Timber.v("DISCOVERED SERVICES")
                if (gatt != null) {
                    connectCharacteristics(gatt)
                }
            }
            else {
                Timber.e("FAILED TO DISCOVER SERVICES (status = $status)")
                handleDisconnect()
                AppGlobals.resetDirective()
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
                Timber.v("WRITE SUCCESS")
            } else {
                Timber.e("WRITE FAILED (status = $status)")
            }
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
                        receivedAcknowledgement = true
                    }
                    else -> {
                        Timber.d( "[DATA STREAM] = '$msg'")
                        processData(msg)
                    }
                }
            }
        }
    }

    fun handleDisconnect() {
        disconnect()
        readCharacteristic      = null
        writeCharacteristic     = null
        hm10Delegate            = null
        receivedAcknowledgement = false
        if( viewModel.isConnected.value == true ) {
            viewModel.setConnectionStatus(false)
        }
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
                    Timber.e("ERROR PARSING DATA (data = $data)")
                }
            }
        }
        else if ( data.contains("V") ) {
            // READING BATTERY DATA
            val values = data.split("V").map { it.trim() }
            if(values.size == 2) {
                try {
                    val b1Value = values[0].toFloat()
                    val b2Value = values[1].toFloat()
                    Timber.v("Battery: $b1Value, $b2Value")
                    AppGlobals.batteryProbe1 = checkBatteryLevel(b1Value)
                    AppGlobals.batteryProbe2 = checkBatteryLevel(b2Value)
                } catch (e: NumberFormatException) {
                    Timber.e("ERROR PARSING DATA (data = $data)")
                }
            }
        }
        else {
            Timber.e("INVALID DATA (data = $data)")
        }
    }

    private fun checkBatteryLevel(level: Float): Int {
        if (level >= 3.5) {
            return 100
        } else if (level >= 2.5) {
            return ((level - 2.5) / 0.1).toInt() * 10
        }
        return 0
    }

    /*******************************************
     * Writing to BLE Functions
     *******************************************/

    @SuppressLint("MissingPermission")
    private fun writeCommand(command: Char) {
        writeCharacteristic?.let { characteristic ->
            val packet = byteArrayOf(command.code.toByte())
            characteristic.value = packet
            if (bluetoothGatt?.writeCharacteristic(characteristic) == true) {
                Timber.d("RETURN TRUE ON WRITE SUCCESS!")
            }
        }
    }

    private const val MAX_ATTEMPTS = 25
    private const val TIMEOUT_DURATION = 15L

    fun sendStartCommand() {
        var attempts = 0

        fun keepSendingStart() {
            if (attempts < MAX_ATTEMPTS) {
                writeCommand('S')

                handler.postDelayed({
                    if (receivedAcknowledgement) {
                        // Acknowledgement received
                        Timber.v( "[START ACKNOWLEDGE]")
                        AppGlobals.deviceState = DeviceState.RUNNING
                        receivedAcknowledgement = false
                        return@postDelayed
                    } else {
                        // No acknowledgment received, retry...
                        attempts++
                        keepSendingStart()
                    }
                }, TIMEOUT_DURATION)
            } else {
                Timber.e("No acknowledgment received after $MAX_ATTEMPTS attempts for START.")
            }
        }
        keepSendingStart()
    }

    fun sendStopCommand() {
        var attempts = 0

        fun keepSendingStop() {
            if (attempts < MAX_ATTEMPTS) {
                writeCommand('E')

                handler.postDelayed({
                    if (receivedAcknowledgement) {
                        // Acknowledgement received
                        Timber.v( "[STOP ACKNOWLEDGE]")
                        AppGlobals.deviceState = DeviceState.STOPPED
                        receivedAcknowledgement = false
                        return@postDelayed
                    } else {
                        // No acknowledgment received, retry...
                        attempts++
                        keepSendingStop()
                    }
                }, TIMEOUT_DURATION)
            } else {
                Timber.e("No acknowledgment received after $MAX_ATTEMPTS attempts for STOP.")
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