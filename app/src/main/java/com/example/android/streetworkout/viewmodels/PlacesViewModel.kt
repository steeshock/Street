package com.example.android.streetworkout.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.streetworkout.data.Repository
import com.example.android.streetworkout.data.model.PlaceObject
import com.example.android.streetworkout.utils.ApiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlacesViewModel(private val repository: Repository) : ViewModel() {

    val allPlacesLive: LiveData<List<PlaceObject>> = repository.allPlaces

    fun clearPlacesTable() = viewModelScope.launch(Dispatchers.IO) {
        repository.clearPlacesTable()
    }

    fun updateProjects() {
        viewModelScope.launch(Dispatchers.IO) {

            val response = ApiUtils.getApiService()?.getPlaces()?.body()

            response?.let {
                repository.clearPlacesTable()
                repository.insertAllPlaces(it)
            }
        }
    }
}