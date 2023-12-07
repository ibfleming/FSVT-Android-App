package me.ian.fsvt

import com.github.mikephil.charting.charts.LineChart
import me.ian.fsvt.csv.CSVProcessing
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

class AppGlobals {
    companion object {

        /*******************************************
         * Primary Objects
         *******************************************/

        lateinit var graphOne           : LineChart
        lateinit var graphTwo           : LineChart
        var graphDataViewModel          = GraphDataViewModel()
        lateinit var graphOneFragment   : GraphOneFragment
        lateinit var graphTwoFragment   : GraphTwoFragment
        var receivedAcknowledgement     : Boolean = false

        /*******************************************
         * Primary Variables
         *******************************************/

        // State Variables
        var deviceState     : DeviceState = DeviceState.STOPPED
        var connectionState : ConnectionState = ConnectionState.DISCONNECTED

        // Measurement Variables
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
        
        /*******************************************
         * Timing
         *******************************************/

        var startProgramTime    : Long? = null
        var firstReadOne        : Boolean = false   // Graph One -> First data read in will be timed at 0 seconds
        var firstReadTwo        : Boolean = false   // Graph Two -> First data read in will be timed at 0 seconds

        /*******************************************
         * Battery Levels
         *******************************************/

        // These are whole numbers between 0-100 representing percentages
        var batteryProbe1 : Int? = 0
        var batteryProbe2 : Int? = 0

        /*******************************************
         * Extension Functions
         *******************************************/

        // Complete reset of globals
        fun resetDirective() {
            Timber.e("[RESET DIRECTIVE]")
            graphOneFragment.clear()
            graphTwoFragment.clear()
            deviceState = DeviceState.STOPPED
            unitType = UnitType.FEET
            fileName = null
            CSVProcessing.closeBuffer()
            distance = null
            velocity = null
            testCount = 0
            firstReadOne = false
            firstReadTwo = false
            startProgramTime = null
        }

        // "Soft" reset of globals
        fun stopDirective() {
            Timber.e("[STOP DIRECTIVE]")
            graphOneFragment.clear()
            graphTwoFragment.clear()
            velocity = null
            firstReadOne = false
            firstReadTwo = false
            startProgramTime = null
        }
    }
}

/*******************************************
 * Important ENUM Definitions
 *******************************************/

enum class ChartClassifier {
    GRAPH_ONE,
    GRAPH_TWO
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