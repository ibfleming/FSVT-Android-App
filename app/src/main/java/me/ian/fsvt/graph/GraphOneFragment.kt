package me.ian.fsvt.graph

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import me.ian.fsvt.R
import timber.log.Timber

class GraphOneFragment(private var chart: LineChart?, private var viewModel: GraphDataViewModel) : Fragment(R.layout.fragment_graph_one) {

    private var firstDataReceivedTime: Long? = null
    private lateinit var dataViewModel: GraphDataViewModel
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataViewModel = viewModel
        dataViewModel.dataPoint1.observe(viewLifecycleOwner) { value ->
            updateChart(value)
        }

        chart = view.findViewById(R.id.GraphOne)
        applyGraphStyling(chart, ChartType.Probe1)
    }

    private fun updateChart(value: Float) {
        val data = chart?.data

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
            chart?.notifyDataSetChanged()
            chart?.invalidate()
        }
    }

    fun clearGraph() {
        firstDataReceivedTime = null
        chart?.clear()
        initializeLineData(chart)
        chart?.notifyDataSetChanged()
        chart?.invalidate()
    }

    fun findMaxEntry(): Pair<Float, Float>? {
        val data = chart?.data

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
