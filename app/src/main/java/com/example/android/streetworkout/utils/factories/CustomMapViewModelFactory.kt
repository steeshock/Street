package com.example.android.streetworkout.utils.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.streetworkout.data.Repository
import com.example.android.streetworkout.viewmodels.MapViewModel

class CustomMapViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MapViewModel(
            repository
        ) as T
    }
}