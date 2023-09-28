package me.example.fsvt

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log

class AccelerometerActivity : Activity(), SensorEventListener {

    private val tag = "AccelerometerActivity"

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var graphActivity : GraphActivity
    private var thread : Thread? = null
    private var plotData = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        // Initialize SensorManager and Accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Initialize GraphActivity
        graphActivity = GraphActivity(findViewById(R.id.line_chart))
        graphActivity.createGraph()

        populateGraph()
    }

    private fun populateGraph() {
        if( thread != null ) {
            thread!!.interrupt()
        }

        thread = Thread() {
            while( true ) {
                plotData = true
                try {
                    Thread.sleep(1000)
                } catch ( e: InterruptedException ) {
                    e.printStackTrace()
                }
            }
        }

        thread!!.start()
    }

    override fun onResume() {
        super.onResume()

        if (::sensorManager.isInitialized) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()

        thread?.interrupt()
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
        thread?.interrupt()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implementation for accuracy change (optional)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if( plotData && event.sensor.type == Sensor.TYPE_ACCELEROMETER ) {
            graphActivity.addEntry(event)
            plotData = false
        }
    }
}