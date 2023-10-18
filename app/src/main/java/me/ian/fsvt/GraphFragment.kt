package me.ian.fsvt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import me.ian.fsvt.databinding.FragmentGraphBinding

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

        applyGraphStyling(probe1Chart, ChartType.Probe1)
        applyGraphStyling(probe2Chart, ChartType.Probe2)

        val mockDataSet = LineDataSet(createMockData(), "Mock Data")
        val dataSets : ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(mockDataSet)
        val data = LineData(dataSets)

        probe1Chart.data = data
        probe2Chart.data = data
        invalidateGraphs()
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
}