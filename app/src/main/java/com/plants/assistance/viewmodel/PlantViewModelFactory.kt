package com.plants.assistance.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.plants.assistance.database.PlantProblemDao

class PlantViewModelFactory(private val plantProblemDao: PlantProblemDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantViewModel(plantProblemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
