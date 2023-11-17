package me.ian.fsvt.graph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import me.ian.fsvt.ChartClassifier
import me.ian.fsvt.MyObjects
import me.ian.fsvt.R

class GraphFragment(private val classifier: ChartClassifier): Fragment() {

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

        val layout = if( classifier == ChartClassifier.Probe1 ) { R.layout.fragment_graph_one }
        else { R.layout.fragment_graph_two  }
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if( classifier == ChartClassifier.Probe1 ) {
            MyObjects.graphOne = view.findViewById(R.id.GraphOne)
            chart = MyObjects.graphOne
            applyGraphStyling(chart, ChartClassifier.Probe1)

            MyObjects.graphDataViewModel.dataPoint1.observe(viewLifecycleOwner) { value ->
                //updateChart(value)
            }
        }
        else {
            MyObjects.graphTwo = view.findViewById(R.id.GraphTwo)
            chart = MyObjects.graphTwo
            applyGraphStyling(chart, ChartClassifier.Probe2)

            MyObjects.graphDataViewModel.dataPoint2.observe(viewLifecycleOwner) { value ->
                //updateChart(value)
            }
        }
    }
}