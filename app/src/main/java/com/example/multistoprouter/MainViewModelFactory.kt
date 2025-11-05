package com.example.multistoprouter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.multistoprouter.data.RoutesRepository
import com.example.multistoprouter.location.LocationRepository

class MainViewModelFactory(
    private val routesRepository: RoutesRepository,
    private val locationRepository: LocationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(routesRepository, locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
