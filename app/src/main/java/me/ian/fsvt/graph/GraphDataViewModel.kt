package me.ian.fsvt.graph

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

class GraphDataViewModel : ViewModel() {

    /*******************************************
     * Connection State
     *******************************************/

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> get() = _isConnected

    /*******************************************
     * Graph Data Entries
     *******************************************/

    private val _dataPoint1 = MutableLiveData<Float>()
    private val _dataPoint2 = MutableLiveData<Float>()
    val dataPoint1: LiveData<Float> get() = _dataPoint1
    val dataPoint2: LiveData<Float> get() = _dataPoint2

    /*******************************************
     * Functions
     *******************************************/

    fun updateGraphs(data1: Float, data2: Float) {
        _dataPoint1.postValue(data1)
        _dataPoint2.postValue(data2)
    }

    fun setConnectionStatus(connect : Boolean) {
        if( connect ) {
            Timber.tag("[VIEW MODEL]").i("Connected = TRUE")
            _isConnected.postValue(true)
        }
        else {
            Timber.tag("[VIEW MODEL]").i("Connected = FALSE")
            _isConnected.postValue(false)
        }
    }
}