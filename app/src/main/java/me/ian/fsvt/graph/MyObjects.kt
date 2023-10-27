package me.ian.fsvt.graph

import com.github.mikephil.charting.charts.LineChart

class MyObjects {
    companion object {
        var graphOne : LineChart? = null
        var graphTwo : LineChart? = null
        lateinit var graphDataViewModel: GraphDataViewModel
        lateinit var graphOneFragment : GraphOneFragment
        lateinit var graphTwoFragment : GraphTwoFragment
    }
}