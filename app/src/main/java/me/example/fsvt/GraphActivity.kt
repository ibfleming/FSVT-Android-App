package me.example.fsvt

import android.graphics.Color
import android.hardware.SensorEvent
import android.util.Log
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class GraphActivity(chart : LineChart) {

    private val tag = "GraphActivity"

    private val lChart: LineChart = chart

    //private var graphThread: Thread? = null
    //private var plotData = true

    fun createGraph() {

        Log.d(tag, "createGraph(): Initializing Graph")

        // Description/Title
        lChart.description.isEnabled = true
        lChart.description.text = "Live Accelerometer Data"
        lChart.description.textColor = Color.WHITE

        // Touch Gestures, Scaling, Dragging (Interactions)
        lChart.setTouchEnabled(true)
        lChart.isDragEnabled = true
        lChart.setScaleEnabled(true)
        lChart.setDrawGridBackground(false)
        lChart.setPinchZoom(true)

        // Style
        lChart.setBackgroundColor(Color.parseColor("#42A5F5"))

        // Add empty set of data initially
        val data = LineData()
        data.setValueTextColor(Color.WHITE)
        lChart.data = data

        // Legend Settings
        val l = lChart.legend
        l.form = Legend.LegendForm.LINE
        l.textColor = Color.WHITE

        // Axis Manipulation
        val axisX = lChart.xAxis
        axisX.textColor = Color.WHITE
        axisX.setDrawGridLines(true)
        axisX.setAvoidFirstLastClipping(true)
        axisX.position = XAxis.XAxisPosition.BOTTOM
        axisX.isEnabled = true

        val axisLeft = lChart.axisLeft
        axisLeft.textColor = Color.WHITE;
        axisLeft.setDrawGridLines(false)
        axisLeft.axisMaximum = 100F
        axisLeft.granularity = 10F
        axisLeft.axisMinimum = 0F
        axisLeft.setDrawGridLines(true)

        val axisRight = lChart.axisRight
        axisRight.isEnabled = false;

        lChart.axisLeft.setDrawGridLines(false)
        lChart.xAxis.setDrawGridLines(false)
        lChart.setDrawBorders(false)
    }

    fun addEntry(e : SensorEvent) {

        val data = lChart.data

        if( data != null ) {
            var set = data.getDataSetByIndex(0)
            // set.addEntry(...)

            if( set == null ) {
                set = createSet()
                data.addDataSet(set)
            }

            data.addEntry(Entry(set.entryCount.toFloat(), e.values[0] + 30F), 0)
            data.notifyDataChanged()

            lChart.notifyDataSetChanged()

            lChart.setVisibleXRangeMaximum(150F)

            lChart.moveViewToX(data.entryCount.toFloat())
        }
    }

    private fun createSet() : LineDataSet {
        val set = LineDataSet(null, "Probe 1")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 2F
        set.color = Color.BLUE
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 0.2F
        return set
    }

}

