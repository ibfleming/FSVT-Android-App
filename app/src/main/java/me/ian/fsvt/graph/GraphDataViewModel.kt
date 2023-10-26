package me.ian.fsvt.graph

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GraphDataViewModel : ViewModel() {
    private val _dataPoint1 = MutableLiveData<Float>()
    private val _dataPoint2 = MutableLiveData<Float>()

    val dataPoint1: LiveData<Float> get() = _dataPoint1
    val dataPoint2: LiveData<Float> get() = _dataPoint2

    fun updateGraphs(data1: Float, data2: Float) {
        _dataPoint1.postValue(data1)
        _dataPoint2.postValue(data2)
    }
}