package me.example.fsvt

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class GraphActivity(private val chart : LineChart) {
    private val lineChart: LineChart = chart
    private var xValues: List<String>? = null
    fun createGraph() {

        /*
        val description = Description()
        description.text = "PPM"
        description.setPosition(500F, 20F)
        lineChart.description = description
        */

        lineChart.axisRight.setDrawLabels(false)
        lineChart.setDrawGridBackground(false)

        xValues = listOf("0", "1", "2", "3")

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
        xAxis.labelCount = 0
        xAxis.granularity = 1F

        val yAxis = lineChart.axisLeft
        yAxis.axisMinimum = 0F
        yAxis.axisMaximum =100F
        yAxis.axisLineWidth = 2F
        yAxis.axisLineColor = Color.BLACK
        yAxis.labelCount = 10

        val entries1 = ArrayList<Entry>()
        entries1.add(Entry(0F, 10F))
        entries1.add(Entry(1F, 10F))
        entries1.add(Entry(2F, 15F))
        entries1.add(Entry(3F, 45F))

        val entries2 = ArrayList<Entry>()
        entries2.add(Entry(0F, 5F))
        entries2.add(Entry(1F, 15F))
        entries2.add(Entry(2F, 25F))
        entries2.add(Entry(3F, 30F))

        val dataset1 = LineDataSet(entries1, "Maths")
        dataset1.color = Color.BLUE

        val dataset2 = LineDataSet(entries2, "Science")
        dataset2.color = Color.RED

        val lineData = LineData(dataset1, dataset2)

        lineChart.data = lineData
        lineChart.invalidate()
    }
}