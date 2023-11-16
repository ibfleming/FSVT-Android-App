package me.ian.fsvt

import com.github.mikephil.charting.charts.LineChart
import me.ian.fsvt.graph.GraphDataViewModel
import me.ian.fsvt.graph.GraphOneFragment
import me.ian.fsvt.graph.GraphTwoFragment
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File

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

        var distance        : Float?  = null
        var velocity        : Float?  = null

        /*******************************************
         * File/CSV Properties
         *******************************************/

        var testCount    : Int = 0                   // Test count for CSV
        var fileName     : String? = null            // The RAW String of the File Name from User
        var csvDirectory : File? = null              // File directory for storage of CSVs on device
        var csvFile      : File? = null              // Current file object for CSV processing
        var fileBuffer   : BufferedWriter? = null    // Current file buffer of the current file object

        /**
         * Resets all objects for CSV processing.
         * No need to reset the csvDirectory as that is initialized
         * on Activity create and shall remain the same for the entire lifespan
         * of the app.
         * The rest of these values will change dynamically by the user in the app.
         */
        fun resetCSV() {
            testCount = 0
            fileName = null
            csvFile = null
            fileBuffer = null
        }

        /*******************************************
         * Timing
         *******************************************/

        var startProgramTime    : Long? = null
        var firstReadInG1       : Boolean = false
        var firstReadInG2       : Boolean = false

        /*******************************************
         * Extension Functions
         *******************************************/

        fun resetDirective() {
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

        fun stopDirective() {
            Timber.i("[STOP DIRECTIVE]")
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