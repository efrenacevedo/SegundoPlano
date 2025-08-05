package mx.edu.segundoplano.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SensorDataViewModelFactory(private val repository: LocalDataRepository):ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorDataViewModel::class.java)){
            return SensorDataViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}