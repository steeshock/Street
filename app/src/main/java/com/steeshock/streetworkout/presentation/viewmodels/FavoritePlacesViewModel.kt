package com.steeshock.streetworkout.presentation.viewmodels

import androidx.lifecycle.*
import com.steeshock.streetworkout.data.model.Place
import com.steeshock.streetworkout.data.repository.interfaces.IPlacesRepository
import com.steeshock.streetworkout.domain.favorites.IFavoritesInteractor
import com.steeshock.streetworkout.presentation.delegates.*
import com.steeshock.streetworkout.presentation.viewStates.EmptyViewState
import com.steeshock.streetworkout.presentation.viewStates.places.PlacesViewEvent
import com.steeshock.streetworkout.presentation.viewStates.places.PlacesViewEvent.NoInternetConnection
import com.steeshock.streetworkout.presentation.viewStates.places.PlacesViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class FavoritePlacesViewModel @Inject constructor(
    placesRepository: IPlacesRepository,
    private val favoritesInteractor: IFavoritesInteractor,
) : ViewModel(),
    ViewEventDelegate<PlacesViewEvent> by ViewEventDelegateImpl(),
    ViewStateDelegate<PlacesViewState> by ViewStateDelegateImpl({ PlacesViewState() }),
    ExceptionHandler by DefaultExceptionHandler() {

    private val mediatorPlaces = MediatorLiveData<List<Place>>()
    val observablePlaces: LiveData<List<Place>> = mediatorPlaces

    private val allFavoritePlaces: LiveData<List<Place>> = placesRepository.allFavoritePlaces
    private val actualPlaces: MutableLiveData<List<Place>> = MutableLiveData()

    private var lastSearchString: String? = null

    init {
        mediatorPlaces.addSource(allFavoritePlaces) {
            actualPlaces.value = it
            filterData()
        }
        mediatorPlaces.addSource(actualPlaces) {
            setupEmptyState()
            mediatorPlaces.value = it.sortedByDescending { i -> i.created }
        }
    }

    fun updateFavoritePlaces() = viewModelScope.launch(Dispatchers.IO + defaultExceptionHandler {
        postViewEvent(NoInternetConnection)
        updateViewState(postValue = true) { copy(isLoading = false) }
    }) {
        coroutineScope {
            updateViewState(postValue = true) { copy(isLoading = true) }
            favoritesInteractor.syncFavoritePlaces(softSync = false, reloadUserData = true)
            updateViewState(postValue = true) { copy(isLoading = false) }
        }
    }

    fun onFavoriteStateChanged(place: Place) = viewModelScope.launch(Dispatchers.IO) {
        favoritesInteractor.updatePlaceFavoriteState(place)
    }

    fun returnPlaceToFavorites(place: Place) = viewModelScope.launch(Dispatchers.IO) {
        favoritesInteractor.updatePlaceFavoriteState(place, newState = true)
    }

    fun filterDataBySearchString(searchString: String?) {
        lastSearchString = searchString
        filterItemsBySearchString(lastSearchString)
    }

    fun resetSearchFilter() {
        if (!lastSearchString.isNullOrEmpty()) {
            filterDataBySearchString(null)
        }
    }

    private fun filterData() {
        if (!lastSearchString.isNullOrEmpty()) {
            filterDataBySearchString(lastSearchString)
        }
    }

    private fun filterItemsBySearchString(lastSearchString: String?) {
        allFavoritePlaces.value?.let {
            actualPlaces.value = if (lastSearchString.isNullOrEmpty()) {
                it
            } else {
                it.filter { place -> place.title.lowercase(Locale.ROOT).contains(lastSearchString) }
            }
        }
    }

    private fun setupEmptyState() {
        when {
            allFavoritePlaces.value.isNullOrEmpty() -> {
                updateViewState {
                    copy(emptyState = EmptyViewState.EMPTY_PLACES)
                }
            }
            actualPlaces.value.isNullOrEmpty() -> {
                updateViewState {
                    copy(emptyState = EmptyViewState.EMPTY_SEARCH_RESULTS)
                }
            }
            else -> {
                updateViewState {
                    copy(emptyState = EmptyViewState.NOT_EMPTY)
                }
            }
        }
    }
}