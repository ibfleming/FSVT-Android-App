package me.ian.fsvt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import me.ian.fsvt.bluetooth.ConnectionManager
import me.ian.fsvt.databinding.FragmentGraphBinding
import timber.log.Timber

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /*******************************************
     * Private functions
     *******************************************/
    private var firstDataReceivedTime: Long? = null

    private fun addProbeData(chart: LineChart, value: Float) {
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
            val elapsedTimeInSeconds = (currentTime - firstDataReceivedTime!!) / 1000

            val x = elapsedTimeInSeconds.toFloat()
            val y = value

            Timber.w("x: $x, y: $y")
            data.addEntry(Entry(x, y), 0)
            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }
}