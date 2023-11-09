package me.ian.fsvt.graph

import com.github.mikephil.charting.charts.LineChart

/*******************************************
 * This is a static class that contains all
 * the important variables and objects on the global
 * scope that essentially allow the app to function
 * properly. It is the heart of the application.
 *******************************************/

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
        var velocity        : Float = 0F
        var testCount       : Int = 0

        /*******************************************
         * Timing
         *******************************************/

        var startProgramTime    : Long? = null
        var firstReadInG1       : Boolean = false
        var firstReadInG2       : Boolean = false

        /*******************************************
         * Extension Functions
         *******************************************/

        fun resetValues() {
            graphOneFragment.clearGraph()
            graphTwoFragment.clearGraph()
            deviceState = DeviceState.STOPPED
            unitType = UnitType.FEET
            fileName = null
            distance = 0F
            velocity = 0F
            testCount = 0
            firstReadInG1 = false
            firstReadInG2 = false
            startProgramTime = null
        }

        fun softResetValues() {
            graphOneFragment.clearGraph()
            graphTwoFragment.clearGraph()
            velocity = 0F
            firstReadInG1 = false
            firstReadInG2 = false
            startProgramTime = null
        }
    }
}

/*******************************************
 * Important ENUM Definitions
 *******************************************/

enum class ChartType {
    Probe1,
    Probe2
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