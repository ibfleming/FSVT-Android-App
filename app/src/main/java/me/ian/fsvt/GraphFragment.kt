package me.ian.fsvt

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import me.ian.fsvt.bluetooth.ConnectionManager
import me.ian.fsvt.databinding.FragmentGraphBinding
import kotlin.math.roundToInt

class GraphFragment : Fragment() {

    /*******************************************
     * Properties
     *******************************************/

    private var _binding : FragmentGraphBinding? = null
    private val binding get() = _binding!!

    private lateinit var probe1Chart : LineChart
    private lateinit var probe2Chart : LineChart

    /*******************************************
     * Activity functions
     *******************************************/

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        probe1Chart = binding.Probe1Graph
        probe2Chart = binding.Probe2Graph

        ConnectionManager.probe1Data.observe(viewLifecycleOwner) { data ->
            addProbeData(probe1Chart, data)
        }

        ConnectionManager.probe2Data.observe(viewLifecycleOwner) { data ->
            addProbeData(probe2Chart, data)
        }

        applyGraphStyling(probe1Chart, ChartType.Probe1)
        applyGraphStyling(probe2Chart, ChartType.Probe2)

        /*
        val mockDataSet = LineDataSet(createMockData(), "Mock Data")
        mockDataSet.lineWidth = 2F
        mockDataSet.valueTextColor = Color.TRANSPARENT
        mockDataSet.color = Color.WHITE
        mockDataSet.setDrawCircles(false)

        val dataSets : ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(mockDataSet)
        val data = LineData(dataSets)
        */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /*******************************************
     * Private functions
     *******************************************/
    private fun invalidateGraphs() {
        probe1Chart.invalidate()
        probe2Chart.invalidate()
    }

    private fun addProbeData(chart: LineChart, value: Float) {
        val data = chart.data
        if( data != null ) {
            val set = data.getDataSetByIndex(0)
            if( set != null ) {
                data.addEntry(Entry(set.entryCount.toFloat(), value), 0)
                data.notifyDataChanged()
                chart.notifyDataSetChanged()
            }
        }
    }
}