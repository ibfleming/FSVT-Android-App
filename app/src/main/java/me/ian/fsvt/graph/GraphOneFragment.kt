package me.ian.fsvt.graph

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import me.ian.fsvt.R
import timber.log.Timber

class GraphOneFragment : Fragment(R.layout.fragment_graph_one) {

    /*******************************************
     * Properties
     *******************************************/

    private lateinit var chart : LineChart
    private lateinit var dataViewModel : GraphDataViewModel
    private var firstDataReceivedTime  : Long? = null

    /*******************************************
     * Fragment function overrides
     *******************************************/

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_graph_one, container, false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

        chart = MyObjects.graphOne
        dataViewModel = MyObjects.graphDataViewModel

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataViewModel.dataPoint1.observe(viewLifecycleOwner) { value ->
            updateChart(value)
        }

        applyGraphStyling(chart, ChartType.Probe1)
    }

    private fun updateChart(value: Float) {
        val data = chart.data

        if (data != null) {
            var set = data.getDataSetByIndex(0)

            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            val currentTime = System.currentTimeMillis()
            if (firstDataReceivedTime == null) {
                firstDataReceivedTime = currentTime
            }
            val x = (currentTime - (firstDataReceivedTime ?: currentTime)).toFloat() / 1000

            Timber.w("x: $x, y: $value")
            data.addEntry(Entry(x, value), 0)
            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }

    fun clearGraph() {
        firstDataReceivedTime = null
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