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

    fun createGraph() {

        Log.d(tag, "createGraph(): Initializing Graph")

        // Description/Title
        lChart.description.isEnabled = true
        lChart.description.text = "PPM Data"
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
        val probe1Set = createSet("Probe 1", Color.BLUE)
        val probe2Set = createSet("Probe 2", Color.GREEN)
        data.addDataSet(probe1Set)
        data.addDataSet(probe2Set)
        lChart.data = data

        // Legend Settings
        val l = lChart.legend
        l.form = Legend.LegendForm.CIRCLE
        l.textColor = Color.WHITE

        // Axis Manipulation
        val axisX = lChart.xAxis
        axisX.granularity = 1F
        axisX.textColor = Color.WHITE
        axisX.setDrawGridLines(true)
        axisX.setAvoidFirstLastClipping(true)
        axisX.position = XAxis.XAxisPosition.BOTTOM
        axisX.isEnabled = true

        val axisLeft = lChart.axisLeft
        axisLeft.textColor = Color.WHITE
        axisLeft.setDrawGridLines(false)
        axisLeft.axisMaximum = 10F
        axisLeft.axisMinimum = 0F
        axisLeft.setDrawGridLines(true)
        val axisRight = lChart.axisRight
        axisRight.isEnabled = false

        lChart.axisLeft.setDrawGridLines(false)
        lChart.xAxis.setDrawGridLines(false)
        lChart.setDrawBorders(false)
    }

    fun addEntry(e : SensorEvent, dataSetIndex: Int) {

        val data = lChart.data
        if( data != null ) {
            val set = data.getDataSetByIndex(dataSetIndex)
            // set.addEntry(...)

            if( set != null ) {
                if( dataSetIndex == 0 ) {
                    data.addEntry(Entry(set.entryCount.toFloat(), e.values[0] + 2), dataSetIndex)
                }
                else if( dataSetIndex == 1 ) {
                    data.addEntry(Entry(set.entryCount.toFloat(), e.values[0] + 3), dataSetIndex)
                }

                data.notifyDataChanged()
                lChart.notifyDataSetChanged()

                lChart.setVisibleXRangeMaximum(60F)
                lChart.moveViewToX(data.entryCount.toFloat())
            }
        }
    }

    private fun createSet(label: String, color: Int) : LineDataSet {
        val set = LineDataSet(null, label)
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 2F
        set.color = color
        set.isHighlightEnabled = false
        set.setDrawValues(true)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.LINEAR
        set.cubicIntensity = 0.2F
        return set
    }

    fun clearGraph() {
        if (lChart.data != null) {
            lChart.data.dataSets.clear()
            lChart.data.notifyDataChanged()
            lChart.notifyDataSetChanged()
            lChart.invalidate()
        }
    }
}

