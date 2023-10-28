package me.ian.fsvt.graph

import com.github.mikephil.charting.charts.LineChart

class MyObjects {
    companion object {
        lateinit var graphOne : LineChart
        lateinit var graphTwo : LineChart
        lateinit var graphDataViewModel: GraphDataViewModel
        lateinit var graphOneFragment : GraphOneFragment
        lateinit var graphTwoFragment : GraphTwoFragment
    }
}