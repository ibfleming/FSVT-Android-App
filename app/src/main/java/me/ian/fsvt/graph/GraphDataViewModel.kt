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
     * Graph Data Entries
     *******************************************/

    private val tdsOne = MutableLiveData<Float>()
    private val tdsTwo = MutableLiveData<Float>()
    val tdsGraphOne: LiveData<Float> get() = tdsOne
    val tdsGraphTwo: LiveData<Float> get() = tdsTwo

    /*******************************************
     * Functions
     *******************************************/

    fun updateGraphs(data1: Float, data2: Float) {
        tdsOne.postValue(data1)
        tdsTwo.postValue(data2)
    }

    fun setConnectionStatus(connect : Boolean) {
        if( connect ) {
            //Timber.tag("[VIEW MODEL]").i("Connected = TRUE")
            _isConnected.postValue(true)
        }
        else {
            //Timber.tag("[VIEW MODEL]").i("Connected = FALSE")
            _isConnected.postValue(false)
        }
    }
}