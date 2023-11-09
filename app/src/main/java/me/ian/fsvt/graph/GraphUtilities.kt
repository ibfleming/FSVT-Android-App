package me.ian.fsvt.graph

import android.graphics.Color
import android.graphics.Typeface
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

private class XAxisFormatter : ValueFormatter() {
    // May not be necessary as the granularity is set to 1F
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return if (value % 1 == 0.0f) {
            "${value.toInt()}s"
        } else {
            "${"%.1f".format(value)}s"
        }
    }
}

private class YAxisFormatter : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return if (value >= 0) {
            "${value.toInt()}"
        } else {
            ""
        }
    }
}

fun applyGraphStyling(chart: LineChart?, probe: ChartType) {
    // Description
    val desc = chart?.description
    desc?.apply {
        text = "Probe ${when (probe) {
            ChartType.Probe1 -> "1"
            ChartType.Probe2 -> "2"
        }}"
        textColor = Color.GRAY
        yOffset = 15F
        textSize = 12F
        typeface = Typeface.MONOSPACE
    }
    chart?.description = desc

    // X Axis
    val xAxis = chart?.xAxis
    xAxis?.apply {
        position = XAxis.XAxisPosition.BOTTOM
        granularity = 0.5F
        axisMinimum = 0F
        axisLineWidth = 2F
        axisLineColor = Color.WHITE
        textColor = Color.WHITE
        valueFormatter = XAxisFormatter()
    }

    // Y Axis (left axis)
    val axisLeft = chart?.axisLeft
    axisLeft?.apply {
        axisMinimum = -5F
        axisMaximum = 250F
        axisLineWidth = 2F
        axisLineColor = Color.WHITE
        textColor = Color.WHITE
        valueFormatter = YAxisFormatter()
    }

    // Right Axis
    val axisRight = chart?.axisRight
    axisRight?.apply {
        isEnabled = false
    }

    // Chart Styling
    axisLeft?.setDrawGridLines(false)
    xAxis?.setDrawGridLines(false)

    // Chart Settings
    chart?.apply {
        // "No Data" Styling
        setNoDataText("< Empty Chart >")
        setNoDataTextColor(Color.WHITE)
        setNoDataTextTypeface(Typeface.MONOSPACE)
        // Styling
        legend.isEnabled = false
        setDrawGridBackground(false)
        setDrawBorders(false)
        // Behavior
        setPinchZoom(true)
        setScaleEnabled(true)
        setTouchEnabled(true)
        isDragEnabled = true
        isAutoScaleMinMaxEnabled = true
        isKeepPositionOnRotation = true
        isDragDecelerationEnabled = false
        isHighlightPerDragEnabled = false
        isHighlightPerTapEnabled = false
        setHardwareAccelerationEnabled(true)
    }

    // Default Chart Data
    initializeLineData(chart)
}

fun initializeLineData(chart: LineChart?) {
    val data = chart?.data
    if( data == null ) {
        val set = createSet()                                       // (1) Create a set
        val dataSets : ArrayList<ILineDataSet> = ArrayList()        // (2) Create a variable that holds the set(s)
        dataSets.add(set)                                           // (3) Add our empty set to the data sets
        val emptyData = LineData(set)                               // (4) Add the data sets to the chart data
        chart?.data = emptyData                                     // (5) Apply that data to the chart's data
        chart?.invalidate()                                         // (6) Update graph view
    }
}

fun createSet(): LineDataSet {
    val set = LineDataSet(createMockData(), "DATA")
    // Line Styling (line that represents the data on the x-y planes)
    set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
    set.axisDependency = YAxis.AxisDependency.LEFT
    set.color = Color.WHITE
    set.lineWidth = 0.5F
    set.setDrawValues(false)
    set.setDrawCircles(false)
    return set
}

fun createMockData() : ArrayList<Entry> {
    val dataValues = ArrayList<Entry>()
    dataValues.add(Entry(1F, 0F))
    return dataValues
}