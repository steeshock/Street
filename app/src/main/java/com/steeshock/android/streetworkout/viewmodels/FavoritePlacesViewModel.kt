package com.steeshock.android.streetworkout.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.steeshock.android.streetworkout.data.model.Place
import com.steeshock.android.streetworkout.data.repository.interfaces.IPlacesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class FavoritePlacesViewModel(private val placesRepository: IPlacesRepository) : ViewModel() {

    val favoritePlacesLive: LiveData<List<Place>> = placesRepository.allFavoritePlaces

    fun removePlaceFromFavorites(place: Place) = viewModelScope.launch(Dispatchers.IO) {
        place.changeFavoriteState()
        insertPlace(place)
    }

    fun returnPlaceToFavorites(place: Place) = viewModelScope.launch(Dispatchers.IO) {
        place.changeFavoriteState()
        insertPlace(place)
    }

    private fun insertPlace(place: Place) = viewModelScope.launch(Dispatchers.IO) {
        placesRepository.insertPlaceLocal(place)
    }
}
class CustomFavoritePlacesViewModelFactory @Inject constructor(
    private val repository: IPlacesRepository
    ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return FavoritePlacesViewModel(
            repository
        ) as T
    }
}