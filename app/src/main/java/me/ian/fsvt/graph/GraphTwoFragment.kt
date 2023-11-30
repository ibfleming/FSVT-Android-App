package me.ian.fsvt.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import me.ian.fsvt.AppGlobals
import me.ian.fsvt.ChartClassifier
import me.ian.fsvt.R

class GraphTwoFragment : Fragment(R.layout.fragment_graph_two) {
    /*******************************************
     * Properties
     *******************************************/

    private lateinit var chart : LineChart

    /*******************************************
     * Fragment Function Overrides
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
        AppGlobals.graphTwo = view.findViewById(R.id.GraphTwo)
        chart = AppGlobals.graphTwo
        applyDefaultSettings(chart, ChartClassifier.GRAPH_TWO)

        AppGlobals.graphDataViewModel.dataTwo.observe(viewLifecycleOwner) { value ->
            update(value)
        }
    }

    /*******************************************
     * Extension Functions
     *******************************************/

    // Update chart with new data values
    private fun update(y: Float) : Pair<Float, Float>? {
        val data = data()
        var time = 0F

        if (data != null) {
            var set = data.getDataSetByIndex(0)

            // In the event the set is null
            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            if (!AppGlobals.firstReadTwo) {
                AppGlobals.firstReadTwo = true
            } else {
                val currentTime = System.currentTimeMillis()
                time = (currentTime - AppGlobals.startProgramTime!!).toFloat() / 1000
            }

            val x = "%.1f".format(time).toFloat()
            data.addEntry(Entry(x, y), 0)
            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
            return Pair(x, y)
        }
        return null
    }

    // Finds the maximum Y-value (TDS) in the chart
    fun maxY(): Pair<Float, Float>? {
        val data = data()

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

    // Return current chart data
    fun data(): LineData? {
        return chart.data
    }

    // Clear data contained in the Chart
    fun clear() {
        chart.clear()
        initialize(chart)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }
}