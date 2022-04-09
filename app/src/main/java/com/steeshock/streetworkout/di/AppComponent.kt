package com.steeshock.streetworkout.di

import com.steeshock.streetworkout.di.common.ViewModelModule
import com.steeshock.streetworkout.di.components.FavoritePlacesComponent
import com.steeshock.streetworkout.di.components.MapComponent
import com.steeshock.streetworkout.di.components.PlacesComponent
import com.steeshock.streetworkout.di.components.ProfileComponent
import com.steeshock.streetworkout.di.modules.DatabaseModule
import com.steeshock.streetworkout.di.modules.NetworkModule
import dagger.Component
import javax.inject.Singleton

@Component(modules = [
    DatabaseModule::class,
    NetworkModule::class,
    ViewModelModule::class,
])
@Singleton
interface AppComponent {

    fun providePlacesComponent(): PlacesComponent

    fun provideMapComponent(): MapComponent

    fun provideFavoritePlacesComponent(): FavoritePlacesComponent

    fun provideProfileComponent(): ProfileComponent
}