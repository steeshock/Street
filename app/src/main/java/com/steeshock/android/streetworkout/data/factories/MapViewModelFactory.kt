package com.steeshock.android.streetworkout.data.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.steeshock.android.streetworkout.data.repository.interfaces.IPlacesRepository
import com.steeshock.android.streetworkout.viewmodels.MapViewModel
import javax.inject.Inject

class MapViewModelFactory @Inject constructor(
    private val placesRepository: IPlacesRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MapViewModel(
            placesRepository
        ) as T
    }
}