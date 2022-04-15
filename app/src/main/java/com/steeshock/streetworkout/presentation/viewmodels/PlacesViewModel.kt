package com.steeshock.streetworkout.presentation.viewmodels

import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.lifecycle.*
import com.steeshock.streetworkout.data.api.APIResponse
import com.steeshock.streetworkout.data.model.Category
import com.steeshock.streetworkout.data.model.Place
import com.steeshock.streetworkout.data.repository.implementation.DataStoreRepository.PreferencesKeys.NIGHT_MODE_PREFERENCES_KEY
import com.steeshock.streetworkout.data.repository.interfaces.ICategoriesRepository
import com.steeshock.streetworkout.data.repository.interfaces.IDataStoreRepository
import com.steeshock.streetworkout.data.repository.interfaces.IPlacesRepository
import com.steeshock.streetworkout.presentation.delegates.ViewEventDelegate
import com.steeshock.streetworkout.presentation.delegates.ViewEventDelegateImpl
import com.steeshock.streetworkout.presentation.viewStates.EmptyViewState.*
import com.steeshock.streetworkout.presentation.viewStates.places.PlacesViewEvent
import com.steeshock.streetworkout.presentation.viewStates.places.PlacesViewEvent.ShowAddPlaceFragment
import com.steeshock.streetworkout.presentation.viewStates.places.PlacesViewEvent.ShowAuthenticationAlert
import com.steeshock.streetworkout.presentation.viewStates.places.PlacesViewState
import com.steeshock.streetworkout.services.auth.IAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class PlacesViewModel @Inject constructor(
    private val placesRepository: IPlacesRepository,
    private val categoriesRepository: ICategoriesRepository,
    private val authService: IAuthService,
    private val dataStoreRepository: IDataStoreRepository,
) : ViewModel(),
    ViewEventDelegate<PlacesViewEvent> by ViewEventDelegateImpl() {

    private val mutableViewState: MutableLiveData<PlacesViewState> = MutableLiveData()
    val viewState: LiveData<PlacesViewState>
        get() = mutableViewState

    val observablePlaces = MediatorLiveData<List<Place>>()
    val observableCategories = categoriesRepository.allCategories

    private val allPlaces = placesRepository.allPlaces
    private val filteredPlaces: MutableLiveData<List<Place>> = MutableLiveData()
    private val actualPlaces: MutableLiveData<List<Place>> = MutableLiveData()

    private var lastSearchString: String? = null

    init {
        observablePlaces.addSource(allPlaces) {
            filteredPlaces.value = it
            filterData(filterList)
        }
        observablePlaces.addSource(observableCategories) {
            filterList = it.filter { category -> category.isSelected == true }.toMutableList()
            filterData(filterList)
        }
        observablePlaces.addSource(actualPlaces) {
            setupEmptyState()
            observablePlaces.value = it.sortedByDescending { i -> i.created }
        }
        setupAppTheme()
    }

    private fun setupAppTheme() = viewModelScope.launch(Dispatchers.IO) {
        dataStoreRepository.getInt(NIGHT_MODE_PREFERENCES_KEY)?.let {
            withContext(Dispatchers.Main) {
                if (it != MODE_NIGHT_FOLLOW_SYSTEM) {
                    setDefaultNightMode(it)
                }
            }
        }
    }

    private fun setupEmptyState() {
        when {
            allPlaces.value.isNullOrEmpty() -> {
                mutableViewState.updateState {
                    copy(emptyState = EMPTY_PLACES)
                }
            }
            actualPlaces.value.isNullOrEmpty() -> {
                mutableViewState.updateState {
                    copy(emptyState = EMPTY_SEARCH_RESULTS)
                }
            }
            else -> {
                mutableViewState.updateState {
                    copy(emptyState = NOT_EMPTY)
                }
            }
        }
    }

    private var filterList: MutableList<Category> = mutableListOf()

    fun fetchPlaces() {
        mutableViewState.updateState { copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            placesRepository.fetchPlaces(object :
                APIResponse<List<Place>> {
                override fun onSuccess(result: List<Place>?) {
                    mutableViewState.updateState(postValue = true) {
                        copy(isLoading = false)
                    }
                    result?.let { insertPlaces(it) }
                }

                override fun onError(t: Throwable) {
                    handleError(t)
                    t.printStackTrace()
                }
            })
        }
    }

    fun fetchCategories() {
        mutableViewState.updateState { copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            categoriesRepository.fetchCategories(object :
                APIResponse<List<Category>> {
                override fun onSuccess(result: List<Category>?) {
                    mutableViewState.updateState(postValue = true) {
                        copy(isLoading = false)
                    }
                    result?.let { insertCategories(it) }
                }

                override fun onError(t: Throwable) {
                    handleError(t)
                    t.printStackTrace()
                }
            })
        }
    }

    fun insertPlaces(places: List<Place>) = viewModelScope.launch(Dispatchers.IO) {
        placesRepository.insertAllPlaces(places)
    }

    fun insertCategories(categories: List<Category>) = viewModelScope.launch(Dispatchers.IO) {
        categoriesRepository.insertAllCategories(categories)
    }

    private fun updateCategory(category: Category) = viewModelScope.launch(Dispatchers.IO) {
        categoriesRepository.updateCategory(category)
    }

    private fun updatePlace(place: Place) = viewModelScope.launch(Dispatchers.IO) {
        placesRepository.updatePlace(place)
    }

    fun clearDatabase() = viewModelScope.launch(Dispatchers.IO) {
        placesRepository.clearPlacesTable()
        categoriesRepository.clearCategoriesTable()
    }

    fun onLikeClicked(place: Place) {
        updatePlace(place.copy(isFavorite = !place.isFavorite))
    }

    fun onFilterByCategory(category: Category) {
        category.changeSelectedState()
        if (filterList.find { it.category_name == category.category_name } != null) {
            filterList.remove(category)
        } else {
            filterList.add(category)
        }
        filterData(filterList)
        updateCategory(category)
    }

    fun onAddNewPlaceClicked() = viewModelScope.launch(Dispatchers.IO) {
        if (authService.isUserAuthorized()) {
            postViewEvent(ShowAddPlaceFragment)
        } else {
            postViewEvent(ShowAuthenticationAlert)
        }
    }

    private fun filterData(filterList: MutableList<Category>) {
        allPlaces.value?.let {
            actualPlaces.value = if (filterList.isEmpty()) {
                it
            } else {
                it.filter { place -> place.categories?.containsAll(filterList.map { i -> i.category_id }) == true }
            }

            filteredPlaces.value = actualPlaces.value
        }

        if (!lastSearchString.isNullOrEmpty()) {
            filterDataBySearchString(lastSearchString)
        }
    }

    fun resetSearchFilter() {
        if (!lastSearchString.isNullOrEmpty()) {
            filterDataBySearchString(null)
        }
    }

    fun filterDataBySearchString(searchString: String?) {
        lastSearchString = searchString
        filterItemsBySearchString(lastSearchString)
    }

    private fun filterItemsBySearchString(lastSearchString: String?) {
        actualPlaces.value = if (lastSearchString.isNullOrEmpty())
            filteredPlaces.value
        else {
            filteredPlaces.value?.filter {
                it.title.lowercase(Locale.ROOT).contains(lastSearchString.lowercase())
            }
        }
    }

    // TODO Handle errors on UI
    private fun handleError(throwable: Throwable) {
        mutableViewState.updateState(postValue = true) {
            copy(isLoading = false)
        }
    }

    private fun MutableLiveData<PlacesViewState>.updateState(
        postValue: Boolean = false,
        block: PlacesViewState.() -> PlacesViewState,
    ) {
        val currentState = value ?: PlacesViewState()
        val newState = currentState.run { block() }

        if (postValue) {
            postValue(newState)
        } else {
            value = newState
        }
    }
}
