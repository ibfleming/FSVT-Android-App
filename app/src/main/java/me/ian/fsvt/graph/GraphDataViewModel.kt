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

    private val _dataOne = MutableLiveData<Float>()
    private val _dataTwo = MutableLiveData<Float>()
    val dataOne: LiveData<Float> get() = _dataOne
    val dataTwo: LiveData<Float> get() = _dataTwo

    /*******************************************
     * Functions
     *******************************************/

    fun updateGraphs(data1: Float, data2: Float) {
        _dataOne.postValue(data1)
        _dataTwo.postValue(data2)
    }

    fun setConnectionStatus(connect: Boolean) {
        if (connect) {
            Timber.i("Connected")
            _isConnected.postValue(true)
        } else {
            Timber.i("Disconnected")
            _isConnected.postValue(false)
        }
    }
}