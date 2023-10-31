package me.ian.fsvt.graph

import com.github.mikephil.charting.charts.LineChart

class MyObjects {
    companion object {

        /*******************************************
         * Primary Objects
         *******************************************/

        lateinit var graphOne : LineChart
        lateinit var graphTwo : LineChart
        lateinit var graphDataViewModel: GraphDataViewModel
        lateinit var graphOneFragment : GraphOneFragment
        lateinit var graphTwoFragment : GraphTwoFragment

        /*******************************************
         * Primary Variables
         *******************************************/

        var deviceState     : DeviceState = DeviceState.STOPPED
        var connectionState : ConnectionState = ConnectionState.DISCONNECTED
        var unitType        : UnitType = UnitType.FEET
        var fileName        : String? = null
        var distance        : Float  = 0F

        /*******************************************
         * Timing
         *******************************************/
        var firstDataReceivedTime : Long? = null

        /*******************************************
         * Extension Functions
         *******************************************/

        fun resetValues() {
            graphOneFragment.clearGraph()
            graphTwoFragment.clearGraph()
            deviceState = DeviceState.STOPPED
            connectionState = ConnectionState.DISCONNECTED
            unitType = UnitType.FEET
            fileName = null
            distance = 0F
        }
    }
}

enum class UnitType {
    FEET,
    METERS,
}

enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
}

enum class DeviceState {
    RUNNING,
    STOPPED,
}