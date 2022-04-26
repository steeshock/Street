package com.steeshock.streetworkout.data.api

import com.steeshock.streetworkout.data.model.Category
import com.steeshock.streetworkout.data.model.Place
import retrofit2.Response
import retrofit2.http.GET

interface PlacesAPI {

    @GET("steeshock/SimpleAPI/places/")
    suspend fun getPlaces(): Response<List<Place>>

    @GET("steeshock/SimpleAPI/categories/")
    suspend fun getCategories(): Response<List<Category>>
}