package com.steeshock.android.streetworkout.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.steeshock.android.streetworkout.data.model.Category
import com.steeshock.android.streetworkout.data.model.Place


@Dao
interface PlacesDao {

    @Transaction
    fun clearDatabase() {
        clearPlacesTable()
        clearCategoriesTable()
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlace(place: Place)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllPlaces(places: List<Place>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllCategories(category: List<Category>)

    @Query("SELECT * FROM ${Place.TABLE_NAME}")
    fun getPlacesLive(): LiveData<List<Place>>

    @Query("SELECT * FROM ${Place.TABLE_NAME} WHERE isFavorite")
    fun getFavoritePlacesLive(): LiveData<List<Place>>

    @Query("SELECT * FROM ${Category.TABLE_NAME}")
    fun getCategoriesLive(): LiveData<List<Category>>

    @Query("DELETE FROM ${Place.TABLE_NAME}")
    fun clearPlacesTable()

    @Query("DELETE FROM ${Category.TABLE_NAME}")
    fun clearCategoriesTable()

    @Query("DELETE FROM ${Place.TABLE_NAME} WHERE isFavorite = :boolean")
    fun removeAllPlacesExceptFavorites(boolean: Boolean)

    @Query("UPDATE ${Place.TABLE_NAME} SET title = :title, address = :address, description = :description, latitude = :latitude, longitude = :longitude WHERE place_id = :place_id")
    fun updatePlacePartly(title: String?, address: String?, description: String?, latitude: Double?, longitude: Double?,  place_id: Int?)

    @Query("UPDATE ${Category.TABLE_NAME} SET category_name = :category_name WHERE category_id = :category_id")
    fun updateCategoryPartly(category_name: String?, category_id: Int?)

    @Update
    fun updateCategory(category: Category)

    @Update
    fun updatePlace(place: Place)

}