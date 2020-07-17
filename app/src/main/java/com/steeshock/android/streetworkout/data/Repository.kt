package com.steeshock.android.streetworkout.data

import androidx.lifecycle.LiveData
import com.steeshock.android.streetworkout.data.api.PlacesAPI
import com.steeshock.android.streetworkout.data.database.PlacesDao
import com.steeshock.android.streetworkout.data.model.PlaceObject

class Repository(
    private val placesDao: PlacesDao,
    private val placesAPI: PlacesAPI
) {

    val allPlaces: LiveData<List<PlaceObject>> = placesDao.getPlacesLive()
    val allFavoritePlaces: LiveData<List<PlaceObject>> = placesDao.getFavoritePlacesLive()

    fun insertPlace(place: PlaceObject) {
        placesDao.insertPlace(place)
    }

    suspend fun updatePlaces() {
        val response = placesAPI.getPlaces().body()
        response?.let {
            placesDao.updatePlaces(it)
        }
    }

    fun clearPlacesTable() {
        placesDao.clearPlacesTable()
    }

    fun removeAllPlacesExceptFavorites(boolean: Boolean) {
        placesDao.removeAllPlacesExceptFavorites(boolean)
    }

    companion object {

        @Volatile
        private var instance: Repository? = null

        fun getInstance(placesDao: PlacesDao, placesAPI: PlacesAPI) =
            instance ?: synchronized(this) {
                instance ?: Repository(placesDao, placesAPI).also { instance = it }
            }
    }
}