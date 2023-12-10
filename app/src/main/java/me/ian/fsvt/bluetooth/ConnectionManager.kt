package me.ian.fsvt.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import me.ian.fsvt.AppGlobals
import me.ian.fsvt.DeviceState
import me.ian.fsvt.graph.GraphDataViewModel
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.CompletableFuture

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
         * On Change
         *******************************************/

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            readCharacteristic?.let { readChar ->
                readChar.value?.let { data ->
                    val msg = String(data.map { it.toInt().toChar() }.toCharArray())

                    when(msg) {
                        "A" -> {
                            Timber.tag("BLUETOOTH DEVICE").v("Read acknowledgement.")
                            receivedAcknowledgement = true
                        }
                        else -> {
                            Timber.tag("BLUETOOTH DEVICE").v("Read: '$msg'")
                            processData(msg)
                        }
                    }
                }
            } ?: run {
                Timber.e("Read characteristic is null.")
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
        return when {
            level >= 2.28 -> 100
            level >= 2.21 -> 90
            level >= 2.15 -> 80
            level >= 2.08 -> 70
            level >= 2.02 -> 60
            level >= 1.95 -> 50
            level >= 1.89 -> 40
            level >= 1.82 -> 30
            level >= 1.76 -> 20
            level >= 1.69 -> 10
            else -> 0
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

            bluetoothGatt?.let { gatt->
                if(gatt.writeCharacteristic(characteristic)) {
                    Timber.d("Writing '$command' to device.")
                } else {
                    Timber.e("Failed writing '$command' to device.")
                }
            }?: run {
                Timber.e("Bluetooth gatt is null.")
            }
        } ?: run {
            Timber.e("Write characteristic is null.")
        }
    }

    private const val PERIOD_DURATION = 10L
    private const val TASK_DURATION = 6000L

    @RequiresApi(Build.VERSION_CODES.N)
    fun sendCommand(command: Char): CompletableFuture<Boolean> {
        val returnAcknowledgement = CompletableFuture<Boolean>()

        val sendCommandRunnable = object : Runnable {
            override fun run() {
                writeCommand(command)
                if (receivedAcknowledgement) {
                    receivedAcknowledgement = false
                    returnAcknowledgement.complete(true)
                    handler.removeCallbacks(this)
                } else {
                    handler.postDelayed(this, PERIOD_DURATION)
                }
            }
        }

        handler.postDelayed(sendCommandRunnable, 0)

        handler.postDelayed({
            if (!returnAcknowledgement.isDone) {
                returnAcknowledgement.complete(false)
                handler.removeCallbacks(sendCommandRunnable)
            }
        }, TASK_DURATION)

        return returnAcknowledgement
    }

    /*
    @RequiresApi(Build.VERSION_CODES.N)
    fun sendStartCommand() : CompletableFuture<Boolean> {
        val receivedAcknowledge = CompletableFuture<Boolean>()
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
                        receivedAcknowledge.complete(true)
                    } else {
                        // No acknowledgment received, retry...
                        attempts++
                        keepSendingStart()
                    }
                }, TIMEOUT_DURATION)
            } else {
                keepSendingStart()
                Timber.e("No acknowledgment received after $MAX_ATTEMPTS attempts for START.")
                receivedAcknowledge.complete(false)
            }
        }

        keepSendingStart()
        return receivedAcknowledge
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun sendStopCommand() : CompletableFuture<Boolean> {
        val receivedAcknowledge = CompletableFuture<Boolean>()
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
                        receivedAcknowledge.complete(true)
                    } else {
                        // No acknowledgment received, retry...
                        attempts++
                        keepSendingStop()
                    }
                }, TIMEOUT_DURATION)
            } else {
                keepSendingStop()
                Timber.e("No acknowledgment received after $MAX_ATTEMPTS attempts for STOP.")
                receivedAcknowledge.complete(false)
            }
        }

        keepSendingStop()
        return receivedAcknowledge
    }*/

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