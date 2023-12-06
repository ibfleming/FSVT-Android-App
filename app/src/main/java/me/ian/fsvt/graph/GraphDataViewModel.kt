package me.ian.fsvt.graph

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GraphDataViewModel : ViewModel() {

    /*******************************************
     * Connection State
     *******************************************/

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> get() = _isConnected

    /*******************************************
     * Graph Data Entry (ppm)
     *******************************************/

    private val _dataOne = MutableLiveData<Float>()
    private val _dataTwo = MutableLiveData<Float>()
    val dataOne: LiveData<Float> get() = _dataOne
    val dataTwo: LiveData<Float> get() = _dataTwo

    /*******************************************
     * Functions
     *******************************************/

    // Update Live Data
    fun updateGraphs(data1: Float, data2: Float) {
        _dataOne.postValue(data1)
        _dataTwo.postValue(data2)
    }

    // Update Connection State
    fun setConnectionStatus(connected: Boolean) {
        if (connected) {
            _isConnected.postValue(true)
        } else {
            _isConnected.postValue(false)
        }
    }
}