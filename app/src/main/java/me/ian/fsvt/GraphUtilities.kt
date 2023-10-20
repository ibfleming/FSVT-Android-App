package me.ian.fsvt

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
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class ChartType {
    Probe1,
    Probe2
}

private class XAxisFormatter : ValueFormatter() {
    // May not be necessary as the granularity is set to 1F
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return "${value.toInt()}s"
    }
}

private class YAxisFormatter : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return "${value.toInt()}"
    }
}

fun applyGraphStyling(chart: LineChart, probe: ChartType) {

    // Hardware Acceleration
    chart.setHardwareAccelerationEnabled(true)

    // No Data Styling
    chart.setNoDataText("< Empty Chart >")
    chart.setNoDataTextColor(Color.WHITE)
    chart.setNoDataTextTypeface(Typeface.MONOSPACE)

    // Description Styling
    val desc = chart.description
    desc.text = "Probe ${when (probe) {
        ChartType.Probe1 -> "1 (Empty)"
        ChartType.Probe2 -> "2 (Empty)"
    }}"
    desc.textColor = Color.GRAY
    desc.textSize = 16F
    desc.yOffset = 4F
    desc.typeface = Typeface.MONOSPACE
    chart.description = desc

    // Legend Styling
    chart.legend.isEnabled = false

    // Axis Styling
    val xAxis = chart.xAxis
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.axisLineWidth = 2F
    xAxis.axisLineColor = Color.WHITE
    xAxis.textColor = Color.WHITE
    xAxis.valueFormatter = XAxisFormatter()
    xAxis.mAxisMinimum = 0F

    val axisLeft = chart.axisLeft
    axisLeft.mAxisMinimum = 0F
    axisLeft.mAxisMaximum = 999F
    axisLeft.axisLineWidth = 2F
    axisLeft.axisLineColor = Color.WHITE
    axisLeft.textColor = Color.WHITE
    axisLeft.valueFormatter = YAxisFormatter()

    val axisRight = chart.axisRight
    axisRight.isEnabled = false

    // Chart Styling
    chart.setDrawGridBackground(false)
    chart.setDrawBorders(false)
    axisLeft.setDrawGridLines(false)
    xAxis.setDrawGridLines(false)

    // Chart Behavior
    chart.setPinchZoom(false)
    chart.setScaleEnabled(true)
    chart.setTouchEnabled(false)
    chart.isDragEnabled = false
    chart.isAutoScaleMinMaxEnabled = true
    chart.isKeepPositionOnRotation = true
    chart.isDragDecelerationEnabled = false

    // Chart data
    initializeLineData(chart)
}

private fun initializeLineData(chart: LineChart) {
    val data = chart.data
    if( data == null ) {
        val set = createSet()                                                       // (1) Create a set
        val dataSets : ArrayList<ILineDataSet> = ArrayList()                        // (2) Create a variable that holds the set(s)
        dataSets.add(set)                                                           // (3) Add our empty set to the data sets
        val emptyData = LineData(set)                                               // (4) Add the data sets to the chart data
        chart.data = emptyData                                                      // (5) Apply that data to the chart's data
        chart.invalidate()                                                          // (6) Update graph view
    }
}

fun createSet(): LineDataSet {
    // Should we name these sets unique to the chart?
    val set = LineDataSet(createMockData(), "Data")
    //val set = LineDataSet(createMockData(), "Data")
    // Line Styling (line that represents the data on the x-y planes)
    set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
    set.axisDependency = YAxis.AxisDependency.LEFT
    set.color = Color.RED
    set.lineWidth = 1F
    set.setDrawValues(false)
    set.setDrawCircles(false)
    return set
}

fun createMockData() : ArrayList<Entry> {
    val dataValues = ArrayList<Entry>()
    dataValues.add(Entry(0F, 0F))
    return dataValues
}