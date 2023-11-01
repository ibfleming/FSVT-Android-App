package me.ian.fsvt.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import me.ian.fsvt.R
import timber.log.Timber

class GraphTwoFragment : Fragment(R.layout.fragment_graph_two) {

    /*******************************************
     * Properties
     *******************************************/

    private lateinit var chart : LineChart

    /*******************************************
     * Fragment function overrides
     *******************************************/

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_graph_two, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MyObjects.graphTwo = view.findViewById(R.id.GraphTwo)
        chart = MyObjects.graphTwo
        applyGraphStyling(chart, ChartType.Probe2)

        MyObjects.graphDataViewModel.dataPoint2.observe(viewLifecycleOwner) { value ->
            updateChart(value)
        }
    }

    private fun updateChart(value: Float) {
        val data = chart.data
        var time = 0.0F

        if (data != null) {
            var set = data.getDataSetByIndex(0)

            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            if( !MyObjects.firstReadInG2 ) {
                MyObjects.firstReadInG2 = true
            }
            else {
                val currentTime = System.currentTimeMillis()
                time = (currentTime - MyObjects.startProgramTime!!).toFloat() / 1000
            }

            Timber.w("[Graph Two] (update) -> (time: ${"%.1f".format(time)}, tds: $value)")
            data.addEntry(Entry("%.1f".format(time).toFloat(), value), 0)
            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }

    fun clearGraph() {
        MyObjects.firstReadInG2 = false
        chart.clear()
        initializeLineData(chart)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    fun findMaxEntry(): Pair<Float, Float>? {
        val data = chart.data

        if (data != null && data.dataSetCount > 0) {
            val set = data.getDataSetByIndex(0)

            if (set != null && set.entryCount > 0) {
                var maxX = set.getEntryForIndex(0).x
                var maxY = set.getEntryForIndex(0).y

                for (i in 1 until set.entryCount) {
                    val entry = set.getEntryForIndex(i)
                    if (entry.y > maxY) {
                        maxX = entry.x
                        maxY = entry.y
                    }
                }
                return Pair(maxX, maxY)
            }
        }
        return null
    }
}