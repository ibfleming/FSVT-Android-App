package me.example.fsvt

/**
 *  Serves to assist in updating the thread UI
 *  with incoming data from Bluetooth BLE
 */
interface BLECallback {
    fun onDataReceived(data: String)
}