package me.example.fsvt

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread

class AccelerometerActivity(
    private val graphActivity: GraphActivity
) : SensorEventListener {

    private val tag = "AccelerometerActivity"

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    private val handlerThread = HandlerThread("AccelerometerActivity").apply { start() }
    private val handler = Handler(handlerThread.looper)
    private var thread: Thread? = null

    private var plotData = true

    private val accelerometerRunnable = Runnable {
        while(true) {
            plotData = true
            try {
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun onCreate(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        populateGraph()
    }

    private fun populateGraph() {
        if( thread != null ) {
            thread!!.interrupt()
        }

        thread = Thread {
            handler.post(accelerometerRunnable)
        }

        thread!!.start()
    }

    fun stopPopulating() {
        thread?.interrupt()
        sensorManager.unregisterListener(this)
        graphActivity.clearGraph()
    }
    override fun onSensorChanged(e: SensorEvent?) {
        if (e != null) {
            if( plotData && e.sensor.type == Sensor.TYPE_ACCELEROMETER ) {
                graphActivity.addEntry(e, 0)
                graphActivity.addEntry(e, 1)
                plotData = false
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Implementation for accuracy change (optional)
    }

}