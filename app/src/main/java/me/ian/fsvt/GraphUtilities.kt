package me.ian.fsvt

import android.graphics.Color
import android.graphics.Typeface
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry

enum class ChartType {
    Probe1,
    Probe2
}

fun applyGraphStyling(chart: LineChart, probe: ChartType) {

    // No Data Styling
    chart.setNoDataText("< Empty Chart >")
    chart.setNoDataTextColor(Color.WHITE)
    chart.setNoDataTextTypeface(Typeface.MONOSPACE)

    // Description Styling
    val desc = chart.description
    desc.text = "Probe ${when (probe) {
        ChartType.Probe1 -> "1"
        ChartType.Probe2 -> "2"
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

    val axisLeft = chart.axisLeft
    axisLeft.axisLineWidth = 2F
    axisLeft.axisLineColor = Color.WHITE
    axisLeft.textColor = Color.WHITE

    val axisRight = chart.axisRight
    axisRight.isEnabled = false

    // Chart Styling
    chart.setDrawGridBackground(false)
    chart.setDrawBorders(false)
    axisLeft.setDrawGridLines(false)
    xAxis.setDrawGridLines(false)

}

fun createMockData() : ArrayList<Entry> {
    val dataValues = ArrayList<Entry>()
    dataValues.add(Entry(0F, 30F))
    dataValues.add(Entry(1F, 60F))
    dataValues.add(Entry(2F, 45F))
    dataValues.add(Entry(3F, 20F))
    dataValues.add(Entry(4F, 10F))
    return dataValues
}